package com.welie.blessedexample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothCentralCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_UINT16;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_UINT8;
import static com.welie.blessed.BluetoothBytesParser.bytes2String;
import static com.welie.blessed.BluetoothPeripheral.GATT_SUCCESS;
import static com.welie.blessed.BluetoothPeripheral.STATE_DISCONNECTED;
import static java.lang.Math.abs;

public class BluetoothHandler {

    // UUIDs for the Blood Pressure service (BLP)
    private static final UUID BLP_SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    private static final UUID BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb");

    //UUIDs for the PulseOximeterMeasurement (POM)
    private static final UUID POM_SERVICE_UUID = UUID.fromString("0000FF12-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OXIMETER_MEASUREMENT_WRITE_UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    private static final UUID PULSE_OXIMETER_MEASUREMENT_NOTIFY_UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");

    //UUIDs for the Scale
    private static final UUID SCALE_CUSTOM_SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"); //CUSTOM SERVICE
    private static final UUID USER_LIST_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final UUID TAKE_MEASUREMENT_UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    private static final UUID WEIGHT_SERVICE_UUID = UUID.fromString("0000181D-0000-1000-8000-00805f9b34fb"); //WEIGHT SERVICE
    private static final UUID WEIGHT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A9D-0000-1000-8000-00805f9b34fb");
    private static final UUID BODY_SERVICE_UUID = UUID.fromString("0000181B-0000-1000-8000-00805f9b34fb"); //BODY SERVICE
    private static final UUID BODY_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A9C-0000-1000-8000-00805f9b34fb");
    private static final UUID USER_DATA_SERVICE_UUID = UUID.fromString("0000181c-0000-1000-8000-00805f9b34fb"); //USER DATA SERVICE
    private static final UUID USER_CONTROL_POINT_CHARACTERISTIC_UUID = UUID.fromString("00002A9F-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Health Thermometer service (HTS)
    private static final UUID HTS_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    private static final UUID TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Heart Rate service (HRS)
    private static final UUID HRS_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HEARTRATE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Device Information service (DIS)
    private static final UUID DIS_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    private static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
    private static final UUID MODEL_NUMBER_CHARACTERISTIC_UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Current Time service (CTS)
    private static final UUID CTS_SERVICE_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    private static final UUID CURRENT_TIME_CHARACTERISTIC_UUID = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb");

    // UUIDs for the Battery Service (BAS)
    private static final UUID BTS_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    // Local variables
    private BluetoothCentral central;
    private static BluetoothHandler instance = null;
    private Context context;
    private BluetoothPeripheral periph = null;
    private Handler handler = new Handler();
    private int currentTimeCounter = 0;
    private int packetNumber = 0;
    public boolean POMStatus = false;
    public boolean BPMStatus = false;
    public boolean BSCStatus = false;
    private Intent pulseoximeterINT = new Intent("OximeterMeasurement");
    private Intent scaleINT = new Intent("ScaleMeasurement");
    private boolean tens = false;

    // Callback for peripherals
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServicesDiscovered(BluetoothPeripheral peripheral) {
            Timber.i("discovered services");

            // Request a new connection priority
            peripheral.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);

            // Read manufacturer and model number from the Device Information Service
            if(peripheral.getService(DIS_SERVICE_UUID) != null) {
                peripheral.readCharacteristic(peripheral.getCharacteristic(DIS_SERVICE_UUID, MANUFACTURER_NAME_CHARACTERISTIC_UUID));
                peripheral.readCharacteristic(peripheral.getCharacteristic(DIS_SERVICE_UUID, MODEL_NUMBER_CHARACTERISTIC_UUID));
            }

            // Turn on notifications for Current Time Service
            if(peripheral.getService(CTS_SERVICE_UUID) != null) {
                BluetoothGattCharacteristic currentTimeCharacteristic = peripheral.getCharacteristic(CTS_SERVICE_UUID, CURRENT_TIME_CHARACTERISTIC_UUID);
                peripheral.setNotify(currentTimeCharacteristic, true);

                // If it has the write property we write the current time
                if((currentTimeCharacteristic.getProperties() & PROPERTY_WRITE) > 0) {
                    // Write the current time unless it is an Omron device
                    if(!(peripheral.getName().contains("BLEsmart_"))) {
                        BluetoothBytesParser parser = new BluetoothBytesParser();
                        parser.setCurrentTime(Calendar.getInstance());
                        peripheral.writeCharacteristic(currentTimeCharacteristic, parser.getValue(), WRITE_TYPE_DEFAULT);
                    }
                }
            }

            // Turn on notifications for Battery Service
            if(peripheral.getService(BTS_SERVICE_UUID) != null) {
                peripheral.setNotify(peripheral.getCharacteristic(BTS_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID), true);
            }

            // Turn on notifications for Blood Pressure Service
            if(peripheral.getService(BLP_SERVICE_UUID) != null) {
                tens = false;
                peripheral.setNotify(peripheral.getCharacteristic(BLP_SERVICE_UUID, BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID), true);
            }

            //Turn on notifications for Pulse Oximeter Service
            if(peripheral.getService(POM_SERVICE_UUID) != null) {
                BluetoothGattCharacteristic acquireDataCharacteristicNotify = peripheral.getCharacteristic(POM_SERVICE_UUID, PULSE_OXIMETER_MEASUREMENT_NOTIFY_UUID);
                Log.d("DEBUG:", String.valueOf(acquireDataCharacteristicNotify.getUuid()));
                BluetoothGattCharacteristic acquireDataCharacteristicWrite = peripheral.getCharacteristic(POM_SERVICE_UUID, PULSE_OXIMETER_MEASUREMENT_WRITE_UUID);
                peripheral.setNotify(acquireDataCharacteristicNotify, true);
                peripheral.setNotify(acquireDataCharacteristicWrite, true);
                packetNumber = 0;

                //if ((acquireDataCharacteristicWrite.getProperties() & PROPERTY_WRITE) > 0) {
                    // Write the desired data
                    byte[] value = new byte[]{(byte) 0x99, 0x00, 0x19};
                    peripheral.writeCharacteristic(acquireDataCharacteristicWrite, value, WRITE_TYPE_NO_RESPONSE);
                //}
            }

            //Turn on notifications for Scale
            /*if(peripheral.getService(USER_DATA_SERVICE_UUID) != null){
                peripheral.setNotify(peripheral.getCharacteristic(USER_DATA_SERVICE_UUID, USER_CONTROL_POINT_CHARACTERISTIC_UUID), true);
                BluetoothGattCharacteristic userControlPointWrite = peripheral.getCharacteristic(USER_DATA_SERVICE_UUID,USER_CONTROL_POINT_CHARACTERISTIC_UUID);
                byte[] value = new byte[]{(byte) 0x02, 0X01/*USER INDEX*"/, 0x00, 0X00};
                peripheral.writeCharacteristic(userControlPointWrite, value, WRITE_TYPE_DEFAULT);
            }*/
            Log.d("DEBUG:", peripheral.getName());
            if(peripheral.getService(SCALE_CUSTOM_SERVICE_UUID) != null) {

                peripheral.setNotify(peripheral.getCharacteristic(SCALE_CUSTOM_SERVICE_UUID, USER_LIST_UUID), true);
                BluetoothGattCharacteristic takeMeasurementWrite = peripheral.getCharacteristic(SCALE_CUSTOM_SERVICE_UUID, TAKE_MEASUREMENT_UUID);
                BluetoothGattCharacteristic userListWrite = peripheral.getCharacteristic(SCALE_CUSTOM_SERVICE_UUID, USER_LIST_UUID);
                byte[] value = new byte[] {0x00};
                //peripheral.writeCharacteristic(takeMeasurementWrite, value, WRITE_TYPE_DEFAULT);
                peripheral.writeCharacteristic(userListWrite, value, WRITE_TYPE_DEFAULT);
                Log.d("Scale", "He escrito");

            }

            if(peripheral.getService(WEIGHT_SERVICE_UUID) != null){
                peripheral.setNotify(peripheral.getCharacteristic(WEIGHT_SERVICE_UUID, WEIGHT_MEASUREMENT_CHARACTERISTIC_UUID), true);
            }
            if(peripheral.getService(BODY_SERVICE_UUID) != null){
                peripheral.setNotify(peripheral.getCharacteristic(BODY_SERVICE_UUID,BODY_MEASUREMENT_CHARACTERISTIC_UUID),true);
            }

            // Turn on notification for Health Thermometer Service
            if(peripheral.getService(HTS_SERVICE_UUID) != null) {
                peripheral.setNotify(peripheral.getCharacteristic(HTS_SERVICE_UUID, TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID), true);
            }

            // Turn on notification for Heart Rate  Service
            if(peripheral.getService(HRS_SERVICE_UUID) != null) {
                peripheral.setNotify(peripheral.getCharacteristic(HRS_SERVICE_UUID, HEARTRATE_MEASUREMENT_CHARACTERISTIC_UUID), true);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onNotificationStateUpdate(BluetoothPeripheral peripheral, BluetoothGattCharacteristic characteristic, int status) {
            if( status == GATT_SUCCESS) {
                if(peripheral.isNotifying(characteristic)) {
                    Timber.i("SUCCESS: Notify set to 'on' for %s", characteristic.getUuid());
                } else {
                    Timber.i("SUCCESS: Notify set to 'off' for %s", characteristic.getUuid());
                }
            } else {
                Timber.e("ERROR: Changing notification state failed for %s", characteristic.getUuid());
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicWrite(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, int status) {
            if( status == GATT_SUCCESS) {
                Timber.i("SUCCESS: Writing <%s> to <%s>", bytes2String(value), characteristic.getUuid().toString());
            } else {
                Timber.i("ERROR: Failed writing <%s> to <%s>", bytes2String(value), characteristic.getUuid().toString());
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, int status) {
            if(status != GATT_SUCCESS) return;
            UUID characteristicUUID = characteristic.getUuid();
            BluetoothBytesParser parser = new BluetoothBytesParser(value);

            if (characteristicUUID.equals(BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                if(!tens) {
                    BloodPressureMeasurement measurement = new BloodPressureMeasurement(value);
                    Intent intent = new Intent("BluetoothMeasurement");
                    intent.putExtra("BloodPressure", measurement);
                    context.sendBroadcast(intent);
                    Timber.d("%s", measurement);
                    BPMStatus = true;
                    tens = true;
                }
            }
            else if(characteristicUUID.equals(PULSE_OXIMETER_MEASUREMENT_NOTIFY_UUID)){
                boolean isNotifying = peripheral.isNotifying(peripheral.getCharacteristic(POM_SERVICE_UUID,PULSE_OXIMETER_MEASUREMENT_NOTIFY_UUID));
                if(isNotifying) packetNumber++;
                if(packetNumber == 1) {
                    PulseOximeterMeasurement measurement = new PulseOximeterMeasurement(value, 1);
                    pulseoximeterINT.putExtra("PulseOximeter1", measurement);
                    //context.sendBroadcast(intent);
                    //Timber.d("%s", measurement);
                }
                else if(packetNumber == 2){
                    PulseOximeterMeasurement measurement = new PulseOximeterMeasurement(value, 2);
                    pulseoximeterINT.putExtra("PulseOximeter2", measurement);
                    context.sendBroadcast(pulseoximeterINT);
                    Timber.d("%s", measurement);
                }
                else{
                    BluetoothGattCharacteristic acquireDataCharacteristicWrite = peripheral.getCharacteristic(POM_SERVICE_UUID, PULSE_OXIMETER_MEASUREMENT_WRITE_UUID);
                    byte[] valus = new byte[]{(byte) 0x99, 0x7F, 0x18};
                    peripheral.writeCharacteristic(acquireDataCharacteristicWrite, valus, WRITE_TYPE_NO_RESPONSE);
                    packetNumber = 0;
                }
                POMStatus = true;
            }
            else if(characteristicUUID.equals(USER_LIST_UUID)){
                Log.d("Scale", "Paso");
                ScaleMeasurement measurement = new ScaleMeasurement(value, 0);
                scaleINT.putExtra("ScaleMeasurement0", measurement);
                context.sendBroadcast(scaleINT);
            }
            else if(characteristicUUID.equals(USER_CONTROL_POINT_CHARACTERISTIC_UUID)){
                ScaleMeasurement measurement = new ScaleMeasurement(value,1);
                scaleINT.putExtra("ScaleMeasurement1", measurement);
                /*context.sendBroadcast(scaleINT);
                Timber.d("%s", measurement);*/
            } else if (characteristicUUID.equals(WEIGHT_MEASUREMENT_CHARACTERISTIC_UUID)){
                ScaleMeasurement measurement = new ScaleMeasurement(value, 2);
                scaleINT.putExtra("ScaleMeasurement2", measurement);
                /*context.sendBroadcast(scaleINT);
                Timber.d("%s", measurement);*/
            }else if (characteristicUUID.equals(BODY_MEASUREMENT_CHARACTERISTIC_UUID)) {
                ScaleMeasurement measurement = new ScaleMeasurement(value, 3);
                scaleINT.putExtra("ScaleMeasurement", measurement);
                context.sendBroadcast(scaleINT);
                Timber.d("%s", measurement);
                BSCStatus = true;
            }

            else if (characteristicUUID.equals(TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                TemperatureMeasurement measurement = new TemperatureMeasurement(value);
                Intent intent = new Intent("TemperatureMeasurement");
                intent.putExtra("Temperature", measurement);
                context.sendBroadcast(intent);
                Timber.d("%s", measurement);
            } else if (characteristicUUID.equals(HEARTRATE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                HeartRateMeasurement measurement = new HeartRateMeasurement(value);
                Intent intent = new Intent("HeartRateMeasurement");
                intent.putExtra("HeartRate", measurement);
                context.sendBroadcast(intent);
                Timber.d("%s", measurement);
            } else if (characteristicUUID.equals(CURRENT_TIME_CHARACTERISTIC_UUID)) {
                Date currentTime = parser.getDateTime();
                Timber.i("Received device time: %s", currentTime);

                // Deal with Omron devices where we can only write currentTime under specific conditions
                if (peripheral.getName().contains("BLEsmart_")) {
                    boolean isNotifying = peripheral.isNotifying(peripheral.getCharacteristic(BLP_SERVICE_UUID, BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID));
                    if (isNotifying) currentTimeCounter++;

                    // We can set device time for Omron devices only if it is the first notification and currentTime is more than 10 min from now
                    long interval = abs(Calendar.getInstance().getTimeInMillis() - currentTime.getTime());
                    if (currentTimeCounter == 1 && interval > 10 * 60 * 1000) {
                        parser.setCurrentTime(Calendar.getInstance());
                        peripheral.writeCharacteristic(characteristic, parser.getValue(), WRITE_TYPE_DEFAULT);
                    }
                }
            } else if (characteristicUUID.equals(BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                int batteryLevel = parser.getIntValue(FORMAT_UINT8);
                Timber.i("Received battery level %d%%", batteryLevel);
            } else if (characteristicUUID.equals(MANUFACTURER_NAME_CHARACTERISTIC_UUID)) {
                String manufacturer = parser.getStringValue(0);
                Timber.i("Received manufacturer: %s", manufacturer);
            } else if (characteristicUUID.equals(MODEL_NUMBER_CHARACTERISTIC_UUID)) {
                String modelNumber = parser.getStringValue(0);
                Timber.i("Received modelnumber: %s", modelNumber);
            }
        }
    };

    // Callback for central
    private final BluetoothCentralCallback bluetoothCentralCallback = new BluetoothCentralCallback() {

        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            Timber.i("connected to '%s'", peripheral.getName());
            periph = peripheral;
        }

        @Override
        public void onConnectionFailed(BluetoothPeripheral peripheral, final int status) {
            Timber.e("connection '%s' failed with status %d", peripheral.getName(), status);
        }

        @Override
        public void onDisconnectedPeripheral(final BluetoothPeripheral peripheral, final int status) {
            Timber.i("disconnected '%s' with status %d", peripheral.getName(), status);

            // Reconnect to this device when it becomes available again
            periph = null;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //central.autoConnectPeripheral(peripheral, peripheralCallback);
                }
            }, 5000);
        }

        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            Timber.i("Found peripheral '%s'", peripheral.getName());
            central.stopScan();
            central.connectPeripheral(peripheral, peripheralCallback);
        }

        @Override
        public void onBluetoothAdapterStateChanged(int state) {
            Timber.i("bluetooth adapter changed state to %d", state);
            if(state == BluetoothAdapter.STATE_ON) {
                // Bluetooth is on now, start scanning again
                // Scan for peripherals with a certain service UUIDs
                central.startPairingPopupHack();
                central.scanForPeripheralsWithServices(new UUID[]{BLP_SERVICE_UUID, POM_SERVICE_UUID, USER_DATA_SERVICE_UUID, WEIGHT_SERVICE_UUID});
            }
        }
    };

    public static synchronized BluetoothHandler getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothHandler(context.getApplicationContext());
        }
        return instance;
    }

    public synchronized void Disconect() {
        if(periph != null) {
            central.cancelConnection(periph);
            central.stopScan();
            central.close();
        }
    }


    private BluetoothHandler(Context context) {
        this.context = context;

        // Create BluetoothCentral
        central = new BluetoothCentral(context, bluetoothCentralCallback, new Handler());

        // Scan for peripherals with a certain service UUIDs
        central.startPairingPopupHack();
        central.scanForPeripheralsWithServices(new UUID[]{BLP_SERVICE_UUID, POM_SERVICE_UUID, USER_DATA_SERVICE_UUID, WEIGHT_SERVICE_UUID});
    }

}
