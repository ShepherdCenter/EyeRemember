package org.shepherd.recall.glass;

import android.app.Application;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import java.util.HashMap;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;

public class App extends Application {


    HashMap<String, Tracker> mTrackers = new HashMap<String, Tracker>();

    synchronized Tracker getTracker() {
        String trackerId = "analytics";
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker(R.xml.tracking);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

    public boolean canAddNotes() {

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        return mPrefs.getBoolean("notes", true);
    }
    public void trackView(String screenName) {
        // Get tracker.

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean track = mPrefs.getBoolean("analytics", true);
        if (track) {
            Tracker t = this.getTracker();

            String androidID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
            // Set screen name.
            t.setScreenName(androidID + "/" + screenName);

            // Send a screen view.
            t.send(new HitBuilders.AppViewBuilder().build());
        }
    }
}