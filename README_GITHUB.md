# Bluetooth Intercom - GitHub

Este repositorio contiene una aplicación Android que permite la comunicación por voz entre dos dispositivos a través de Bluetooth, utilizando los micrófonos de los audífonos conectados por Bluetooth.

## Descargar APK

La forma más sencilla de instalar la aplicación es descargar el APK generado automáticamente:

1. Ve a la pestaña "Actions" en la parte superior del repositorio
2. Haz clic en el flujo de trabajo "Build Android APK" más reciente
3. Desplázate hacia abajo hasta la sección "Artifacts"
4. Descarga "bluetooth-intercom-debug" o "bluetooth-intercom-apk"

## Instalar el APK

1. Transfiere el APK a tu dispositivo Android
2. En tu dispositivo, abre el archivo APK
3. Si aparece un mensaje de advertencia sobre "fuentes desconocidas", deberás permitir la instalación desde fuentes desconocidas:
   - Ve a Configuración > Seguridad > Instalar aplicaciones desconocidas
   - Activa la opción para la aplicación que estás utilizando para instalar el APK

## Compilación manual

Si prefieres compilar la aplicación manualmente:

### Requisitos

- Java JDK 11 o superior
- Android SDK

### Pasos para compilar

1. Clona el repositorio:
   ```
   git clone https://github.com/[tu-usuario]/BluetoothIntercom.git
   ```

2. Navega al directorio del proyecto:
   ```
   cd BluetoothIntercom
   ```

3. Compila el proyecto:
   ```
   ./gradlew assembleDebug
   ```

4. El APK generado estará en:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

## Ejecutar manualmente el flujo de trabajo de GitHub Actions

Si quieres generar un nuevo APK sin hacer cambios al código:

1. Ve a la pestaña "Actions" en GitHub
2. Selecciona el flujo de trabajo "Build Android APK"
3. Haz clic en "Run workflow" en el lado derecho
4. Selecciona la rama principal (main/master) y haz clic en "Run workflow"
5. Espera a que el flujo de trabajo termine y descarga el APK como se mencionó anteriormente 