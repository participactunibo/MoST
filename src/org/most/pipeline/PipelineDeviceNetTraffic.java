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

import java.util.Set;
import java.util.TreeSet;

import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.input.Input;
import org.most.input.NetTrafficInput;
import org.most.persistence.DBAdapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public class PipelineDeviceNetTraffic extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineDeviceNetTraffic.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineDeviceNetTraffic.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	// intent
	public static final String KEY_ACTION = "PipelineDeviceNetTraffic";
	public final static String KEY_TX_TOT_DEVICE_NET_TRAFFIC = "PipelineDeviceNetTraffic.TxDeviceNetTraffic";
	public final static String KEY_RX_TOT_DEVICE_NET_TRAFFIC = "PipelineDeviceNetTraffic.RxDeviceNetTraffic";

	public final static String TBL_NET_TRAFFIC_DEVICE = "NET_TRAFFIC_DEVICE";

	public final static String FLD_TIMESTAMP = "TIMESTAMP";
	public final static String FLD_TX_BYTES = "TX_BYTES";
	public final static String FLD_RX_BYTES = "RX_BYTES";

	public static final String CREATE_DEVICE_NET_TRAFFIC_TABLE = String.format(
			"_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL",
			FLD_TIMESTAMP, FLD_TX_BYTES, FLD_RX_BYTES);

	private boolean _isDump;
	private boolean _isSend;
	private DBAdapter _dbAdapter;

	public PipelineDeviceNetTraffic(MoSTApplication context) {
		super(context);
	}

	@Override
	public boolean onActivate() {
		_isDump = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES,
				Context.MODE_PRIVATE).getBoolean(PREF_KEY_DUMP_TO_DB, PREF_DEFAULT_DUMP_TO_DB);
		_isSend = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES,
				Context.MODE_PRIVATE).getBoolean(PREF_KEY_SEND_INTENT, PREF_DEFAULT_SEND_INTENT);
		_dbAdapter = getContext().getDbAdapter();
		return super.onActivate();
	}

	public void onData(DataBundle b) {
		try {
			if (_isDump || _isSend) {

				if (_isDump) {

					ContentValues cv = new ContentValues();
					cv.put(KEY_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
					cv.put(FLD_TX_BYTES, b.getLong(NetTrafficInput.KEY_TX_TOT_DEVICE_NET_TRAFFIC));
					cv.put(FLD_RX_BYTES, b.getLong(NetTrafficInput.KEY_RX_TOT_DEVICE_NET_TRAFFIC));
					_dbAdapter.storeData(TBL_NET_TRAFFIC_DEVICE, cv, true);

				}

				if (_isSend) {
					Intent i = new Intent(KEY_ACTION);
					i.putExtra(FLD_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
					i.putExtra(KEY_TX_TOT_DEVICE_NET_TRAFFIC,
							b.getLong(NetTrafficInput.KEY_TX_TOT_DEVICE_NET_TRAFFIC));
					i.putExtra(KEY_RX_TOT_DEVICE_NET_TRAFFIC,
							b.getLong(NetTrafficInput.KEY_RX_TOT_DEVICE_NET_TRAFFIC));
					getContext().sendBroadcast(i);
				}
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.DEVICE_NET_TRAFFIC;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.NET_TRAFFIC);
		return result;
	}

}
