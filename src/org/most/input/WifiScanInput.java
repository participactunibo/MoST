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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.utils.DelayedWakeLockRelease;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiScanInput extends PeriodicInput {

	/** The Constant DEBUG. */
	@SuppressWarnings("unused")
	private final static boolean DEBUG = true;

	/** The Constant TAG. */
	private final static String TAG = WifiScanInput.class.getSimpleName();

	public final static String KEY_WIFISCAN = "WifiScanInput.WifiSSID";

	/**
	 * {@link SharedPreferences} key to set the wi-fi scan period.
	 */
	public final static String PREF_KEY_WIFISCAN_PERIOD = "WifiScanInputPediodMs";

	/**
	 * Default wi-fi scan monitoring interval in milliseconds. Currently set to
	 * {@value #PREF_DEFAULT_STATISTICS_PERIOD}.
	 */
	public final static int PREF_DEFAULT_WIFISCAN_PERIOD = 1000 * 60 * 15;

	private GregorianCalendar _lastScanTime;

	WifiManager _wifiManager;
	WifiReceiver _wifiReceiver;
	IntentFilter _intentFilter;

	/**
	 * @param context
	 * @param period
	 */
	public WifiScanInput(MoSTApplication context) {
		super(context, context.getSharedPreferences(MoSTApplication.PREF_INPUT,
				Context.MODE_PRIVATE).getInt(PREF_KEY_WIFISCAN_PERIOD,
				PREF_DEFAULT_WIFISCAN_PERIOD));
		_wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
	}

	@Override
	public void onInit() {
		checkNewState(Input.State.INITED);
		if (_wifiReceiver == null)
			_wifiReceiver = new WifiReceiver();
		if (_intentFilter == null)
			_intentFilter = new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		super.onInit();
	}

	@Override
	public boolean onActivate() {
		checkNewState(Input.State.ACTIVATED);
		getContext().registerReceiver(_wifiReceiver, _intentFilter);
		_lastScanTime = new GregorianCalendar();
		_lastScanTime.add(Calendar.MILLISECOND,
				-(PREF_DEFAULT_WIFISCAN_PERIOD + 2000));
		return super.onActivate();
	}

	@Override
	public void onDeactivate() {
		checkNewState(Input.State.DEACTIVATED);
		getContext().unregisterReceiver(_wifiReceiver);
		super.onDeactivate();
	}

	@Override
	public void workToDo() {
		if (_wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
			boolean successfulStart = _wifiManager.startScan();
			if (successfulStart) {
				Log.i(TAG, "WIFI scan started succesfully");
				getContext().getWakeLockHolder().acquireWL();
				new DelayedWakeLockRelease(getContext(), 10000).start();
			} else {
				Log.e(TAG, "WIFI scan failed.");
			}
		}
		scheduleNextStart();
	}

	@Override
	public Type getType() {
		return Input.Type.WIFISCAN;
	}

	public class WifiReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			GregorianCalendar now = new GregorianCalendar();
			long timeDiff = now.getTimeInMillis()
					- _lastScanTime.getTimeInMillis();
			if (timeDiff > PREF_DEFAULT_WIFISCAN_PERIOD) {
				/*
				 * send result on the data bus
				 */
				List<ScanResult> result = _wifiManager.getScanResults();
				DataBundle b = _bundlePool.borrowBundle();
				b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis());
				b.putInt(Input.KEY_TYPE, Input.Type.WIFISCAN.toInt());
				b.putObject(KEY_WIFISCAN, result);
				post(b);
				_lastScanTime = now;
			}
		}

	}

}
