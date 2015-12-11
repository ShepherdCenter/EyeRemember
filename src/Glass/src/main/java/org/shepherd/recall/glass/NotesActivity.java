package org.shepherd.recall.glass;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.shepherd.recall.DatabaseHandler;
import org.shepherd.recall.Helper;
import org.shepherd.recall.glass.adapter.CardAdapter;
import org.shepherd.recall.glass.adapter.ConfirmAdapter;
import org.shepherd.recall.model.Contact;
import org.shepherd.recall.model.Note;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

public class NotesActivity extends Activity  {

	
	   private List<CardBuilder> mCards;
	    private CardScrollView mNoteCardScrollView;
	    private CardScrollView mConfirmCardScrollView;
	    private NoteScrollAdapter  mAdapter;
	    private ConfirmAdapter  mConfirmAdapter;
	int iPosition;
	long contactid = 0;
    boolean activeNotes = true;
	boolean showingDeleteConfirmation = false;
	private TextToSpeech mSpeech;
 private static final String LOG_TAG = NotesActivity.class.getName();
 

 @Override
 protected void onCreate(Bundle bundle) {
     super.onCreate(bundle);
     
 
     contactid = getIntent().getExtras().getLong("contactid");
     activeNotes = getIntent().getExtras().getBoolean("active");
     mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				// Do nothing.				
			}
		});
     
     createNoteCards();
     getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);	
     mNoteCardScrollView = new CardScrollView(this);
     
     mConfirmAdapter = new ConfirmAdapter(this, "Yes, delete note", "No, keep the note");
     mConfirmCardScrollView = new CardScrollView(this);
     mConfirmCardScrollView.setAdapter(mConfirmAdapter);

     
     mAdapter = new NoteScrollAdapter();
     mNoteCardScrollView.setAdapter(mAdapter);
     
     showNotesView();
     
     
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
         			deleteNote();
         			AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
         			audio.playSoundEffect(Sounds.SUCCESS);         			
         		}
         		showNotesView();
         		
         }
    });
     
     mNoteCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() 
     {
         @Override
         public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
         {
                 //save the card index that was selected
                 iPosition = position;
                 playClickSound();
         		invalidateOptionsMenu();
         		openOptionsMenu();
         }
    });
 }
 
 
 @Override
 protected void onDestroy()
 {
	 mNoteCardScrollView = null;
	 mConfirmAdapter = null;
	 mSpeech.shutdown();
	 mSpeech = null;
     super.onDestroy();
 }
 

 	private void showNotesView() {
		showingDeleteConfirmation = false;
		mNoteCardScrollView.activate();
		mConfirmCardScrollView.deactivate();
		setContentView(mNoteCardScrollView);
	}
 	private void showDeleteConfirmation() {
 		mSpeech.speak("Are you sure you want to delete this note?", TextToSpeech.QUEUE_FLUSH, null);
 		showingDeleteConfirmation = true;
 		mNoteCardScrollView.deactivate();
 		mConfirmCardScrollView.activate();
 		setContentView(mConfirmCardScrollView);
 	}
 @Override
	public boolean onCreateOptionsMenu(Menu menu) {
     if (activeNotes) {
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.notemenu, menu);
     }

		return super.onCreateOptionsMenu(menu);
	}
 
 private Note getSelectedNote()
 {
 	DatabaseHandler db = new DatabaseHandler(this);
 	List<Note> notes = db.getAllNotesForContact(contactid,activeNotes);
 	return notes.get(iPosition);
 }
 
 private void deleteNote() {    	
	 Note noteToDelete = getSelectedNote();
     ((App) this.getApplication()).trackView("contact/"+ contactid+ "/notes/delete/"+ noteToDelete.get_id());
 	DatabaseHandler db = new DatabaseHandler(this);
 	db.deleteNote(noteToDelete.get_id());
 	AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    am.playSoundEffect(Sounds.DISMISSED);
 	refresh();
 }
 
 private void readAloud() {
	 Note note = getSelectedNote();
     ((App) this.getApplication()).trackView("contact/"+ contactid+ "/notes/readaloud/"+ note.get_id());
	 mSpeech.speak(note.get_note(), TextToSpeech.QUEUE_FLUSH, null);
 }
 
 private void refresh() {
 	createNoteCards();
 	mAdapter.notifyDataSetChanged();
 }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {		
		 switch (item.getItemId()) {
	         case R.id.note_delete:
	        	 showDeleteConfirmation();
		    		return true;
	         case R.id.note_read_aloud:
	        	 	readAloud();
	        	 	return true;
	         default:
	             return super.onOptionsItemSelected(item);
			 }
	}
 
 private void playClickSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);
	}
 

 
 private void createNoteCards() {
     mCards = new ArrayList<CardBuilder>();
     SimpleDateFormat sdf = new SimpleDateFormat("E - MM/dd/yyyy h:mm a", Locale.US);
     DatabaseHandler db = new DatabaseHandler(this);
     List<Note> notes = db.getAllNotesForContact(contactid,activeNotes);
     Contact c = db.getContact(contactid);
     if (notes.size() == 0) {
    		CardBuilder cb =new CardBuilder(this, CardBuilder.Layout.TEXT)
	         .setText("No notes");
	     mCards.add(cb);
     }
     else {
	     for(Note n : notes) {     	
	     	
	     	CardBuilder cb =new CardBuilder(this, CardBuilder.Layout.TEXT)
	         .setText(n.get_note());

             if (c.get_picture() != null) {
                 // has picture
                 cb.addImage(Helper.getImageFromByteArray(c.get_picture()));
             }
     		cb.setFootnote(sdf.format(n.get_createdOn()));
	     	
	     	
	     	mCards.add(cb);
	     }
     }
 }
 
 
 
 	private class NoteScrollAdapter extends CardScrollAdapter {

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
