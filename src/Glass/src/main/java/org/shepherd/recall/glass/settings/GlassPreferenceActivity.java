package org.shepherd.recall.glass.settings;

import java.util.List;

import org.shepherd.recall.glass.App;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import org.shepherd.recall.glass.settings.adapter.CardPreferenceAdapter;
import org.shepherd.recall.glass.settings.option.PreferenceOption;
import org.shepherd.recall.glass.settings.types.AbstractPreference;
import org.shepherd.recall.glass.settings.types.ChooserPreference;
import org.shepherd.recall.glass.settings.types.TogglePreference;
import org.shepherd.recall.glass.settings.types.ActivityPreference;

public class GlassPreferenceActivity extends Activity implements OnItemClickListener {

	private CardScrollView mScrollView;
	private CardPreferenceAdapter mAdapter;

	private SharedPreferences mPrefs;

	// These are used for Preferences like ChooserPreference
	// that return false for onSelected() and make
	// the OptionsMenu open with options
	private List<PreferenceOption> mCurrentOptions;
	private AbstractPreference mCurrentPreference;
	private int mCurrentPreferenceIndex;

    private TextToSpeech mSpeech;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mScrollView = new CardScrollView(this);
		mAdapter = new CardPreferenceAdapter(this);

		mScrollView.setOnItemClickListener(this);
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.

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


    protected void buildAndShowOptions() {
		mAdapter.buildCards();
		mScrollView.setAdapter(mAdapter);
		mScrollView.activate();
		setContentView(mScrollView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		for (int i = 0; i < mCurrentOptions.size(); i++) {
			menu.add(Menu.NONE, i, i, mCurrentOptions.get(i).getTitle());
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCurrentPreference.onOptionSelected(item.getItemId());

        String key= mCurrentPreference.getKey();
        String value = mCurrentPreference.getValue();
        ((App) this.getApplication()).trackView("settings/" + key + "/" + value);
		// Update the View for this AbstractPreference
		// in case the View changed
		mAdapter.buildCard(mCurrentPreferenceIndex);
		mAdapter.notifyDataSetChanged();
		
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> view, View clicked, int index, long id) {
		AbstractPreference preference = (AbstractPreference) mAdapter.getItem(index);
		if (preference.onSelect()) {
			// True: the AbstractPreference handled everything

			if(preference.playSuccessSoundOnSelect()){
				playSuccessSound();
			}else{
				playClickSound();
			}

            String key= preference.getKey();
            String value = preference.getValue();
            ((App) this.getApplication()).trackView("settings/" + key + "/" + value);
			// Update the View for this AbstractPreference
			// in case the View changed
			mAdapter.buildCard(index);
			mAdapter.notifyDataSetChanged();
		} else {
			// False: show the AbstractPreference's options as a Menu

			playClickSound();

			if (preference instanceof ChooserPreference) {
                // speak
                if (preference.getSpeech() != null )
                {
                    mSpeech.speak(preference.getSpeech(), TextToSpeech.QUEUE_ADD, null);
                }

				mCurrentOptions = ((ChooserPreference) preference).getOptions();
				mCurrentPreference = preference;
				mCurrentPreferenceIndex = index;
				
				invalidateOptionsMenu();
				openOptionsMenu();
			}
		}
	}

	/**
	 * Add a Preference that can be on or off
	 */
	protected void addTogglePreference(String key, String title) {
		mAdapter.addPreference(new TogglePreference(mPrefs, key, title));
	}

	/**
	 * Add a Preference that can be on or off
	 */
	protected void addTogglePreference(String key, String title, boolean defaultValue) {
		mAdapter.addPreference(new TogglePreference(mPrefs, key, title, defaultValue));
	}

	/**
	 * Add a Preference that gives a list of options. The index of the chosen option is saved.
	 */
	protected void addChoicePreference(String key, String title, String speech, List<PreferenceOption> options) {
		mAdapter.addPreference(new ChooserPreference(mPrefs, key, title, speech, options));
	}

	/**
	 * Add a Preference that gives a list of options. The index of the chosen option is saved.
	 */
	protected void addChoicePreference(String key, String title, List<PreferenceOption> options, int defaultValueIndex) {
		mAdapter.addPreference(new ChooserPreference(mPrefs, key, title, options, defaultValueIndex));
	}

	/**
	 * Add a Preference that launches a specified Activity, passing in the preference key as an extra.
	 * For ease of use, extend AbstractPreferenceActivity for the Activity to launch
	 */
	protected void addActivityPreference(String key, String title, Class<?> activityClass){
		mAdapter.addPreference(new ActivityPreference(this, mPrefs, key, title, activityClass));
	}

	/**
	 * Add a Preference that launches a specified Activity, passing in the preference key as an extra.
	 * For ease of use, extend AbstractPreferenceActivity for the Activity to launch
	 */
	protected void addActivityPreference(String key, String title, int imageResource, Class<?> activityClass){
		mAdapter.addPreference(new ActivityPreference(this, mPrefs, key, title, imageResource, activityClass));
	}
	

	/**
	 * Add a generic Preference
	 */
	protected void addPreference(AbstractPreference preference) {
		mAdapter.addPreference(preference);
	}

	/**
	 * Play the standard Glass success sound
	 */
	protected void playSuccessSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.SUCCESS);
	}

	/**
	 * Play the standard Glass tap sound
	 */
	protected void playClickSound() {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);
	}

}
