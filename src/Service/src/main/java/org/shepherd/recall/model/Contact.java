package org.shepherd.recall.model;

import java.util.Date;

import org.shepherd.recall.ble.BLEDevice;

public class Contact {
    
	    
    //private variables
    long _id;
    String _device;
    String _contactname;
    String _relationship;
    byte[] _picture;
  
	int _rssi;
    int _txPower;

    public double getDistance() {
        return _distance;
    }

    public void setDistance(double distance) {
        this._distance = distance;
    }

    double _distance;
    String _notes;
    boolean _active;
    String _googlecontactid; // for future

	Date _lastseen;
     
    // Empty constructor
    public Contact(){
         
    }
    // constructor
    public Contact(long id, boolean active, BLEDevice device, String contact, String googlecontactid, String relationship, byte[] picture, String notes, Date lastseen) {
    	this._id = id;
        this._active = active;
        this._device = device.getProximityUuid();
        this._rssi = device.getRssi();
        this._txPower = device.getTxPower();
        this._contactname = contact;
        this._picture = picture;
        this._relationship = relationship;
        this._notes = notes;
        this._googlecontactid = googlecontactid;
        this._lastseen = lastseen;
    }
    
    public Contact(long id, boolean active, String device, int rssi, int txPower, String contact, String googlecontactid, String relationship, byte[] picture, String notes, Date lastseen) {
    
        this._id = id;
        this._active = active;
        this._device = device;
        this._rssi = rssi;
        this._txPower = txPower;
        this._contactname = contact;
        this._picture = picture;
        this._relationship = relationship;
        this._notes = notes;
        this._googlecontactid = googlecontactid;
        this._lastseen = lastseen;        
    }
     
 // constructor
    public Contact(BLEDevice device, String contact) {        
        this._device = device.getProximityUuid();
        this._rssi = device.getRssi();
        this._txPower = device.getTxPower();
        this._contactname = contact;               
    }
    
    public int get_rssi() {
  		return _rssi;
  	}
  	public void set_rssi(int _rssi) {
  		this._rssi = _rssi;
  	}
  	public int get_txPower() {
  		return _txPower;
  	}
  	public void set_txPower(int _txPower) {
  		this._txPower = _txPower;
  	}
    
    public long get_id() {
		return _id;
	}
	public void set_id(long _id) {
		this._id = _id;
	}
	
	public boolean get_active() {
		return _active;
	}
	public void set_active(boolean _active) {
		this._active = _active;
	}
	
	public String get_device() {
		return _device;
	}
	public void set_device(String _device) {
		this._device = _device;
	}
	public String get_contactname() {
		return _contactname;
	}
	public void set_contactname(String _contactname) {
		this._contactname = _contactname;
	}
    public String get_googlecontactid() {
		return _googlecontactid;
	}
	public void set_googlecontactid(String _googlecontactid) {
		this._googlecontactid = _googlecontactid;
	}
	public String get_relationship() {
		return _relationship;
	}
	public void set_relationship(String _relationship) {
		this._relationship = _relationship;
	}
	public byte[] get_picture() {
		return _picture;
	}
	public void set_picture(byte[] _picture) {
		this._picture = _picture;
	}
	public String get_notes() {
		return _notes;
	}
	public void set_notes(String _notes) {
		this._notes = _notes;
	}
	public Date get_lastseen() {
		return _lastseen;
	}
	public void set_lastseen(Date _lastseen) {
		this._lastseen = _lastseen;
	}

	
}