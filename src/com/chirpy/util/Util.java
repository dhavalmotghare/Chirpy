package com.chirpy.util;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Utility class for commonly used methods
 * 
 * @author dhaval.motghare
 *
 */
public class Util {
	
	/** Log tag */
	public static final String TAG = "ChirpyUtil";
	
	/**
	 * Used to decide whether a WIFI network is available.
	 * 
	 * @param context
	 * @return true: WIFI network is available. false: Wifi network is not
	 *         available.
	 */
	public static boolean networkAvailable(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connManager.getActiveNetworkInfo();
		if (info != null) {
			// Only support download over WiFi
			if (info.getType() == ConnectivityManager.TYPE_WIFI) {
				return info.isConnected();
			} else {
				return true;//doesn't work on emulator so returning true
			}
		}
		return true;//doesn't work on emulator so returning true
	}
	
	/**
	 * Get a JSONObject for the passed string
	 * 
	 * @param response
	 * @return JSONObject
	 */
	public static JSONObject getJsonObject(String response){
		try {
			return new JSONObject(response);
		} catch (JSONException e) {
			return new JSONObject();
		}
	}
	
	/**
	 * Convert to integer
	 * 
	 * @param value
	 * @return int
	 */
	public static int convertToInt(String value){
		int v = 0;
		try{
			v = Integer.parseInt(value);
		}catch(Exception e){
			
		}
		return v;
	}
	
	
}
