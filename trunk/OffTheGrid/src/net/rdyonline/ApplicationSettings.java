package net.rdyonline;

import com.flurry.android.FlurryAgent;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ApplicationSettings {
	
	private final String KEY_GRID = "KEY_GRID";
	private final String KEY_START_Y = "KEY_START_Y";
	private final String KEY_PADDING_OPTION = "KEY_PADDING_OPTION";
	private final String KEY_MAX_CHARACTERS = "KEY_MAX_CHARACTERS";
	
	private final boolean DEFAULT_USE_PADDING = false;
	private final int DEFAULT_MAX_CHARACTERS = 12;
	private final int DEFAULT_START_Y = 1;
	
	private Context context;
	
	public ApplicationSettings(Context context)
	{
		this.context = context;
	}
	
	public String getGrid()
	{
		return PreferenceManager.getDefaultSharedPreferences(this.context).getString(this.KEY_GRID, null);
	}
	public void setGrid(String value)
	{
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this.context).edit();
		
		editor.putString(this.KEY_GRID, value);
		editor.commit();
	}
	
	public int getStartY()
	{
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this.context).getString(this.KEY_START_Y, Integer.toString(DEFAULT_START_Y)));
	}
	public void logStartY(int value)
	{
		FlurryAgent.logEvent(String.format(Config.FLURRY_EVENT_SETTING_START_SQUARE, new Object[] { Integer.toString(value) }));
	}
	
	public boolean getPaddingOption()
	{
		return PreferenceManager.getDefaultSharedPreferences(this.context).getBoolean(this.KEY_PADDING_OPTION, DEFAULT_USE_PADDING);
	}
	
	public void logPaddingOption(boolean value)
	{
		if (value) {
			FlurryAgent.logEvent(Config.FLURRY_EVENT_SETTING_PAD_ON);
		} else {
			FlurryAgent.logEvent(Config.FLURRY_EVENT_SETTING_PAD_OFF);
		}
	}
	
	public int getMaxCharacters()
	{
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this.context).getString(this.KEY_MAX_CHARACTERS, Integer.toString(this.DEFAULT_MAX_CHARACTERS)));
	}
	
	public void logMaxCharacters(int value)
	{
		FlurryAgent.logEvent(String.format(Config.FLURRY_EVENT_SETTING_MAX_CHAR, new Object[] { Integer.toString(value) }));
	}
}
