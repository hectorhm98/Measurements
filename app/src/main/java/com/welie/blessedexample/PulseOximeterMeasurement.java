package com.welie.blessedexample;

import com.welie.blessed.BluetoothBytesParser;

import java.io.Serializable;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
<<<<<<< HEAD
=======
import android.util.Log;
>>>>>>> origin/master
import java.util.Locale;
import android.bluetooth.BluetoothGattCharacteristic;
import com.welie.blessed.BluetoothPeripheral;


import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SFLOAT;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

public class PulseOximeterMeasurement implements Serializable
{
    public int header;
    public int yearIni;
    public int monthIni;
    public int dayIni;
    public int hourIni;
    public int minuteIni;
    public int secondIni;
    public int yearFin;
    public int monthFin;
    public int dayFin;
    public int hourFin;
    public int minuteFin;
    public int secondFin;
    public int storageTime;
    public int storageTimeAvg;
    public int spo2Max;
    public int spo2Min;
    public int spo2;
    public int pulseRateMax;
    public int pulseRateMin;
    public int pulseRate;
    public String timestampHour;
    public String timestampDay;
    public int packetNum;
    public int checksum;
    public Date timestamp;


    public PulseOximeterMeasurement(byte[] value, int packetNumber)
    {
        BluetoothBytesParser parser = new BluetoothBytesParser(value);
        switch(packetNumber)
        {
            case(1):
                header = parser.getIntValue(FORMAT_UINT8);
                packetNum = parser.getIntValue(FORMAT_UINT8);
                yearIni = 2000 + parser.getIntValue(FORMAT_UINT8);
                monthIni = parser.getIntValue(FORMAT_UINT8);
                dayIni = parser.getIntValue(FORMAT_UINT8);
                hourIni = parser.getIntValue(FORMAT_UINT8);
                minuteIni = parser.getIntValue(FORMAT_UINT8);
                secondIni = parser.getIntValue(FORMAT_UINT8);
                yearFin = 2000 + parser.getIntValue(FORMAT_UINT8);
                monthFin = parser.getIntValue(FORMAT_UINT8);
                dayFin = parser.getIntValue(FORMAT_UINT8);
                hourFin = parser.getIntValue(FORMAT_UINT8);
                minuteFin = parser.getIntValue(FORMAT_UINT8);
                secondFin = parser.getIntValue(FORMAT_UINT8);
                storageTime = parser.getIntValue(FORMAT_UINT8);
                storageTimeAvg = parser.getIntValue(BluetoothBytesParser.FORMAT_UINT16, ByteOrder.LITTLE_ENDIAN);
                spo2Max = parser.getIntValue(FORMAT_UINT8);
                spo2Min = parser.getIntValue(FORMAT_UINT8);
                spo2 = parser.getIntValue(FORMAT_UINT8);
                break;
            case(2):
                pulseRateMax = parser.getIntValue(FORMAT_UINT8);
                pulseRateMin = parser.getIntValue(FORMAT_UINT8);
                pulseRate = parser.getIntValue(FORMAT_UINT8);
                checksum = parser.getIntValue(FORMAT_UINT8);
<<<<<<< HEAD
                break;
        }



=======
                Log.d("Debug", String.valueOf(pulseRate));
                break;
        }

>>>>>>> origin/master
        timestampHour = hourIni + "" + ":" + minuteIni + "" + ":" + secondIni + "";
        timestampDay = dayIni + "" + "/" + monthIni + "" + "/" + yearIni + "";
        timestamp = Calendar.getInstance().getTime();
    }
    @Override
    public String toString()
    {
<<<<<<< HEAD
        return String.format(Locale.ENGLISH,"%.0f/%.0f at %s", spo2, pulseRate,timestamp);
=======
        return String.format(Locale.ENGLISH,"%d/%d at %s", spo2, pulseRate,timestamp);
>>>>>>> origin/master

    }
}
