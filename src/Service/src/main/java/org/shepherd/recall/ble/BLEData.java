package org.shepherd.recall.ble;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import android.os.Parcel;
import android.os.Parcelable;

public class BLEData extends BLEDevice implements Parcelable {
    public BLEData(BLEDevice iBeacon) {
    	super(iBeacon);
    }
    public static Collection<BLEData> fromBLEDevices(Collection<BLEDevice> iBeacons) {
    	ArrayList<BLEData> iBeaconDatas = new ArrayList<BLEData>();
    	Iterator<BLEDevice> iBeaconIterator = iBeacons.iterator();
    	while (iBeaconIterator.hasNext()) {
    		iBeaconDatas.add(new BLEData(iBeaconIterator.next()));
    	}    	
    	return iBeaconDatas;
    }
    public static Collection<BLEDevice> fromBLEDatas(Collection<BLEData> iBeaconDatas) {
    	ArrayList<BLEDevice> iBeacons = new ArrayList<BLEDevice>();
        if (iBeaconDatas != null) {
            Iterator<BLEData> iBeaconIterator = iBeaconDatas.iterator();
            while (iBeaconIterator.hasNext()) {
                iBeacons.add(iBeaconIterator.next());
            }
        }
    	return iBeacons;
    }

	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(proximityUuid);
        out.writeInt(getProximity());
        out.writeDouble(getAccuracy());
        out.writeInt(rssi);
        out.writeInt(txPower);
        out.writeString(bluetoothAddress);
        out.writeString(deviceName);
    }

    public static final Parcelable.Creator<BLEData> CREATOR
            = new Parcelable.Creator<BLEData>() {
        public BLEData createFromParcel(Parcel in) {
            return new BLEData(in);
        }

        public BLEData[] newArray(int size) {
            return new BLEData[size];
        }
    };
    
    private BLEData(Parcel in) {       
        proximityUuid = in.readString();
        proximity = in.readInt();
        accuracy = in.readDouble();
        rssi = in.readInt();
        txPower = in.readInt();
        bluetoothAddress = in.readString();
        deviceName = in.readString();
    }
}
