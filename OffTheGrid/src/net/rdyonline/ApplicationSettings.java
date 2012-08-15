package net.rdyonline;

import net.rdyonline.strategy.BorderCharStrategy;
import net.rdyonline.strategy.CompositePasswordStrategy;
import net.rdyonline.strategy.NumericBorderCharStrategy;
import net.rdyonline.strategy.PasswordStrategy;
import net.rdyonline.strategy.ScanForwardStrategy;
import net.rdyonline.strategy.SymbolicBorderCharStrategy;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flurry.android.FlurryAgent;

public class ApplicationSettings {
	
	private final String KEY_GRID = "KEY_GRID";
	private final String KEY_START_Y = "KEY_START_Y";
	private final String KEY_MAX_CHARACTERS = "KEY_MAX_CHARACTERS";
	private final String KEY_PASSWORD_STRATEGY = "KEY_PASSWORD_STRATEGY";
	
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
		return getPasswordStrategyName().equals("original-padded");
	}
	
	public int getMaxCharacters()
	{
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this.context).getString(this.KEY_MAX_CHARACTERS, Integer.toString(this.DEFAULT_MAX_CHARACTERS)));
	}
	
	public void logMaxCharacters(int value)
	{
		FlurryAgent.logEvent(String.format(Config.FLURRY_EVENT_SETTING_MAX_CHAR, new Object[] { Integer.toString(value) }));
	}
	
	
	public PasswordStrategy getPasswordStrategy()
	{
		String strategyName = getPasswordStrategyName();
		PasswordStrategy strategy = null;
		
		if (strategyName.equals("letters-only")) {
			strategy = new ScanForwardStrategy(2);
		} else if (strategyName.equals("use-border-char")) {
			strategy = new CompositePasswordStrategy()
				.add(new ScanForwardStrategy(2))
				.add(new BorderCharStrategy());
		} else if (strategyName.equals("use-border-digit")) {
			strategy = new CompositePasswordStrategy()
				.add(new ScanForwardStrategy(2))
				.add(new NumericBorderCharStrategy());
		} else if (strategyName.equals("use-border-symbol")) {
			strategy = new CompositePasswordStrategy()
				.add(new ScanForwardStrategy(2))
				.add(new SymbolicBorderCharStrategy());			
		}
		
		return strategy;
	}

	public String getPasswordStrategyName()
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PASSWORD_STRATEGY, "original");
	}
	
	public void logPasswordStrategyName(String value)
	{
		FlurryAgent.logEvent(String.format(Config.FLURRY_EVENT_SETTING_PASSWORD_STRATEGY, value));
	}
	
}
