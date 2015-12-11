package org.shepherd.recall.service;

public interface MonitorNotifier {
	/**
	 * Indicates the Android device is inside the Region of iBeacons
	 */
	public static final int INSIDE = 1;
	/**
	 * Indicates the Android device is outside the Region of iBeacons
	 */
	public static final int OUTSIDE = 0;
	
	/**
	 * Called when at least one iBeacon in a <code>Region</code> is visible.
	 * @param region a Region that defines the criteria of iBeacons to look for
	 */
	public void didEnter(MonitoringData data);

	/**
	 * Called when no iBeacons in a <code>Region</code> are visible.
	 * @param region a Region that defines the criteria of iBeacons to look for
	 */
	//public void didExit(BLEDevice device);
	
	/**
	 * Called with a state value of MonitorNotifier.INSIDE when at least one iBeacon in a <code>Region</code> is visible.
	 * Called with a state value of MonitorNotifier.OUTSIDE when no iBeacons in a <code>Region</code> are visible.
	 * @param state either MonitorNotifier.INSIDE or MonitorNotifier.OUTSIDE
	 * @param region a Region that defines the criteria of iBeacons to look for
	 */
	//public void didDetermineStateForRegion(int state, Region region);
}
