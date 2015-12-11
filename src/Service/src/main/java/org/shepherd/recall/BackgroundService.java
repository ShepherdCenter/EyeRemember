package org.shepherd.recall;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.shepherd.recall.ble.BLEConsumer;
import org.shepherd.recall.ble.BLEDevice;
import org.shepherd.recall.model.Contact;
import org.shepherd.recall.service.BLEManager;
import org.shepherd.recall.service.MonitorNotifier;
import org.shepherd.recall.service.MonitoringData;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class BackgroundService extends Service implements BLEConsumer {

	private static final String LOG_TAG = BackgroundService.class.getName();
	public android.support.v4.app.NotificationManagerCompat mNotificationMgr;
	Context context;
	private BLEManager bleManager;
	List<BLEDevice> trackedDevices;
	List<Contact> contacts;
	DatabaseHandler db;
	
	public class LocalBinder extends Binder {
		BackgroundService getService() {
			return BackgroundService.this;
        }
    }
	
	private final IBinder mBinder = new LocalBinder();
	
	  @Override
	    public IBinder onBind(Intent intent) {
	        Log.i(LOG_TAG, "binding");
	        return mBinder;
	    }

	  @Override
	    public boolean onUnbind(Intent intent) {
	        Log.i(LOG_TAG, "unbinding");	        
	        return false;
	    }
	  
	  /**
	     * Initializes the service when it is first created
	     */
	    public void onCreate() {
	        super.onCreate();
	        
	            Log.v(LOG_TAG, "Service onCreate");
	            bleManager = BLEManager.getInstanceForApplication(this);
	            start();
	            mNotificationMgr = NotificationManagerCompat.from(this);
	            //(NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
	    }

	    public void start() {	    	
			bleManager.bind(this);
			bleManager.setBackgroundMode(this,true);
			trackedDevices = new ArrayList<BLEDevice>();
			db = new DatabaseHandler(this);
			contacts = db.getAllContacts();
			
		}
	    @Override
	    public void onDestroy() {
	    	stop();
	    	super.onDestroy();
	    }
		
		public void stop() {
			bleManager.setBackgroundMode(this,false);
			bleManager.unBind(this);
		}
		
		private boolean isNew(Contact c) {
			return true;
			// fix me
		}
		
		private void zzprocessDevices(List<BLEDevice> devices) {
			// check to see when I last saw a certain device
			String caption = "";
			String title = "Contact nearby";
			boolean foundNew = false;
			Iterator<BLEDevice> iter = devices.iterator();
        	while (iter.hasNext()) {
        		BLEDevice device = (BLEDevice)iter.next();
        		String deviceId = device.getProximityUuid();
        		for (Contact c : contacts) {
        			if (c.get_active()) {
	        		   if (c.get_device() == deviceId) {
	        			 // device found
	        			   if (isNew(c)) {   
	        				   caption += c.get_contactname();
	        				   foundNew = true;
	        			   }
	        			   // update last seen
	        			   c.set_lastseen(new Date());
        				   db.updateContact(c);
	        		   }
        			}
        		}
        		
        		//addDeviceToList(device);	        	  
        	}
			// if it was recent, send a push notification
	        if (foundNew) {
	        	 final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
	        	 Intent intent = new Intent();
	        	 PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);       
	            
	             //PendingIntent dpi = PendingIntent.getBroadcast(this, 0, deleteIntent, 0);
	             builder.setContentTitle(title);
	             builder.setContentText(caption);
	             builder.setSmallIcon(R.drawable.ic_launcher);
	             //builder.setContentIntent(pi);
	            // builder.setDeleteIntent(dpi);
	             //builder.setWhen(c.getLong(lastModColumnId));            
	             //expandedView.setOnClickPendingIntent(R.id.progress_layout_text, pi);
	             //builder.setContentIntent(pi).setAutoCancel(true);
	             //builder.setStyle(new Notification.BigTextStyle().bigText(longText))
	             Notification n = builder.build();
	             //n.setLatestEventInfo(mContext, title, caption, PendingIntent.getBroadcast(mContext, 0, intent, 0));
	             //n.icon = R.drawable.icondone;//android.R.drawable.stat_sys_download_done;
	             
	             //n.deleteIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
	
	             //n.when = c.getLong(lastModColumnId);
	
	             mNotificationMgr.notify(0, n);
	             Log.i(LOG_TAG, "Notification sent");
	        }
		}

		 @Override
		    public void onIBeaconServiceConnect() {
		    	bleManager.setMonitorNotifier(new MonitorNotifier() {
			        @Override
			        public void didEnter(final MonitoringData data) {
			        	// push notification
			        	Log.i(LOG_TAG, "Received devices: " + data.getDevices().size());
			        	//processDevices(data.getDevices());
			        	
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
		 
		
/* public Context getApplicationContext() {
			 return this.getApplicationContext();
		 }
		 
		 public void unbindService(ServiceConnection connection)
		 {
			 this.unbindService(connection);
		 }
		 
		 public boolean bindService(Intent intent, ServiceConnection connection, int mode)
		 {
			 return this.bindService(intent, connection, mode);
		 }
	*/    
	    
	    
	    
}
