# Project Superhuman Glance

A one-screen, no-scroll tablet companion for Project Superhuman.

## Design principle

Glance is intentionally not another full tracker. It is a passive, instant-read dashboard: open it, understand the current state in seconds, close it.

The visual language mirrors Project Superhuman: light clinical background, navy hierarchy, rounded cards, restrained shadows, and module-specific accent colours.

## Current v0.2

- Landscape tablet layout
- Absolutely no page scrolling
- Daily readiness / overall state card
- Sleep, Training, Nutrition, Hydration, Body, and Clinical summary cards
- Native local-LAN sync client with persistent host/token pairing
- Automatic 5-second polling with retry backoff and offline/auth states
- Tap the sync pill to configure the phone address and pairing token
- Demo values remain visible until the first live snapshot arrives
- Public renderer API: `window.SuperhumanGlance.update(snapshot)`
- Sync contract documented in `docs/sync-protocol.md`
- GitHub Actions APK build on every push to `main`

## Build

```bash
gradle assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Phone-side integration

Glance is already prepared for the phone connection. The main Project Superhuman app only needs to expose `GET /api/v1/glance` with bearer-token authentication and return the documented snapshot schema. See [docs/sync-protocol.md](docs/sync-protocol.md).
