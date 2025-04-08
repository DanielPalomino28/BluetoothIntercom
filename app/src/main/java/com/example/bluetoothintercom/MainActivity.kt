package com.example.bluetoothintercom

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothintercom.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var pairedDevicesAdapter: DeviceAdapter
    private lateinit var availableDevicesAdapter: DeviceAdapter
    private var bluetoothService: BluetoothService? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isInCall = false

    // Receiver para eventos de Bluetooth
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    
                    device?.let {
                        if (hasBluetoothPermission() && it.name != null) {
                            availableDevicesAdapter.addDevice(it)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    binding.scanButton.isEnabled = true
                }
            }
        }
    }

    // Launcher para solicitar activar Bluetooth
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            initBluetooth()
        } else {
            Toast.makeText(this, R.string.bluetooth_not_enabled, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Launcher para solicitar permisos
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            initBluetooth()
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar RecyclerViews
        binding.pairedDevicesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.availableDevicesRecyclerView.layoutManager = LinearLayoutManager(this)

        pairedDevicesAdapter = DeviceAdapter { device ->
            connectToDevice(device)
        }
        availableDevicesAdapter = DeviceAdapter { device ->
            connectToDevice(device)
        }

        binding.pairedDevicesRecyclerView.adapter = pairedDevicesAdapter
        binding.availableDevicesRecyclerView.adapter = availableDevicesAdapter

        // Configurar botones
        binding.scanButton.setOnClickListener {
            if (hasBluetoothPermission()) {
                startDiscovery()
            } else {
                requestBluetoothPermission()
            }
        }

        binding.callButton.setOnClickListener {
            initiateCall()
        }

        binding.endCallButton.setOnClickListener {
            endCall()
        }

        binding.acceptCallButton.setOnClickListener {
            acceptCall()
        }

        binding.rejectCallButton.setOnClickListener {
            rejectCall()
        }

        // Iniciar Bluetooth
        checkBluetoothSupport()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService?.stop()
        if (hasBluetoothPermission() && bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        unregisterReceiver(bluetoothReceiver)
    }

    private fun checkBluetoothSupport() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter ?: run {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableIntent)
        } else {
            requestBluetoothPermission()
        }
    }

    private fun requestBluetoothPermission() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun initBluetooth() {
        // Registrar receptor para eventos Bluetooth
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(bluetoothReceiver, filter)

        // Cargar dispositivos emparejados
        loadPairedDevices()
    }

    private fun loadPairedDevices() {
        if (!hasBluetoothPermission()) {
            requestBluetoothPermission()
            return
        }

        pairedDevicesAdapter.clearDevices()
        
        val pairedDevices = bluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            pairedDevicesAdapter.addDevice(device)
        }
    }

    private fun startDiscovery() {
        if (!hasBluetoothPermission()) {
            requestBluetoothPermission()
            return
        }

        availableDevicesAdapter.clearDevices()
        binding.scanButton.isEnabled = false

        // Cancelar descubrimiento anterior si está en progreso
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        // Iniciar nuevo descubrimiento
        bluetoothAdapter.startDiscovery()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (!hasBluetoothPermission()) {
            requestBluetoothPermission()
            return
        }

        // Cancelar descubrimiento para mejorar la conexión
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        binding.statusTextView.text = "Conectando a ${device.name}..."
        connectedDevice = device
        
        // Iniciar servicio Bluetooth
        bluetoothService = BluetoothService(this, device, object : BluetoothService.ConnectionCallback {
            override fun onConnected() {
                runOnUiThread {
                    binding.statusTextView.text = getString(R.string.connected_to, device.name)
                    binding.callButton.isEnabled = true
                    
                    // Mostrar UI de llamada normal
                    showCallUI()
                }
            }

            override fun onConnectionFailed() {
                runOnUiThread {
                    binding.statusTextView.text = getString(R.string.connection_failed)
                    binding.callButton.isEnabled = false
                    bluetoothService = null
                    connectedDevice = null
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    resetUI()
                    binding.statusTextView.text = "Estado: Desconectado"
                    binding.callButton.isEnabled = false
                    bluetoothService = null
                    connectedDevice = null
                    isInCall = false
                }
            }
            
            override fun onIncomingCall() {
                runOnUiThread {
                    // Mostrar UI de llamada entrante
                    showIncomingCallUI()
                    binding.statusTextView.text = getString(R.string.incoming_call, device.name)
                }
            }
            
            override fun onCallAccepted() {
                runOnUiThread {
                    isInCall = true
                    // Mostrar UI en llamada
                    showActiveCallUI()
                    binding.statusTextView.text = getString(R.string.on_call, device.name)
                }
            }
            
            override fun onCallRejected() {
                runOnUiThread {
                    // Volver a UI normal
                    showCallUI()
                    binding.statusTextView.text = getString(R.string.connected_to, device.name)
                    Toast.makeText(this@MainActivity, "Llamada rechazada", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onCallEnded() {
                runOnUiThread {
                    isInCall = false
                    // Volver a UI normal
                    showCallUI()
                    binding.statusTextView.text = getString(R.string.connected_to, device.name)
                    Toast.makeText(this@MainActivity, R.string.call_ended, Toast.LENGTH_SHORT).show()
                }
            }
        })
        
        bluetoothService?.connect()
    }

    private fun initiateCall() {
        bluetoothService?.let {
            it.initiateCall()
            binding.statusTextView.text = getString(R.string.calling)
            // Cambiar a UI de llamada en curso
            showActiveCallUI()
        }
    }
    
    private fun acceptCall() {
        bluetoothService?.let {
            it.acceptCall()
            // UI se actualiza en el callback onCallAccepted
        }
    }
    
    private fun rejectCall() {
        bluetoothService?.let {
            it.rejectCall()
            // Volver a UI normal
            showCallUI()
            binding.statusTextView.text = getString(R.string.connected_to, connectedDevice?.name)
        }
    }
    
    private fun endCall() {
        bluetoothService?.let {
            it.endCall()
            // UI se actualiza en el callback onCallEnded
        }
    }
    
    // Métodos para gestionar la interfaz de usuario
    
    private fun resetUI() {
        binding.callControlLayout.visibility = View.VISIBLE
        binding.incomingCallLayout.visibility = View.GONE
        binding.callButton.visibility = View.VISIBLE
        binding.endCallButton.visibility = View.GONE
        binding.callButton.isEnabled = false
    }
    
    private fun showCallUI() {
        binding.callControlLayout.visibility = View.VISIBLE
        binding.incomingCallLayout.visibility = View.GONE
        binding.callButton.visibility = View.VISIBLE
        binding.endCallButton.visibility = View.GONE
        binding.callButton.isEnabled = true
    }
    
    private fun showIncomingCallUI() {
        binding.callControlLayout.visibility = View.GONE
        binding.incomingCallLayout.visibility = View.VISIBLE
    }
    
    private fun showActiveCallUI() {
        binding.callControlLayout.visibility = View.VISIBLE
        binding.incomingCallLayout.visibility = View.GONE
        binding.callButton.visibility = View.GONE
        binding.endCallButton.visibility = View.VISIBLE
        binding.endCallButton.isEnabled = true
    }
} 