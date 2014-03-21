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
import java.util.Set;
import java.util.TreeSet;

import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.input.Input;
import org.most.input.NetTrafficInput;
import org.most.input.NetTrafficInput.TrafficRecord;
import org.most.persistence.DBAdapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public class PipelineAppsNetTraffic extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineAppsNetTraffic.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineAppsNetTraffic.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	// intent
	public static final String KEY_ACTION = "PipelineAppsNetTraffic";
	public final static String KEY_TX_TOT_DEVICE_NET_TRAFFIC = "PipelineAppsNetTraffic.TxDeviceNetTraffic";
	public final static String KEY_RX_TOT_DEVICE_NET_TRAFFIC = "PipelineAppsNetTraffic.RxDeviceNetTraffic";
	public final static String KEY_APPS_NET_TRAFFIC_LIST = "PipelineAppsNetTraffic.AppsNetTrafficList";

	public final static String TBL_NET_TRAFFIC_APPS = "NET_TRAFFIC_APPS";

	public final static String FLD_TIMESTAMP = "TIMESTAMP";
	public final static String FLD_TX_BYTES = "TX_BYTES";
	public final static String FLD_RX_BYTES = "RX_BYTES";
	public final static String FLD_APP_NAME = "APP_NAME";

	public static final String CREATE_APPS_NET_TRAFFIC_TABLE = String
			.format("_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s TEXT NOT NULL, %s INT NOT NULL, %s INT NOT NULL",
					FLD_TIMESTAMP, FLD_APP_NAME, FLD_TX_BYTES, FLD_RX_BYTES);

	private boolean _isDump;
	private boolean _isSend;
	private DBAdapter _dbAdapter;

	public PipelineAppsNetTraffic(MoSTApplication context) {
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

					@SuppressWarnings("unchecked")
					LinkedList<TrafficRecord> list = (LinkedList<TrafficRecord>) b
							.getObject(NetTrafficInput.KEY_APPS_NET_TRAFFIC_LIST);
					if (list != null) {
						for (TrafficRecord trafficRecord : list) {
							if (trafficRecord.getRx() > 0 || trafficRecord.getTx() > 0) {
								ContentValues cv = new ContentValues();
								cv.put(KEY_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
								cv.put(FLD_APP_NAME, trafficRecord.getTag());
								cv.put(FLD_TX_BYTES, trafficRecord.getTx());
								cv.put(FLD_RX_BYTES, trafficRecord.getRx());
								_dbAdapter.storeData(TBL_NET_TRAFFIC_APPS, cv, true);
							}
						}
					}
				}

				if (_isSend) {
					Intent i = new Intent(KEY_ACTION);
					i.putExtra(FLD_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
					i.putExtra(KEY_APPS_NET_TRAFFIC_LIST,
							b.getLong(NetTrafficInput.KEY_APPS_NET_TRAFFIC_LIST));
					getContext().sendBroadcast(i);
				}
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.APPS_NET_TRAFFIC;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.NET_TRAFFIC);
		return result;
	}

}
