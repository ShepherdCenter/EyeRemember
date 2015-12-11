package org.shepherd.recall;

import org.ocpsoft.prettytime.PrettyTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Helper {

	private static final String LOG_TAG = Helper.class.getName();
	
	public static String getFriendlyDate(Date date) {
		PrettyTime p = new PrettyTime();
		return p.format(date);

     
	}
	
	public static String getDayOfWeek(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("E", Locale.US);
	 	if (date != null) {
	 		return sdf.format(date);
	 	} else {
	 		return "???";
	 	}
 
	}
	
	public static String getDate(Date date) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.US);
     	if (date != null) {
     		return sdf.format(date);
     	} else {
     		return "Unknown";
     	}
     
	}
	
	public static Bitmap getImageFromByteArray(byte[] picture) {
		Bitmap bmp;
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inMutable = true;
    	bmp = BitmapFactory.decodeByteArray(picture, 0, picture.length);//, options);
    	return bmp;
	}
	
	public static boolean isServiceRunning(Context context, String serviceName) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	 
	    for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	      
	    	//Log.d(LOG_TAG,"Service: " + service.service.getClassName() + "; " + service.pid + "; " + service.clientCount + "; " + service.foreground + "; " + service.process);
	      
	 
	      if(serviceName.equals(service.service.getClassName())) {
	        return true;
	      }
	    }
	 
	    return false;
	  }
}
