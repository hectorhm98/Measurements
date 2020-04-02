package com.welie.blessedexample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.util.Log;
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
    private int userID;
    byte[] result = new byte[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scale);

        userList = (ListView) findViewById(R.id.userList);
        BluetoothHandler.getInstance(getApplicationContext());
        registerReceiver(scaleDataReceiver, new IntentFilter("ScaleMeasurement"));
        users = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, users);
        userList.setAdapter(arrayAdapter);

        userList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                consentCode = (EditText) findViewById(R.id.cnstCode);
                stringCode = Integer.toHexString(3333);
                cnstCode = stringCode.getBytes();

                userID = position;
                Intent sclINT = new Intent("userControlPoint");
                sclINT.putExtra("ConsentCode", cnstCode);
                sclINT.putExtra("userID", userID);
            }
        });
    }

    private void ListaUser() {
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, users);
        userList.setAdapter(arrayAdapter);

        userList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                consentCode = (EditText) findViewById(R.id.cnstCode);
                stringCode = "0x"+ Integer.toHexString(3333);
                cnstCode = getByteArray(3333);

                userID = position;
                Intent sclINT = new Intent("userControlPoint");
                sclINT.putExtra("ConsentCode", cnstCode);
                sclINT.putExtra("userID", userID);
            }
        });
    }

    private byte[] getByteArray(int x) {
        int i;
        byte[] b = new byte[4];
        for (i = 3; i > 0; i--) {
            float y = (float) x;
            float c = (y / 16 - x / 16) * 16;
            b[i] = (byte) c;
            x = x / 16;
        }
        return b;
    }

    private final BroadcastReceiver scaleDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ScaleMeasurement measurement = (ScaleMeasurement) intent.getSerializableExtra("ScaleMeasurement0");
            users.add(String.format(Locale.ENGLISH, "USER ID: %d. Date: %s Height: %d Gender %s",measurement.userIndex, measurement.birth, measurement.height1, measurement.sgender));
            Log.d("Scale", "Users" + users);
            ListaUser();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scaleDataReceiver);
    }

}
