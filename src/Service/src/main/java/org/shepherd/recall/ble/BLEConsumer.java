package org.shepherd.recall.ble;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

public interface BLEConsumer {
	/**
	 * Called when the iBeacon service is running and ready to accept your commands through the IBeaconManager
	 */
	public void onIBeaconServiceConnect();
	/**
	 * Called by the IBeaconManager to get the context of your Service or Activity.  This method is implemented by Service or Activity.
	 * You generally should not override it.
	 * @return the application context of your service or activity
	 */
	public Context getApplicationContext();
	/**
	 * Called by the IBeaconManager to bind your IBeaconConsumer to the  IBeaconService.  This method is implemented by Service or Activity, and
	 * You generally should not override it.
	 * @return the application context of your service or activity
	 */
	public void unbindService(ServiceConnection connection);
	/**
	 * Called by the IBeaconManager to unbind your IBeaconConsumer to the  IBeaconService.  This method is implemented by Service or Activity, and
	 * You generally should not override it.
	 * @return the application context of your service or activity
	 */
	public boolean bindService(Intent intent, ServiceConnection connection, int mode);
}
