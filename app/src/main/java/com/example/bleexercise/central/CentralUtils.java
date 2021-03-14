package com.example.bleexercise.central;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.example.bleexercise.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CentralUtils {

    /* BLE 속성을 찾아줌 */
    public List<BluetoothGattCharacteristic> findBLECharacteristics(BluetoothGatt bluetoothGatt) {
        List<BluetoothGattCharacteristic> matchingCharacteristics = new ArrayList<>();
        List<BluetoothGattService> serviceList = bluetoothGatt.getServices();
        BluetoothGattService service = findGattService(serviceList);

        /* 서비스가 없으면 현재 리스트 넘김 */
        if(service == null)
            return matchingCharacteristics;

        /* Gatt service 에서 속성 리스트 가져온 다음 매치 되는 속성을 찾아서 리스트에 넣음 */
        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for(BluetoothGattCharacteristic characteristic : characteristicList)
        {
            if(isMatchingCharacteristic(characteristic))
            {
                matchingCharacteristics.add(characteristic);
            }
        }

        return matchingCharacteristics;
    }

    /* 특정 UUID로 속성 찾기 */
    public BluetoothGattCharacteristic findCharacteristic(BluetoothGatt bluetoothGatt, UUID uuid) {
        List<BluetoothGattService> serviceList = bluetoothGatt.getServices();
        BluetoothGattService service = findGattService(serviceList);

        if (service == null) {
            return null;
        }

        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList) {
            if (matchCharacteristic(characteristic, uuid)) {
                return characteristic;
            }
        }

        return null;
    }

    /* 주어진 UUID와 매치되는 속성 찾기 */
    private boolean matchCharacteristic(BluetoothGattCharacteristic characteristic, UUID uuid)
    {
        if(characteristic == null)
        {
            return false;
        }

        UUID characteristicUuid = characteristic.getUuid();
        return matchUUIDs(characteristicUuid, uuid.toString());
    }

    /* Service 리스트 중 service uuid와 일치하는 service 찾기 */
    private BluetoothGattService findGattService(List<BluetoothGattService> serviceList)
    {
        for(BluetoothGattService service : serviceList)
        {
            UUID serviceUuid = service.getUuid();
            if(isMatchingService(serviceUuid))
            {
                return service;
            }
        }
        return null;
    }

    private boolean isMatchingService(UUID uuid)
    {
        return matchUUIDs(uuid, Constants.SERVICE_STRING);
    }

    /* 발견된 속성의 UUID 를 가져와서 속성 UUID 랑 일치하는지 비교*/
    private boolean isMatchingCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        if(characteristic == null)
        {
            return false;
        }

        UUID uuid = characteristic.getUuid();
        return matchCharacteristicUUID(uuid);
    }

    private boolean matchCharacteristicUUID(UUID uuid)
    {
        return matchUUIDs(uuid, Constants.CHARACTERISTIC_STRING);
    }

    private boolean matchUUIDs(UUID uuid, String... matches)
    {
        for(String match : matches)
        {
            if(uuid.toString().equalsIgnoreCase(match))
            {
                return true;
            }
        }
        return false;
    }
}
