package org.shepherd.recall;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.shepherd.recall.ble.BLEConsumer;
import org.shepherd.recall.ble.BLEDevice;
import org.shepherd.recall.service.BLEManager;
import org.shepherd.recall.service.MonitorNotifier;
import org.shepherd.recall.service.MonitoringData;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

public class BackgroundProcess implements BLEConsumer {

	Context context;

	private static final String LOG_TAG = BackgroundProcess.class.getName();
	
    private BLEManager bleManager;
    
	public BackgroundProcess (Context context) {
		this.context = context;
		bleManager = BLEManager.getInstanceForApplication(this.context);
	}
	public void start() {
		bleManager.bind(this);
		bleManager.setBackgroundMode(this,true);		
		
	}
	
	public void stop() {
		bleManager.setBackgroundMode(this,false);
	}
	
	private void processDevices(List<BLEDevice> devices) {
		// check to see when I last saw a certain device
		
		// if it was recent, send a push notification
		
	}

	 @Override
	    public void onIBeaconServiceConnect() {
	    	bleManager.setMonitorNotifier(new MonitorNotifier() {
		        @Override
		        public void didEnter(final MonitoringData data) {
		        	// push notification
		        	Log.i(LOG_TAG, "Received devices: " + data.getDevices().size());
		        	processDevices(data.getDevices());
		        	
	//	        	DeviceScanListFragment.this.getActivity().runOnUiThread(new Runnable() {
	//	                @Override
	//	                public void run() {
	//	                	HashSet<BLEDevice> devices = data.getDevices();
	//	    	        	Iterator<BLEDevice> iter = devices.iterator();
	//	    	        	while (iter.hasNext()) {
	//	    	        		BLEDevice device = (BLEDevice)iter.next();
	//	    	        		addDeviceToList(device);	        	  
	//	    	        	}
	//	    	        	leDeviceListAdapter.notifyDataSetChanged();
	//	    	        	getActivity().setProgressBarIndeterminateVisibility(false);
	//	    	        	bleManager.unBind(DeviceScanListFragment.this);	       
	//	    		        try {
	//	    		        	bleManager.stopMonitoringBeaconsInRegion("myMonitoringUniqueId");
	//	    		        }
	//	    		        catch (Exception e) {
	//	    		        	Log.e(LOG_TAG, e.toString());
	//	    		        }
	//	                }
	//	            });
		        }
	        		       
	        });

	        try {
	        	// Scan every 5 secs
	        	bleManager.setBackgroundBetweenScanPeriod(15000);
	        	bleManager.setBackgroundScanPeriod(2000);
	        	bleManager.updateScanPeriods();
	        	bleManager.startMonitoringBeaconsInRegion("background");        		        	

	        } catch (RemoteException e) {   }
	    }
	 
	

	 public Context getApplicationContext() {
		 return this.context.getApplicationContext();
	 }
	 
	 public void unbindService(ServiceConnection connection)
	 {
		 this.context.unbindService(connection);
	 }
	 
	 public boolean bindService(Intent intent, ServiceConnection connection, int mode)
	 {
		 return this.context.bindService(intent, connection, mode);
	 }
	
}
