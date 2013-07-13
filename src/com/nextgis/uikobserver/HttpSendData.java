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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;


public class HttpSendData extends AsyncTask<String, Void, Void> {
    private Context mContext;
    private String mError = null;
    private ProgressDialog mDownloadDialog = null;
    private String mDownloadDialogMsg;
    private int mnType;
    private Handler mEventReceiver;
    private boolean mbShowProgress;
	
    public HttpSendData(Context c, int nType, String sMsg, Handler eventReceiver, boolean bShowProgress) {        
        super();
        mbShowProgress = bShowProgress;
        mContext = c;
        if(mbShowProgress){
        	mDownloadDialog = new ProgressDialog(mContext);
        }
        mnType = nType;  
        mEventReceiver = eventReceiver;
        mDownloadDialogMsg = sMsg;      
    }
    
    @Override
    protected void onPreExecute() {
    	super.onPreExecute();
    	if(mbShowProgress){
    		mDownloadDialog.setMessage(mDownloadDialogMsg);
    		mDownloadDialog.show();
    	}
    } 
    
	static boolean IsNetworkAvailible(Context c)
	{
		ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);  
        
        NetworkInfo info = cm.getActiveNetworkInfo();
		if (info == null /*|| !cm.getBackgroundDataSetting()*/) 
			return false;
		
		int netType = info.getType();
		//int netSubtype = info.getSubtype();
		if (netType == ConnectivityManager.TYPE_WIFI) {
			return info.isConnected();
		} 
		else if (netType == ConnectivityManager.TYPE_MOBILE
		&& /*netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
		&&*/ !tm.isNetworkRoaming()) {
			return info.isConnected();
		} 
		else {
			return false;
		}	
	}    

    @Override
    protected Void doInBackground(String... urls) {
        if(IsNetworkAvailible(mContext))
        {    	
        	String sPostBody = urls[0];
        	
        	// Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            
            HttpPost httppost = new HttpPost("http://gis-lab.info:8090/");
            
            HttpParams params = httppost.getParams();
            params.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
            
            HttpContext localContext = new BasicHttpContext();

            try {
            	StringEntity se = new StringEntity(sPostBody, "UTF8");
            	se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httppost.setEntity(se);                
                httppost.setHeader("Content-type", "application/json");

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost, localContext);
                
	            Bundle bundle = new Bundle();
				if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
					bundle.putBoolean("error", false);
				}
				else{
					bundle.putBoolean("error", true);
				}				
                
                bundle.putInt("src", mnType);
	            Message msg = new Message();
	            msg.setData(bundle);
	            if(mEventReceiver != null){
	            	mEventReceiver.sendMessage(msg);
	            }
	            
            } catch (ClientProtocolException e) {
            	mError = e.getMessage();
	            cancel(true);
            } catch (IOException e) {
            	mError = e.getMessage();
	            cancel(true);
            }
        }
        else {
        	Bundle bundle = new Bundle();
            bundle.putBoolean("error", true);
            bundle.putString("err_msq", mContext.getString(R.string.sNetworkUnreach));
            bundle.putInt("src", mnType);
            
            Message msg = new Message();
            msg.setData(bundle);
            if(mEventReceiver != null){
            	mEventReceiver.sendMessage(msg);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
    	super.onPostExecute(unused);
    	if(mbShowProgress){
    		mDownloadDialog.dismiss();
    	}
        if (mError != null) {
        	Bundle bundle = new Bundle();
            bundle.putBoolean("error", true);
            bundle.putString("err_msq", mError);
            bundle.putInt("src", mnType);
            
            Message msg = new Message();
            msg.setData(bundle);
            if(mEventReceiver != null){
            	mEventReceiver.sendMessage(msg);
            }
        } else {
            //Toast.makeText(FireReporter.this, "Source: " + Content, Toast.LENGTH_LONG).show();
        }
    }

	@Override
	protected void onCancelled() {		
		super.onCancelled();
		if(mbShowProgress){
			mDownloadDialog.dismiss();
		}
	}
}
