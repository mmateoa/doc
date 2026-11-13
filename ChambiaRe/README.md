# CHAMB-IA — Rediseño UX/UI

Proyecto Android nativo Java actualizado sobre la copia recibida, con XML, Material, RecyclerView y ViewBinding.

- Abre este directorio (`ChambiaRe`) en Android Studio. JDK 17, SDK 35, Gradle wrapper 8.13 y AGP 8.9.2.
- Compila: `gradlew.bat assembleDebug testDebugUnitTest` (Windows) o `./gradlew assembleDebug testDebugUnitTest` (Linux/macOS).
- Con emulador o dispositivo: `gradlew.bat connectedDebugAndroidTest`.
- Reporte vigente: [docs/UX_IMPLEMENTATION_REPORT.md](docs/UX_IMPLEMENTATION_REPORT.md).
- Archivos modificados: [docs/UX_CHANGED_FILES.md](docs/UX_CHANGED_FILES.md).
- Especificaciones originales conservadas en la raíz.

**Conservar datos:** usa tu firma habitual para actualizar una instalación existente. No desinstales la app para resolver una incompatibilidad de firma. Room migra de forma explícita hasta la versión 9; no hay migración destructiva automática. Los APK de revisión de este entorno usan otra clave debug.

Los catálogos y diseños gratuitos siguen funcionando localmente. USB y TTS dependen del dispositivo y se documentan con sus límites, sin éxitos simulados. No se agregaron compras, anuncios o backend.

---

