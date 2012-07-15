package net.rdyonline.activity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.rdyonline.Config;
import net.rdyonline.Grid;
import net.rdyonline.offthegrid.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;

import com.flurry.android.FlurryAgent;

/***
 * Activity with the sole purpose to display the users grid to them
 * This might be to confirm that the grid has captured properly or just as a confirmation that a new grid has been created
 * @author rdy
 *
 */
public class ShowGrid extends Activity {

	private Grid grid = null;
	private Button btnEmail = null;
	
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
		setContentView(R.layout.show_grid);
		
		this.grid = Grid.getInstance(this);
		this.btnEmail = (Button) findViewById(R.id.btnEmailGrid);
        
        btnEmail.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent sendEmail = new Intent(ShowGrid.this, SendEmail.class); 
				ShowGrid.this.startActivity(sendEmail);
				finish();
			}
		});
        
        // generate a HTML grid on the fly (similar to email)
        // this is added to a webview so that the pinch to zoom, etc functionality can be exposed
        // simply by using native controls rather than having to code in pinch to zoom functionality
        String templateHTML = "";
		
		try {
			
	        InputStream myInput = getAssets().open("template_grid_only.html");
	        BufferedReader inputStream = new BufferedReader(
	                new InputStreamReader(myInput));

	        StringBuilder sb = new StringBuilder();
	        String line;
	        
	        while ((line = inputStream.readLine()) != null) {
	        	sb.append(line);
	        	sb.append("\n");
	        }
	        
	        inputStream.close();
	        
	        templateHTML = sb.toString();
	        templateHTML = templateHTML.replace("[GRID]", SendEmail.buildHtmlGrid(this.grid));
	        
	        WebView wvShowGrid = (WebView) findViewById(R.id.wvShowGrid);
	        wvShowGrid.getSettings().setJavaScriptEnabled(false);
	        wvShowGrid.getSettings().setBuiltInZoomControls(true);
	        
	        wvShowGrid.loadDataWithBaseURL(null, templateHTML.toString(), "text/html", "utf-8", null);
	        
		} catch (Exception ex) {
			Log.e("Grid template", "error creating a grid using template.html");
		}
	}
	
    @Override
    protected void onResume() {
    	
        Animation slideInView = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        slideInView.setStartTime(1000);
        slideInView.setDuration(1000);
        
        ((WebView) findViewById(R.id.wvShowGrid)).startAnimation(slideInView);
        
        FlurryAgent.logEvent(Config.FLURRY_EVENT_GRID_VIEWED);
        
    	super.onResume();
    }
}
