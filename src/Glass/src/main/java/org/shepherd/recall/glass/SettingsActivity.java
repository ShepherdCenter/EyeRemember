package org.shepherd.recall.glass;

import org.shepherd.recall.glass.settings.GlassPreferenceActivity;
import org.shepherd.recall.glass.settings.option.OptionsBuilder;

import android.os.Bundle;

public class SettingsActivity  extends GlassPreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		


//
		String[] timeShort = this.getResources().getStringArray(R.array.time_short);
		String[] timeELong = this.getResources().getStringArray(R.array.time_extra_long);

        String[] beaconTimes = this.getResources().getStringArray(R.array.min_beacon_distance);

		OptionsBuilder scanTimeOptions = new OptionsBuilder();
        OptionsBuilder scanSleepOptions = new OptionsBuilder();
		for (String s: timeShort)
	    {
			scanTimeOptions.addOption(s);
            scanSleepOptions.addOption(s);
	    }
		addChoicePreference("scantime", "Scan Time", "This setting controls how long the application will scan for contacts before resting", scanTimeOptions.build());
		addChoicePreference("sleeptime", "Sleep Between Scan", "This setting controls how long the application will rest before scanning again", scanSleepOptions.build());

        OptionsBuilder beaconOptions = new OptionsBuilder();
        for (String s: beaconTimes)
        {
            beaconOptions.addOption(s);
        }
		OptionsBuilder notifyInternalOptions = new OptionsBuilder();
		for (String s: timeELong)
	    {
			notifyInternalOptions.addOption(s);
	    }		
		addChoicePreference("notifytime", "Notification Away Time", "This setting controls how long a contact has been away before the application will notify you they have returned.", notifyInternalOptions.build());
        addChoicePreference("minbeacondistance", "Contact Distance", "This setting controls the minimum distance a contact must be from you before the application sees them", beaconOptions.build());
        this.addTogglePreference("notes", "Add notes for contacts", true);
		this.addTogglePreference("runonstart", "Scan When Glass Starts", false);
        this.addTogglePreference("analytics", "Toggle Reporting", true);
		
		// Builds all the preferences and shows them in a CardScrollView
		buildAndShowOptions();
	}


}
