package org.shepherd.recall;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.shepherd.recall.model.Contact;
import org.shepherd.recall.model.Note;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

	 // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 4;
 
    // Database Name
    private static final String DATABASE_NAME = "recall";
 
    // Contacts table name
    private static final String TABLE_CONTACTS = "contacts";

    private static final String TABLE_NOTES = "notes";
 
    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_DEVICE = "device";
    private static final String KEY_CONTACT_NAME= "name";
    private static final String KEY_GOOGLE_CONTACT_ID= "googlecontactid";
    private static final String KEY_RELATIONSHIP= "relationship";
    private static final String KEY_PICTURE= "picture";
    private static final String KEY_NOTES= "notes";
    private static final String KEY_ACTIVE= "active";
    private static final String KEY_LAST_SEEN= "lastseen";
    private static final String KEY_RSSI= "rssi";
    private static final String KEY_TXPOWER= "txpower";
    
    
    private static final String NOTE_KEY_ID = "id";
    private static final String NOTE_CONTACT_ID = "contactid";
    private static final String NOTE_NOTE = "note";
    private static final String NOTE_DELETED = "deleted";
    private static final String NOTE_CREATED_ON = "createdate";
    
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
 
    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY autoincrement," 
        		+ KEY_DEVICE + " TEXT,"
        		+ KEY_ACTIVE + " INTEGER,"
        		+ KEY_RSSI + " INTEGER,"
        		+ KEY_TXPOWER + " INTEGER,"
        		+ KEY_CONTACT_NAME + " TEXT,"
        		+ KEY_GOOGLE_CONTACT_ID + " TEXT,"
        		+ KEY_RELATIONSHIP + " TEXT,"
        		+ KEY_PICTURE + " BLOB,"
        		+ KEY_NOTES + " TEXT,"
                + KEY_LAST_SEEN + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
        
        String CREATE_NOTES_TABLE = "CREATE TABLE " + TABLE_NOTES + "("
                + NOTE_KEY_ID + " INTEGER PRIMARY KEY autoincrement,"         	
        		+ NOTE_CONTACT_ID + " INTEGER,"
        		+ NOTE_CREATED_ON + " TEXT,"
                + NOTE_NOTE + " TEXT,"
                + NOTE_DELETED + " INTEGER"+ ")";
        db.execSQL(CREATE_NOTES_TABLE);
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 4){
            Log.w(DatabaseHandler.class.getName(),
                    "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ".");
            // add  column
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + NOTE_DELETED + " INTEGER");
            // set all to not deleted
            db.execSQL("UPDATE " + TABLE_NOTES + " SET " + NOTE_DELETED + "=0");
        }
    }
 
    public void wipeData() {
    	SQLiteDatabase db = this.getWritableDatabase();
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        // Create tables again
        onCreate(db);
    }
    
    private String dateToSqliteDateString(Date date) {
        // The format is the same as CURRENT_TIMESTAMP: "YYYY-MM-DD HH:MM:SS"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (date != null) {
          return sdf.format(date);
        }
        return null;
      }
    
    private Date SqliteDateStringToDate(String stringTime) {

        SimpleDateFormat formatter;
        Date time = null;
        if (stringTime == null) return null;
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        try {
			time =  (Date) formatter.parse(stringTime);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        

        return time;
      }
    
    
    // Adding new contact
       public void addNote(Note note) {
       	
       	 SQLiteDatabase db = this.getWritableDatabase();
      
       	    ContentValues values = new ContentValues();
       	    values.put(NOTE_CONTACT_ID, note.get_contactid()); 
       	    values.put(NOTE_NOTE, note.get_note());
        	  values.put(NOTE_DELETED, note.get_deleted());
           	    values.put(NOTE_CREATED_ON,  dateToSqliteDateString(note.get_createdOn()));
       	        	 
       	    // Inserting Row
       	    long row  = db.insert(TABLE_NOTES, null, values);
       	    note.set_id(row);
       	    db.close(); // Closing database connection
       	
       }
       
    // Getting All Contacts
    public List<Note> getAllNotesForContact(long contactid) {
        return getAllNotesForContact(contactid, true);
    }
       public List<Note> getAllNotesForContact(long contactid, boolean active) {
       	List<Note> notes = new ArrayList<Note>();
           // Select All Query
           String selectQuery = "SELECT  * FROM " + TABLE_NOTES + " WHERE "+ NOTE_CONTACT_ID + "=? AND " + NOTE_DELETED +" =? ORDER BY datetime(" + NOTE_CREATED_ON + ") DESC ";
        
           SQLiteDatabase db = this.getWritableDatabase();
           Cursor cursor = db.rawQuery(selectQuery, new String[] { String.valueOf(contactid), (active) ? "0" : "1" });
        
           // looping through all rows and adding to list
           if (cursor.moveToFirst()) {
               do {
                   Note n = new Note();
                   n.set_id(Integer.parseInt(cursor.getString(0)));
                   n.set_contactid(Integer.parseInt(cursor.getString(1)));
                   n.set_createdOn(SqliteDateStringToDate(cursor.getString(2)));               
                   n.set_note(cursor.getString(3));                
                   
                   notes.add(n);
               } while (cursor.moveToNext());
           }
           cursor.close();
           db.close();
           return notes;
       	
       	
       }
 // Adding new contact
    public void addContact(Contact contact) {
    	
    	 SQLiteDatabase db = this.getWritableDatabase();
    	 
    	    ContentValues values = new ContentValues();
    	    values.put(KEY_DEVICE, contact.get_device()); 
    	    values.put(KEY_RSSI, contact.get_rssi());
    	    values.put(KEY_TXPOWER, contact.get_txPower());
    	    values.put(KEY_ACTIVE, contact.get_active());     	   
    	    values.put(KEY_CONTACT_NAME, contact.get_contactname());
    	    values.put(KEY_GOOGLE_CONTACT_ID, contact.get_googlecontactid());
    	    values.put(KEY_RELATIONSHIP, contact.get_relationship());
    	    values.put(KEY_PICTURE, contact.get_picture());
    	    values.put(KEY_NOTES,  contact.get_notes());    	    
    	    values.put(KEY_LAST_SEEN,  dateToSqliteDateString(contact.get_lastseen()));
    	        	 
    	    // Inserting Row
    	    long row  = db.insert(TABLE_CONTACTS, null, values);
    	    contact.set_id(row);
    	    db.close(); // Closing database connection
    	
    }
     
    // Getting single contact
    public Contact getContact(long id) {
    	  SQLiteDatabase db = this.getReadableDatabase();
    	  
    	    Cursor cursor = db.query(TABLE_CONTACTS, new String[] { KEY_ID,
    	    		KEY_ACTIVE, KEY_DEVICE, KEY_CONTACT_NAME, KEY_GOOGLE_CONTACT_ID, KEY_RELATIONSHIP, KEY_PICTURE,  KEY_NOTES, KEY_LAST_SEEN,KEY_RSSI, KEY_TXPOWER }, KEY_ID + "=?",
    	            new String[] { String.valueOf(id) }, null, null, null, null);
    	    if (cursor != null)
    	        cursor.moveToFirst();
    	 
    	    Date dLastSeen = SqliteDateStringToDate(cursor.getString(8));
    	    Contact contact = new Contact(Integer.parseInt(cursor.getString(0)),
    	            (cursor.getInt(1) != 0), cursor.getString(2), cursor.getInt(9), cursor.getInt(10),cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getBlob(6), cursor.getString(7), dLastSeen);
    	    // return contact
    	    db.close();
    	    return contact;
    	
    	
    }
     
    // Getting All Contacts
    public List<Contact> getAllContacts() {
    	List<Contact> contactList = new ArrayList<Contact>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS + " ORDER BY datetime(" + KEY_LAST_SEEN + ") DESC";
     
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
     
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.set_id(Integer.parseInt(cursor.getString(0)));
                contact.set_device(cursor.getString(1));
                contact.set_rssi(cursor.getInt(2));
                contact.set_txPower(cursor.getInt(3));
                contact.set_active((cursor.getInt(4) != 0));
                contact.set_contactname(cursor.getString(5));
                contact.set_googlecontactid(cursor.getString(6));
                contact.set_relationship(cursor.getString(7));
                contact.set_picture(cursor.getBlob(8));
                contact.set_notes(cursor.getString(9));                
                contact.set_lastseen(SqliteDateStringToDate(cursor.getString(10)));
                
                // Adding contact to list
                contactList.add(contact);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
     
        // return contact list
        return contactList;
    	
    	
    }
     
    // Getting contacts Count
    public int getContactsCount() {
    	 String countQuery = "SELECT  * FROM " + TABLE_CONTACTS;
         SQLiteDatabase db = this.getReadableDatabase();
         Cursor cursor = db.rawQuery(countQuery, null);
         cursor.close();
         db.close();
         // return count
         return cursor.getCount();
    	
    }
    // Updating single contact
    public int updateContact(Contact contact) {
    	 SQLiteDatabase db = this.getWritableDatabase();
    	 
    	    ContentValues values = new ContentValues();
    	    values.put(KEY_DEVICE, contact.get_device());
    	    values.put(KEY_RSSI, contact.get_rssi()); 
    	    values.put(KEY_TXPOWER, contact.get_txPower()); 
    	    values.put(KEY_ACTIVE, (contact.get_active() ? 1 : 0));
    	    values.put(KEY_CONTACT_NAME, contact.get_contactname());
    	    values.put(KEY_GOOGLE_CONTACT_ID, contact.get_googlecontactid());
    	    values.put(KEY_RELATIONSHIP, contact.get_relationship());
    	    values.put(KEY_PICTURE, contact.get_picture());
    	    values.put(KEY_NOTES,  contact.get_notes());    	    
    	    values.put(KEY_LAST_SEEN,  dateToSqliteDateString(contact.get_lastseen()));
    	 
    	    // updating row
    	    return db.update(TABLE_CONTACTS, values, KEY_ID + " = ?",
    	            new String[] { String.valueOf(contact.get_id()) });
    	
    }
     
    // Deleting single contact
    public void deleteContact(Contact contact) {
    	deleteContact(contact.get_id());
    }
    
    public void deleteNotesForContact(long contactid) {
    	SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, NOTE_CONTACT_ID + " = ?",
                new String[] { String.valueOf(contactid) });
        db.close();
    	
    }
    
    public void deleteNote(long noteid) {
    	SQLiteDatabase db = this.getWritableDatabase();

        // updating row

        ContentValues values = new ContentValues();
        values.put(NOTE_DELETED, 1);

        db.update(TABLE_NOTES, values, NOTE_KEY_ID + " = ?",
                new String[] { String.valueOf(noteid) });
        db.close();
    	
    }
    
    private void deleteContact(long id) {
    	deleteNotesForContact(id);

    	SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACTS, KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
        db.close();
    	
    }

    
}

