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
import org.most.input.PeriodicConnectionTypeInput;
import org.most.persistence.DBAdapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public class PipelineConnectionType extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineConnectionType.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineConnectionType.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	// intent
	public static final String KEY_ACTION = "PipelineConnectionType";
	public final static String KEY_CONNECTION_TYPE = "PipelineConnectionType.ConnectionType";
	public final static String KEY_MOBILE_NETWORK_TYPE = "PipelineConnectionType.MobileNetworkType";

	public final static String TBL_CONNECTION_TYPE = "CONNECTION_TYPE";

	public final static String FLD_TIMESTAMP = "TIMESTAMP";
	public final static String FLD_TYPE = "TYPE";
	public final static String FLD_MOBILE_NETWORK_TYPE = "MOBILE_NETWORK_TYPE";

	public static final String CREATE_CONNECTION_TYPE_TABLE = String.format(
			"_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL",
			FLD_TIMESTAMP, FLD_TYPE, FLD_MOBILE_NETWORK_TYPE);

	private boolean _isDump;
	private boolean _isSend;
	private DBAdapter _dbAdapter;

	public PipelineConnectionType(MoSTApplication context) {
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

				String type = b.getString(PeriodicConnectionTypeInput.KEY_CONNECTION_TYPE);
				if (type == null || type.length() == 0) {
					type = "";
				}
				String mobileType = b
						.getString(PeriodicConnectionTypeInput.KEY_MOBILE_NETWORK_TYPE);
				if (mobileType == null || mobileType.length() == 0) {
					mobileType = "";
				}

				if (_isDump) {

					ContentValues cv = new ContentValues();
					cv.put(KEY_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
					cv.put(FLD_TYPE, type);
					cv.put(FLD_MOBILE_NETWORK_TYPE, mobileType);
					_dbAdapter.storeData(TBL_CONNECTION_TYPE, cv, true);

				}

				if (_isSend) {
					Intent i = new Intent(KEY_ACTION);
					i.putExtra(FLD_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
					i.putExtra(KEY_CONNECTION_TYPE, type);
					i.putExtra(KEY_MOBILE_NETWORK_TYPE, mobileType);
					getContext().sendBroadcast(i);
				}
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.CONNECTION_TYPE;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.PERIODIC_CONNECTION_TYPE);
		return result;
	}

}
