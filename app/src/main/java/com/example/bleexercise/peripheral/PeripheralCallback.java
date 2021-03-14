package com.example.bleexercise.peripheral;

public interface PeripheralCallback {
    void requestEnableBLE();
    void onStatusMsg(String message);
    void onToast(String message);
}
