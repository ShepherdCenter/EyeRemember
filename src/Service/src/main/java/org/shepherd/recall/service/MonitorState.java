package org.shepherd.recall.service;

import java.util.Date;

import org.shepherd.recall.service.Callback;

import android.util.Log;

public class MonitorState {
	private static final String TAG = "MonitorState";
	public static long INSIDE_EXPIRATION_MILLIS = 10000l;
	private boolean inside = false;
	private long lastSeenTime = 0l;
	private Callback callback;
	
	public MonitorState(Callback c) {
		callback = c;		
	}
	
	public Callback getCallback() {
		return callback;
	}

	// returns true if it is newly inside 
	public boolean markInside() {
		lastSeenTime = (new Date()).getTime();
		if (!inside) {
			inside = true;
			return true;
		}
		return false;
	}
	public boolean isNewlyOutside() {
		if (inside) {
			if (lastSeenTime > 0 && (new Date()).getTime() - lastSeenTime > INSIDE_EXPIRATION_MILLIS) {
				inside = false;
				Log.d(TAG, "We are newly outside the region because the lastSeenTime of "+lastSeenTime+" was "+((new Date()).getTime() - lastSeenTime)+" seconds ago, and that is over the expiration duration of  "+INSIDE_EXPIRATION_MILLIS);
				lastSeenTime = 0l;
				return true;
			}			
		}
		return false;		
	}
	public boolean isInside() {
		if (inside) {
			if (!isNewlyOutside()) {
				return true;
			}			
		}
		return false;
	}
}
