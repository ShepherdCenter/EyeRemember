package org.shepherd.recall.glass;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.shepherd.recall.DatabaseHandler;
import org.shepherd.recall.Helper;
import org.shepherd.recall.glass.adapter.ConfirmAdapter;
import org.shepherd.recall.model.Contact;
import org.shepherd.recall.model.Note;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.glass.content.Intents;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

public class ContactsActivity extends Activity  {

	
	   private List<CardBuilder> mCards;
	    private CardScrollView mCardScrollView;
	    private ContactScrollAdapter  mAdapter;
	    
	    private CardScrollView mConfirmCardScrollView;	    
	    private ConfirmAdapter  mConfirmAdapter;
	    
	int iPosition;
    private static final String LOG_TAG = ContactsActivity.class.getName();
    private static final int SPEECH_REQUEST_ADDNOTE = 0;
    private static final int CONTACT_ADD = 1;
    private TextToSpeech mSpeech;
    private boolean mServiceWasRunning = false;
    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
   			@Override
   			public void onInit(int status) {
   				// Do nothing.				
   			}
   		});
        
        createCards();
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);	
        mCardScrollView = new CardScrollView(this);
        
        mAdapter = new ContactScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        
        mConfirmAdapter = new ConfirmAdapter(this, "Yes, delete this contact", "No, keep the contact");
        mConfirmCardScrollView = new CardScrollView(this);
        mConfirmCardScrollView.setAdapter(mConfirmAdapter);
        
        showContactsView();        
        
        mConfirmCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() 
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
            {
                   playClickSound();
            		invalidateOptionsMenu();
            		if (position == 0)
            		{
            			// delete note
            			deleteContact();
            			AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            			audio.playSoundEffect(Sounds.SUCCESS);         			
            		}
            		showContactsView();
            		
            }
       });
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() 
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
            {
            	playClickSound();
            	if (mAdapter.getCount()-1 == position) {
            		// add contact
            		if(Helper.isServiceRunning(ContactsActivity.this, BackgroundService.class.getName())) {
            			mServiceWasRunning = true;
            			 mHandler.post(new Runnable() {

            	                @Override
            	                public void run() {
            	                	doStopService();            	               
            	                }
            	            });
            			
            		}
            		startActivityForResult(new Intent(ContactsActivity.this,  AddContactActivity.class), CONTACT_ADD);
            	    
            	}
            	else
            	{
                    //save the card index that was selected
                    iPosition = position;                    
            		invalidateOptionsMenu();
            		openOptionsMenu();
            	}
            }
       });
    }
    

    @Override
    protected void onDestroy()
    {
   	 	mSpeech.shutdown();
   	 	mSpeech = null;
        super.onDestroy();
    }
    
    private void showContactsView() {
    	mCardScrollView.activate();
		mConfirmCardScrollView.deactivate();
		setContentView(mCardScrollView);
	}
 	private void showDeleteConfirmation() {
 		mSpeech.speak("Are you sure you want to delete this contact?", TextToSpeech.QUEUE_FLUSH, null); 		
 		mCardScrollView.deactivate();
 		mConfirmCardScrollView.activate();
 		setContentView(mConfirmCardScrollView);
 	}
 	
	   private void doStartService()
	    {
	        startService(new Intent(this, BackgroundService.class));
	    }
	    private void doStopService()
	    {
	        stopService(new Intent(this, BackgroundService.class));
	    }
    private void refresh() {
    	createCards();
    	mAdapter.notifyDataSetChanged();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
    
    private void playClickSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);
	}
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contactmenu, menu);
        if (((App)this.getApplication()).canAddNotes()) {
            Contact contact = getSelectedContact();
            DatabaseHandler db = new DatabaseHandler(this);
            List<Note> notes = db.getAllNotesForContact(contact.get_id());
            if (notes.size() == 0) {
                menu.removeItem(R.id.contact_viewnotes);
            }
        } else {
            menu.removeItem(R.id.contact_viewnotes);
            menu.removeItem(R.id.contact_addnote);
            menu.removeItem(R.id.contact_view_deleted_notes);
        }
		
		return super.onCreateOptionsMenu(menu);
	}

    private Contact getSelectedContact()
    {
    	DatabaseHandler db = new DatabaseHandler(this);
    	List<Contact> contacts = db.getAllContacts();    	
    	return contacts.get(iPosition);
    }
    
    private void deleteContact() {    	
    	Contact contactToDelete = getSelectedContact();
        ((App) this.getApplication()).trackView("contact/"+ contactToDelete.get_id()+ "/delete");
    	DatabaseHandler db = new DatabaseHandler(this);
    	db.deleteContact(contactToDelete);    	
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(Sounds.DISMISSED);
    	refresh();
    }
    
    private void toggleActive() {
    	//FIXME - not implemented yet
    }
    
    private void addNote() {
    	Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				"Say note");
		startActivityForResult(intent, SPEECH_REQUEST_ADDNOTE);
    }
    
    private void addNote(String noteSpoken) {
    	Note note = new Note();
    	note.set_note(noteSpoken);
    	Contact contact = getSelectedContact();
    	note.set_contactid(contact.get_id());
    	note.set_createdOn(new Date());
    	DatabaseHandler db = new DatabaseHandler(this);
    	db.addNote(note);
        ((App) this.getApplication()).trackView("contact/"+ contact.get_id()+ "/notes/add/"+ note.get_id());
    	refresh();
    }
    
    private void viewNotes(boolean active) {
    	Contact contact = getSelectedContact();
        ((App) this.getApplication()).trackView("contact/"+ contact.get_id()+ "/notes/view");
    	Intent intent = new Intent(ContactsActivity.this, NotesActivity.class);
    	Bundle mBundle = new Bundle();
    	mBundle.putLong("contactid", contact.get_id());
        mBundle.putBoolean("active", active);
    	intent.putExtras(mBundle);
    	startActivity(intent);
	
    	
    	    	
    }

    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SPEECH_REQUEST_ADDNOTE && resultCode == RESULT_OK) {
			List<String> results = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			String spokenText = results.get(0);
			addNote(spokenText);			
		}
		if (requestCode == CONTACT_ADD && resultCode == RESULT_OK) {
			if (mServiceWasRunning) {
				// start the service again
				doStartService();
			}
			mServiceWasRunning = false;
		}		
		
		super.onActivityResult(requestCode, resultCode, data);
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		
		
		 switch (item.getItemId()) {
	         case R.id.contact_delete:
	        	 showDeleteConfirmation();
		    		return true;
	         case R.id.contact_addnote:
	        	 addNote();
	        	 return true;
	         case R.id.contact_viewnotes:
	        	 viewNotes(true);
	        	 return true;

             case R.id.contact_view_deleted_notes:
                 viewNotes(false);
                 return true;
//	         case R.id.toggle_active:
//	        	 	toggleActive();
//	        	 	return true;
	         default:
	             return super.onOptionsItemSelected(item);
			 }
	}
	
	
    private void createCards() {
        mCards = new ArrayList<CardBuilder>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);
        DatabaseHandler db = new DatabaseHandler(this);
        List<Contact> contacts = db.getAllContacts();

        for(Contact c : contacts) {
        	List<Note> notes = db.getAllNotesForContact(c.get_id(), true);
        	String text = String.format("%s\n\n%s",c.get_contactname(), c.get_relationship());
        	
        	
        	CardBuilder cb =new CardBuilder(this, CardBuilder.Layout.COLUMNS)
            .setText(text);
            if (((App)this.getApplication()).canAddNotes()) {
                if (notes.size() == 0) {
                    cb.setFootnote(String.format("No notes"));
                } else {
                    cb.setFootnote(String.format("%d note%s", notes.size(), (notes.size() < 2) ? "" : "s"));
                }
            }
        	cb.setTimestamp(Helper.getFriendlyDate(c.get_lastseen()));
        	
        	
        	if (c.get_picture() != null) {
        		// has picture
        		cb.addImage(Helper.getImageFromByteArray(c.get_picture()));        		
        	}
        	
        	mCards.add(cb);
        
        }
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU).setIcon(R.drawable.ic_action_add_person).setText(R.string.text_addcontact));
        

    }
    
    private class ContactScrollAdapter extends CardScrollAdapter {

        @Override
        public int getPosition(Object item) {
            return mCards.indexOf(item);
        }

        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        @Override
        public int getItemViewType(int position){
            return mCards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView(convertView, parent);
        }
    }
    
    
}
