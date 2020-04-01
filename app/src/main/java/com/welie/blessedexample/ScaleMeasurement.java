package com.welie.blessedexample;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothPeripheral;

import java.io.Serializable;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import static com.welie.blessed.BluetoothPeripheral.GATT_SUCCESS;


public class ScaleMeasurement implements Serializable {
    public int responseOPCode;
    public int requestOPCode;
    public int responseValue;
    public int responseParameter;

    public int flags;
    public float weight;
    public Date timestamp;
    public int userID;
    public float BMI;
    public float height;

    public float bodyFat;
    public float BMR;
    public float muscle;
    public float softLeanMass;
    public float bodyWaterMass;
    public float impedance;

    public ScaleMeasurement(byte[] value, int step) {
        BluetoothBytesParser parser = new BluetoothBytesParser(value);
        switch(step){
            case(1):
                responseOPCode = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8);
                requestOPCode = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8);
                responseValue = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8);
                if(responseValue == 0x01){
                    responseParameter = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8);
                }else{
                    return; //Something went wrong
                }

            case(2):
                flags = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8);
                weight = parser.getFloatValue(BluetoothBytesParser.FORMAT_SFLOAT, ByteOrder.LITTLE_ENDIAN);
                timestamp = parser.getDateTime();
                userID = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8);
                BMI = parser.getFloatValue(BluetoothBytesParser.FORMAT_SFLOAT, ByteOrder.LITTLE_ENDIAN);
                height = parser.getFloatValue(BluetoothBytesParser.FORMAT_SFLOAT, ByteOrder.LITTLE_ENDIAN);

            case(3):
                flags = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT8);
                bodyFat = parser.getFloatValue(BluetoothBytesParser.FORMAT_SFLOAT, ByteOrder.LITTLE_ENDIAN);
                BMR = parser.getFloatValue(BluetoothBytesParser.FORMAT_SFLOAT, ByteOrder.LITTLE_ENDIAN);
                muscle = parser.getFloatValue(BluetoothBytesParser.FORMAT_SFLOAT, ByteOrder.LITTLE_ENDIAN);
                softLeanMass = parser.getFloatValue(BluetoothBytesParser.FORMAT_SFLOAT, ByteOrder.LITTLE_ENDIAN);
                bodyWaterMass = parser.getFloatValue(BluetoothBytesParser.FORMAT_SFLOAT,ByteOrder.LITTLE_ENDIAN);
                impedance = parser.getFloatValue(BluetoothBytesParser.FORMAT_SFLOAT, ByteOrder.LITTLE_ENDIAN);
        }
    }
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"Weight: %.0f, Height: %.0f, BMI: %s,BodyFat: %.0f, Others: %.0f BMI,%.0f,%.0f,%.0f,%.0f, user %d at (%s)", weight,height,BMI,bodyFat,BMR,muscle,softLeanMass,bodyWaterMass,impedance, userID, timestamp);
    }
}
