# LYGANE Smart TV – Rediseño 2.0

Proyecto Android (Jetpack Compose, minSdk 24) con el launcher rediseñado para Smart TV.

## Cómo compilar
1. Android Studio (Ladybug o superior) → File ▸ Open → esta carpeta.
2. Sync Gradle (descarga dependencias; Android Studio crea el wrapper).
3. Build ▸ Build APK(s) → `app/build/outputs/apk/debug/app-debug.apk`.
4. Instalar en la TV: `adb connect IP_TV` y `adb install -r app-debug.apk`.

## Qué incluye
- Tema: Void / Ember / Moon / Angel (cian de foco). Foco con escala 1.08 + aro cian.
- Hero que cambia según la Cartelera enfocada, mini reproductor, fila de apps reales
  (Leanback), ticker de noticias, reloj.
- `preview/lygane-tv-nuevo-launcher.html`: vista previa en navegador (usa las flechas).

## No incluido respecto a la v1 (el código original no estaba disponible)
- Clima en vivo (hoy texto fijo), noticias en vivo, MediaMonitorService / reproductor real,
  diálogo de ajustes rápidos, buscador, favoritos persistentes, tabs funcionales.
Se pueden portar si me compartes el código fuente original.
