package com.example.bluetoothintercom

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

@SuppressLint("MissingPermission")
class BluetoothService(
    private val context: Context,
    private val device: BluetoothDevice,
    private val callback: ConnectionCallback
) {
    interface ConnectionCallback {
        fun onConnected()
        fun onConnectionFailed()
        fun onDisconnected()
        fun onIncomingCall()
        fun onCallAccepted()
        fun onCallRejected()
        fun onCallEnded()
    }

    companion object {
        private const val TAG = "BluetoothService"
        
        // UUID SPP (Serial Port Profile) estándar para Bluetooth
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        // Configuración de audio
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE = 2048
        
        // Comandos para llamadas
        private const val CMD_CALL_REQUEST = "CALL_REQ"
        private const val CMD_CALL_ACCEPT = "CALL_ACC"
        private const val CMD_CALL_REJECT = "CALL_REJ"
        private const val CMD_CALL_END = "CALL_END"
    }

    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var audioInputThread: AudioInputThread? = null
    private var audioOutputThread: AudioOutputThread? = null
    
    private var isRunning = false
    private var isInCall = false
    private var isInitiator = false

    fun connect() {
        connectThread = ConnectThread().apply { start() }
    }

    fun stop() {
        isRunning = false
        
        if (isInCall) {
            endCall()
        }
        
        connectThread?.cancel()
        connectThread = null
        
        connectedThread?.cancel()
        connectedThread = null
        
        audioInputThread?.cancel()
        audioInputThread = null
        
        audioOutputThread?.cancel()
        audioOutputThread = null
    }

    fun initiateCall() {
        connectedThread?.sendCommand(CMD_CALL_REQUEST)
        isInitiator = true
    }

    fun acceptCall() {
        if (!isInitiator) {
            connectedThread?.sendCommand(CMD_CALL_ACCEPT)
            startCall()
        }
    }

    fun rejectCall() {
        if (!isInitiator) {
            connectedThread?.sendCommand(CMD_CALL_REJECT)
            callback.onCallRejected()
        }
    }

    fun endCall() {
        connectedThread?.sendCommand(CMD_CALL_END)
        stopCall()
    }

    private fun startCall() {
        isInCall = true
        audioInputThread?.startRecording()
        audioOutputThread?.startPlayback()
        callback.onCallAccepted()
    }

    private fun stopCall() {
        isInCall = false
        audioInputThread?.stopRecording()
        audioOutputThread?.stopPlayback()
        callback.onCallEnded()
    }

    private inner class ConnectThread : Thread() {
        private var socket: BluetoothSocket? = null

        init {
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "Error al crear socket", e)
                callback.onConnectionFailed()
            }
        }

        override fun run() {
            try {
                socket?.connect()
                connected(socket)
            } catch (e: IOException) {
                Log.e(TAG, "Error en la conexión", e)
                try {
                    socket?.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "Error al cerrar socket", e2)
                }
                callback.onConnectionFailed()
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error al cerrar socket", e)
            }
        }
    }

    private fun connected(socket: BluetoothSocket?) {
        if (socket == null) {
            callback.onConnectionFailed()
            return
        }

        connectThread?.cancel()
        connectThread = null

        isRunning = true
        
        connectedThread = ConnectedThread(socket).apply { start() }
        audioInputThread = AudioInputThread().apply { start() }
        audioOutputThread = AudioOutputThread().apply { start() }
        
        callback.onConnected()
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        private val buffer = ByteArray(1024)

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "Error al crear streams", e)
            }

            inputStream = tmpIn
            outputStream = tmpOut
        }

        override fun run() {
            while (isRunning) {
                try {
                    // Leer comandos del otro dispositivo
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val command = String(buffer, 0, bytes)
                        handleCommand(command)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error en la conexión", e)
                    callback.onDisconnected()
                    break
                }
            }
        }

        private fun handleCommand(command: String) {
            when {
                command.startsWith(CMD_CALL_REQUEST) -> {
                    callback.onIncomingCall()
                }
                command.startsWith(CMD_CALL_ACCEPT) -> {
                    if (isInitiator) {
                        startCall()
                    }
                }
                command.startsWith(CMD_CALL_REJECT) -> {
                    if (isInitiator) {
                        callback.onCallRejected()
                    }
                }
                command.startsWith(CMD_CALL_END) -> {
                    stopCall()
                }
            }
        }

        fun sendCommand(command: String) {
            try {
                outputStream?.write(command.toByteArray())
            } catch (e: IOException) {
                Log.e(TAG, "Error al enviar comando", e)
            }
        }

        fun write(buffer: ByteArray) {
            try {
                outputStream?.write(buffer)
            } catch (e: IOException) {
                Log.e(TAG, "Error al escribir datos", e)
            }
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error al cerrar socket", e)
            }
        }
    }

    private inner class AudioInputThread : Thread() {
        private var audioRecord: AudioRecord? = null
        private var isRecording = false

        override fun run() {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                )

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    minBufferSize
                )

                val buffer = ByteArray(BUFFER_SIZE)

                while (isRunning) {
                    if (isRecording) {
                        val readSize = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                        if (readSize > 0) {
                            connectedThread?.write(buffer.copyOfRange(0, readSize))
                        }
                    } else {
                        sleep(50)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en AudioInputThread", e)
            } finally {
                audioRecord?.stop()
                audioRecord?.release()
            }
        }

        fun startRecording() {
            isRecording = true
            audioRecord?.startRecording()
        }

        fun stopRecording() {
            isRecording = false
            audioRecord?.stop()
        }

        fun cancel() {
            isRecording = false
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error al liberar AudioRecord", e)
            }
        }
    }

    private inner class AudioOutputThread : Thread() {
        private var audioTrack: AudioTrack? = null
        private var isPlaying = false

        override fun run() {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AUDIO_FORMAT
                )

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AUDIO_FORMAT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .build()

                val buffer = ByteArray(BUFFER_SIZE)
                val inputStream = socket.inputStream

                while (isRunning) {
                    if (isPlaying) {
                        try {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead > 0) {
                                audioTrack?.write(buffer, 0, bytesRead)
                            }
                        } catch (e: IOException) {
                            Log.e(TAG, "Error leyendo del socket", e)
                            break
                        }
                    } else {
                        sleep(50)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en AudioOutputThread", e)
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error liberando AudioTrack", e)
                }
            }
        }

        fun startPlayback() {
            isPlaying = true
            audioTrack?.play()
        }

        fun stopPlayback() {
            isPlaying = false
            audioTrack?.pause()
        }

        fun cancel() {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error liberando AudioTrack", e)
            }
        }
    }
} 