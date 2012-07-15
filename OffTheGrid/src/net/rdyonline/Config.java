package net.rdyonline;

public class Config {
	
	// use Dev key
	// TODO(benp) change to live API key before publishing
	public static final String FLURRY_KEY = "78N3TBQT2MGDKHKDYH86";
	
	// flurry events
	public static final	String FLURRY_EVENT_GRID_GENERATED = "Grid generated";
	public static final	String FLURRY_EVENT_GRID_CAPTURED = "Grid captured";
	
	public static final	String FLURRY_EVENT_GRID_EMAILED = "Grid emailed";
	public static final	String FLURRY_EVENT_GRID_VIEWED = "Grid viewed";
	
	public static final	String FLURRY_EVENT_SETTING_PAD_OFF = "Padding turned off";
	public static final	String FLURRY_EVENT_SETTING_PAD_ON = "Padding turned on";
	public static final	String FLURRY_EVENT_SETTING_MAX_CHAR = "Max char set to %s";
	public static final	String FLURRY_EVENT_SETTING_START_SQUARE = "Start square set to %s";
	
	public static final	String FLURRY_EVENT_PASSWORD_GEN = "Password shown";
	public static final	String FLURRY_EVENT_PASSWORD_COPIED = "Password copied";
}
