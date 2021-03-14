package com.example.bleexercise.central;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bleexercise.R;
import com.example.bleexercise.Utils;

import java.util.Calendar;

public class CentralActivity extends Activity {

    private final String TAG = getClass().getSimpleName();

    private TextView tvData;
    private Button btnSend;

    private Utils utils;

    private CentralManager centralManager;

    private final int REQUEST_LOCATION_ON = 0x1001;
    private final int REQUEST_ENABLE_BLUETOOTH = 0x1002;
    private final int REQUEST_LOCATION_PERMISSION = 0x1003;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        StartScanTask startScanTask = new StartScanTask();
        startScanTask.execute();
    }

    private void init() {
        tvData = findViewById(R.id.tv_data);
        btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(onClickListener);

        utils = new Utils();

        centralManager = CentralManager.getInstance(this);
    }

    private void scrollToBottom()
    {
        final ScrollView scrollView = findViewById(R.id.sv_data);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void showStatusMsg(final String msg)
    {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                String oldMsg = tvData.getText().toString();
                tvData.setText(oldMsg + "\n" + msg);
                scrollToBottom();
            }
        };
        handler.sendEmptyMessage(1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if ((ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                centralCallback.onToast("Location permission denied");
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH && resultCode == RESULT_CANCELED) {
            centralCallback.onToast("Bluetooth is not enabled");
            finish();
        } else if (requestCode == REQUEST_LOCATION_ON && resultCode == RESULT_CANCELED) {
            centralCallback.onToast("Location is not enabled");
            finish();
        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.btn_send) {
                Calendar calendar = Calendar.getInstance();
                String todayTime = (calendar.get(Calendar.MONTH))
                        + "월" + calendar.get(Calendar.DAY_OF_MONTH)
                        + "일" + calendar.get(Calendar.HOUR_OF_DAY)
                        + ":" + calendar.get(Calendar.MINUTE)
                        + ":" + calendar.get((Calendar.SECOND));

                centralManager.sendData(todayTime);
            }
        }
    };

    private CentralCallback centralCallback = new CentralCallback() {
        @Override
        public void requestEnableBLE() {
            Intent intent = new Intent((BluetoothAdapter.ACTION_REQUEST_ENABLE));
            startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
        }

        @Override
        public void requestLocationPermission() {
            ActivityCompat.requestPermissions(CentralActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION
                            , Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

        @Override
        public void requestLocationOn() {
            Intent intent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(intent, REQUEST_LOCATION_ON);
        }

        @Override
        public void onStatus(String msg) {
            showStatusMsg(msg);
        }

        @Override
        public void onToast(String msg) {
            utils.showToast(CentralActivity.this, msg, false);
        }
    };

    private class StartScanTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean result = centralManager.startScan(CentralActivity.this);
            return result;
        }

        @Override
        protected void onPostExecute(Boolean isFindDevice) {
            super.onPostExecute(isFindDevice);
            if (isFindDevice) {
                centralCallback.onStatus("Connected successfully");
            } else {
                centralCallback.onStatus("Connection Failed");
            }
        }
    }
}
