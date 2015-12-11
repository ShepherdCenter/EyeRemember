package org.shepherd.recall;

import android.bluetooth.BluetoothDevice;

public class ScanData {
    public ScanData(BluetoothDevice device, int rssi, byte[] scanRecord) {
        this.device = device;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
    }

    @SuppressWarnings("unused")
    public BluetoothDevice device;
    public int rssi;
    public byte[] scanRecord;
}