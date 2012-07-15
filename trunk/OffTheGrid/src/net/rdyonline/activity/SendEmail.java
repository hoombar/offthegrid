package net.rdyonline.activity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import net.rdyonline.Config;
import net.rdyonline.Grid;
import net.rdyonline.offthegrid.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

/***
 * Allowing the user to send themself a copy of their grid is important in case they change devices or want to share their grid with someone else
 * This activity provides the functionality to email themselves the grid with instructions on how to use it. When the grid is emailed,
 * it's encrypted with 256bit AES.
 * @author rdy
 *
 */
public class SendEmail extends Activity {
	
	private Context activityContext = null;
	
	private Grid grid = null;
	
	// grid is created dynamically and added to HTML template- this includes all styling
	public static final String CSS_BG_SHADED = "bg-shaded";
	public static final String CSS_FG_LIGHT = "fg-light";
	public static final String CSS_FG_DARK = "fg-dark";
	public static final String CSS_BORDER_BOTTOM_LIGHT = "border-bottom-light";
	public static final String CSS_BORDER_BOTTOM_DARK = "border-bottom-dark";
	public static final String CSS_BORDER_RIGHT_LIGHT = "border-right-light";
	public static final String CSS_BORDER_LEFT_LIGHT = "border-left-light";
	public static final String CSS_BORDER_TOP_LIGHT = "border-top-light";

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
		setContentView(R.layout.send_email);
		
		this.grid = Grid.getInstance(this);
	
		final Button btnSendEmail = (Button) findViewById(R.id.btnSendEmail);
		final TextView txtPassword = (TextView) findViewById(R.id.txtPassword);
		
		txtPassword.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {

				// enable the email button if the password is longer than (or equal to) 8 characters 
				btnSendEmail.setEnabled(
							(txtPassword.getText().toString().length() >= 8)
						);
				
			}
		});
		
		btnSendEmail.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				// check free space available - if there isn't enough a zip can't be created
				StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
				long bytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
				long kbAvailable = bytesAvailable / 1024;
				
				// be pro-active about checking whether the IO operation will fail as a result of lack of disk space
				if (kbAvailable < 50)
				{
					Toast.makeText(
							activityContext, 
							"At least 50Kb of space is required to create a temporary encrypted zip file. Please free up 50K of space.", 
							Toast.LENGTH_LONG
						)
						.show();
					
					return;
				}
				
				// create a QR code 300x300 px
				Bitmap bm = generateQRBitmap(grid.getGridString(), 300, 300);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();  
				bm.compress(Bitmap.CompressFormat.PNG, 100, baos); //bm is the bitmap object   
				byte[] b = baos.toByteArray(); 
				
				// need to base 64 encode the bitmap so it can be embedded in the HTML
				String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
				
				// space available to create a zip file - use the template to create the HTML required in a memory stream
		        String outFileName = android.os.Environment.getExternalStorageDirectory() + File.separator + "content.zip";
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				String templateHTML = "";
				
				try {
					
			        InputStream myInput = getAssets().open("template.html");
			        BufferedReader inputStream = new BufferedReader(new InputStreamReader(myInput));
			        StringBuilder sb = new StringBuilder();
			        String line;
			        File checkfile = new File(outFileName);
			        
			        if (checkfile.exists()) {
			            checkfile.delete();
			        }
			        
			        while ((line = inputStream.readLine()) != null) {
			        	sb.append(line);
			        	sb.append("\n");
			        }
			        
			        inputStream.close();
			        
			        templateHTML = sb.toString();
			        templateHTML = templateHTML.replace("[GRID]", buildHtmlGrid(grid));
			        templateHTML = templateHTML.replace("[QR_IMAGE_DATA]", encodedImage);
			        
			        InputStream is = new ByteArrayInputStream( templateHTML.getBytes( /* default charset */ ) );
			        
					try {
						ZipFile zipFile = new ZipFile(outFileName);
						
						ZipParameters parameters = new ZipParameters();
						parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE); // set compression method to deflate compression
						
						// DEFLATE_LEVEL_NORMAL - Optimal balance between compression level/speed
						parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL); 
						
						// Set the encryption flag to true
						parameters.setEncryptFiles(true);
						
						// Set the encryption method to AES Zip Encryption
						parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
						
						// AES_STRENGTH_256 - For both encryption and decryption
						parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
						
						// Set password
						parameters.setPassword(txtPassword.getText().toString());
						
						// this would be the name of the file for this entry in the zip file
						parameters.setFileNameInZip("grid.html");
						
						// we set this flag to true. If this flag is true, Zip4j identifies that
						// the data will not be from a file but directly from a stream
						parameters.setSourceExternalStream(true);
						
						// Creates a new entry in the zip file and adds the content to the zip file
						zipFile.addStream(is, parameters);
						
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (is != null) {
							try {
								is.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
			        
			        // TODO(benp): looks like the tempFile isn't being cleaned. clean up in finally block
			        File tempfile = new File(outFileName);
					
					emailIntent.setType("text/plain");
					emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Off the Grid: zip containing your grid and QR code");
					emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempfile));
					emailIntent.putExtra(
						android.content.Intent.EXTRA_TEXT, getString(
								R.string.email_text)
							);
					startActivity(Intent.createChooser(emailIntent, "Email:"));
					
					FlurryAgent.logEvent(Config.FLURRY_EVENT_GRID_EMAILED);

				} catch (Exception e) {
					Log.e("email error", e.toString());
				}
				
				finish();
			}
		});
	}
	
	/***
	 * Grid is built on the fly to attach to the email
	 * @param grid an instance of the current users grid
	 * @return a HTML table tag with all required child nodes that compose an entire HTML instance of the grid
	 */
	public static String buildHtmlGrid(Grid grid)
	{
		StringBuilder result = new StringBuilder();
		char[] gridChars = grid.getGridString().toCharArray();
		
		result.append("<table cellspacing=\"0\">");
		
		for (int i = 0; i < gridChars.length; i++)
		{
			List<String> currentCellCSS = new ArrayList<String>();
			
			if (i % 28 == 0)
			{
				result.append("<tr>");
			}

			if (i < 28) {
				// top cells all have a light foreground
				currentCellCSS.add(CSS_FG_LIGHT);
			}
			else if (i % 28 == 0 || i % 28 == 27)
			{
				// the first cells (on the left) all have a light foreground
				currentCellCSS.add(CSS_FG_LIGHT);
			}
			
			if ((i % 28 == 1 || i % 28 == 27) && i < 757 && i > 27)
			{
				currentCellCSS.add(CSS_BORDER_LEFT_LIGHT);
			}
			
			if (i > 28 && i < 55)
			{
				currentCellCSS.add(CSS_BORDER_TOP_LIGHT);
			}
			
			if ((Math.floor(i / 28) + 4) % 2 == 0 && Math.floor(i / 28) < 26 && Math.floor(i / 28) > 1 && (i % 28 > 0 && i % 28 < 27))
			{
				currentCellCSS.add(CSS_BORDER_BOTTOM_DARK);
			}
			
			if ( ((i % 28) + 5) % 4 > 1 &&  ( (Math.floor(i / 28) > 0) && (Math.floor(i / 28) < 27) ) ) 
			{
				currentCellCSS.add(CSS_BG_SHADED);
			}
			
			if (i > 756 && i < 783)
			{
				currentCellCSS.add(CSS_FG_LIGHT);
				currentCellCSS.add(CSS_BORDER_TOP_LIGHT);
			}
			
			String styleRule = "";
			if (currentCellCSS.size() > 0)
			{
				StringBuilder sb = new StringBuilder();
				
				for (String rule : currentCellCSS)
					sb.append(rule + " ");
				
				sb.delete(sb.length()-1, sb.length());

				styleRule = " class=\"" + sb.toString() + "\"";
			}
			
			
			result.append("<td" + styleRule + ">" + gridChars[i] + "</td>\n");
			
			if (i % 28 == 27)
			{
				result.append("</tr>");
			}
		}
		
		result.append("</table>");
		
		return result.toString();
	}
	
	/***
	 * Generate a QR code using the ZXing library
	 * @param grid flattened grid string
	 * @param width width of the QR code in px
	 * @param height height of the QR code in px
	 * @return
	 */
	public static Bitmap generateQRBitmap(String grid, int width, int height) {
		
        Hashtable hints = null;
        MultiFormatWriter writer = new MultiFormatWriter();    
        BitMatrix result = null;
		try {
			result = writer.encode(grid, BarcodeFormat.QR_CODE, width, height, hints);
		} catch (WriterException e) {
			e.printStackTrace();
		}

        int[] pixels = new int[width * height];

		if (result != null) {
            // All are 0, or black, by default
            for (int y = 0; y < height; y++) {
              int offset = y * width;
              for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
              }
            }
		}

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        
        return bitmap;
    }
}
