package org.shepherd.recall.service;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

public class Callback {
	private String TAG = "Callback";
	private Messenger messenger;
	private Intent intent;
	public Callback(String intentPackageName) {
		if (intentPackageName != null) {
			intent = new Intent();
            intent.setComponent(new ComponentName(intentPackageName, "IBeaconIntentProcessor"));
        }
	}
	public Intent getIntent() {
		return intent;
	}
	public void setIntent(Intent intent) {
		this.intent = intent;
	}
	/**
	 * Tries making the callback, first via messenger, then via intent
	 * 
	 * @param context
	 * @param dataName
	 * @param data
	 * @return false if it callback cannot be made
	 */
	public boolean call(Context context, String dataName, MonitoringData data) {
	//public boolean call(Context context, String dataName, Parcelable data) {
		if (intent != null) {
			
			
			//Log.d(TAG, "attempting callback via intent: "+intent.getComponent());
			//intent.putExtra(dataName, data.);
			//context.startService(intent);
			
			
			MonitorNotifier notifier = BLEManager.getInstanceForApplication(context).getMonitoringNotifier();
			notifier.didEnter(data);
//			if (notifier != null) {
//				MonitoringData monitoringData = (MonitoringData)data;
//				if (BLEManager.debug) Log.d(TAG, "Calling monitoring notifier:"+notifier);
//				//notifier.didDetermineStateForRegion(monitoringData.isInside() ? MonitorNotifier.INSIDE : MonitorNotifier.OUTSIDE, monitoringData.getDevice());
//				if (monitoringData.isInside()) {
//					notifier.didEnter(monitoringData.getDevice());
//				}
//				else {
//					notifier.didExit(monitoringData.getDevice());					
//				}
//					
//			}
			
			
			return true;			
		}
		return false;
	}
}
