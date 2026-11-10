# Dragón Zeta — Android

Aplicación Android nativa en Kotlin inspirada en la imagen compartida.

## Funciones
- **SOS:** guarda un contacto en el teléfono y permite abrir el marcador o preparar un SMS con un mensaje SOS. La persona usuaria confirma la llamada o el envío.
- **Anti-chuponeo:** accesos a los ajustes de privacidad y permisos de la aplicación para revisar micrófono/cámara.
- **Anti-rastreo:** acceso a los ajustes de ubicación y al panel de privacidad para revisar permisos.

## Instalar / generar APK
1. Instala Android Studio (versión reciente) en una computadora.
2. Abre esta carpeta como proyecto (`File > Open`).
3. Espera a que Gradle sincronice y acepta instalar el Android SDK 35 si lo solicita.
4. Conecta un teléfono Android o crea un emulador.
5. Para probar: `Run > Run 'app'`.
6. Para generar un APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
7. El APK de depuración se crea en `app/build/outputs/apk/debug/app-debug.apk`. Para distribuirlo, genera un APK firmado desde `Build > Generate Signed Bundle / APK`.

## Límites de seguridad
Una app Android normal no puede garantizar detectar todo el spyware, escuchar el entorno, bloquear toda forma de rastreo ni apagar micrófono/cámara de otras aplicaciones por su cuenta. Esta app no realiza vigilancia ni envía datos a servidores. SOS requiere confirmación del usuario en el marcador o app SMS. No se solicitan permisos sensibles ni se recopila ubicación.

## Requisitos
- Android Studio
- JDK 17
- Android SDK 35
- Android 8.0 (API 26) o superior


## Generar APK automáticamente en GitHub
Este proyecto incluye `.github/workflows/android.yml`. Sube el contenido del ZIP a un repositorio GitHub y abre **Actions > Build Android APK > Run workflow**. Al terminar, descarga el artefacto `DragonZeta-debug-apk`; contiene `app-debug.apk`. También se ejecuta automáticamente al hacer push a `main` o `master`.
