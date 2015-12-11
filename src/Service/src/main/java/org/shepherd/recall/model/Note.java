package org.shepherd.recall.model;

import java.util.Date;

public class Note {

    public Note(){
        this._deleted = false;
    }
	public String get_note() {
		return _note;
	}
	public void set_note(String _note) {
		this._note = _note;
	}
	public long get_id() {
		return _id;
	}
	public void set_id(long _id) {
		this._id = _id;
	}
	public long get_contactid() {
		return _contactid;
	}
	public void set_contactid(long _contactid) {
		this._contactid = _contactid;
	}
	public Date get_createdOn() {
		return _createdOn;
	}
	public void set_createdOn(Date _createdOn) {
		this._createdOn = _createdOn;
	}


    public boolean get_deleted() {
        return _deleted;
    }
    public void set_deleted(boolean _deleted) {
        this._deleted = _deleted;
    }
    private String _note;
	private long _id;
	private long _contactid;
	private Date _createdOn;
    private boolean _deleted= false;
	
}
