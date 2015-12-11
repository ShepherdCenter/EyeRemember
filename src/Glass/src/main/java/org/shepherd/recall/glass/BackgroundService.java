package org.shepherd.recall.glass;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.shepherd.recall.DatabaseHandler;
import org.shepherd.recall.Helper;
import org.shepherd.recall.ble.BLEConsumer;
import org.shepherd.recall.ble.BLEDevice;
import org.shepherd.recall.model.Contact;
import org.shepherd.recall.service.BLEManager;
import org.shepherd.recall.service.MonitorNotifier;
import org.shepherd.recall.service.MonitoringData;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.widget.CardBuilder;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

public class BackgroundService extends Service implements BLEConsumer {

	private static final String LOG_TAG = BackgroundService.class.getName();
	Context context;
	private BLEManager bleManager;
	List<BLEDevice> trackedDevices;
	List<Contact> contacts;
	DatabaseHandler db;
	// For live card
    private LiveCard liveCard;
    int iBeaconDistance;
    long mScantime = 0;
    long mSleeptime = 0;
    long iBeaconFeet = 0;
    float mBeaconDistance = 0.0f;
    long mNotifyBreakTime = 0;
    private TextToSpeech mSpeech;
    
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
	            //(NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
	    }

	    private long getArrayValue(int array, int position) {
	    	String[] arr = this.getResources().getStringArray(array);
	    	return Long.parseLong(arr[position]);					
	    }
    private void getContacts() {
        if (db == null) {
            db = new DatabaseHandler(this);
        }
        contacts = db.getAllContacts();

    }
	    private void refreshSettings() {	    

			
			SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

            long oScantime = mScantime;
            long oSleeptime = mSleeptime;
            oScantime = getArrayValue(R.array.time_short_values, mPrefs.getInt("scantime", 0));
            oSleeptime = getArrayValue(R.array.time_short_values, mPrefs.getInt("sleeptime", 0));

            iBeaconDistance = mPrefs.getInt("minbeacondistance", 0);
            iBeaconFeet = getArrayValue(R.array.min_beacon_distance_values, iBeaconDistance);
            if (iBeaconFeet > 0) {
                mBeaconDistance = iBeaconFeet / (float)3.2808;
            }
			mNotifyBreakTime = getArrayValue(R.array.time_extra_long_values, mPrefs.getInt("notifytime", 0));

            if ((oScantime!=mScantime) || (oSleeptime!=mSleeptime)) {
                mScantime = oScantime;
                mSleeptime = oSleeptime;

                if (bleManager != null) {

                    try {

                        bleManager.setBackgroundBetweenScanPeriod(mSleeptime * 1000);
                        bleManager.setBackgroundScanPeriod(mScantime * 1000 + 200);
                        bleManager.updateScanPeriods();

                    } catch (RemoteException e) {
                    }


                }
            }
	    }

	    public void start() {
			bleManager.bind(this);		
			trackedDevices = new ArrayList<BLEDevice>();
            getContacts();
			refreshSettings();

            publishCard(this);

			mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
				@Override
				public void onInit(int status) {
					// Do nothing.
					
				}
			});
		}
	    
	    private void publishCard(Context context)
	    {
	        Log.d(LOG_TAG,"publishCard() called.");
	        if (liveCard == null) {
	            String cardId = "recall";
	            liveCard = new LiveCard(context, cardId);            	            //
	            //RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.livecard);
	            //view.setTextViewText(R.id.contact_info, "Service running");
	            //liveCard.setViews(view);
	            
	            updateLiveCard(context, new ArrayList<Contact>(), false);
	           
	            liveCard.publish(LiveCard.PublishMode.SILENT);

	        } else {
	            // Card is already published.
	            return;
	        }
	    }
	    
	    private void updateLiveCard(Context context, List<Contact> foundContacts, boolean navigate) {
	    	
	    	CardBuilder cb;

            RemoteViews vLiveCard;
			if (foundContacts.size() == 0) {
                vLiveCard = this.getInitialView();
                Intent intent = new Intent(context, MainActivity.class);
		         liveCard.setAction(PendingIntent.getActivity(context, 0, intent, 0));
			}
			else
			{
                vLiveCard = this.getContactView(foundContacts);
                Intent intent = new Intent(context, ContactsActivity.class);
		        liveCard.setAction(PendingIntent.getActivity(context, 0, intent, 0));

			}
			
			liveCard.setViews(vLiveCard);
	        if (navigate) {
	        	liveCard.navigate();
	        }
		}

    private RemoteViews getInitialView()
    {
        CardBuilder cb = new CardBuilder(this, CardBuilder.Layout.COLUMNS);
        cb.setText("Service running");
        cb.setIcon(R.drawable.ic_launcher);
        //cb.setFootnote(String.format("Scan %d seconds / Wait %d seconds", mScantime, mSleeptime,mNotifyBreakTime ));
        if (iBeaconFeet > 0) {
            cb.setFootnote(String.format("Scanning %d ft away", iBeaconFeet));
        }
        return cb.getRemoteViews();

    }


    private RemoteViews getContactView(List<Contact> foundContacts)
    {
        String cardText = "";
        String cardFooter = null;
        CardBuilder cb = new CardBuilder(this, CardBuilder.Layout.COLUMNS);
        cb.setFootnote(String.format("Locating contacts less than %d ft away", iBeaconFeet));
        cardFooter = String.format(Locale.US, "%d contact(s) near-by.", foundContacts.size());
        for (Contact c : foundContacts) {
            if (c.get_picture() != null) {
                // has picture
                cb.addImage(Helper.getImageFromByteArray(c.get_picture()));
            }
            double feet = c.getDistance() / 0.305;
            //cardText += String.format(Locale.US, "%s - %s  (%.1f ft)\n", c.get_contactname().split(" ")[0],c.get_relationship().toLowerCase(Locale.US),feet);
            cardText += String.format(Locale.US, "%s - %s\n", getName(c),c.get_relationship().toLowerCase(Locale.US));
        }
        cb.setText(cardText);
        cb.setFootnote(cardFooter);

        return cb.getRemoteViews();
    }

    public RemoteViews zzgetContactViewRows(List<Contact> foundContacts) {

        String cardFooter = String.format(Locale.US, "%d contact(s) near-by.", foundContacts.size());
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.contact_layout)
                .setFootnote(cardFooter);
        View view = card.getView();//convertView, parent);

        // Get a reference to an embedded view from the custom layout and then manipulate it.
        ViewGroup tableView = (ViewGroup) view.findViewById(R.id.simple_table);
        populateTableRows(tableView, foundContacts);

        return card.getRemoteViews();
    }


    /** Populates all of the rows in the card at the specified position. */
    private void populateTableRows(ViewGroup tableView,List<Contact> foundContacts) {

        for (int i = 0; i < foundContacts.size(); i++) {
            ViewGroup rowView = (ViewGroup) tableView.getChildAt(i);

                Contact item = foundContacts.get(i);
                populateTableRow(item, rowView);
                rowView.setVisibility(View.VISIBLE);
        }
    }

    /** Populates a row in the table with the specified item data. */
    private void populateTableRow(Contact item, ViewGroup rowView) {
        ImageView imageView = (ImageView) rowView.getChildAt(0);
        TextView primaryTextView = (TextView) rowView.getChildAt(1);
        TextView secondaryTextView = (TextView) rowView.getChildAt(2);

        imageView.setImageBitmap(Helper.getImageFromByteArray(item.get_picture()));
        primaryTextView.setText(item.get_contactname());
        secondaryTextView.setText(item.get_relationship());
    }

	    private void unpublishCard(Context context)
	    {
	        Log.d(LOG_TAG,"unpublishCard() called.");
	        if (liveCard != null) {
	            liveCard.unpublish();
	            liveCard = null;
	        }
	    }
	    
	    
	    @Override
	    public void onDestroy() {
	    	stop();
	    	super.onDestroy();
	    }
		
		public void stop() {
			unpublishCard(this);
			bleManager.setBackgroundMode(this,false);
			bleManager.unBind(this);
			mSpeech.shutdown();

			mSpeech = null;
			db.close();
			db = null;
		}


        private boolean withinDistance(BLEDevice d, double distance) {
            if ((iBeaconDistance == 0) || (distance < mBeaconDistance)) {
               return true;
            }
            return false;
        }

		private boolean isNew(Contact c) {
			Date lastSeen = c.get_lastseen();
			Date now = new Date();
			long seconds = (now.getTime()-lastSeen.getTime())/1000;
			if (seconds > mNotifyBreakTime) {
				return true;
			}
			else
			{
				return false;
			}			
		}
		
		
		private void processDevices(List<BLEDevice> devices) {
			// check to see when I last saw a certain device
			String caption = "";
			String title = "Contact nearby";
			ArrayList<Contact> foundContacts = new ArrayList<Contact>();
			ArrayList<Contact> newContacts = new ArrayList<Contact>();
			Iterator<BLEDevice> iter = devices.iterator();
        	while (iter.hasNext()) {
        		BLEDevice device = (BLEDevice)iter.next();
        		String deviceId = device.getProximityUuid();
                double distance = device.calculateDistance();
                Log.i(LOG_TAG, String.format("Found %s (%.1f m) away", device.getName(), distance));
                if (this.withinDistance(device, distance)) {
                    //Log.i(LOG_TAG, "Found device "+ deviceId);
                    for (Contact c : contacts) {
                        if (c.get_device().equals(deviceId)) {
                            // device found
                            Log.i(LOG_TAG, "Device/Contact match: " + c.get_device());
                            if (isNew(c)) {
                                Log.i(LOG_TAG, "Device/Contact is NEW: " + c.get_device());
                                newContacts.add(c);
                                ((App) this.getApplication()).trackView("contact/" + c.get_id() + "/notify");
                            }
                            // update last seen
                            c.set_lastseen(new Date());
                            c.set_rssi(device.getRssi());
                            c.setDistance(distance);
                            foundContacts.add(c);

                            db.updateContact(c);
                        }
                        //}
                    }
                } else
                {
                    double feet = distance / 0.305;
                    Log.i(LOG_TAG, String.format("Device %s is too far away (%.1f ft).", deviceId, feet));
                }
        		
        		//addDeviceToList(device);	        	  
        	}
        	
        	boolean navigateToCard = false;
			// if it was recent, send a push notification
	        if (newContacts.size() > 0) {
	        	AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	            am.playSoundEffect(Sounds.SUCCESS);
                ((App) this.getApplication()).trackView("notify/"+ newContacts.size());
	        	Contact c = newContacts.get(0);
	        	String notifyText = "";
                StringBuilder sb = new StringBuilder();
                for (int ci=0;ci<newContacts.size(); ci++) {
                    String name = getName(newContacts.get(ci)); // get first name
                    sb.append(name);
                    if (ci < newContacts.size()-1) {
                        sb.append(" and ");
                    }
                }
                sb.append(newContacts.size() == 1 ? " is nearby" : " are nearby");

	        	mSpeech.speak(sb.toString(), TextToSpeech.QUEUE_ADD, null);

	             Log.i(LOG_TAG, "Notification sent");
	             navigateToCard = true;
	        }
	        updateLiveCard(this,foundContacts,navigateToCard);
		}

    private String getName(Contact c) {
        String names[] = c.get_contactname().split(" ");
        if (names.length == 1) {
            return names[0];
        }
        else
        {
            String lastName = names[1];
            if (lastName.length()< 4) {
                return names[0] + " " + names[1];
            }
            else {
                return names[0];
            }
        }
    }

    private List<BLEDevice> removeDupes(List<BLEDevice> devices)
    {
        ArrayList<BLEDevice> newDeviceList = new ArrayList<BLEDevice>();

        for(BLEDevice d: devices) {
            if (!newDeviceList.contains(d)) {
                newDeviceList.add(d);
            }
        }
        return newDeviceList;
    }

		private List<BLEDevice> zremoveDupes(List<BLEDevice> devices)
		{
			ArrayList<BLEDevice> newDeviceList = new ArrayList<BLEDevice>();

            for(BLEDevice d: devices) {
                double distance = d.calculateDistance();
                Log.i(LOG_TAG, String.format("%s is %.1f far away", d.getName(), distance));
                if ((mBeaconDistance < 1) || (distance < mBeaconDistance)) {
                    if (!newDeviceList.contains(d)) {
                        newDeviceList.add(d);
                    }
                }
            }
	        return newDeviceList;
		}
		
		 @Override
		    public void onIBeaconServiceConnect() {
		    	bleManager.setMonitorNotifier(new MonitorNotifier() {
			        @Override
			        public void didEnter(final MonitoringData data) {
			        	// push notification
			        	Log.i(LOG_TAG, "Received devices: " + data.getDevices().size());
			        	processDevices(removeDupes(data.getDevices()));
                        refreshSettings();
		
			        }
		        		       
		        });

		        try {
 
		        	bleManager.setBackgroundBetweenScanPeriod(mSleeptime*1000);
		        	bleManager.setBackgroundScanPeriod(mScantime*1000+200);
		        	bleManager.updateScanPeriods();
		        	bleManager.setBackgroundMode(this,true);
		        	bleManager.startMonitoringBeaconsInRegion("background");        		        	

		        } catch (RemoteException e) {   }
		    }
		 
	    
	    
}
