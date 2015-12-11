package org.shepherd.recall.service;
import java.util.HashSet;
import java.util.List;

import org.shepherd.recall.ble.BLEDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class MonitoringData {// implements Parcelable {
	@SuppressWarnings("unused")
	private static final String TAG = "MonitoringData";
	private boolean inside;
	
	List<BLEDevice> trackedDevices;
	
	public MonitoringData (boolean inside, List<BLEDevice> devices) {
		this.inside = inside;
		this.trackedDevices = devices;
	}
	public boolean isInside() {
		return inside;
	}
	public List<BLEDevice> getDevices() {
		return trackedDevices;
	}
	
	
    public void writeToParcel(Parcel out, int flags) {    
    	out.writeByte((byte) (inside ? 1 : 0));  
    	out.writeList(trackedDevices);

    }

    public static final Parcelable.Creator<MonitoringData> CREATOR
            = new Parcelable.Creator<MonitoringData>() {
        public MonitoringData createFromParcel(Parcel in) {
            return new MonitoringData(in);
        }

        public MonitoringData[] newArray(int size) {
            return new MonitoringData[size];
        }
    };
    
    private MonitoringData(Parcel in) {
    	inside = in.readByte() == 1;
    	in.readList(trackedDevices,this.getClass().getClassLoader());
    }
}
