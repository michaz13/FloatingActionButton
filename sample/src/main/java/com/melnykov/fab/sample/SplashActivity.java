package com.melnykov.fab.sample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.graphics.Color;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

public class SplashActivity extends Activity {

	private static String TAG = SplashActivity.class.getName();
	private static long SLEEP_TIME = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
      	this.requestWindowFeature(Window.FEATURE_NO_TITLE);	// Removes title bar
      	this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);	// Removes notification bar
      	setContentView(R.layout.splash);

		ImageView imageView = (ImageView)findViewById(R.id.logo_image);
		//Parse the SVG file from the resource
		SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.app_logo);
		//Get a drawable from the parsed SVG and apply to ImageView
		imageView.setImageDrawable(svg.createPictureDrawable());
        // Disable hardware acceleration on all versions starting from Honeycomb
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        // Start timer and launch main activity
        IntentLauncher launcher = new IntentLauncher();
        launcher.start();
	}

	private class IntentLauncher extends Thread {
    
		@Override
    	/**
    	 * Sleep for some time and than start new activity.
    	 */
		public void run() {
    		try {
                // TODO: 14/09/2015 sync with cloud instead
                // Sleeping
    			Thread.sleep(SLEEP_TIME*1000);
            } catch (Exception e) {
            	Log.e(TAG, e.getMessage());
            }
            
            // Start main activity
          	Intent intent = new Intent(SplashActivity.this, MainActivity.class);
          	SplashActivity.this.startActivity(intent);
          	SplashActivity.this.finish();
    	}
    }
	
}
