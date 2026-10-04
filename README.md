# Project Superhuman Glance

A one-screen, no-scroll tablet companion for Project Superhuman.

## Design principle

Glance is intentionally not another full tracker. It is a passive, instant-read dashboard: open it, understand the current state in seconds, close it.

The visual language mirrors Project Superhuman: light clinical background, navy hierarchy, rounded cards, restrained shadows, and module-specific accent colours.

## Current v0.1

- Landscape tablet layout
- Absolutely no page scrolling
- Daily readiness / overall state card
- Sleep, Training, Nutrition, Hydration, Body, and Clinical summary cards
- Demo data until phone sync is connected
- Public JS API: `window.SuperhumanGlance.update(snapshot)`
- GitHub Actions APK build on every push to `main`

## Build

```bash
gradle assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`
