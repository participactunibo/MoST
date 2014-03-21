/*
 * Copyright (C) 2014 University of Bologna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.most.input;

import java.util.Locale;

import org.most.DataBundle;
import org.most.MoSTApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class PeriodicConnectionTypeInput extends PeriodicInput {

	@SuppressWarnings("unused")
	private final static String TAG = PeriodicConnectionTypeInput.class.getSimpleName();

	/**
	 * {@link SharedPreferences} key to set the statistics monitoring period.
	 */
	public final static String PREF_KEY_CONNECTION_TYPE_PERIOD = "PeriodicConnectionTypeInputMs";

	/**
	 * Default net traffic monitoring interval in milliseconds. Currently set to
	 * {@value #PREF_DEFAULT_CONNECTION_TYPE_PERIOD}.
	 */
	public final static int PREF_DEFAULT_CONNECTION_TYPE_PERIOD = 1000 * 60 * 5;

	public final static String KEY_CONNECTION_TYPE = "PeriodicConnectionTypeInput.connectionType";
	public final static String KEY_MOBILE_NETWORK_TYPE = "PeriodicConnectionTypeInput.mobileNetworkType";

	private final static String NO_CONNECTION = "NONE";
	private final static String NO_DATA = "";


	/**
	 * @param context
	 */
	public PeriodicConnectionTypeInput(MoSTApplication context) {
		super(context, context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getInt(
				PREF_KEY_CONNECTION_TYPE_PERIOD, PREF_DEFAULT_CONNECTION_TYPE_PERIOD));
	}

	@Override
	public void workToDo() {
		DataBundle b = _bundlePool.borrowBundle();
		b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis());
		b.putInt(Input.KEY_TYPE, Input.Type.PERIODIC_CONNECTION_TYPE.toInt());
		if(isConnected(getContext())){
			if(isConnectedWifi(getContext())){
				b.putString(KEY_CONNECTION_TYPE, getNetworkInfo(getContext()).getTypeName());
				b.putString(KEY_MOBILE_NETWORK_TYPE, NO_DATA);
				
			}else if(isConnectedMobile(getContext())){
				b.putString(KEY_CONNECTION_TYPE, getNetworkInfo(getContext()).getTypeName().toUpperCase(Locale.ITALY));
				b.putString(KEY_MOBILE_NETWORK_TYPE, getNetworkInfo(getContext()).getSubtypeName());
			}
			
		}else{
			b.putString(KEY_CONNECTION_TYPE, NO_CONNECTION);
			b.putString(KEY_MOBILE_NETWORK_TYPE, NO_DATA);
		}

		post(b);
		scheduleNextStart();
	}

	@Override
	public Type getType() {
		return Input.Type.PERIODIC_CONNECTION_TYPE;
	}
	
	
	public static NetworkInfo getNetworkInfo(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    /**
     * Check if there is any connectivity
     * @param context
     * @return
     */
    public static boolean isConnected(Context context){
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected());
    }

    /**
     * Check if there is any connectivity to a Wifi network
     * @param context
     * @param type
     * @return
     */
    public static boolean isConnectedWifi(Context context){
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }

    /**
     * Check if there is any connectivity to a mobile network
     * @param context
     * @param type
     * @return
     */
    public static boolean isConnectedMobile(Context context){
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
    }
    
}
