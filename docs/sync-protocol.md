# Glance sync contract

Glance is now ready to consume live Project Superhuman data. The phone app only needs to expose the endpoint below on the local network.

## Transport

- HTTP on the local LAN is supported for v0.2.
- Glance polls every 5 seconds while connected.
- Failed requests back off automatically up to 30 seconds.
- The configured host and token are stored only in Android SharedPreferences on the tablet.

Example configured host:

```
http://192.168.1.25:8765
```

## Endpoint

```
GET /api/v1/glance
Authorization: Bearer <pairing-token>
Accept: application/json
X-Superhuman-Client: glance-android/0.2
```

The phone should return:

- `200` with the current snapshot.
- `401` or `403` when the pairing token is invalid.
- Other status codes are treated as temporary unavailability.

## Response

The response may be either the snapshot object itself or:

```json
{
  "snapshot": {
    "...": "..."
  }
}
```

Recommended complete snapshot:

```json
{
  "score": 84,
  "restingHR": "54",
  "steps": "6,842",
  "weight": "78.8 kg",
  "bodyWeight": "78.8",
  "bmi": "22.8",

  "sleepDuration": "7h 48m",
  "sleepSummary": "Sleep score 87 · efficiency 92%",

  "training": "Zone 2 Run",
  "trainingSummary": "3 × 700 m · controlled pace",

  "calories": "1,240",
  "calorieTarget": "1,900",
  "protein": "142 g",
  "carbs": "96 g",
  "fat": "43 g",

  "water": "2.4 L",
  "waterPct": 68,

  "clinical": "No active clinical flags",
  "focusTitle": "Today’s focus · easy aerobic work",
  "focusText": "Recovery is solid. Keep the run controlled and prioritise a calm evening."
}
```

All fields are optional. Glance only replaces values that are present, so the phone can ship the endpoint incrementally.

## Pairing

For the first phone-side implementation, the phone app only needs to show:

1. Its LAN address and port, for example `192.168.1.25:8765`.
2. A generated pairing token.

The user taps the sync pill in Glance, enters those two values, and Glance starts polling immediately.

A QR-code pairing flow can later encode the same host + token without changing the transport contract.

## Security expectations

The main app should:

- Generate a high-entropy random pairing token.
- Require the bearer token on every Glance request.
- Bind only to the local network interface, never expose this endpoint to the public internet.
- Rotate/revoke the token from Project Superhuman settings.
- Return only the compact Glance snapshot, not the full local database.

## Glance implementation

Native Android bridge:

`app/src/main/java/com/projectsuperhuman/glance/SyncBridge.java`

UI callback:

```js
window.ProjectSuperhumanSync.onSnapshot(jsonString)
```

Dashboard renderer:

```js
window.SuperhumanGlance.update(snapshot)
```

No additional Glance-side networking work should be required for the first phone integration.
