package org.shepherd.recall.service;

import java.util.HashSet;
import java.util.Set;

import org.shepherd.recall.ble.BLEDevice;



public class RangeState {
	private Callback callback;
	private Set<BLEDevice> iBeacons = new HashSet<BLEDevice>();
	
	public RangeState(Callback c) {
		callback = c;		
	}
	
	public Callback getCallback() {
		return callback;
	}
	public void clearIBeacons() {
		synchronized (iBeacons) {
			iBeacons.clear();
		}
	}
	public Set<BLEDevice> getIBeacons() {
		return iBeacons;
	}
	public void addIBeacon(BLEDevice iBeacon) {
		synchronized (iBeacons) {
			iBeacons.add(iBeacon);
		}
	}
	

}
