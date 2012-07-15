package net.rdyonline.activity;

import net.rdyonline.Config;
import net.rdyonline.Grid;
import net.rdyonline.IntentIntegrator;
import net.rdyonline.IntentResult;
import net.rdyonline.offthegrid.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

/***
 * This activity is used to show the user the options that are available for changing their grid
 * They should be able to either generate a new grid to use or capture a grid that already exists
 * @author rdy
 *
 */
public class ChangeGrid extends Activity {

	private Context context;
	private Grid grid = null;
	
	private ProgressDialog pd;
	
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
		setContentView(R.layout.change_grid);
		
		this.context = this;
		this.grid = Grid.getInstance(this);
        
        /**
         * Button to allow user to capture an existing grid
         * Use the BarCode scanner app to capture the QR code that was printed out on to the grid sheet
         */
        final Button btnCaptureGrid = (Button)findViewById(R.id.btnCaptureGrid);
        btnCaptureGrid.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			
				// check to see if there's already a grid
				if (grid.getGridString() != null)
				{
					new AlertDialog.Builder(ChangeGrid.this)
				    .setTitle("Confirm action")
				    .setMessage("This will clear any existing grid and overwrite it with the one that is captured. Are you sure you want to do this?")
				    
				    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {

				        	// user confirmed it's OK to override their grid. Capture one
				        	IntentIntegrator integrator = new IntentIntegrator(ChangeGrid.this);
				        	integrator.initiateScan();
				        	   			        	
				        }
				    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				            // nothing to be done
				        }
				    }).show();
				}
				else
				{
					// user doesn't have a grid, go ahead and try to capture one
					IntentIntegrator integrator = new IntentIntegrator(ChangeGrid.this);
					integrator.initiateScan();
				}
			}
		});
        
        /***
         * Button to allow user to generate a new grid.
         * Grid generation is quite an intensive operation and might take a couple of seconds
         */
        final Button btnGenerateGrid = (Button) findViewById(R.id.btnGenerateGrid);
        btnGenerateGrid.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				// check to see if there's already a grid
				if (grid.getGridString() != null)
				{
					new AlertDialog.Builder(ChangeGrid.this)
				    .setTitle("Confirm action")
				    .setMessage("This will clear any existing grid and generate a brand new one. Are you sure you want to do this?")
				    
				    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				            
				        	// user confirmed they want to override their grid. Create a new one
				        	generateGrid();
				        	   			        	
				        }
				    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				            // nothing to be done
				        }
				    }).show();
				}
				else
				{
					// there isn't already a grid, so just generate one (no confirmation needed)
					generateGrid();
				}
			}
		});
	}
	
	/***
	 * Generation of the grid can be quite expensive operation, so make sure the UI tells them the phone is busy
	 */
	protected void generateGrid()
	{
		pd = ProgressDialog.show(context, "Generating grid", "Please be patient, generating a new grid to use can take a few seconds..");
    	
    	Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Grid.getInstance(getApplicationContext()).generateGrid(getApplicationContext());
				
				pd.dismiss();
				
				// grid has been generated, show the user the grid that they have just captured/created
				Intent viewGrid = new Intent(ChangeGrid.this, ShowGrid.class);
	    		ChangeGrid.this.startActivity(viewGrid);
	    		finish();
			}
		});
    	
    	thread.start();
	}

    /**
     * When the intent returns the value from the QR code scanning, use the value passed back to generate the grid
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	try
    	{
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
			
			if (scanResult != null)
			{
				FlurryAgent.logEvent(Config.FLURRY_EVENT_GRID_CAPTURED);
				
				String contents = intent.getStringExtra("SCAN_RESULT");
				
				// the ZXing lib trims the string - add the first and last char back in
				contents = " " + contents.trim() + " ";

				grid.setGridString(contents, getApplicationContext());

				// grid has been updated, show the user the grid
				Intent viewGrid = new Intent(ChangeGrid.this, ShowGrid.class);
				ChangeGrid.this.startActivity(viewGrid);
				finish();
			}
    	}
    	catch (Exception ex)
    	{
    		Toast.makeText(context, "No QR code was captured, so no grid could be read. Please try again", Toast.LENGTH_LONG).show();
    		Log.w("ZXing", "Couldn't get QR code");
    	}
    }
}
