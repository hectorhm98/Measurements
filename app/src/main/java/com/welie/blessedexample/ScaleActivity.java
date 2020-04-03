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
    private BluetoothHandler bh;
    private byte cnstCode[];
    private int userID;
    byte[] result = new byte[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scale);

        userList = (ListView) findViewById(R.id.userList);
        bh = BluetoothHandler.getInstance(getApplicationContext());
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
                byte p[] = intToByteArray(4);
                userID = position+1;
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
                stringCode = Integer.toHexString(3333);
                cnstCode = stringCode.getBytes();
                result = intToByteArray(238);
                userID = position+1;
                bh.Disconect();
                unregisterReceiver(scaleDataReceiver);
                BluetoothScaleHandler.getInstance(getApplicationContext(), userID, result);
                registerReceiver(scaleDataReceiver2, new IntentFilter("ScaleMeasurement1"));
                /*Intent sclINT = new Intent("userControlPoint");
                sclINT.putExtra("ConsentCode", cnstCode);
                sclINT.putExtra("userID", userID);*/
            }
        });
    }

    private final BroadcastReceiver scaleDataReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //ScaleMeasurement measurement = (ScaleMeasurement) intent.getSerializableExtra("ScaleMeasurement0");//cambiar el name intent
            //users.add(String.format(Locale.ENGLISH, "USER ID: %d. Date: %s Height: %d Gender %s",measurement.userIndex, measurement.birth, measurement.height1, measurement.sgender));
            Log.d("Scale", "BroadcastR");
        }
    };

    private final BroadcastReceiver scaleDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ScaleMeasurement measurement = (ScaleMeasurement) intent.getSerializableExtra("ScaleMeasurement0");
            users.add(String.format(Locale.ENGLISH, "USER ID: %d. Date: %s Height: %d Gender %s",measurement.userIndex, measurement.birth, measurement.height1, measurement.sgender));
            Log.d("Scale", "Users" + users);
            ListaUser();
        }
    };

    public static byte[] intToByteArray(int a)
    {
        byte[] ret = new byte[4];
        ret[0] = (byte) (a & 0xFF);
        ret[1] = (byte) ((a >> 8) & 0xFF);
        ret[2] = (byte) ((a >> 16) & 0xFF);
        ret[3] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scaleDataReceiver);
    }

}
