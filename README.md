# Bluetooth Intercom

Aplicación para Android que permite la comunicación por voz entre dos dispositivos Android a través de Bluetooth, utilizando los micrófonos de los audífonos conectados por Bluetooth.

## Características

- Búsqueda de dispositivos Bluetooth cercanos
- Visualización de dispositivos emparejados
- Conexión directa entre dispositivos
- Sistema de llamadas en tiempo real a través de Bluetooth
- Comunicación de audio en tiempo real utilizando los micrófonos de los audífonos Bluetooth
- Interfaz intuitiva similar a una llamada telefónica convencional

## Requisitos

- Android 6.0 (API 23) o superior
- Dispositivo con soporte Bluetooth
- Audífonos Bluetooth con micrófono

## Permisos

La aplicación requiere los siguientes permisos:

- Bluetooth (para la comunicación entre dispositivos)
- Ubicación (necesario para el escaneo Bluetooth en Android 6.0+)
- Micrófono (para grabación de audio)
- Modificar configuración de audio

## Uso

1. Asegúrate de tener los audífonos Bluetooth conectados a tu dispositivo Android
2. Abre la aplicación en ambos dispositivos
3. En uno de los dispositivos, presiona "Buscar dispositivos"
4. Selecciona el otro dispositivo de la lista y presiona "Conectar"
5. Una vez conectados, presiona "Llamar" para iniciar una llamada
6. En el otro dispositivo, acepta la llamada entrante
7. ¡Ahora puedes hablar libremente sin necesidad de mantener presionado ningún botón!
8. Para finalizar la llamada, presiona "Colgar" en cualquiera de los dispositivos

## Limitaciones

- Ambos dispositivos deben tener la aplicación instalada
- Los dispositivos deben estar dentro del rango de Bluetooth (aproximadamente 10 metros)
- El rendimiento puede variar dependiendo de la calidad de los audífonos Bluetooth

## Estructura del proyecto

- `MainActivity`: Actividad principal que maneja la interfaz de usuario y la lógica de llamadas
- `DeviceAdapter`: Adaptador para mostrar los dispositivos Bluetooth en los RecyclerViews
- `BluetoothService`: Servicio que maneja la conexión Bluetooth y la transmisión de audio bidireccional

## Tecnologías utilizadas

- Kotlin
- Android Bluetooth API
- Android Audio APIs (AudioRecord, AudioTrack)
- ViewBinding
- RecyclerView

## Instalación

### Descargar desde GitHub

La forma más sencilla de instalar la aplicación es descargar el APK generado automáticamente:

1. Ve a la pestaña "Actions" en la parte superior del repositorio
2. Haz clic en el flujo de trabajo "Build Android APK" más reciente
3. Desplázate hacia abajo hasta la sección "Artifacts"
4. Descarga "bluetooth-intercom-debug" o "bluetooth-intercom-apk"

### Instalar el APK

1. Transfiere el APK a tu dispositivo Android
2. En tu dispositivo, abre el archivo APK
3. Si aparece un mensaje de advertencia sobre "fuentes desconocidas", deberás permitir la instalación desde fuentes desconocidas:
   - Ve a Configuración > Seguridad > Instalar aplicaciones desconocidas
   - Activa la opción para la aplicación que estás utilizando para instalar el APK

### Compilación manual

Si prefieres compilar la aplicación manualmente:

#### Requisitos

- Java JDK 11 o superior
- Android SDK

#### Pasos para compilar

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

## Futuras mejoras

- Soporte para múltiples dispositivos (conferencia)
- Mejora en la calidad del audio
- Opción para grabar conversaciones
- Ajustes de volumen
- Notificaciones de llamadas perdidas
- Soporte para Bluetooth LE Audio (cuando esté más disponible)
- Optimización del consumo de batería

## Contribuciones

Las contribuciones son bienvenidas. Si deseas contribuir a este proyecto, por favor:

1. Realiza un fork del repositorio
2. Crea una rama para tu función (`git checkout -b feature/nueva-funcion`)
3. Realiza tus cambios
4. Haz commit de tus cambios (`git commit -m 'Añadir nueva función'`)
5. Haz push a la rama (`git push origin feature/nueva-funcion`)
6. Abre un Pull Request 