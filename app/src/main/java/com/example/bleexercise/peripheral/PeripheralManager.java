package com.example.bleexercise.peripheral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.bleexercise.util.Constants;

public class PeripheralManager {

    private final String TAG = getClass().getSimpleName();
    private Context context;

    public static PeripheralManager peripheralManager;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothGattService bluetoothGattService;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothDevice bluetoothDevice;
    private BluetoothGattCharacteristic characteristic;

    private PeripheralCallback peripheralCallback;

    public static PeripheralManager getInstance() {
        if (peripheralManager == null)
            peripheralManager = new PeripheralManager();

        return peripheralManager;
    }

    public void setCallback(PeripheralCallback callback) {
        peripheralCallback = callback;
    }

    /* Initialise objects relevant to GATT server */
    public void initServer(Context context) {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        /* Check whether bluetooth is available or not */
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            peripheralCallback.requestEnableBLE();
            return;
        }

        bluetoothGattService = new BluetoothGattService(Constants.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        characteristic = new BluetoothGattCharacteristic(Constants.CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        characteristic.addDescriptor(new BluetoothGattDescriptor(Constants.DESCRIPTOR_UUID, (BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE)));
        characteristic.setValue(new byte[]{0, 0});
        bluetoothGattService.addCharacteristic(characteristic);

        startAdvertising();
        startServer();
    }

    public void startAdvertising() {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (bluetoothLeAdvertiser == null) {
            peripheralCallback.onStatusMsg("Failed to create advertiser");
            return;
        }

        /* Advertiser setting */
        AdvertiseSettings advSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build();

        /* Advertising 할 데이터 패킷 */
        AdvertiseData advData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(ParcelUuid.fromString(Constants.SERVICE_STRING))
                .build();

        /* 디바이스가 Scan 되었을때 보여질 데이터 패킷 */
        AdvertiseData advScanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        bluetoothLeAdvertiser.startAdvertising(advSettings, advData, advScanResponse, advCallback);
    }

    private void stopAdvertising()
    {
        if(bluetoothLeAdvertiser == null)
            return;

        bluetoothLeAdvertiser.stopAdvertising(advCallback);
    }

    private void startServer()
    {
        bluetoothGattServer = bluetoothManager.openGattServer(context, bluetoothGattServerCallback);
        if(bluetoothGattServer == null)
        {
            Log.d(TAG, "Unable to create GATT server");
            peripheralCallback.onStatusMsg("Unable to create GATT server");
            return;
        }
        bluetoothGattServer.addService(bluetoothGattService);
    }

    /* Advertise Callback */
    private final AdvertiseCallback advCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            peripheralCallback.onStatusMsg("GattServer onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            peripheralCallback.onStatusMsg("GattServer onStartFailure");
        }
    };

    /* Gatt Server Callback */
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        /* 연결 상태가 변할 경우 */
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                if(newState == BluetoothGatt.STATE_CONNECTED)
                {
                    bluetoothDevice = device;
                    peripheralCallback.onStatusMsg("GattServer STATE_CONNECTED");
                }
                else if(newState == BluetoothGatt.STATE_DISCONNECTED)
                {
                    bluetoothDevice = null;
                    peripheralCallback.onStatusMsg("GattServer STATE_DISCONNECTED");
                }
            }
            else
            {
                bluetoothDevice = null;
                peripheralCallback.onStatusMsg("GattSever GATT_FAILURE");
            }
        }

        /* Central에서 속성 읽기 요청이 온 경우 */
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "onCharacteristicReadRequest");
            /* Central에 데이터 보내기 */

            /* offset이 0이 아닌 경우는 GATT_INVALID_OFFSET 보내줌 */
            if(offset != 0)
            {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null);
                return;
            }
            bluetoothGattServer.sendResponse(device,requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        /* Central에서 속성 쓰기 요청이 온 경우 */
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.d(TAG, "onCharacteristicWriteRequest");
            /* Central에서 데이터를 읽어옴 */
            peripheralCallback.onStatusMsg("characteristic read : " + value);
            peripheralCallback.onToast(value.toString());

            /* Central이 데이터를 쓴 후 응답이 필요할 경우에는 응담 데이터 보내줌 */
            if(responseNeeded)
                bluetoothGattServer.sendResponse(device, requestId, 0, 0, null);
        }

        /* 설명자 읽기 요청이 들어 온 경우 */
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.d(TAG, "onDescriptorReadRequest");
            if(offset != 0)
            {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null);
                return;
            }
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());

        }

        /* 설명자 쓰쓰기 요청이 들어 온 경우 */
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            Log.d(TAG, "onDescriptorWriteRequest");
            peripheralCallback.onStatusMsg("Description read : " + value.toString());
            peripheralCallback.onToast(value.toString());

            bluetoothGattServer.sendResponse(device,requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }
    };
}
