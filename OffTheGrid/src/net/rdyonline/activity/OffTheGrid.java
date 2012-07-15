package net.rdyonline.activity;

import net.rdyonline.ApplicationSettings;
import net.rdyonline.Config;
import net.rdyonline.Grid;
import net.rdyonline.offthegrid.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

/***
 * This is the main activity for off the grid.
 * The activity is kept quite simple so that accessing a password is a quick operation
 * @author rdy
 *
 */
public class OffTheGrid extends Activity implements TextToSpeech.OnInitListener {
    
	private final double			SPEECH_RATE			= 1.2;
	private final double			SPEECH_PITCH		= 1.2;
	
	private TextToSpeech 			mTts;
	private static final int 		MY_DATA_CHECK_CODE 	= 1234;
	
	private ApplicationSettings 	settings 			= null;
	private Grid 					grid 				= null;
	
    private TextView 				txtShowPassword	 	= null;
    private EditText 				txtEnterDomain 		= null;
    private Button 					btnCopyPassword 	= null;
    private Button					btnSpeak			= null;
    private Spinner					spnIteration		= null;
    
    private static String 			lastGen 			= ""; 

    private Context 				context				= null;
    
    private PowerManager			powerManager		= null;
    private WakeLock				wakeLock			= null;
	
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // required to obtain a wake lock. Make sure the screen doesn't sleep when you are reading a password
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        
        settings = new ApplicationSettings(this);
        
        grid = Grid.getInstance(this);
        txtShowPassword = (TextView) findViewById(R.id.txtShowPassword);
        txtEnterDomain = (EditText) findViewById(R.id.txtEnterDomain);
        btnCopyPassword = (Button) findViewById(R.id.btnCopyPassword);
        btnSpeak = (Button) findViewById(R.id.speak);
        spnIteration = (Spinner) findViewById(R.id.choose_iteration);
        
        context = this;
        
        // enter domain control should have focus on load
        txtEnterDomain.requestFocus();
        
        updateTextPassword();
        
        // when the text in the domain changes, the password shown should be updated
        txtEnterDomain.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				
				updateTextPassword();
				
			}
		});
        
        // copy the password to the clipboard
        btnCopyPassword.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 

				clipboard.setText(txtShowPassword.getText().toString());
				Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show();
			}
		});
        
        // the app will "speak" the password, letter by letter
        btnSpeak.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				// should only speak the password if it is safe to do so
				new AlertDialog.Builder(context)
        		.setTitle("All clear?")
        		.setMessage("Have you checked there is nobody around? They might hear your password!")
        		.setPositiveButton("Speak", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// use voice API to "speak" letter by letter
						speakPassword();
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do nothing, save for later or read it
					}
				})
				.show();
			}
		});
        
        // bind the possible start square options to the spinner in the main UI
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.grid_start_values, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnIteration.setAdapter(adapter);
        
        spnIteration.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				updateTextPassword();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
        
        // Fire off an intent to check if a TTS engine is installed
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
    }
    
    /***
     * Use the TTS engine to speak the password that is currently being displayed
     * To make sure that it's read letter by letter, it's split up
     * Each letter should be read phonetically, so they are looked up in turn
     */
    protected void speakPassword() {
    	String speech = "";
		String query =  txtShowPassword.getText().toString();

		for (int i = 0; i < query.length(); i++) {
			Character c = query.charAt(i);
			
			String append = "";
			
			if (Character.isUpperCase(c)) {
				append = "capital";
			}
			
			String letter = getPhonetic(Character.toLowerCase(c));
			
			speech += append + " " + letter + ". ";
		}
		
		// pass the modified string through to the TTS engine
		mTts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
    }
    
    @Override
    protected void onResume() {
        
        Animation 	fadeInLogo 	= AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeInLogo.setStartTime(1500);
        fadeInLogo.setDuration(1500);
        
        ((ImageView) findViewById(R.id.main_in_app_title)).startAnimation(fadeInLogo);
        
        wakeLock				= powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "");    
        wakeLock.acquire();
        
        spnIteration.setSelection(new ApplicationSettings(this).getStartY()-1);
        
    	super.onResume();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater menuInflater = getMenuInflater();
    	menuInflater.inflate(R.layout.main_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch (item.getItemId())
    	{
	    	case R.id.generate_grid:
	    		
	    		Intent changeGrid = new Intent(OffTheGrid.this, ChangeGrid.class);
	    		OffTheGrid.this.startActivity(changeGrid);
	    		
	    		return true;
	    		
	    	case R.id.email_grid:
	    		
	    		Intent sendEmail = new Intent(OffTheGrid.this, SendEmail.class);
	    		OffTheGrid.this.startActivity(sendEmail);
	    		
	    		return true;
	    		
	    	case R.id.settings:
	    		
	    		Intent changeSettings = new Intent(OffTheGrid.this, Preferences.class);
	    		OffTheGrid.this.startActivity(changeSettings);
	    		
	    		return true;
	    		
	    	case R.id.view_grid:
	    		
	    		Intent viewGrid = new Intent(OffTheGrid.this, ShowGrid.class);
	    		OffTheGrid.this.startActivity(viewGrid);
	    		
	    		return true;
	    		
    		default:
    			return super.onOptionsItemSelected(item);
    	}
    }
    
    /***
     * A grid must be created, otherwise the app is useless.
     * Each time they get back to the main screen, prompt them to create a grid if one does not exist
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	
    	if (hasFocus)
    	{
	        if (settings.getGrid() == null || settings.getGrid().equals(""))
	        {
	        	// redirect to grid generation activity (after telling the user what is going on)
	        	new AlertDialog.Builder(this)
	        		.setTitle("No Grid Exists!")
	        		.setMessage("There is no grid generated. A grid is required for this app to work.\n\nWould you like to create one? (No will exit the app)")
	        		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							Intent changeGrid = new Intent(OffTheGrid.this, ChangeGrid.class);
				    		OffTheGrid.this.startActivity(changeGrid);
						}
					})
					.setNegativeButton("No", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// can't use the app without a grid, so exit
							finish();
						}
					})
					.show();
	        }
	        else
	        {
	        	// there is a grid value in the settings
	        	grid.setGridString(settings.getGrid(), this);
	        	
	    		// set the default state when the window gains focus (grid could have been changed or anything
	    		updateTextPassword();
	        }
    	}
    }

    /***
     * Every time the text changes or one of the settings changes, this method should be called to make sure
     * the password is displayed correctly
     */
    private void updateTextPassword()
    {
    	if (txtEnterDomain.getText().toString().equals(""))
		{
			txtShowPassword.setText(R.string.no_domain_entered);
			btnCopyPassword.setEnabled(false);
			btnSpeak.setEnabled(false);
		}
		else
		{
			String text = txtEnterDomain.getText().toString();
			
			String newPass = grid.getPasswordFromText(this, text, settings.getMaxCharacters(), spnIteration.getSelectedItemPosition()+1);
			if (newPass.equals("")) {
				txtShowPassword.setText(R.string.no_domain_entered);
			} else {
				txtShowPassword.setText(newPass);
				btnCopyPassword.setEnabled(true);
				btnSpeak.setEnabled(true);
			}
		}
    }
    
    @Override
    protected void onPause() {
    	
		String text = txtEnterDomain.getText().toString();
		
		if (!lastGen.equals(text) && !text.equals("")) {
			// password has changed or generated, log event to Flurry
			FlurryAgent.logEvent(Config.FLURRY_EVENT_PASSWORD_GEN);
		}
		lastGen = text;

		if (wakeLock != null) {
			try {
				wakeLock.release();
			} catch (Exception e) {
				// no need to release the wake lock if there isn't one
			}
		}
    	
    	super.onPause();
    }

	@Override
	public void onInit(int status) {
		
		mTts.setSpeechRate((float) SPEECH_RATE);
		mTts.setPitch((float) SPEECH_PITCH);
	}
	
	/**
     * This is the callback from the TTS engine check, if a TTS is installed we
     * create a new TTS instance (which in turn calls onInit), if not then we will
     * create an intent to go off and install a TTS engine
     * @param requestCode int Request code returned from the check for TTS engine.
     * @param resultCode int Result code returned from the check for TTS engine.
     * @param data Intent Intent returned from the TTS check.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == MY_DATA_CHECK_CODE)
        {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }
    
    /**
     * shut down TTS engine so other applications can use it
     */
    @Override
    public void onDestroy()
    {
        // Don't forget to shutdown!
        if (mTts != null)
        {
            mTts.stop();
            mTts.shutdown();
        }
        super.onDestroy();
    }
    
    /***
     * Get the letter in a way that will pronounced correctly by the TTS engine and append phrase from phonetic alphabet
     * @param character letter to look up
     * @return string that will be passed to TTS engine
     */
    private String getPhonetic(Character character) {
    	String result = "";
    	
    	switch (character) {
	    	case 'a':
	    		result = "ey for Alfa";
	    		break;
	    	case 'b':
	    		result = "B for Bravo";
	    		break;
	    	case 'c':
	    		result = "C for Charlie";
	    		break;
	    	case 'd':
	    		result = "D for Delta";
	    		break;
	    	case 'e':
	    		result = "E for Echo";
	    		break;
	    	case 'f':
	    		result = "F for Fox-trott"; 
	    		break;
	    	case 'g':
	    		result = "G for Goawlff";
	    		break;
	    	case 'h':
	    		result = "H for Hotel";
	    		break;
	    	case 'i':
	    		result = "I for India";
	    		break;
	    	case 'j':
	    		result = "J for Juliet";
	    		break;
	    	case 'k':
	    		result = "K for Kilo";
	    		break;
	    	case 'l':
	    		result = "L for Lima";
	    		break;
	    	case 'm':
	    		result = "M for Mike";
	    		break;
	    	case 'n':
	    		result = "N for November";
	    		break;
	    	case 'o':
	    		result = "O for Oscar";
	    		break;
	    	case 'p':
	    		result = "P for Pappa";
	    		break;
	    	case 'q':
	    		result = "Q for Quebec";
	    		break;
	    	case 'r':
	    		result = "R for Romeo";
	    		break;
	    	case 's':
	    		result = "S for Sierra";
	    		break;
	    	case 't':
	    		result = "T for Tango";
	    		break;
	    	case 'u':
	    		result = "U for Uniform";
	    		break;
	    	case 'v':
	    		result = "V for Victor";
	    		break;
	    	case 'w':
	    		result = "W for Whiskey";
	    		break;
	    	case 'x':
	    		result = "X for ex-ray";
	    		break;
	    	case 'y':
	    		result = "Y for Yankee";
	    		break;
	    	case 'z':
	    		result = "zet for Zulu";
	    		break;
	    	case '!':
	    		result = "exclaimation mark";
	    		break;
	    	case '\\':
	    		result = "back slash";
	    		break;
	    	case '"':
	    		result = "double quote";
	    		break;
	    	case '(':
	    		result = "open bracket";
	    		break;
	    	case ')':
	    		result = "close bracket";
	    		break;
	    	case ',':
	    		result = "comma";
	    		break;
	    	case '/':
	    		result = "forward slash";
	    		break;
	    	case ':':
	    		result = "colon";
	    		break;
	    	case ';':
	    		result = "semi-colon";
	    		break;
	    	case '?':
	    		result = "question mark";
	    		break;
	    		
	    	default:
	    		result = character.toString();
	    		break;
    	}
    	
    	return result;
    }
    
}