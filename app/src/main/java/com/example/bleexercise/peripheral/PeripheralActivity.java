package com.example.bleexercise.peripheral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bleexercise.R;
import com.example.bleexercise.util.Utils;

import java.util.Calendar;

public class PeripheralActivity extends AppCompatActivity {
    private TextView tvData;
    private Button btnSend;
    private Button btnClose;

    private Utils utils;

    private final int REQUEST_BLE = 0x1001;

    private PeripheralManager peripheralManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        initServer();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void initView()
    {
        tvData = findViewById(R.id.tv_data);
        btnSend = findViewById(R.id.btn_send);
        btnClose = findViewById(R.id.btn_close);

        btnSend.setOnClickListener(onClickListener);
        btnClose.setOnClickListener(onClickListener);

        utils = Utils.getInstance();

        peripheralManager = PeripheralManager.getInstance();
    }

    private void initServer()
    {
        peripheralManager.setCallback(peripheralCallback);
        peripheralManager.initServer(this);
    }

    private void showStatusMsg(final String message)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String oldMsg = tvData.getText().toString();
                tvData.setText(oldMsg + "\n" + message);
                scrollToBottom();
            }
        });
    }

    private void scrollToBottom()
    {
        final ScrollView scrollView = ((ScrollView) findViewById(R.id.sv_data));
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void requestEnableBLE()
    {
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if(btAdapter == null || !btAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_BLE && resultCode != RESULT_OK)
            requestEnableBLE();
    }

    //    private Handler handler = new Handler(Looper.getMainLooper())
//    {
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            super.handleMessage(msg);
//            switch (msg.what)
//            {
//                case EXTRA_MSG :
//                    showStatusMsg();
//
//            }
//        }
//    };

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.btn_send)
            {
                Calendar calendar = Calendar.getInstance();
                String data = (calendar.get(Calendar.MONTH) + 1)
                        + "/" + calendar.get(Calendar.DAY_OF_MONTH)
                        + " " + calendar.get(Calendar.HOUR_OF_DAY)
                        + ":" + calendar.get(Calendar.HOUR_OF_DAY)
                        + ":" + calendar.get(Calendar.SECOND);

            }
            else if(v.getId() == R.id.btn_close)
            {

            }
        }
    };

    private PeripheralCallback peripheralCallback = new PeripheralCallback() {
        @Override
        public void requestEnableBLE() {
            requestEnableBLE();
        }

        @Override
        public void onStatusMsg(String message) {
            showStatusMsg(message);
        }

        @Override
        public void onToast(String message) {
            utils.showToast(PeripheralActivity.this, message, false);
        }
    };
}
