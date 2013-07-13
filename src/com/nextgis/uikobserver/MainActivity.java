/*******************************************************************************
*
* UIKObserver
* ---------------------------------------------------------
* Send location of UIK to Internet based Database
*
* Copyright (C) 2013 NextGIS (http://nextgis.ru)
*
* This source is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free
* Software Foundation; either version 2 of the License, or (at your option)
* any later version.
*
* This code is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
* details.
*
* A copy of the GNU General Public License is available on the World Wide Web
* at <http://www.gnu.org/copyleft/gpl.html>. You can also obtain it by writing
* to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
* MA 02111-1307, USA.
*
*******************************************************************************/
package com.nextgis.uikobserver;

import java.text.DecimalFormat;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	public static Button sendButton;
    protected LocationManager locationManager;
    protected CurrentLocationListener currentLocationListener;
    
    public static double dfLon;
	public static double dfLat;
	public static double dfAcc;
	public static String sProv, sUIK, sMail, sNote;
	
	public static TextView tvLoc, tvUIK, tvEMail, tvNote;
	
	public static final char DEGREE_CHAR = (char) 0x00B0;
	private String sN, sS, sW, sE;
	private String sCoordLat, sCoordLon;
	
	private Handler mFillDataHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        mFillDataHandler = new Handler() {
            public void handleMessage(Message msg) {
            	Bundle resultData = msg.getData();
            	boolean bHaveErr = resultData.getBoolean("error");
            	if(bHaveErr){
            		Toast.makeText(MainActivity.this, getResources().getText(R.string.sFailed), Toast.LENGTH_LONG).show();
            	}
            	else{
            		Toast.makeText(MainActivity.this, getResources().getText(R.string.sSuccess), Toast.LENGTH_LONG).show();
            	}
            };
        };	    
		
		
		sendButton =  (Button) findViewById(R.id.sendDataBtn);
		sendButton.setEnabled(false);
		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				SendData();
			} 
		});	
		
		tvLoc = (TextView)findViewById(R.id.locationString);
		
		tvUIK = (TextView)findViewById(R.id.uikNo);
		tvEMail = (TextView)findViewById(R.id.userMail);
		tvNote = (TextView)findViewById(R.id.notesString);
		
		
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		currentLocationListener = new CurrentLocationListener();
		
		dfLon = 200;
		dfLat = 200;
		
    	sN = (String) getResources().getText(R.string.compas_N);
    	sS = (String) getResources().getText(R.string.compas_S);
    	sW = (String) getResources().getText(R.string.compas_W);
    	sE = (String) getResources().getText(R.string.compas_E);
    	
    	sCoordLat = (String) getResources().getText(R.string.coord_lat);
    	sCoordLon = (String) getResources().getText(R.string.coord_lon);
    	
    	//sUIK, sMail, sNote

		requestLocationUpdates();		
	}
	
	private void SendData(){
				
		sUIK = tvUIK.getText().toString();
		sMail = tvEMail.getText().toString();
		sNote = tvNote.getText().toString();
		 
		JSONObject holder = new JSONObject();
		try {
			holder.put("uik", sUIK);
			holder.put("email", sMail);
			holder.put("note", sNote);
			holder.put("lat", dfLat);
			holder.put("lon", dfLon);
			holder.put("accuracy", dfAcc);
			holder.put("loc_provide", sProv);
			HttpSendData oPost = new HttpSendData(this, 1, getResources().getString(R.string.sDownLoading), mFillDataHandler, true);
			String sPayLoad = holder.toString();
			oPost.execute(sPayLoad);		
		} catch (JSONException e) {
			Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}		

	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void requestLocationUpdates(){
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, currentLocationListener);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, currentLocationListener);				 
	}
	
	private void removeUpdates(){
		locationManager.removeUpdates(currentLocationListener);
	}
	
	@Override
	protected void onResume() {
        super.onResume();
        
        requestLocationUpdates();
    }
 
	@Override
    protected void onPause() {
        super.onPause();
        
        removeUpdates();
    }	
    
	@Override
    protected void onStop() {
        super.onStop();
        
        removeUpdates();
    }	
	
	
	private final class CurrentLocationListener implements LocationListener {
		public CurrentLocationListener() {
			super();
		}

		public void onLocationChanged(Location location) {
			MainActivity.dfLat = location.getLatitude();
			MainActivity.dfLon = location.getLongitude();
			MainActivity.dfAcc = location.getAccuracy();
			MainActivity.sProv = location.getProvider();
			MainActivity.sendButton.setEnabled(true);
			
			tvLoc.setText(GetCoordinates());
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub	
		}
	}
	
	public String formatLat(double lat, int outputType) {

		String direction = sN;
		if (lat < 0) {
			direction = sS;
			lat = -lat;
		}

		return formatCoord(lat, outputType) + direction;

	}

	public String formatLat(double lat) {
		return formatLat(lat, Location.FORMAT_DEGREES);
	}

	public String formatLon(double lng, int outputType) {

		String direction = sE;
		if (lng < 0) {
			direction = sW;
			lng = -lng;
		}

		return formatCoord(lng, outputType) + direction;

	}

	public String formatLon(double lng) {
		return formatLon(lng, Location.FORMAT_DEGREES);
	}
	
	/**
	 * Formats coordinate value to string based on output type (modified version
	 * from Android API)
	 */
	public static String formatCoord(double coordinate, int outputType) {

		StringBuilder sb = new StringBuilder();
		char endChar = DEGREE_CHAR;

		DecimalFormat df = new DecimalFormat("###.######");
		if (outputType == Location.FORMAT_MINUTES || outputType == Location.FORMAT_SECONDS) {

			df = new DecimalFormat("##.###");

			int degrees = (int) Math.floor(coordinate);
			sb.append(degrees);
			sb.append(DEGREE_CHAR); // degrees sign
			endChar = '\''; // minutes sign
			coordinate -= degrees;
			coordinate *= 60.0;

			if (outputType == Location.FORMAT_SECONDS) {

				df = new DecimalFormat("##.##");

				int minutes = (int) Math.floor(coordinate);
				sb.append(minutes);
				sb.append('\''); // minutes sign
				endChar = '\"'; // seconds sign
				coordinate -= minutes;
				coordinate *= 60.0;
			}
		}

		sb.append(df.format(coordinate));
		sb.append(endChar);

		return sb.toString();
	}	
	
	public String GetCoordinates(){
    	String sOut;
    	sOut = sCoordLat + ": " + formatLat(dfLat, Location.FORMAT_SECONDS);
    	sOut += "\n";
    	sOut += sCoordLon + ": " + formatLon(dfLon, Location.FORMAT_SECONDS);			

		return sOut;
	}
}
