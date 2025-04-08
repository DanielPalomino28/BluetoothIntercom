package com.example.bluetoothintercom

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothintercom.databinding.DeviceItemBinding

class DeviceAdapter(private val onConnectClick: (BluetoothDevice) -> Unit) : 
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<BluetoothDevice>()

    class DeviceViewHolder(val binding: DeviceItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = DeviceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        
        holder.binding.deviceNameTextView.text = "${device.name ?: "Dispositivo desconocido"} (${device.address})"
        
        holder.binding.connectButton.setOnClickListener {
            onConnectClick(device)
        }
    }

    override fun getItemCount(): Int = devices.size
    
    @SuppressLint("NotifyDataSetChanged")
    fun clearDevices() {
        devices.clear()
        notifyDataSetChanged()
    }
    
    @SuppressLint("MissingPermission")
    fun addDevice(device: BluetoothDevice) {
        // Evitar duplicados
        if (devices.none { it.address == device.address }) {
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        }
    }
} 