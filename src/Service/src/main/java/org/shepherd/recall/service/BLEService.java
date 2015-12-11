package org.shepherd.recall.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.radiusnetworks.bluetooth.BluetoothCrashResolver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.shepherd.recall.ble.BLEDevice;


@TargetApi(5)
public class BLEService extends Service {
    public static final String TAG = "BLEService";

    private Map<String, RangeState> rangedRegionState = new HashMap<String, RangeState>();
    private Map<String, MonitorState> monitoredRegionState = new HashMap<String, MonitorState>();
    private BluetoothAdapter bluetoothAdapter;
    private boolean scanning;
    private boolean scanningPaused;
    private Date lastIBeaconDetectionTime = new Date();
    private List<BLEDevice> trackedBeacons;
    int trackedBeaconsPacketCount;
    private Handler handler = new Handler();
    private int bindCount = 0;
    private BluetoothCrashResolver bluetoothCrashResolver;
    private boolean scanCyclerStarted = false;
    private boolean scanningEnabled = false;

    /*
     * The scan period is how long we wait between restarting the BLE advertisement scans
     * Each time we restart we only see the unique advertisements once (e.g. unique iBeacons)
     * So if we want updates, we have to restart.  iOS gets updates once per second, so ideally we
     * would restart scanning that often to get the same update rate.  The trouble is that when you 
     * restart scanning, it is not instantaneous, and you lose any iBeacon packets that were in the 
     * air during the restart.  So the more frequently you restart, the more packets you lose.  The
     * frequency is therefore a tradeoff.  Testing with 14 iBeacons, transmitting once per second,
     * here are the counts I got for various values of the SCAN_PERIOD:
     * 
     * Scan period     Avg iBeacons      % missed
     *    1s               6                 57
     *    2s               10                29
     *    3s               12                14
     *    5s               14                0
     *    
     * Also, because iBeacons transmit once per second, the scan period should not be an even multiple
     * of seconds, because then it may always miss a beacon that is synchronized with when it is stopping
     * scanning.
     * 
     */

    private long scanPeriod = BLEManager.DEFAULT_FOREGROUND_SCAN_PERIOD;
    private long betweenScanPeriod = BLEManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;

    private List<BLEDevice> simulatedScanData = null;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class IBeaconBinder extends Binder {
        public BLEService getService() {
            Log.i(TAG, "getService of IBeaconBinder called");
            // Return this instance of LocalService so clients can call public methods
            return BLEService.this;
        }
    }


    /**
     * Command to the service to display a message
     */
    public static final int MSG_START_RANGING = 2;
    public static final int MSG_STOP_RANGING = 3;
    public static final int MSG_START_MONITORING = 4;
    public static final int MSG_STOP_MONITORING = 5;
    public static final int MSG_SET_SCAN_PERIODS = 6;


    static class IncomingHandler extends Handler {
        private final WeakReference<BLEService> mService;

        IncomingHandler(BLEService service) {
            mService = new WeakReference<BLEService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            BLEService service = mService.get();
            StartRMData startRMData = (StartRMData) msg.obj;
//
            if (service != null) {
                switch (msg.what) {
//                    case MSG_START_RANGING:
//                        Log.i(TAG, "start ranging received");
//                        service.startRangingBeaconsInRegion(startRMData.getRegionData(), new org.shepherd.recall.service.Callback(startRMData.getCallbackPackageName()));
//                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
//                        break;
//                    case MSG_STOP_RANGING:
//                        Log.i(TAG, "stop ranging received");
//                        service.stopRangingBeaconsInRegion(startRMData.getRegionData());
//                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
//                        break;
                    case MSG_START_MONITORING:
                        Log.i(TAG, "start monitoring received");
                        service.startMonitoringBeaconsInRegion(startRMData.getRegionData(), new org.shepherd.recall.service.Callback(startRMData.getCallbackPackageName()));
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
                        break;
                    case MSG_STOP_MONITORING:
                        Log.i(TAG, "stop monitoring received");
                        service.stopMonitoringBeaconsInRegion(startRMData.getRegionData());
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
                        break;
                    case MSG_SET_SCAN_PERIODS:
                        Log.i(TAG, "set scan intervals received");
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
                        break;
                    default:
                        super.handleMessage(msg);
                }
           }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "binding");
        bindCount++;
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "unbinding");
        bindCount--;
        return false;
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "BLEService is starting up");
        getBluetoothAdapter();
        bluetoothCrashResolver = new BluetoothCrashResolver(this);
        bluetoothCrashResolver.start();

    }

    @Override
    @TargetApi(18)
    public void onDestroy() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.");
            return;
        }
        bluetoothCrashResolver.stop();
        Log.i(TAG, "onDestroy called.  stopping scanning");
        handler.removeCallbacksAndMessages(null);
        scanLeDevice(false);
        if (bluetoothAdapter != null) {
            bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback)getLeScanCallback());
            lastScanEndTime = new Date().getTime();
        }
    }

    private int ongoing_notification_id = 1;

    /* 
     * Returns true if the service is running, but all bound clients have indicated they are in the background
     */
    private boolean isInBackground() {
        if (BLEManager.debug) Log.d(TAG, "bound client count:" + bindCount);
        return bindCount == 0;
    }

    /**
     * methods for clients
     */
//
//    public void startRangingBeaconsInRegion(String region, Callback callback) {
//        synchronized (rangedRegionState) {
//            if (rangedRegionState.containsKey(region)) {
//                Log.i(TAG, "Already ranging that region -- will replace existing region.");
//                rangedRegionState.remove(region); // need to remove it, otherwise the old object will be retained because they are .equal
//            }
//            rangedRegionState.put(region, new RangeState(callback));
//        }
//        if (BLEManager.debug)
//            Log.d(TAG, "Currently ranging " + rangedRegionState.size() + " regions.");
//        if (!scanningEnabled) {
//            enableScanning();
//        }
//    }
//
//    public void stopRangingBeaconsInRegion(String region) {
//        synchronized (rangedRegionState) {
//            rangedRegionState.remove(region);
//        }
//        if (BLEManager.debug)
//            Log.d(TAG, "Currently ranging " + rangedRegionState.size() + " regions.");
//
//        if (scanningEnabled && rangedRegionState.size() == 0 && monitoredRegionState.size() == 0) {
//            disableScanning();
//        }
//    }

    public void startMonitoringBeaconsInRegion(String region, Callback callback) {
        if (BLEManager.debug) Log.d(TAG, "startMonitoring called");
        synchronized (monitoredRegionState) {
            if (monitoredRegionState.containsKey(region)) {
                Log.i(TAG, "Already monitoring that region -- will replace existing region monitor.");
                monitoredRegionState.remove(region); // need to remove it, otherwise the old object will be retained because they are .equal
            }
            monitoredRegionState.put(region, new MonitorState(callback));
        }
        if (BLEManager.debug)
            Log.d(TAG, "Currently monitoring " + monitoredRegionState.size() + " regions.");
        if (!scanningEnabled) {
            enableScanning();
        }
    }

    public void stopMonitoringBeaconsInRegion(String region) {
        if (BLEManager.debug) Log.d(TAG, "stopMonitoring called");
        synchronized (monitoredRegionState) {
            monitoredRegionState.remove(region);
        }
        if (BLEManager.debug)
            Log.d(TAG, "Currently monitoring " + monitoredRegionState.size() + " regions.");
        if (scanningEnabled && rangedRegionState.size() == 0 && monitoredRegionState.size() == 0) {
            disableScanning();
        }
    }

    public void setScanPeriods(long scanPeriod, long betweenScanPeriod) {
        this.scanPeriod = scanPeriod;
        this.betweenScanPeriod = betweenScanPeriod;
        long now = new Date().getTime();
        if (nextScanStartTime > now) {
            // We are waiting to start scanning.  We may need to adjust the next start time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedNextScanStartTime = (lastScanEndTime + betweenScanPeriod);
            if (proposedNextScanStartTime < nextScanStartTime) {
                nextScanStartTime = proposedNextScanStartTime;
                Log.i(TAG, "Adjusted nextScanStartTime to be " + new Date(nextScanStartTime));
            }
        }
        if (scanStopTime > now) {
            // we are waiting to stop scanning.  We may need to adjust the stop time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedScanStopTime = (lastScanStartTime + scanPeriod);
            if (proposedScanStopTime < scanStopTime) {
                scanStopTime = proposedScanStopTime;
                Log.i(TAG, "Adjusted scanStopTime to be " + new Date(scanStopTime));
            }
        }
    }

    private long lastScanStartTime = 0l;
    private long lastScanEndTime = 0l;
    private long nextScanStartTime = 0l;
    private long scanStopTime = 0l;

    public void enableScanning() {
        scanningEnabled = true;
        if (!scanCyclerStarted) {
            scanLeDevice(true);
        }
    }
    public void disableScanning() {
        scanningEnabled = false;
    }

    @TargetApi(18)
    private void scanLeDevice(final Boolean enable) {
        scanCyclerStarted = true;
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.");
            return;
        }
        if (getBluetoothAdapter() == null) {
            Log.e(TAG, "No bluetooth adapter.  BLEService cannot scan.");
            if ((simulatedScanData == null)) {
                Log.w(TAG, "exiting");
                return;
            } else {
                Log.w(TAG, "proceeding with simulated scan data");
            }
        }
        if (enable) {
            long millisecondsUntilStart = nextScanStartTime - (new Date().getTime());
            if (millisecondsUntilStart > 0) {
                if (BLEManager.debug)
                    Log.d(TAG, "Waiting to start next bluetooth scan for another " + millisecondsUntilStart + " milliseconds");
                // Don't actually wait until the next scan time -- only wait up to 1 second.  this
                // allows us to start scanning sooner if a consumer enters the foreground and expects
                // results more quickly
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanLeDevice(true);
                    }
                }, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
                return;
            }

            trackedBeacons = new ArrayList<BLEDevice>();
            trackedBeaconsPacketCount = 0;
            if (scanning == false || scanningPaused == true) {
                scanning = true;
                scanningPaused = false;
                try {
                    if (getBluetoothAdapter() != null) {
                        if (getBluetoothAdapter().isEnabled()) {
                            if (bluetoothCrashResolver.isRecoveryInProgress()) {
                                Log.w(TAG, "Skipping scan because crash recovery is in progress.");
                            }
                            else {
                                if (scanningEnabled) {
                                    getBluetoothAdapter().startLeScan((BluetoothAdapter.LeScanCallback)getLeScanCallback());
                                }
                                else {
                                    if (BLEManager.debug) Log.d(TAG, "Scanning unnecessary - no monitoring or ranging active.");
                                }
                            }
                            lastScanStartTime = new Date().getTime();
                        } else {
                            Log.w(TAG, "Bluetooth is disabled.  Cannot scan for iBeacons.");
                        }
                    }
                } catch (Exception e) {
                    Log.e("TAG", "Exception starting bluetooth scan.  Perhaps bluetooth is disabled or unavailable?");
                }
            } else {
                if (BLEManager.debug) Log.d(TAG, "We are already scanning");
            }
            scanStopTime = (new Date().getTime() + scanPeriod);
            scheduleScanStop();

            if (BLEManager.debug) Log.d(TAG, "Scan started");
        } else {
            if (BLEManager.debug) Log.d(TAG, "disabling scan");
            scanning = false;
            if (getBluetoothAdapter() != null) {
                getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback)getLeScanCallback());
                lastScanEndTime = new Date().getTime();
            }
        }
    }

    private void scheduleScanStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = scanStopTime - (new Date().getTime());
        if (millisecondsUntilStop > 0) {
            if (BLEManager.debug)
                Log.d(TAG, "Waiting to stop scan for another " + millisecondsUntilStop + " milliseconds");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scheduleScanStop();
                }
            }, millisecondsUntilStop > 1000 ? 1000 : millisecondsUntilStop);
        } else {
            finishScanCycle();
        }


    }

    @TargetApi(18)
    private void finishScanCycle() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.");
            return;
        }
        if (BLEManager.debug) Log.d(TAG, "Done with scan cycle");
        
        
        Iterator<String> monitoredRegionIterator = monitoredRegionState.keySet().iterator();
	      while (monitoredRegionIterator.hasNext()) {
	      	String region = monitoredRegionIterator.next();
	          MonitorState state = monitoredRegionState.get(region);
	          state.getCallback().call(BLEService.this, "monitoringData", new MonitoringData(state.isInside(), new ArrayList<BLEDevice>(trackedBeacons)));
	      }
        
        processExpiredMonitors();
        if (scanning == true) {
            processRangeData();
                      
            if (getBluetoothAdapter() != null) {
                if (getBluetoothAdapter().isEnabled()) {
                    getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback)getLeScanCallback());
                    lastScanEndTime = new Date().getTime();
                } else {
                    Log.w(TAG, "Bluetooth is disabled.  Cannot scan for iBeacons.");
                }
            }

            if (!anyRangingOrMonitoringRegionsActive()) {
                if (BLEManager.debug)
                    Log.d(TAG, "Not starting scan because no monitoring or ranging regions are defined.");
                scanCyclerStarted = false;
            } else {
                if (BLEManager.debug)
                    Log.d(TAG, "Restarting scan.  Unique beacons seen last cycle: " + trackedBeacons.size()+" Total iBeacon advertisement packets seen: "+trackedBeaconsPacketCount);

                scanningPaused = true;
                nextScanStartTime = (new Date().getTime() + betweenScanPeriod);
                if (scanningEnabled) {
                    scanLeDevice(true);
                }
                else {
                    if (BLEManager.debug) Log.d(TAG, "Scanning disabled.  No ranging or monitoring regions are active.");
                    scanCyclerStarted = false;
                }
            }
        }
    }

    private Object leScanCallback;
    @TargetApi(18)
    private Object getLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback =
                    new BluetoothAdapter.LeScanCallback() {

                        @Override
                        public void onLeScan(final BluetoothDevice device, final int rssi,
                                             final byte[] scanRecord) {
                            if (BLEManager.debug) Log.d(TAG, "got record " + device.getName());
                            new ScanProcessor().execute(new ScanData(device, rssi, scanRecord));

                        }
                    };
        }
        return leScanCallback;
    }

    private class ScanData {
        public ScanData(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }

        @SuppressWarnings("unused")
        public BluetoothDevice device;
        public int rssi;
        public byte[] scanRecord;
    }

    private void processRangeData() {
//        Iterator<String> regionIterator = rangedRegionState.keySet().iterator();
//        while (regionIterator.hasNext()) {
//        	String region = regionIterator.next();
//            RangeState rangeState = rangedRegionState.get(region);
//            if (BLEManager.debug)
//                Log.d(TAG, "Calling ranging callback with " + rangeState.getIBeacons().size() + " iBeacons");
//            rangeState.getCallback().call(BLEService.this, "rangingData", new RangingData(rangeState.getIBeacons(), region));
//            synchronized (rangeState) {
//            	rangeState.clearIBeacons();
//			}
//        }

    }

    private void processExpiredMonitors() {
//        Iterator<String> monitoredRegionIterator = monitoredRegionState.keySet().iterator();
//        while (monitoredRegionIterator.hasNext()) {
//        	String region = monitoredRegionIterator.next();
//            MonitorState state = monitoredRegionState.get(region);
//            if (state.isNewlyOutside()) {
//                if (BLEManager.debug) Log.d(TAG, "found a monitor that expired: " + region);
//                state.getCallback().call(BLEService.this, "monitoringData", new MonitoringData(state.isInside(), region));
//            }
//        }
    }

    private void processIBeaconFromScan(BLEDevice device) {
        lastIBeaconDetectionTime = new Date();
        trackedBeaconsPacketCount++;
        if (trackedBeacons.contains(device)) {
//            if (BLEManager.debug) Log.d(TAG,
//                    "Bluetooth device detected multiple times in scan cycle :" + device.getProximityUuid() + " "
//                            + " accuracy: " + device.getAccuracy()
//                            + " proximity: " + device.getProximity());
            // remove first device
            // we should probably calculate the running RSSI
            //trackedBeacons.remove(device);

        }
        trackedBeacons.add(device);
        
//        Iterator<String> monitoredRegionIterator = monitoredRegionState.keySet().iterator();
//	      while (monitoredRegionIterator.hasNext()) {
//	      	String region = monitoredRegionIterator.next();
//	          MonitorState state = monitoredRegionState.get(region);
//	          state.getCallback().call(BLEService.this, "monitoringData", new MonitoringData(state.isInside(), trackedBeacons));	          
//	      }
        
//        if (BLEManager.debug) Log.d(TAG,
//                "iBeacon detected :" + iBeacon.getProximityUuid() + " "
//                        + iBeacon.getMajor() + " " + iBeacon.getMinor()
//                        + " accuracy: " + iBeacon.getAccuracy()
//                        + " proximity: " + iBeacon.getProximity());
//
//        List<Region> matchedRegions = null;
//        synchronized(monitoredRegionState) {
//            matchedRegions = matchingRegions(iBeacon,
//                    monitoredRegionState.keySet());
//        }
//        Iterator<Region> matchedRegionIterator = matchedRegions.iterator();
//        while (matchedRegionIterator.hasNext()) {
//            Region region = matchedRegionIterator.next();
//            MonitorState state = monitoredRegionState.get(region);
//            if (state.markInside()) {
//                state.getCallback().call(BLEService.this, "monitoringData",
//                        new MonitoringData(state.isInside(), region));
//            }
//        }
//
//        if (BLEManager.debug)
//            Log.d(TAG, "looking for ranging region matches for this ibeacon");
//        synchronized (rangedRegionState) {
//            matchedRegions = matchingRegions(iBeacon, rangedRegionState.keySet());
//        }
//        matchedRegionIterator = matchedRegions.iterator();
//        while (matchedRegionIterator.hasNext()) {
//            Region region = matchedRegionIterator.next();
//            if (BLEManager.debug) Log.d(TAG, "matches ranging region: " + region);
//            RangeState rangeState = rangedRegionState.get(region);
//            synchronized (rangeState) {
//            	rangeState.addIBeacon(iBeacon);
//			}
//        }
    }

    private class ScanProcessor extends AsyncTask<ScanData, Void, Void> {

        @Override
        protected Void doInBackground(ScanData... params) {
            ScanData scanData = params[0];

            BLEDevice iBeacon = BLEDevice.fromScanData(scanData.scanRecord,
                    scanData.rssi, scanData.device);

            if (iBeacon != null) {
                processIBeaconFromScan(iBeacon);
            }
            bluetoothCrashResolver.notifyScannedDevice(scanData.device, (BluetoothAdapter.LeScanCallback)getLeScanCallback());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

//    private List<Region> matchingRegions(BLEDevice iBeacon, Collection<Region> regions) {
//        List<Region> matched = new ArrayList<Region>();
//            Iterator<Region> regionIterator = regions.iterator();
//            while (regionIterator.hasNext()) {
//                Region region = regionIterator.next();
//                if (region.matchesIBeacon(iBeacon)) {
//                    matched.add(region);
//                } else {
//                    if (BLEManager.debug) Log.d(TAG, "This region does not match: " + region);
//                }
//
//            }
//
//        return matched;
//    }

    /*
     Returns false if no ranging or monitoring regions have beeen requested.  This is useful in determining if we should scan at all.
     */
    private boolean anyRangingOrMonitoringRegionsActive() {
        return (rangedRegionState.size() + monitoredRegionState.size()) > 0;
    }

    @TargetApi(18)
    private BluetoothAdapter getBluetoothAdapter() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.");
            return null;
        }
        if (bluetoothAdapter == null) {
            // Initializes Bluetooth adapter.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        return bluetoothAdapter;
    }

}