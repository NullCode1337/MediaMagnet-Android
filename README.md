# MediaMagnet for Android

Native Kotlin client for a [MediaMagnet](../MediaMagnet) server. Material 3 Expressive UI, full REST + SSE API coverage, and downloads saved to the phone's shared Downloads collection by default.

## Build

Requires JDK 17+, Android SDK Platform 36, Build Tools 36. The Gradle wrapper downloads Gradle 8.13.

```sh
printf 'sdk.dir=/path/to/android-sdk\n' > local.properties
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

Open **Settings → Server connection**, enter the server base URL (`http://host:8080`; `/api/v1` is appended automatically if absent) and the optional `MM_TOKEN` bearer token. Both are persisted with DataStore.

The app uses cleartext HTTP so a LAN-hosted server works out of the box.

Download, backend and appearance settings are stored on the phone in DataStore, not written to the server. They are sent as per-job `overrides` with every `POST /downloads`, with the advanced per-job overrides in the New download form taking precedence.

## Download locations

| Android | Default destination | Configurable |
| --- | --- | --- |
| 29+ | MediaStore `Downloads/MediaMagnet/…` | Settings → Local storage → Choose folder (SAF, grant persisted) |
| 26–28 | `Environment.DIRECTORY_DOWNLOADS/MediaMagnet/…` (runtime permission requested at launch) | same |

Files stream from the server without buffering in memory. The server's relative subdirectory structure is preserved under the chosen root (`Downloads/MediaMagnet/<server path>` by default). Duplicate names get ` (n)` suffixes, and `WRITE_EXTERNAL_STORAGE` is declared only up to API 28.

Auto-download is always on: when a job finishes, every output file is streamed from `/downloads/{id}/file` into the selected destination. The server deletes each output once the response body has been delivered in full, so finished jobs do not accumulate on the server. The History tab keeps a persisted lifetime record of every saved file; swipe an entry left (or tap its X) to remove it from that history.

While work is active the app runs a `dataSync` foreground service with an ongoing notification, so the SSE stream and file saves keep running when the app is closed or backgrounded. The connection, job list and auto-save loop live in a process-wide engine rather than the activity. Partially written MediaStore/SAF entries are recorded before writing and deleted on the next launch if the process died mid-save.

## API coverage

| Endpoint | UI |
| --- | --- |
| `GET /health` | Settings status |
| `POST /downloads` | Downloads → New download (URL, forced backend, per-job backend settings, user agent, cookies) |
| `GET /downloads` | Jobs panel (merged with SSE) |
| `DELETE /downloads` | Cancel all |
| `GET /downloads/{id}` | Job refresh when a state event arrives |
| `DELETE /downloads/{id}` | Job → Cancel |
| `POST /downloads/{id}/pause` | Job → Pause |
| `POST /downloads/{id}/resume` | Job → Resume |
| `GET /downloads/{id}/file` | Automatic download on job completion |
| `GET /settings` | Not used — settings are stored on the phone |
| `PUT /settings` | Not used — settings are stored on the phone |
| `POST /settings/reset` | Not used — Reset restores local defaults |
| `GET /cookies` | Settings → Cookies list |
| `POST /cookies` | Settings → Cookies → Add cookie (Content or FilePath) |
| `DELETE /cookies` | Settings → Cookies → Delete all |
| `DELETE /cookies/{domain}` | Settings → Cookies → Per-domain delete |
| `GET /paths/download-root` | Settings → Server storage |
| `GET /paths/disk-usage` | Settings → Server storage (wavy indicator) |
| `GET /events` | Live job progress, status, errors, notifications; reconnects with backoff |
| `GET /tools` | Settings → Tools |
| `POST /tools/update` | Settings → Tools → Update tools (output shown in a dialog) |

## Notes

- Job progress, state and notification events come from the SSE stream; the REST list is used at connect time and on reconnect.
- Finished job outputs are deleted by the server after a complete download. On reconnect, outputs that already disappeared from the server are skipped quietly; `savedOutputs` guards against duplicate downloads within the app process.
- `material3` is pinned to `1.5.0-alpha18`: it is the last release with the Expressive component set (`ToggleButton`, `ShortNavigationBar`, wavy progress, `MotionScheme.expressive()`) that still builds against compileSdk 36 / AGP 8.13. `1.5.0-beta02` requires compileSdk 37 and AGP 9.1. `MaterialExpressiveTheme` itself is an internal AndroidX API in this version, so the app builds the same theme via `MaterialTheme(motionScheme = MotionScheme.expressive(), …)`.

## Layout

```
app/src/main/java/dev/nullcode/mediamagnet/
├── data/        ApiClient, API models, preferences, file saver
├── ui/          App shell, ViewModel, components
│   └── screens/ Downloads, History, Settings (+ Cookies, Tools, backend editors)
└── MainActivity.kt
```
