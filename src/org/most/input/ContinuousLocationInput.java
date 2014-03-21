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

import org.most.DataBundle;
import org.most.MoSTApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

/**
 * This inputs periodically provides the location of the phone, from the best
 * available location provider. It periodically outputs a {@link DataBundle}
 * containing:
 * <ul>
 * <li>{@link Input#KEY_TYPE} (int): type of the sensor, convert it to a
 * {@link Input#Type} using {@link Input#Type.fromInt()}. Set to
 * {@link Input.Type#LOCATION}.</li>
 * <li> {@link Input#KEY_TIMESTAMP} (long): timestamp in milliseconds of the
 * location.</li>
 * <li> {@link #KEY_LATITUDE} (double): location latitute.</li>
 * <li> {@link #KEY_LONGITUDE} (double): location longitude.</li>
 * <li> {@link #KEY_ACCURACY} (double): accuracy of the location.</li>
 * <li> {@link #KEY_PROVIDER} (String): name of the location provider.</li>
 * </ul>
 * 
 * This input supports the minimum interval in milliseconds between two location
 * queries to be configured. The {@link SharedPreferences} name to use is
 * {@link MoSTApplication#PREF_INPUT}, the key value to use is
 * {@link #PREF_KEY_LOCATION_MINTIME_MS}. Its default value is
 * {@link #PREF_DEFAULT_LOCATION_MINTIME} (
 * {@value #PREF_DEFAULT_LOCATION_MINTIME} milliseconds). Choosing a sensible
 * value for minTime is important to conserve battery life. Each location update
 * requires power from GPS, WIFI, Cell and other radios. Select a minTime value
 * as high as possible. The minimum interval is just a hint for Android versions
 * prior to Jellybean, from Jellybean onwards the minimum time is respected by
 * all location providers. For example:
 * 
 * <pre>
 * {
 * 	Editor editor = context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).edit();
 * 	editor.putLong(PREF_DEFAULT_LOCATION_MINTIME, 5 * 60 * 1000);
 * 	editor.apply();
 * }
 * </pre>
 * 
 * The {@link #PREF_KEY_LOCATION_ENABLE_NETWORK} preference sets whether this
 * input will use the network as location provider. By default it is set to
 * {@link #PREF_DEFAULT_LOCATION_ENABLE_NETWORK} (
 * {@value #PREF_DEFAULT_LOCATION_ENABLE_NETWORK}). The
 * {@link #PREF_KEY_LOCATION_ENABLE_GPS} preference sets whether this input will
 * use the network as location provider. By default it is set to
 * {@link #PREF_DEFAULT_LOCATION_ENABLE_GPS} (
 * {@value #PREF_DEFAULT_LOCATION_ENABLE_GPS}).
 * 
 * @author acirri
 * @author gcardone
 * 
 */
public class ContinuousLocationInput extends Input {

	/** The Constant DEBUG. */
	private final static boolean DEBUG = true;

	/** The Constant TAG. */
	private final static String TAG = ContinuousLocationInput.class.getSimpleName();

	public final static String KEY_LONGITUDE = "LocationInput.Longitude";
	public final static String KEY_LATITUDE = "LocationInput.Latitude";
	public final static String KEY_ACCURACY = "LocationInput.Accuracy";
	public final static String KEY_PROVIDER = "LocationInput.Provider";

	public static final long SIGNIFICANT_TIME_DIFFERENCE = 15000; // 15 seconds

	public static final String PREF_KEY_LOCATION_MINTIME_MS = "LocationInput.MinTime";
	public static final long PREF_DEFAULT_LOCATION_MINTIME = 1000 * 60 * 10;

	public static final String PREF_KEY_LOCATION_ENABLE_NETWORK = "LocationInput.EnableNetwork";
	public static final boolean PREF_DEFAULT_LOCATION_ENABLE_NETWORK = true;
	public static final String PREF_KEY_LOCATION_ENABLE_GPS = "LocationInput.EnableGPS";
	public static final boolean PREF_DEFAULT_LOCATION_ENABLE_GPS = true;

	private LocationManager _locationManager;
	private LocationListener _locationListener;
	private Location _lastLocation;

	/**
	 * Creates a new LocationInput.
	 * 
	 * @param context
	 *            The reference {@link MoSTApplication} context.
	 */
	public ContinuousLocationInput(MoSTApplication context) {
		super(context);
	}

	@Override
	public void onInit() {
		checkNewState(Input.State.INITED);
		_locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
		_lastLocation = bestCachedLocation();
		_locationListener = new MoSTLocationListener();
		super.onInit();
	}

	@Override
	public boolean onActivate() {
		checkNewState(Input.State.ACTIVATED);
		long minTime = getContext().getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getLong(
				PREF_KEY_LOCATION_MINTIME_MS, PREF_DEFAULT_LOCATION_MINTIME);
		boolean useNetwork = getContext().getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE)
				.getBoolean(PREF_KEY_LOCATION_ENABLE_NETWORK, PREF_DEFAULT_LOCATION_ENABLE_NETWORK);
		boolean useGPS = getContext().getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE)
				.getBoolean(PREF_KEY_LOCATION_ENABLE_GPS, PREF_DEFAULT_LOCATION_ENABLE_GPS);
		if (!useNetwork && !useGPS) {
			Log.w(TAG, "No location provider enabled, check the preferences for this input");
		}
		
		if (useNetwork && !_locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			Log.e(TAG, "NETWORK location requested but NETWORK provider disabled");
		}
		
		if (useGPS && !_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Log.e(TAG, "GPS location requested but GPS provider disabled");
		}

		if (useNetwork) {
			_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, 0, _locationListener);
		}
		if (useGPS) {
			_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 0, _locationListener);
		}
		return super.onActivate();
	}

	@Override
	public void onDeactivate() {
		_locationManager.removeUpdates(_locationListener);
		super.onDeactivate();
	}

	@Override
	public void onFinalize() {
		super.onFinalize();
	}

	private Location bestCachedLocation() {
		Location lastGps = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location lastNetwork = _locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location result = lastGps;
		if (result == null || (lastNetwork != null && lastNetwork.getTime() > result.getTime())) {
			result = lastNetwork;
		}
		return result;
	}

	@Override
	public Type getType() {
		return Input.Type.CONTINUOUS_LOCATION;
	}

	private class MoSTLocationListener implements LocationListener {

		public void onLocationChanged(Location newLocation) {
			if (newLocation == null || (newLocation.getLatitude() == 0.0 && newLocation.getLongitude() == 0.0)) {
				// filter out 0.0, 0.0 locations
				return;
			}
			if (DEBUG) {
				Log.d(TAG, String.format("New location:  lat %f - long %f (accuracy: %f)", newLocation.getLatitude(),
						newLocation.getLongitude(), newLocation.getAccuracy(), newLocation.getProvider()));
			}
			if (isBetterThanCurrent(newLocation)) {
				_lastLocation = newLocation;
				DataBundle b = _bundlePool.borrowBundle();
				b.putDouble(KEY_LATITUDE, _lastLocation.getLatitude());
				b.putDouble(KEY_LONGITUDE, _lastLocation.getLongitude());
				b.putDouble(KEY_ACCURACY, _lastLocation.getAccuracy());
				b.putString(KEY_PROVIDER, _lastLocation.getProvider());
				b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis());
				b.putInt(Input.KEY_TYPE, Input.Type.CONTINUOUS_LOCATION.toInt());
				post(b);
			}

		}

		private boolean isBetterThanCurrent(Location newLocation) {
			if (_lastLocation == null) {
				return true;
			}
			long timeDiff = newLocation.getTime() - _lastLocation.getTime();
			return timeDiff > SIGNIFICANT_TIME_DIFFERENCE || (newLocation.getAccuracy() <= _lastLocation.getAccuracy());
		}

		public void onProviderEnabled(String provider) {
			Log.i(TAG, String.format("Provider %s enabled", provider));
		}

		public void onProviderDisabled(String provider) {
			Log.e(TAG, String.format("Provider %s disabled", provider));
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (status == LocationProvider.OUT_OF_SERVICE) {
				Log.e(TAG, "Location provider out of service: " + provider);
			} else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
				Log.w(TAG, "Location provider temp unavailable: " + provider);
			}
		}
	}
	
	@Override
	public boolean isWakeLockNeeded() {
		return true;
	}
}
