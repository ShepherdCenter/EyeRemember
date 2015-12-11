package org.shepherd.recall.glass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootUpReceiver extends BroadcastReceiver{

	private static final String LOG_TAG = BootUpReceiver.class.getName();
    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    	
    	boolean start = mPrefs.getBoolean("runonstart", false);
		Log.i(LOG_TAG, "Boot up received");
           /***** For start Service  ****/
		if (start) {  
            Intent myIntent = new Intent(context, BackgroundService.class);
            context.startService(myIntent);
		}
    }   

}