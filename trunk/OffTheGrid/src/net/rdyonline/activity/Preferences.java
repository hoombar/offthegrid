package net.rdyonline.activity;

import com.flurry.android.FlurryAgent;

import net.rdyonline.ApplicationSettings;
import net.rdyonline.Config;
import net.rdyonline.offthegrid.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	public void onStart()
	{
	   super.onStart();
	   FlurryAgent.onStartSession(this, Config.FLURRY_KEY);
	}

	@Override
	public void onStop()
	{
	   super.onStop();
	   FlurryAgent.onEndSession(this);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}
	

	@Override
	protected void onResume() {
	    super.onResume();
	    // Set up a listener whenever a key changes
	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    // Unregister the listener whenever a key changes
	    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key) 
	{
		ApplicationSettings as = new ApplicationSettings(this);
		
		if (key.equals("KEY_START_Y")) {
			as.logStartY(as.getStartY());
		} else if (key.equals("KEY_MAX_CHARACTERS")) {
			as.logMaxCharacters(as.getMaxCharacters()); // trigger flurry event
		} else if (key.equals("KEY_PADDING_OPTION")) {
			as.logPaddingOption(as.getPaddingOption());
		}
	}
}
