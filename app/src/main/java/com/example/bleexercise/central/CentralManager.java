package com.example.bleexercise.central;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.bleexercise.util.BluetoothUtils;
import com.example.bleexercise.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CentralManager {

    private final String TAG = getClass().getSimpleName();

    private static CentralManager centralManager;

    private Context context;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private LocationManager locationManager;

    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private BluetoothGatt bluetoothGatt;

    private CentralCallback centralCallback;
    private Map<String, BluetoothDevice> scanResults;

    private Handler scanHandler;

    private BluetoothUtils bluetoothUtils;

    private boolean isConnected = false;
    private boolean isScanning = false;

    private final int SCAN_PERIOD = 10000;

    public CentralManager(Context context) {
        this.context = context;

        if (bluetoothUtils == null)
            bluetoothUtils = new BluetoothUtils();
    }

    public static CentralManager getInstance(Context context) {
        if (centralManager == null)
            centralManager = new CentralManager(context);

        return centralManager;
    }

    public void setCentralCallback(CentralCallback callback) {
        centralCallback = callback;
    }

    /* ???????????? ?????????, ???????????? ????????? ????????? */
    public void initBle(Context context) {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /* BLE ????????? ?????? */
    public boolean startScan(Context context) {

        /* ?????? ???????????? ?????? ?????? ????????? ?????? */
        if (isConnected)
            return false;

        /* ?????? ?????? ??????. android9 ????????? ACCESS_COARSE_LOCATION, ??? ?????? ????????? ACCESS_FINE_LOCATION ?????? ??? */
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            centralCallback.requestLocationPermission();
            centralCallback.onStatus("Requesting location permission");
            return false;
        }

        centralCallback.onStatus("Scanning...");

        /* ???????????? ?????? ???????????? ?????? */
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            centralCallback.requestEnableBLE();
            centralCallback.onStatus("Requesting enable bluetooth");
            return false;
        }

        /* GPS on ??????????????? ?????? */
            centralCallback.requestLocationOn();
            centralCallback.onStatus("Requesting enable location on");


        /* BLE Scanning ??? ?????? BluetoothLEScanner ?????? ?????? */
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        /* ?????? ????????? ????????? ????????? ????????? disconnect ?????? */

        /* ?????? ????????? ???????????? ????????? */
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(Constants.SERVICE_UUID)).build();
        filters.add(scanFilter);

        /* ????????? ?????? ????????? ????????? */
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        /* ?????? ?????? ?????? ?????? ?????? Callback ?????? */
        scanResults = new HashMap<>();
        scanCallback = new BLEScanCallback(scanResults);

        /* ????????? ????????????. ?????? ?????? */
        bluetoothLeScanner.startScan(filters, settings, scanCallback);
        isScanning = true;

        /* ?????? ?????? scanning ?????? Scanning stop */
        scanHandler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (isScanning)
                    stopScan();
            }
        };
        scanHandler.postDelayed(runnable, SCAN_PERIOD);

        return isConnected;
    }

    /* ?????? ?????? */
    private void stopScan() {
        if (isScanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
            scanComplete();
        }

        if (scanCallback != null)
            scanCallback = null;
        if (scanHandler != null)
            scanHandler = null;
        isScanning = false;

        centralCallback.onStatus("scanning stopped");
    }

    /* ????????? ???????????? ?????? */
    private void scanComplete() {
        /* ?????? ????????? ???????????? ?????? ??????*/
        if (scanResults.isEmpty()) {
            centralCallback.onStatus("scan results is empty");
            Log.d(TAG, "scan result is empty");
            return;
        }

        String selectedDevice = "";
        for (String deviceAddr : scanResults.keySet()) {
            Log.d(TAG, "Found device : " + deviceAddr);
            if (selectedDevice.equalsIgnoreCase(deviceAddr)) {
                Log.d(TAG, "selectedDevice : " + deviceAddr);
                BluetoothDevice device = scanResults.get(deviceAddr);
                connectDevice(device);
            }
        }
    }

    /* ???????????? ?????? */
    private void connectDevice(BluetoothDevice device) {
        centralCallback.onStatus("Connecting to " + device.getAddress());
        GattClientCallback gattClientCallback = new GattClientCallback();
        bluetoothGatt = device.connectGatt(context, false, gattClientCallback);
    }


    /* ???????????? ?????? ?????? */
    public void disconnectGattServer() {
        Log.d(TAG, "Closing Gatt connection");
        centralCallback.onStatus("Closing Gatt connection");

        isConnected = false;

        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }

    /* ????????? ??????. 20Byte ????????? ?????? ??? ?????? */
    public void sendData(String message) {
        if (!isConnected) {
            Log.d(TAG, "Failed to sendData due to no connection");
            return;
        }

        /* GATT ???????????? command ????????? ?????? */
        BluetoothGattCharacteristic characteristic = bluetoothUtils.findCharacteristic(bluetoothGatt, Constants.CHARACTERISTIC_UUID);

        if (characteristic == null) {
            Log.e(TAG, "Unable to find cmd characteristic");
            disconnectGattServer();
            return;
        }

        characteristic.setValue(message.getBytes()); // 20 byte limit
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        boolean success = bluetoothGatt.writeCharacteristic(characteristic);

        if (success) {
            centralCallback.onStatus("write : " + message);
            Log.d(TAG, "Success to write command");
        } else {
            Log.e(TAG, "Failed to write command : " + characteristic.getUuid());
            centralCallback.onStatus("Failed to write command");
            disconnectGattServer();
        }
    }

    private class BLEScanCallback extends ScanCallback {
        private Map<String, BluetoothDevice> scanResults;

        private BLEScanCallback(Map<String, BluetoothDevice> scanResult) {
            this.scanResults = scanResult;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult");
            //addScanResult(result);

            /* Connect with macAddress : 00:24:06:f7:4f:d9 */
            if(result.getDevice().getAddress().equalsIgnoreCase("00:24:06:f7:4f:d9"))
            {
                connectDevice(result.getDevice());
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "onBatchScanResults");
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "BLE scan failed with code : " + errorCode);
        }


        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();
            scanResults.put(deviceAddress, device);

            Log.e(TAG, "scanned device : " + device.getName() + ", " + device.getAddress());
            centralCallback.onStatus("scanned device : " + device.getName() + ": " + device.getAddress());
        }
    }

    /* Gatt client callback class */
    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
                if(status != BluetoothGatt.GATT_SUCCESS) {
                    disconnectGattServer();
                    return;
                }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                centralCallback.onStatus("Connected");
                isConnected = true;
                Log.d(TAG, "Connected to the GATT server");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Device service discovery failed, status:" + status);
                return;
            }

            List<BluetoothGattCharacteristic> matchingCharacteristics = bluetoothUtils.findBLECharacteristics(gatt);
            for (BluetoothGattCharacteristic characteristic : matchingCharacteristics) {
                Log.d(TAG, "characteristic: " + characteristic.getUuid());
            }

            if (matchingCharacteristics.isEmpty()) {
                Log.d(TAG, "Unable to find characteristics");
                return;
            }

            Log.d(TAG, " Service discovery is successful");

            /* ?????? ????????? ?????? onCharacteristicChanged callback ??? ?????? ??? ?????? */
            BluetoothGattCharacteristic characteristic = bluetoothUtils.findCharacteristic(bluetoothGatt, Constants.CHARACTERISTIC_UUID);
            gatt.setCharacteristicNotification(characteristic, true);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(Constants.DESCRIPTOR_STRING));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean success = gatt.writeDescriptor(descriptor);
            if (success)
                Log.e(TAG, "writeCharacteristic success");
            else
                Log.d(TAG, "writeCharacteristic fail");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "characteristic changed : " + characteristic.getUuid().toString());
            readCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Characteristic written successfully");
            } else {
                Log.e(TAG, "Characteristic write unsuccessful, status: " + status);
                disconnectGattServer();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read successfully");

                byte[] msg = characteristic.getValue();
                Log.d(TAG, "read:" + msg.toString());
                centralCallback.onStatus("read : " + msg.toString());
                centralCallback.onToast("read : " + msg);

            } else {
                Log.e(TAG, "Characteristic read unsuccessful, status: " + status);
            }
        }

        private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
            byte[] msg = characteristic.getValue();
            Log.d(TAG, "read:" + msg.toString());
            centralCallback.onStatus("read : " + msg.toString());
            centralCallback.onToast("read : " + msg);
        }
    }
}
