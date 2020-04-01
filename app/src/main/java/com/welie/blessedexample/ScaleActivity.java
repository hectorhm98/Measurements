package com.welie.blessedexample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ScaleActivity extends AppCompatActivity {
    private ListView userList;
    private ArrayList<String> users;
    private ArrayAdapter<String> arrayAdapter;
    private EditText consentCode;
    private String stringCode;
    private byte[] cnstCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scale);

        userList = (ListView) findViewById(R.id.userList);
        consentCode = (EditText) findViewById(R.id.cnstCode);
        BluetoothHandler.getInstance(getApplicationContext());
        registerReceiver(scaleDataReceiver, new IntentFilter("ScaleMeasurement0"));
        users = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, users);
        userList.setAdapter(arrayAdapter);

        userList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                stringCode = Integer.toHexString(Integer.parseInt(consentCode.getText().toString()));
                cnstCode = stringCode.getBytes();

            }
        });
    }

    private final BroadcastReceiver scaleDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ScaleMeasurement measurement = (ScaleMeasurement) intent.getSerializableExtra("ScaleMeasurement0");
            users.add(String.format(Locale.ENGLISH, "USER ID: %d. Date: %s Height: %d Gender %s",measurement.userIndex, measurement.birth, measurement.height1, measurement.sgender));
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scaleDataReceiver);
    }
}
