package org.shepherd.recall.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class BLEDevice implements Parcelable {

    private static final String LOG_TAG = BLEDevice.class.getName();

	public int describeContents() {
        return 0;
    }
	/**
	 * Less than half a meter away
	 */
	public static final int PROXIMITY_IMMEDIATE = 1;
	/**
	 * More than half a meter away, but less than four meters away
	 */
	public static final int PROXIMITY_NEAR = 2;
	/**
	 * More than four meters away
	 */
	public static final int PROXIMITY_FAR = 3;
	/**
	 * No distance estimate was possible due to a bad RSSI value or measured TX power
	 */
	public static final int PROXIMITY_UNKNOWN = 0;

    final private static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	private static final String TAG = "BLEDevice";	
		
    /**
     * A 16 byte UUID that typically represents the company owning a number of iBeacons
     * Example: E2C56DB5-DFFB-48D2-B060-D0F5A71096E0 
     */
	protected String proximityUuid;

	/**
	 * An integer with four possible values representing a general idea of how far the iBeacon is away
	 * @see #PROXIMITY_IMMEDIATE
	 * @see #PROXIMITY_NEAR
	 * @see #PROXIMITY_FAR
	 * @see #PROXIMITY_UNKNOWN
	 */
	protected Integer proximity;
	/**
	 * A double that is an estimate of how far the iBeacon is away in meters.  This name is confusing, but is copied from
	 * the iOS7 SDK terminology.   Note that this number fluctuates quite a bit with RSSI, so despite the name, it is not
	 * super accurate.   It is recommended to instead use the proximity field, or your own bucketization of this value. 
	 */
	protected Double accuracy;
	/**
	 * The measured signal strength of the Bluetooth packet that led do this iBeacon detection.
	 */

    /**
     * A double that is an estimate of how far the Beacon is away in meters.   Note that this number
     * fluctuates quite a bit with RSSI, so despite the name, it is not super accurate.
     */
    protected Double mDistance;

	protected int rssi;
	/**
	 * The calibrated measured Tx power of the iBeacon in RSSI
	 * This value is baked into an iBeacon when it is manufactured, and
	 * it is transmitted with each packet to aid in the distance estimate
	 */
	protected int txPower;

    /**
     * The bluetooth mac address
     */
    protected String bluetoothAddress;
	
    /**
     * The bluetooth device name
     */
    protected String deviceName;
    
    protected BluetoothDevice bluetoothDevice;
    
	/**
	 * If multiple RSSI samples were available, this is the running average
	 */
	protected Double runningAverageRssi = null;

    /**
     * Sets the running average rssi for use in distance calculations
     * @param rssi the running average rssi
     */
    public void setRunningAverageRssi(double rssi) {
        runningAverageRssi = rssi;
        mDistance = null; // force calculation of accuracy and proximity next time they are requested
    }

	 public void writeToParcel(Parcel out, int flags) {	        
	        out.writeString(proximityUuid);
	        out.writeString(bluetoothAddress);
	        out.writeString(deviceName);
	    }

//	    public static final Parcelable.Creator<BLEDevice> CREATOR
//	            = new Parcelable.Creator<RegionData>() {
//	        public RegionData createFromParcel(Parcel in) {
//	            return new RegionData(in);
//	        }
//
//	        public RegionData[] newArray(int size) {
//	            return new RegionData[size];
//	        }
//	    };
	    
	    private BLEDevice(Parcel in) { 	    	
		   	 
		   	 proximityUuid = in.readString();
		   	bluetoothAddress = in.readString();
		   	deviceName = in.readString();
		   	 
	    }
	
	
	
	/**
	 * @see #accuracy
	 * @return accuracy
	 */
	public double getAccuracy() {
		if (accuracy == null) {
			accuracy = calculateAccuracy(txPower, runningAverageRssi != null ? runningAverageRssi : rssi );		
		}
		return accuracy;
	}


    public double calculateDistance() {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        Log.d(TAG, String.format("calculating distance based on mRssi of %s and txPower of %s", rssi, txPower));


        double mCoefficient1 = 0.42093;
        double mCoefficient2 = 6.9476;
        double mCoefficient3 = 0.54992;
        double ratio = rssi*1.0/txPower;
        double distance;
        if (ratio < 1.0) {
            distance =  Math.pow(ratio,10);
        }
        else {
            distance =  (mCoefficient1)*Math.pow(ratio,mCoefficient2) + mCoefficient3;
        }
        Log.d(TAG, String.format("avg mRssi: %s distance: %s", rssi, distance));
        return distance;
    }

    /**
	 * @see #proximity
	 * @return proximity
	 */
	public int getProximity() {
		if (proximity == null) {
			proximity = calculateProximity(getAccuracy());		
		}
		return proximity;		
	}

	/**
	 * @see #rssi
	 * @return rssi
	 */
	public int getRssi() {
		return rssi;
	}
	/**
	 * @see #txPower
	 * @return txPowwer
	 */
	public int getTxPower() {
		return txPower;
	}

	/**
	 * @see #proximityUuid
	 * @return proximityUuid
	 */
	public String getProximityUuid() {
		return proximityUuid;
	}

    /**
     * @see #bluetoothAddress
     * @return bluetoothAddress
     */
    public String getAddress() {
        return bluetoothAddress;
    }

    public String getName() {
    	return this.deviceName;
    }


	
	/**
	 * Two detected iBeacons are considered equal if they share the same three identifiers, regardless of their distance or RSSI.
	 */
	@Override
	public boolean equals(Object that) {
		if (!(that instanceof BLEDevice)) {
			return false;
		}
		BLEDevice thatIBeacon = (BLEDevice) that;		
		return (thatIBeacon.bluetoothAddress.equals(this.bluetoothAddress));
	}

	/**
	 * Construct an iBeacon from a Bluetooth LE packet collected by Android's Bluetooth APIs,
     * including the raw bluetooth device info
	 * 
	 * @param scanData The actual packet bytes
	 * @param rssi The measured signal strength of the packet
     * @param device The bluetooth device that was detected
	 * @return An instance of an <code>IBeacon</code>
	 */
    @TargetApi(5)
	public static BLEDevice fromScanData(byte[] scanData, int rssi, BluetoothDevice device) {
    	
    	BLEDevice bleDevice = new BLEDevice();
    	bleDevice.bluetoothDevice= device;
    	bleDevice.deviceName = device.getName();    	
    	bleDevice.bluetoothAddress = device.getAddress();
    	bleDevice.rssi = rssi;
        boolean isIBeacon = false;
        //Log.i(LOG_TAG, String.format("%s found", device.getName()));
        	
		int startByte = 2;
		boolean patternFound = false;
		while (startByte <= 5) {
			if (((int)scanData[startByte+2] & 0xff) == 0x02 &&
				((int)scanData[startByte+3] & 0xff) == 0x15) {			
				// yes!  This is an iBeacon
                bleDevice.txPower = (int)scanData[startByte+24]; // this one is signed
                isIBeacon = true;
				patternFound = true;
				break;
			}
			else if (((int)scanData[startByte] & 0xff) == 0x2d &&
					((int)scanData[startByte+1] & 0xff) == 0x24 &&
					((int)scanData[startByte+2] & 0xff) == 0xbf &&
					((int)scanData[startByte+3] & 0xff) == 0x16) {
                //if (BLEManager.debug) Log.d(TAG, "This is a proprietary Estimote beacon advertisement that does not meet the iBeacon standard.  Identifiers cannot be read.");
				bleDevice.deviceName = "Estimote";
			}
            else if (((int)scanData[startByte] & 0xff) == 0xad &&
                     ((int)scanData[startByte+1] & 0xff) == 0x77 &&
                     ((int)scanData[startByte+2] & 0xff) == 0x00 &&
                     ((int)scanData[startByte+3] & 0xff) == 0xc6) {
                    //if (BLEManager.debug) Log.d(TAG, "This is a proprietary Gimbal beacon advertisement that does not meet the iBeacon standard.  Identifiers cannot be read.");
            	bleDevice.deviceName = "Gimbal";
            }
            else if (((int)scanData[startByte] & 0xff) ==6 &&
                    ((int)scanData[startByte+1] & 0xff) == 3 &&
                    ((int)scanData[startByte+2] & 0xff) == 3 &&
                    ((int)scanData[startByte+3] & 0xff) == 237) {
                bleDevice.deviceName = "Tile";
            }
			startByte++;
		}
        if (bleDevice.deviceName == null) {
            bleDevice.deviceName = (isIBeacon) ? "iBeacon" : "Unknown";
        }

        if (bleDevice.getName().equals("Smart Key")) {
            bleDevice.txPower = (int)scanData[19];
        }
        if (bleDevice.txPower == 0) {
            bleDevice.txPower = -68;
        }
		bleDevice.proximityUuid = bleDevice.getName()+" " +bleDevice.getAddress();
		return bleDevice;
		/*
		if (patternFound == false) {
			// This is not an iBeacon
			//if (BLEManager.debug) Log.d(TAG, "This is not an iBeacon advertisment (no 0215 seen in bytes 4-7).  The bytes I see are: "+bytesToHex(scanData));
			BLEDevice iBeacon = new BLEDevice();
            iBeacon.major = 0;
            iBeacon.minor = 0;
            iBeacon.deviceName = device.getName();
            iBeacon.proximityUuid = device.getName()+" " +device.getAddress();
            iBeacon.bluetoothAddress = device.getAddress();
            iBeacon.txPower = -55;
            return iBeacon;
			//return null;
		}
								
		BLEDevice iBeacon = new BLEDevice();
		
		iBeacon.major = (scanData[startByte+20] & 0xff) * 0x100 + (scanData[startByte+21] & 0xff);
		iBeacon.minor = (scanData[startByte+22] & 0xff) * 0x100 + (scanData[startByte+23] & 0xff);
		iBeacon.txPower = (int)scanData[startByte+24]; // this one is signed
		iBeacon.rssi = rssi;
				
		// AirLocate:
		// 02 01 1a 1a ff 4c 00 02 15  # Apple's fixed iBeacon advertising prefix
		// e2 c5 6d b5 df fb 48 d2 b0 60 d0 f5 a7 10 96 e0 # iBeacon profile uuid
		// 00 00 # major 
		// 00 00 # minor 
		// c5 # The 2's complement of the calibrated Tx Power

		// Estimote:		
		// 02 01 1a 11 07 2d 24 bf 16 
		// 394b31ba3f486415ab376e5c0f09457374696d6f7465426561636f6e00000000000000000000000000000000000000000000000000

		byte[] proximityUuidBytes = new byte[16];
		System.arraycopy(scanData, startByte+4, proximityUuidBytes, 0, 16); 
		String hexString = bytesToHex(proximityUuidBytes);
		StringBuilder sb = new StringBuilder();
		sb.append(hexString.substring(0,8));
		sb.append("-");
		sb.append(hexString.substring(8,12));
		sb.append("-");
		sb.append(hexString.substring(12,16));
		sb.append("-");
		sb.append(hexString.substring(16,20));
		sb.append("-");
		sb.append(hexString.substring(20,32));
		iBeacon.proximityUuid = sb.toString();

        if (device != null) {
            iBeacon.bluetoothAddress = device.getAddress();
        }

		return iBeacon;
		*/
    	
	}
	

	protected BLEDevice(BLEDevice otherIBeacon) {		
		this.accuracy = otherIBeacon.accuracy;
		this.proximity = otherIBeacon.proximity;
		this.rssi = otherIBeacon.rssi;
		this.proximityUuid = otherIBeacon.proximityUuid;
		this.txPower = otherIBeacon.txPower;
        this.bluetoothAddress = otherIBeacon.bluetoothAddress;
	}
	
	protected BLEDevice() {
		
	}

	protected BLEDevice(String proximityUuid, int txPower, int rssi) {
		this.proximityUuid = proximityUuid.toLowerCase();
		this.rssi = rssi;
		this.txPower = txPower;
	}
	
	public BLEDevice(String proximityUuid) {
		this.proximityUuid = proximityUuid.toLowerCase();		
		this.txPower = -59;
		this.rssi = 0;
	}

	protected static double calculateAccuracy(int txPower, double rssi) {
		if (rssi == 0) {
			return -1.0; // if we cannot determine accuracy, return -1.
		}
		
		//if (BLEManager.debug) Log.d(TAG, "calculating accuracy based on rssi of "+rssi);


		double ratio = rssi*1.0/txPower;
		if (ratio < 1.0) {
			return Math.pow(ratio,10);
		}
		else {
			double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;	
			//if (BLEManager.debug) Log.d(TAG, " avg rssi: "+rssi+" accuracy: "+accuracy);
			return accuracy;
		}
	}	
	
	protected static int calculateProximity(double accuracy) {
		if (accuracy < 0) {
			return PROXIMITY_UNKNOWN;	 
			// is this correct?  does proximity only show unknown when accuracy is negative?  I have seen cases where it returns unknown when
			// accuracy is -1;
		}
		if (accuracy < 0.5 ) {
			return BLEDevice.PROXIMITY_IMMEDIATE;
		}
		// forums say 3.0 is the near/far threshold, but it looks to be based on experience that this is 4.0
		if (accuracy <= 4.0) { 
			return BLEDevice.PROXIMITY_NEAR;
		}
		// if it is > 4.0 meters, call it far
		return BLEDevice.PROXIMITY_FAR;

	}

	private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    } 
}
