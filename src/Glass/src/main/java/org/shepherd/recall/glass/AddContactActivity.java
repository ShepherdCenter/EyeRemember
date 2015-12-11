package org.shepherd.recall.glass;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;

import org.shepherd.recall.DatabaseHandler;
import org.shepherd.recall.ble.BLEConsumer;
import org.shepherd.recall.glass.adapter.CardAdapter;
import org.shepherd.recall.model.Contact;
import org.shepherd.recall.service.BLEManager;
import org.shepherd.recall.service.MonitorNotifier;
import org.shepherd.recall.service.MonitoringData;

import com.glass.cuxtomcam.CuxtomCamActivity;
import com.glass.cuxtomcam.constants.CuxtomIntent;
import com.glass.cuxtomcam.constants.CuxtomIntent.CAMERA_MODE;
import com.glass.cuxtomcam.constants.CuxtomIntent.FILE_TYPE;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.app.Card;
import com.google.android.glass.content.Intents;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;

import org.shepherd.recall.ble.BLEDevice;

public class AddContactActivity extends Activity implements BLEConsumer {

	private static final String LOG_TAG = AddContactActivity.class.getName();
	private CardScrollAdapter mAdapter;
	private CardScrollView mCardScroller;

	private static final int SPEECH_REQUEST_NAME = 0;
	private static final int SPEECH_REQUEST_RELATIONSHIP = 1;
	private static final int CAMERA_REQUEST = 2;
	private static final int SHOW_SUMMARY = 3;
	private static final int CUXTOM_CAM_REQUEST = 4;

	private int currentCard = 0;
	private Contact contact = new Contact();

	private BLEManager mBleManager;
	private TextToSpeech mSpeech;
	private List<BLEDevice> deviceList;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
        ((App) this.getApplication()).trackView("contact/create/start");
		mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				// Do nothing.
				mSpeech.speak(
						"Scanning for new devices.",
						TextToSpeech.QUEUE_FLUSH, null);
			}
		});

		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);		
		mAdapter = new CardAdapter(createLoadingCard(this));
		mCardScroller = new CardScrollView(this);
		mCardScroller.setAdapter(mAdapter);
		setContentView(mCardScroller);
		mBleManager = BLEManager.getInstanceForApplication(this);
		setCardScrollerListener();
		mBleManager.bind(this);
	}

	// This is the display the user will see in Glass
//	private void setCard(String body, String footnote) {
//		Card card = new Card(this);
//		card.setText(body);
//		card.setFootnote(footnote);
//		setContentView(card.getView());
//	}

	private void wait(int mseconds) {
		try {
			Thread.sleep(mseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void promptName() {
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.playSoundEffect(Sounds.TAP);
		mSpeech.speak("What is the contact name", TextToSpeech.QUEUE_FLUSH,
				null);
		wait(1600);
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				"What is the contact's name?");
		startActivityForResult(intent, SPEECH_REQUEST_NAME);
	}

	private void promptRelationship() {
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.playSoundEffect(Sounds.TAP);
		mSpeech.speak("What is your relationship?", TextToSpeech.QUEUE_FLUSH,
				null);
		wait(2000);
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				"What is your relationship?");
		startActivityForResult(intent, SPEECH_REQUEST_RELATIONSHIP);
	}

	
	private void takePicture() {
        mSpeech.speak("Please take a picture. Swipe to zoom and tap to capture.",
                TextToSpeech.QUEUE_FLUSH, null);
		
		 String folder = Environment.getExternalStorageDirectory()
	                + File.separator + Environment.DIRECTORY_PICTURES
	                + File.separator + "Recall";
	        Intent intent = new Intent(getApplicationContext(),
	                CuxtomCamActivity.class);
	        intent.putExtra(CuxtomIntent.CAMERA_MODE, CAMERA_MODE.PHOTO_MODE);
	        intent.putExtra(CuxtomIntent.ENABLE_ZOOM, true);   // Enable zoom Gesture
	        intent.putExtra(CuxtomIntent.FILE_NAME, "picture"); // No need for extensions	        
	        intent.putExtra(CuxtomIntent.FOLDER_PATH, folder); // Set folder to save image and video
	        startActivityForResult(intent, CUXTOM_CAM_REQUEST);		

	}

	private void showSummary(File picturePath) {
		
		
		//calculate how many bytes our image consists of.
		Bitmap bitmap1 = BitmapFactory.decodeFile(picturePath.getPath());
		Bitmap b = Bitmap.createScaledBitmap(bitmap1, 320, 180, false);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		b.compress(Bitmap.CompressFormat.JPEG, 80, stream);
		byte[] byteArray = stream.toByteArray();
		contact.set_picture(byteArray); //Get the underlying array containing the data.
		
		picturePath.delete();
		//int bytes = b.getByteCount();
		//ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
		//b.copyPixelsToBuffer(buffer); //Move the byte data to the buffer
		//contact.set_picture(buffer.array()); //Get the underlying array containing the data.
		
		String text = String.format(
				"%s\n\n%s",
				contact.get_contactname(), contact.get_relationship());
		
		
		
		View view1 = new CardBuilder(this, CardBuilder.Layout.COLUMNS)
	    .setText(text)
	    .setFootnote(contact.get_device())
	    .addImage(b)
	    .getView();
		setContentView(view1);
		
		
		currentCard = SHOW_SUMMARY;
	}
	
	 @Override
	    public boolean onCreatePanelMenu(int featureId, Menu menu) {
		 MenuInflater inflater = getMenuInflater();
		 if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
	    	switch (currentCard) {
	
		    	case SHOW_SUMMARY:
		    		inflater.inflate(R.menu.confirmcontactadd, menu);
		    		return true;		    		
		    	default: 
		    		break;
	    		
	    	}	    
	        // Pass through to super to setup touch menu.
		 }
	        return super.onCreatePanelMenu(featureId, menu);
	    }

	private void addContact() {
		DatabaseHandler db = new DatabaseHandler(this);
		contact.set_lastseen(new Date());
		contact.set_active(true);
        db.addContact(contact);        
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(Sounds.SUCCESS);
        this.setResult(RESULT_OK);
        ((App) this.getApplication()).trackView("contact/"+ contact.get_id()+"/created");
		finish(); // done
	}
	
	private void startOver() {
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(Sounds.DISMISSED);
		this.setResult(RESULT_CANCELED);
		finish();
	}
	
	 @Override
	    public boolean onMenuItemSelected(int featureId, MenuItem item) {
            switch (item.getItemId()) {
	            case R.id.menu_add_contact:
		    		addContact();
		    		return true;
		    	case R.id.menu_start_over:
		    		startOver();
                    ((App) this.getApplication()).trackView("contact/create/cancel");
		    		return true;		    		
                default:
                    return super.onMenuItemSelected(featureId, item);
            }
	         
	    }
	 

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SPEECH_REQUEST_NAME && resultCode == RESULT_OK) {
			List<String> results = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			String spokenText = results.get(0);
			contact.set_contactname(spokenText);
			promptRelationship();
		}
		if (requestCode == SPEECH_REQUEST_RELATIONSHIP
				&& resultCode == RESULT_OK) {
			List<String> results = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			String spokenText = results.get(0);
			contact.set_relationship(spokenText);
			takePicture();
		}
		 if (requestCode == CUXTOM_CAM_REQUEST) {
	            if (resultCode == RESULT_OK) {
	                String path = data.getStringExtra(CuxtomIntent.FILE_PATH);	               
	                processPictureWhenReady(path);
	                
	            }
	        }
		if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
			String thumbnailPath = data
					.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
			String picturePath = data
					.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
			// smaller picture available with EXTRA_THUMBNAIL_FILE_PATH
			processPictureWhenReady(thumbnailPath);
			// file might not be ready for a while }
			super.onActivityResult(requestCode, resultCode, data);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void processPictureWhenReady(final String picturePath) {
		final File pictureFile = new File(picturePath);

		if (pictureFile.exists()) {
			// The picture is ready; process it.
			showSummary(pictureFile);
		} else {
			// The file does not exist yet. Before starting the file observer,
			// you
			// can update your UI to let the user know that the application is
			// waiting for the picture (for example, by displaying the thumbnail
			// image and a progress indicator).

			final File parentDirectory = pictureFile.getParentFile();
			FileObserver observer = new FileObserver(parentDirectory.getPath(),
					FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
				// Protect against additional pending events after CLOSE_WRITE
				// or MOVED_TO is handled.
				private boolean isFileWritten;

				@Override
				public void onEvent(int event, String path) {
					if (!isFileWritten) {
						// For safety, make sure that the file that was created
						// in
						// the directory is actually the one that we're
						// expecting.
						File affectedFile = new File(parentDirectory, path);
						isFileWritten = affectedFile.equals(pictureFile);

						if (isFileWritten) {
							stopWatching();

							// Now that the file is ready, recursively call
							// processPictureWhenReady again (on the UI thread).
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									processPictureWhenReady(picturePath);
								}
							});
						}
					}
				}
			};
			observer.startWatching();
		}
	}

	private void setCardScrollerListener() {
		mCardScroller
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						Log.d(LOG_TAG, "Clicked view at position " + position
								+ ", row-id " + id);
												
						mBleManager.unBind(AddContactActivity.this);
						try {
							mBleManager
									.stopMonitoringBeaconsInRegion("myMonitoringUniqueId");
						} catch (Exception e) {
							//Log.e(LOG_TAG, e.toString());
						}
						
						
						contact.set_device(deviceList.get(position).getProximityUuid());
						promptName();
					}
				});
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCardScroller.activate();
	}

	@Override
	protected void onPause() {
		mCardScroller.deactivate();
		mBleManager.unBind(this);
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(LOG_TAG, "onDestroy - unbinding");
		mBleManager.unBind(this);
		mSpeech.shutdown();

		mSpeech = null;
	}

	private List<CardBuilder> createLoadingCard(Context context) {
		ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
		cards.add(new CardBuilder(this, CardBuilder.Layout.TEXT).setText("Scanning devices..."));
		return cards;
	}

	private List<BLEDevice> removeDevicesAlreadyPaired(List<BLEDevice> devices)
	{
		ArrayList<BLEDevice> newDeviceList = new ArrayList<BLEDevice>();
		DatabaseHandler db = new DatabaseHandler(this);
        List<Contact> contacts = db.getAllContacts();
        
        	for(BLEDevice d: devices) {
        		boolean found = false;
        		for(Contact c : contacts) {
                
	        		if (c.get_device().equals(d.getProximityUuid())) {
	        			// remove this device
	        			Log.i(LOG_TAG, c.get_device() + " was already found");
	        			found = true;
	        			break;
	        		}
        		}
        		if (!found) {
            		newDeviceList.add(d);
            	}        	        		
        	}
        	return newDeviceList;
	}
	
	private void updateCards(List<BLEDevice> devices) {		

		
		mBleManager.unBind(AddContactActivity.this);
		try {
			mBleManager
					.stopMonitoringBeaconsInRegion("myMonitoringUniqueId");
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}

		
		ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
		int i = 0;
		boolean changed = true;
		deviceList = removeDevicesAlreadyPaired(devices);

		if (deviceList.size() > 0) {
            String speakDevices = String.format(Locale.US, "%d new devices in area", deviceList.size());
            mSpeech.speak(speakDevices, TextToSpeech.QUEUE_ADD, null);
			for (BLEDevice device : deviceList) {
				String text = String.format(Locale.US, "%s\n\n%s", device.getName(), device.getAddress());
				CardBuilder cb =new CardBuilder(this, CardBuilder.Layout.TEXT)
	            .setText(text)				
				.setFootnote(String.format(Locale.US, "Approx %f m away.",
						device.calculateDistance()));
				cards.add(cb);
				i++;
			}	
			mAdapter = new CardAdapter(cards);
			mCardScroller.setAdapter(mAdapter);			
			// mAdapter.notifyDataSetChanged();
		}
		else
		{
			mSpeech.speak("No new devices found.", TextToSpeech.QUEUE_ADD, null);
			wait(1500);
            this.setResult(RESULT_OK);
			finish();
		}
		

	}

	private List<BLEDevice> removeDupes(List<BLEDevice> devices)
	{
		ArrayList<BLEDevice> newDeviceList = new ArrayList<BLEDevice>();
		        
        	for(BLEDevice d: devices) {
        		if (!newDeviceList.contains(d)) {
        			newDeviceList.add(d);
        		}        		        	        	
        	}
        	return newDeviceList;
	}
	
	@Override
	public void onIBeaconServiceConnect() {
		mBleManager.setMonitorNotifier(new MonitorNotifier() {
			@Override
			public void didEnter(final MonitoringData data) {

				AddContactActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						List<BLEDevice> devices = removeDupes(data.getDevices());

						updateCards(devices);
						
					}
				});
			}

		});

		try {
			// Scan every 5 secs
			mBleManager.setForegroundScanPeriod(7000);
			mBleManager.setForegroundBetweenScanPeriod(10000);
			mBleManager.updateScanPeriods();
			mBleManager.startMonitoringBeaconsInRegion("myMonitoringUniqueId");

		} catch (RemoteException e) {
		}
	}

}
