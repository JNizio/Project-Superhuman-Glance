package com.projectsuperhuman.glance;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SyncBridge {
    private static final String PREFS = "superhuman_glance_sync";
    private static final String KEY_HOST = "host";
    private static final String KEY_TOKEN = "token";
    private static final long POLL_MS = 5000L;
    private static final long MAX_RETRY_MS = 30000L;

    private final Activity activity;
    private final WebView webView;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private boolean running = false;
    private boolean requestInFlight = false;
    private long retryMs = POLL_MS;

    public SyncBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @JavascriptInterface
    public String getConfig() {
        try {
            JSONObject j = new JSONObject();
            j.put("host", prefs.getString(KEY_HOST, ""));
            j.put("paired", !prefs.getString(KEY_TOKEN, "").isEmpty());
            return j.toString();
        } catch (Exception e) {
            return "{\"host\":\"\",\"paired\":false}";
        }
    }

    @JavascriptInterface
    public void configure(String host, String token) {
        final String cleanHost = normalizeHost(host);
        final String cleanToken = token == null ? "" : token.trim();
        prefs.edit().putString(KEY_HOST, cleanHost).putString(KEY_TOKEN, cleanToken).apply();
        emitStatus("configured", "Connection saved", false);
        start();
        requestNow();
    }

    @JavascriptInterface
    public void clearConfiguration() {
        stop();
        prefs.edit().clear().apply();
        emitStatus("unpaired", "Tap to connect to your phone", false);
    }

    @JavascriptInterface
    public void start() {
        main.post(() -> {
            if (running) return;
            running = true;
            retryMs = POLL_MS;
            schedule(150L);
        });
    }

    @JavascriptInterface
    public void stop() {
        main.post(() -> {
            running = false;
            main.removeCallbacks(pollRunnable);
        });
    }

    @JavascriptInterface
    public void requestNow() {
        main.post(() -> {
            if (!running) running = true;
            main.removeCallbacks(pollRunnable);
            schedule(0L);
        });
    }

    public void destroy() {
        running = false;
        main.removeCallbacksAndMessages(null);
        io.shutdownNow();
    }

    private final Runnable pollRunnable = this::poll;

    private void schedule(long delayMs) {
        if (!running) return;
        main.removeCallbacks(pollRunnable);
        main.postDelayed(pollRunnable, delayMs);
    }

    private void poll() {
        if (!running || requestInFlight) return;

        final String host = prefs.getString(KEY_HOST, "");
        final String token = prefs.getString(KEY_TOKEN, "");
        if (host.isEmpty() || token.isEmpty()) {
            emitStatus("unpaired", "Tap to connect to your phone", false);
            schedule(MAX_RETRY_MS);
            return;
        }

        requestInFlight = true;
        emitStatus("connecting", "Connecting to Project Superhuman…", false);

        io.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(host + "/api/v1/glance");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(2500);
                connection.setReadTimeout(3500);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("X-Superhuman-Client", "glance-android/0.2");

                int code = connection.getResponseCode();
                if (code == 200) {
                    String body = readAll(connection.getInputStream());
                    JSONObject root = new JSONObject(body);
                    JSONObject snapshot = root.optJSONObject("snapshot");
                    if (snapshot == null) snapshot = root;

                    emitSnapshot(snapshot.toString());
                    emitStatus("synced", "Synced just now", true);
                    retryMs = POLL_MS;
                    scheduleFromWorker(POLL_MS);
                } else if (code == 401 || code == 403) {
                    emitStatus("auth_error", "Pairing token rejected", false);
                    retryMs = MAX_RETRY_MS;
                    scheduleFromWorker(retryMs);
                } else {
                    emitStatus("offline", "Phone unavailable · retrying", false);
                    backoff();
                }
            } catch (Exception e) {
                emitStatus("offline", "Phone unavailable · retrying", false);
                backoff();
            } finally {
                if (connection != null) connection.disconnect();
                requestInFlight = false;
            }
        });
    }

    private void backoff() {
        retryMs = Math.min(MAX_RETRY_MS, Math.max(POLL_MS, retryMs * 2));
        scheduleFromWorker(retryMs);
    }

    private void scheduleFromWorker(long delayMs) {
        main.post(() -> schedule(delayMs));
    }

    private String normalizeHost(String host) {
        String h = host == null ? "" : host.trim();
        if (h.isEmpty()) return "";
        if (!h.startsWith("http://") && !h.startsWith("https://")) h = "http://" + h;
        while (h.endsWith("/")) h = h.substring(0, h.length() - 1);
        return h;
    }

    private String readAll(InputStream stream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) out.append(line);
        reader.close();
        return out.toString();
    }

    private void emitSnapshot(String json) {
        evaluate("window.ProjectSuperhumanSync&&ProjectSuperhumanSync.onSnapshot(" + JSONObject.quote(json) + ");");
    }

    private void emitStatus(String code, String message, boolean connected) {
        try {
            JSONObject j = new JSONObject();
            j.put("code", code);
            j.put("message", message);
            j.put("connected", connected);
            evaluate("window.ProjectSuperhumanSync&&ProjectSuperhumanSync.onStatus(" + JSONObject.quote(j.toString()) + ");");
        } catch (Exception ignored) {}
    }

    private void evaluate(String script) {
        activity.runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(script, null);
        });
    }
}
