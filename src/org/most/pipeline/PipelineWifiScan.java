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
package org.most.pipeline;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.input.Input;
import org.most.input.WifiScanInput;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;

public class PipelineWifiScan extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineWifiScan.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineWifiScan.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	public static final String KEY_ACTION = "PipelineWifiScan";
	public static final String KEY_VALUE = "PipelineWifiScan.value";

	public static final String TBL_WIFI_SCAN = "WIFI_SCAN";
	public static final String FLD_TIMESTAMP = "timestamp";
	public static final String FLD_BSSID = "bssid";
	public static final String FLD_SSID = "ssid";
	public static final String FLD_CAPABILITIES = "capabilities";
	public static final String FLD_FREQUENCY = "frequency";
	public static final String FLD_LEVEL = "level";

	public static final String CREATE_WIFI_SCAN_TABLE = String.format(
			"_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL, %s INT NOT NULL, %s INT NOT NULL",
			FLD_TIMESTAMP, FLD_BSSID, FLD_SSID, FLD_CAPABILITIES, FLD_FREQUENCY, FLD_LEVEL);

	protected boolean _isDump;
	protected boolean _isSend;

	public PipelineWifiScan(MoSTApplication context) {
		super(context);
	}

	@Override
	public boolean onActivate() {
		checkNewState(State.ACTIVATED);
		_isDump = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES, Context.MODE_PRIVATE).getBoolean(
				PREF_KEY_DUMP_TO_DB, PREF_DEFAULT_DUMP_TO_DB);
		_isSend = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES, Context.MODE_PRIVATE).getBoolean(
				PREF_KEY_SEND_INTENT, PREF_DEFAULT_SEND_INTENT);
		return super.onActivate();
	}

	@SuppressWarnings("unchecked")
	public void onData(DataBundle b) {
		List<ScanResult> result = null;
		try {
			result = (List<ScanResult>) b.getObject(WifiScanInput.KEY_WIFISCAN);
			
			if (_isDump) {
				for (ScanResult scanResult : result) {
					ContentValues cv = new ContentValues();
					cv.put(FLD_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
					cv.put(FLD_BSSID, scanResult.BSSID);
					cv.put(FLD_SSID, scanResult.SSID);
					cv.put(FLD_CAPABILITIES, scanResult.capabilities);
					cv.put(FLD_FREQUENCY, scanResult.frequency);
					cv.put(FLD_LEVEL, scanResult.level);
					getContext().getDbAdapter().storeData(TBL_WIFI_SCAN, cv, true);
				}
			}
			if (_isSend) {
				Intent i = new Intent(KEY_ACTION);
				i.putExtra(KEY_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
				LinkedList<ScanResult> resutlLinked =  new LinkedList<ScanResult>(result);
				i.putExtra(KEY_VALUE, resutlLinked);
				getContext().sendBroadcast(i);
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.WIFI_SCAN;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.WIFISCAN);
		return result;
	}

}
