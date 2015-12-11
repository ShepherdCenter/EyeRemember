package org.shepherd.recall.glass.settings.types;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.google.android.glass.widget.CardBuilder;
import org.shepherd.recall.glass.settings.option.PreferenceOption;

public class ChooserPreference extends AbstractPreference{

	private SharedPreferences mPrefs;
	
	private int mSelection = 0;
	private List<PreferenceOption> mOptions;  
	
	/**
	 * The "prompt" for the title
	 */
	private String mBaseTitle;

	public ChooserPreference(SharedPreferences prefs, String key, String title, String speech, List<PreferenceOption> options) {
		super(prefs, key, title, speech);
		mBaseTitle = title;
		mOptions = options;
		mSelection = getInt();
	}
	
	public ChooserPreference(SharedPreferences prefs, String key, String title, int imageResource, List<PreferenceOption> options) {
		super(prefs, key, title, imageResource);
		mBaseTitle = title;
		mOptions = options;
		mSelection = getInt();
	}

	public ChooserPreference(SharedPreferences prefs, String key, String title, List<PreferenceOption> options, int defaultValueIndex) {
		super(prefs, key, title);
		mBaseTitle = title;
		mOptions = options;
		mSelection = getInt(defaultValueIndex);
	}
	
	public ChooserPreference(SharedPreferences prefs, String key, String title, int imageResource, List<PreferenceOption> options, int defaultValueIndex) {
		super(prefs, key, title, imageResource);
		mBaseTitle = title;
		mOptions = options;
		mSelection = getInt(defaultValueIndex);
	}
	
	
	@Override
	public boolean onSelect() {
		// Return false, because a menu should be generated from the items in getOptions()
		return false;
	}

	@Override
	public View getCard(Context context) {
		// Update the title to be the prompt + the selected item
		// Example:
		//		Update every...
		//			Two hours
		//updateTitle();
		int selection = mSelection;
		if (selection > mOptions.size()-1) {
			selection = 0;
		}
		String selected = mOptions.get(selection).getTitle();
		//return getDefaultCard(context);
		return new CardBuilder(context, CardBuilder.Layout.MENU).setText(mBaseTitle).setFootnote(selected).getView();
	}

	@Override
	public List<PreferenceOption> getOptions() {
		return mOptions;
	}
	
	@Override
	public void onOptionSelected(int index){
		putInt(index);
		mSelection = index;
	}

    @Override
    public String getValue() {
        int selection = getInt();
        String selected = mOptions.get(selection).getTitle();
        return selected;
    }
	/**
	 * Make the title from the title and the current value
	 */
	private void updateTitle(){
		int selection = mSelection;
		if (selection > mOptions.size()-1) {
			selection = 0;
		}
		setTitle("<b>" + mBaseTitle + "</b> <br/>" + mOptions.get(selection).getTitle());
	}
	
}
