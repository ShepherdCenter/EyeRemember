package org.shepherd.recall;

import java.lang.reflect.Constructor;

import org.shepherd.recall.ble.BLEDevice;
import org.shepherd.recall.ble.BLEData;
import org.shepherd.recall.service.BLEManager;
import org.shepherd.recall.service.MonitorNotifier;
import org.shepherd.recall.service.MonitoringData;


import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.util.Log;

@TargetApi(3)
public class IBeaconIntentProcessor extends IntentService {
	private static final String TAG = "IBeaconIntentProcessor";
	private boolean initialized = false;

	public IBeaconIntentProcessor() {
		super("IBeaconIntentProcessor");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (BLEManager.debug) Log.d(TAG, "got an intent to process");
//		
//		MonitoringData monitoringData = null;
//		RangingData rangingData = null;
//		
//		if (intent != null && intent.getExtras() != null) {
//			monitoringData = (MonitoringData) intent.getExtras().get("monitoringData");
//			rangingData = (RangingData) intent.getExtras().get("rangingData");			
//		}
//		
//		if (rangingData != null) {
//			if (BLEManager.debug) Log.d(TAG, "got ranging data");
//            if (rangingData.getIBeacons() == null) {
//                Log.w(TAG, "Ranging data has a null iBeacons collection");
//            }
//			RangeNotifier notifier = BLEManager.getInstanceForApplication(this).getRangingNotifier();
//            java.util.Collection<BLEDevice> iBeacons = BLEData.fromBLEDatas(rangingData.getIBeacons());
//			if (notifier != null) {
//				notifier.didRangeBeaconsInRegion(iBeacons, rangingData.getRegion());
//			}
//            else {
//                if (BLEManager.debug) Log.d(TAG, "but ranging notifier is null, so we're dropping it.");
//            }
//            RangeNotifier dataNotifier = BLEManager.getInstanceForApplication(this).getDataRequestNotifier();
//            if (dataNotifier != null) {
//                dataNotifier.didRangeBeaconsInRegion(iBeacons, rangingData.getRegion());
//            }
//
//		}
//		if (monitoringData != null) {
//			if (BLEManager.debug) Log.d(TAG, "got monitoring data");
//			MonitorNotifier notifier = BLEManager.getInstanceForApplication(this).getMonitoringNotifier();
//			if (notifier != null) {
//				if (BLEManager.debug) Log.d(TAG, "Calling monitoring notifier:"+notifier);
//				notifier.didDetermineStateForRegion(monitoringData.isInside() ? MonitorNotifier.INSIDE : MonitorNotifier.OUTSIDE, monitoringData.getRegion());
//				if (monitoringData.isInside()) {
//					notifier.didEnterRegion(monitoringData.getRegion());
//				}
//				else {
//					notifier.didExitRegion(monitoringData.getRegion());					
//				}
//					
//			}
//		}
//				
	}

}
