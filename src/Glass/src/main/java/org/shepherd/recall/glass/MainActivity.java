package org.shepherd.recall.glass;

import java.util.ArrayList;
import java.util.List;

import org.shepherd.recall.glass.BackgroundService;
import org.shepherd.recall.DatabaseHandler;
import org.shepherd.recall.Helper;
import org.shepherd.recall.glass.adapter.CardAdapter;
import org.shepherd.recall.model.Contact;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

public class MainActivity extends Activity
{
	
    // Index of api demo cards.
    // Visible for testing.    
    static final int STATUS = 0;    
    static final int SERVICE = 1;
    static final int SETTINGS = 2;
    
    private final Handler mHandler = new Handler();
    
    private CardScrollAdapter mAdapter;
    private CardScrollView mCardScroller;
	
	
	
    // Service to handle liveCard publishing, etc...
    private boolean mIsBound = false;
    private BackgroundService glassService;
    private static final String LOG_TAG = MainActivity.class.getName();
    
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(LOG_TAG,"onServiceConnected() called.");
            glassService = ((BackgroundService.LocalBinder)service).getService();
        }
        public void onServiceDisconnected(ComponentName className) {
            Log.d(LOG_TAG,"onServiceDisconnected() called.");
            glassService = null;
        }
    };


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mAdapter = new CardAdapter(createCards(this));
        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(mAdapter);
        setContentView(mCardScroller);
        setCardScrollerListener();
        //
    }

    private void toggleService() {
    	AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(Sounds.SUCCESS);
    	if(Helper.isServiceRunning(this, BackgroundService.class.getName())) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                	doStopService();
                	refreshCards();
                }
            });
    	}
    	else
    	{
    		doStartService();
    		refreshCards();
    	}


    }

    private void refreshCards() {
    	mAdapter = new CardAdapter(createCards(this));
    	mCardScroller.setAdapter(mAdapter);

    }

    private CardBuilder getContactsCard() {
    	DatabaseHandler db = new DatabaseHandler(this);
    	List<Contact> contacts = db.getAllContacts();
    	CardBuilder cb = new CardBuilder(this, CardBuilder.Layout.MENU);
    	if (contacts.size() == 0) {
    		cb.setFootnote("No contacts paired");
    	} else
    	{
    		cb.setFootnote(String.format("%d paired contact%s", contacts.size(), contacts.size() < 2 ? "" : "s" ));
    	}
    	cb.setIcon(R.drawable.ic_action_group);
    	cb.setText(R.string.text_status);	
		return cb;
    }
    
    private List<CardBuilder> createCards(Context context) {
        ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
        cards.add(getContactsCard());
                         
        if(!Helper.isServiceRunning(this, BackgroundService.class.getName())) {
        	cards.add(new CardBuilder(this, CardBuilder.Layout.MENU).setText(R.string.start_scanning).setFootnote(R.string.service_stopped).setIcon(R.drawable.ic_action_bluetooth));
        }
        else
        {
        	cards.add(new CardBuilder(this, CardBuilder.Layout.MENU).setText(R.string.stop_scanning).setFootnote(R.string.service_started).setIcon(R.drawable.ic_action_bluetooth_searching));
        }
        PackageInfo pinfo;
        String footerNote = "";
        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            footerNote = "Version " + pinfo.versionName + " build " + pinfo.versionCode;

            String androidID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            footerNote += "\nDevice Id: " + androidID;
            //ET2.setText(versionNumber);
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block            
        }
        cards.add(new CardBuilder(this, CardBuilder.Layout.MENU).setText(R.string.text_settings).setFootnote(footerNote).setIcon(R.drawable.ic_action_settings));
        
        return cards;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshCards();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }
    
    @Override
    protected void onDestroy()
    {
        //doUnbindService();
        super.onDestroy();
    }
    
    /**
     * Different type of activities can be shown, when tapped on a card.
     */
    private void setCardScrollerListener() {
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "Clicked view at position " + position + ", row-id " + id);
                int soundEffect = Sounds.TAP;
                boolean bPlaySound = true;
                switch (position) {
                    case STATUS:
                        startActivity(new Intent(MainActivity.this, ContactsActivity.class));
                        break;

                    case SERVICE:
                        toggleService();//startService(new Intent(ApiDemoActivity.this, OpenGlService.class));
                        bPlaySound = false;
                        break;

                    case SETTINGS:
                        ((App) MainActivity.this.getApplication()).trackView("settings");
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        break;


                    default:
                        soundEffect = Sounds.ERROR;
                        Log.d(LOG_TAG, "Don't show anything");
                }

                // Play sound.
                if (bPlaySound) {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.playSoundEffect(soundEffect);
                }
            }
        });
    }
    
    
    private void doBindService()
    {
        bindService(new Intent(this, BackgroundService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    private void doUnbindService() {
        if (mIsBound) {
            unbindService(serviceConnection);
            mIsBound = false;
        }
    }
    
    private void doStartService()
    {
        ((App) this.getApplication()).trackView("service/start");
        startService(new Intent(this, BackgroundService.class));
    }
    private void doStopService()
    {
        ((App) this.getApplication()).trackView("service/stop");
        stopService(new Intent(this, BackgroundService.class));
    }


}