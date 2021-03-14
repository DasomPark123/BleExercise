package com.example.bleexercise;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.bleexercise.central.CentralActivity;
import com.example.bleexercise.peripheral.PeripheralActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnCentral;
    private Button btnPeripheral;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCentral = findViewById(R.id.btn_central);
        btnPeripheral = findViewById(R.id.btn_peripheral);

        btnCentral.setOnClickListener(onClickListener);
        btnPeripheral.setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.btn_central)
            {
                 startActivity(new Intent(MainActivity.this, CentralActivity.class
                 ));
            }
            else if(v.getId() == R.id.btn_peripheral)
            {
                startActivity(new Intent(MainActivity.this, PeripheralActivity.class));
            }
        }
    };
}
