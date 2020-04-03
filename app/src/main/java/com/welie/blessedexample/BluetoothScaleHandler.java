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
import static java.lang.Math.abs;

public class BluetoothScaleHandler {

    //UUIDs for the Scale
    private static final UUID SCALE_CUSTOM_SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"); //CUSTOM SERVICE
    private static final UUID USER_LIST_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final UUID ACTIVITY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
    private static final UUID TAKE_MEASUREMENT_UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    private static final UUID WEIGHT_SERVICE_UUID = UUID.fromString("0000181D-0000-1000-8000-00805f9b34fb"); //WEIGHT SERVICE
    private static final UUID WEIGHT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A9D-0000-1000-8000-00805f9b34fb");
    private static final UUID BODY_SERVICE_UUID = UUID.fromString("0000181B-0000-1000-8000-00805f9b34fb"); //BODY SERVICE
    private static final UUID BODY_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A9C-0000-1000-8000-00805f9b34fb");
    private static final UUID USER_DATA_SERVICE_UUID = UUID.fromString("0000181c-0000-1000-8000-00805f9b34fb"); //USER DATA SERVICE
    private static final UUID DATE_OF_BIRTH_CHARACTERISTIC_UUID = UUID.fromString("00002A85-0000-1000-8000-00805f9b34fb");
    private static final UUID GENDER_CHARACTERISTIC_UUID = UUID.fromString("00002A8C-0000-1000-8000-00805f9b34fb");
    private static final UUID HEIGHT_CHARACTERISTIC_UUID = UUID.fromString("00002A8E-0000-1000-8000-00805f9b34fb");
    private static final UUID USER_CONTROL_POINT_CHARACTERISTIC_UUID = UUID.fromString("00002A9F-0000-1000-8000-00805f9b34fb");



    // Local variables
    private BluetoothCentral central;
    private static BluetoothScaleHandler instance = null;
    private Context context;
    private Handler handler = new Handler();
    private int UserIndex = -1;
    private byte[] ConsentHex = new byte[4];
    private int month = -1;

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


            //Turn on notifications for Scale
            if (peripheral.getService(USER_DATA_SERVICE_UUID) != null) {
                peripheral.setNotify(peripheral.getCharacteristic(USER_DATA_SERVICE_UUID, USER_CONTROL_POINT_CHARACTERISTIC_UUID), true);
                BluetoothGattCharacteristic userControlPointWrite = peripheral.getCharacteristic(USER_DATA_SERVICE_UUID, USER_CONTROL_POINT_CHARACTERISTIC_UUID);
                byte[] value = new byte[]{(byte) 0x02, (byte) UserIndex, ConsentHex[0], ConsentHex[1]}; //CONSENT CODE
                peripheral.writeCharacteristic(userControlPointWrite, value, WRITE_TYPE_DEFAULT);
            }
            Log.d("DEBUG:", peripheral.getName());
            if (peripheral.getService(SCALE_CUSTOM_SERVICE_UUID) != null) {

                peripheral.setNotify(peripheral.getCharacteristic(SCALE_CUSTOM_SERVICE_UUID, USER_LIST_UUID), true);
                /*BluetoothGattCharacteristic takeMeasurementWrite = peripheral.getCharacteristic(SCALE_CUSTOM_SERVICE_UUID, TAKE_MEASUREMENT_UUID);
                BluetoothGattCharacteristic userListWrite = peripheral.getCharacteristic(SCALE_CUSTOM_SERVICE_UUID, USER_LIST_UUID);
                byte[] value = new byte[]{0x00};
                peripheral.writeCharacteristic(takeMeasurementWrite, value, WRITE_TYPE_DEFAULT);
                peripheral.writeCharacteristic(userListWrite, value, WRITE_TYPE_DEFAULT);
                Log.d("Scale", "He escrito");*/

            }

            if (peripheral.getService(WEIGHT_SERVICE_UUID) != null) {
                peripheral.setNotify(peripheral.getCharacteristic(WEIGHT_SERVICE_UUID, WEIGHT_MEASUREMENT_CHARACTERISTIC_UUID), true);
            }
            if (peripheral.getService(BODY_SERVICE_UUID) != null) {
                peripheral.setNotify(peripheral.getCharacteristic(BODY_SERVICE_UUID, BODY_MEASUREMENT_CHARACTERISTIC_UUID), true);
            }

        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onNotificationStateUpdate(BluetoothPeripheral peripheral, BluetoothGattCharacteristic characteristic, int status) {
            if (status == GATT_SUCCESS) {
                if (peripheral.isNotifying(characteristic)) {
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
            if (status == GATT_SUCCESS) {
                Timber.i("SUCCESS: Writing <%s> to <%s>", bytes2String(value), characteristic.getUuid().toString());
            } else {
                Timber.i("ERROR: Failed writing <%s> to <%s>", bytes2String(value), characteristic.getUuid().toString());
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, int status) {
            if (status != GATT_SUCCESS) return;
            UUID characteristicUUID = characteristic.getUuid();
            BluetoothBytesParser parser = new BluetoothBytesParser(value);

            if (characteristicUUID.equals(USER_CONTROL_POINT_CHARACTERISTIC_UUID)) {
                ScaleMeasurement measurement = new ScaleMeasurement(value, 1);
                if(measurement.responseValue == 0x01){
                    BluetoothGattCharacteristic takeMeasurementWrite = peripheral.getCharacteristic(SCALE_CUSTOM_SERVICE_UUID, TAKE_MEASUREMENT_UUID);
                    BluetoothGattCharacteristic birthDateWrite = peripheral.getCharacteristic(USER_DATA_SERVICE_UUID, DATE_OF_BIRTH_CHARACTERISTIC_UUID);
                    BluetoothGattCharacteristic genderWrite = peripheral.getCharacteristic(USER_DATA_SERVICE_UUID, GENDER_CHARACTERISTIC_UUID);
                    BluetoothGattCharacteristic heightWrite = peripheral.getCharacteristic(USER_DATA_SERVICE_UUID, HEIGHT_CHARACTERISTIC_UUID);
                    BluetoothGattCharacteristic activityLevelWrite = peripheral.getCharacteristic(SCALE_CUSTOM_SERVICE_UUID, ACTIVITY_LEVEL_CHARACTERISTIC_UUID);
                    /*Supongo que aqui es un buen sitio donde escribir los datos del usuario
                    Estan puestos los valores por defecto
                     */
                    byte[] birth = new byte[]{(byte)0xC7,0x07,0x0A,0x01}; //Birthdate
                    byte[] gender = new byte[]{0x01};//Gender
                    byte[] height = new byte[]{(byte)0xA5};
                    byte[] activity = new byte[]{0x03};
                    byte[] valus = new byte[]{0x00};
                    peripheral.writeCharacteristic(birthDateWrite, birth, WRITE_TYPE_DEFAULT);
                    peripheral.writeCharacteristic(genderWrite, gender, WRITE_TYPE_DEFAULT);
                    peripheral.writeCharacteristic(heightWrite, height, WRITE_TYPE_DEFAULT);
                    peripheral.writeCharacteristic(activityLevelWrite, activity, WRITE_TYPE_DEFAULT);
                    peripheral.writeCharacteristic(takeMeasurementWrite, valus, WRITE_TYPE_DEFAULT);
                }else{
                    return; //Something went wrong
                }
                scaleINT.putExtra("ScaleMeasurement1", measurement);
                /*context.sendBroadcast(scaleINT);
                Timber.d("%s", measurement);*/
            } else if (characteristicUUID.equals(WEIGHT_MEASUREMENT_CHARACTERISTIC_UUID)) {
                ScaleMeasurement measurement = new ScaleMeasurement(value, 2);
                scaleINT.putExtra("ScaleMeasurement2", measurement);
                /*context.sendBroadcast(scaleINT);
                Timber.d("%s", measurement);*/
            } else if (characteristicUUID.equals(BODY_MEASUREMENT_CHARACTERISTIC_UUID)) {
                ScaleMeasurement measurement = new ScaleMeasurement(value, 3);
                scaleINT.putExtra("ScaleMeasurement", measurement);
                context.sendBroadcast(scaleINT);
                Timber.d("%s", measurement);
            }

        }

    };

        // Callback for central
        private final BluetoothCentralCallback bluetoothCentralCallback = new BluetoothCentralCallback() {

            @Override
            public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
                Timber.i("connected to '%s'", peripheral.getName());
            }

            @Override
            public void onConnectionFailed(BluetoothPeripheral peripheral, final int status) {
                Timber.e("connection '%s' failed with status %d", peripheral.getName(), status);
            }

            @Override
            public void onDisconnectedPeripheral(final BluetoothPeripheral peripheral, final int status) {
                Timber.i("disconnected '%s' with status %d", peripheral.getName(), status);

                // Reconnect to this device when it becomes available again
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        central.autoConnectPeripheral(peripheral, peripheralCallback);
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
                if (state == BluetoothAdapter.STATE_ON) {
                    // Bluetooth is on now, start scanning again
                    // Scan for peripherals with a certain service UUIDs
                    central.startPairingPopupHack();
                    central.scanForPeripheralsWithServices(new UUID[]{USER_DATA_SERVICE_UUID, WEIGHT_SERVICE_UUID});
                }
            }
        };

        public static synchronized BluetoothScaleHandler getInstance(Context context, int UserI, byte[] ConsentHex) {
            if (instance == null) {
                instance = new BluetoothScaleHandler(context.getApplicationContext(), UserI, ConsentHex);
            }
            return instance;
        }




        private BluetoothScaleHandler(Context context, int UserI, byte[] consentHex) {
            this.context = context;
            this.UserIndex  = UserI;
            this.ConsentHex = consentHex;
            // Create BluetoothCentral
            Log.d("Scale", "bluetoothScale");
            central = new BluetoothCentral(context, bluetoothCentralCallback, new Handler());
            // Scan for peripherals with a certain service UUIDs
            central.startPairingPopupHack();
            central.scanForPeripheralsWithServices(new UUID[]{USER_DATA_SERVICE_UUID, WEIGHT_SERVICE_UUID});
        }

    }
