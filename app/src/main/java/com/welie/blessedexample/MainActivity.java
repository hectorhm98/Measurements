package com.welie.blessedexample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.FormatFlagsConversionMismatchException;
import java.util.Locale;

import timber.log.Timber;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private TextView measurementValue;
    private TextView measurementValue2;
    private TextView measurementValue3;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int ACCESS_LOCATION_REQUEST = 2;
    boolean BPMStatus;
    boolean POMStatus;
    boolean BSCStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate called");

        Timber.plant(new Timber.DebugTree());

        setContentView(R.layout.activity_main);
        measurementValue = (TextView) findViewById(R.id.bloodPressureValue);
        measurementValue2 = (TextView) findViewById(R.id.pulseoximeterValue);
        measurementValue3 = (TextView) findViewById(R.id.ScaleValue);
        Button tensiometer = (Button) findViewById(R.id.tensbtn);
        Button pulseOximeter = (Button) findViewById(R.id.pobtn);
        Button scale = (Button) findViewById(R.id.sclbtn);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) return;

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if(hasPermissions()) {
            initBluetoothHandler();
        }

        tensiometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                measurementValue = (TextView) findViewById(R.id.bloodPressureValue);
                measurementValue.setText("Scaning");
                measurementValue2.setText("Stoped");
                measurementValue3.setText("Stoped");
                initBluetoothHandler();
                unregister();
                BluetoothHandler.getInstance(getApplicationContext());
                registerReceiver(bloodPressureDataReceiver, new IntentFilter( "BluetoothMeasurement" ));
            }
        });

        pulseOximeter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                measurementValue2 = (TextView) findViewById(R.id.pulseoximeterValue);
                initBluetoothHandler();
                unregister();
                measurementValue.setText("Stoped");
                measurementValue2.setText("Scaning");
                measurementValue3.setText("Stoped");
                BluetoothHandler.getInstance(getApplicationContext());
                registerReceiver(pulseOximeterDataReceiver, new IntentFilter("OximeterMeasurement"));
            }
        });

        scale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                measurementValue3 = (TextView) findViewById(R.id.ScaleValue);
                initBluetoothHandler();
                unregister();
                String a = String.format("%04X", 3333);
                Log.d("IntToHexInt", a);
                measurementValue.setText("Stoped");
                measurementValue2.setText("Stoped");
                measurementValue3.setText("Scaning");
                //BluetoothHandler.getInstance(getApplicationContext());
                //registerReceiver(scaleDataReceiver, new IntentFilter("ScaleMeasurement"));
                Intent intent = new Intent(MainActivity.this, ScaleActivity.class);
                startActivity(intent);
            }
        });
    }



    private void initBluetoothHandler()
    {
        //BluetoothHandler.getInstance(getApplicationContext());
        registerReceiver(bloodPressureDataReceiver, new IntentFilter( "BluetoothMeasurement" ));
        registerReceiver(temperatureDataReceiver, new IntentFilter( "TemperatureMeasurement" ));
        registerReceiver(heartRateDataReceiver, new IntentFilter( "HeartRateMeasurement" ));
        registerReceiver(pulseOximeterDataReceiver, new IntentFilter("OximeterMeasurement"));
        registerReceiver(scaleDataReceiver, new IntentFilter("ScaleMeasurement"));

    }

    private void unregister() {
        unregisterReceiver(bloodPressureDataReceiver);
        unregisterReceiver(temperatureDataReceiver);
        unregisterReceiver(heartRateDataReceiver);
        unregisterReceiver(pulseOximeterDataReceiver);
        unregisterReceiver(scaleDataReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bloodPressureDataReceiver);
        unregisterReceiver(temperatureDataReceiver);
        unregisterReceiver(heartRateDataReceiver);
        unregisterReceiver(pulseOximeterDataReceiver);
        unregisterReceiver(scaleDataReceiver);

    }

    private final BroadcastReceiver bloodPressureDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BloodPressureMeasurement measurement = (BloodPressureMeasurement) intent.getSerializableExtra("BloodPressure");
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
            String formattedTimestamp = df.format(measurement.timestamp);
            Log.d("DEBUG:", String.valueOf(measurement.systolic));
            measurementValue.setText(String.format(Locale.ENGLISH, "%.0f/%.0f %s, %.0f bpm\n%s", measurement.systolic, measurement.diastolic, measurement.isMMHG ? "mmHg" : "kpa", measurement.pulseRate, formattedTimestamp));
        }
    };
    private final BroadcastReceiver pulseOximeterDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PulseOximeterMeasurement measurement = (PulseOximeterMeasurement) intent.getSerializableExtra("PulseOximeter1");
            PulseOximeterMeasurement measurement2 = (PulseOximeterMeasurement) intent.getSerializableExtra("PulseOximeter2");
            measurementValue2.setText(String.format(Locale.ENGLISH, "SpO2: %d  PR: %d \n %s", measurement.spo2, measurement2.pulseRate, measurement.timestamp));
        }
    };

    private final BroadcastReceiver scaleDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ScaleMeasurement measurement = (ScaleMeasurement) intent.getSerializableExtra("ScaleMeasurement2");
            ScaleMeasurement measurement2 = (ScaleMeasurement) intent.getSerializableExtra("ScaleMeasurement3");
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
            String formattedTimestamp = df.format(measurement.timestamp);
            measurementValue3.setText(String.format(Locale.ENGLISH, "SpO2: %.0f  PR: %.0f\n%s", measurement.weight, measurement2.bodyFat, formattedTimestamp));
        }
    };


    private final BroadcastReceiver temperatureDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TemperatureMeasurement measurement = (TemperatureMeasurement) intent.getSerializableExtra("Temperature");
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
            String formattedTimestamp = df.format(measurement.timestamp);
            measurementValue.setText(String.format(Locale.ENGLISH, "%.1f %s (%s)\n%s", measurement.temperatureValue, measurement.unit == TemperatureUnit.Celsius ? "celcius" : "fahrenheit", measurement.type, formattedTimestamp));
        }
    };

    private final BroadcastReceiver heartRateDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            HeartRateMeasurement measurement = (HeartRateMeasurement) intent.getSerializableExtra("HeartRate");
            measurementValue.setText(String.format(Locale.ENGLISH, "%d bpm", measurement.pulse));
        }
    };

    private boolean hasPermissions() {
        int targetSdkVersion = getApplicationInfo().targetSdkVersion;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST);
                return false;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, ACCESS_LOCATION_REQUEST);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ACCESS_LOCATION_REQUEST:
                if(grantResults.length > 0) {
                    if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        initBluetoothHandler();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
