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
import org.most.input.CellInput;
import org.most.input.Input;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PipelineCell extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineCell.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineCell.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	public static final String KEY_ACTION = "PipelineCell";
	public static final String KEY_PHONE_TYPE = "PipelineCell.phone_type";
	public static final String KEY_GSM_CELL_ID = "PipelineCell.gsm_cell_id";
	public static final String KEY_GSM_LAC = "PipelineCell.gsm_lac";
	public static final String KEY_BASE_STATION_ID = "PipelineCell.base_station_id";
	public static final String KEY_BASE_STATION_LATITUDE = "PipelineCell.base_station_latitude";
	public static final String KEY_BASE_STATION_LONGITUDE = "PipelineCell.base_station_longitude";
	public static final String KEY_BASE_NETWORK_ID = "PipelineCell.base_network_id";
	public static final String KEY_BASE_SYSTEM_ID = "PipelineCell.base_system_id";
	
	public static final String TBL_CELL = "CELL";
	public static final String FLD_TIMESTAMP = "timestamp";
	public static final String FLD_PHONE_TYPE = "phone_type";
	//for TelephonyManager.PHONE_TYPE_GSM
	public static final String FLD_GSM_CELL_ID = "gsm_cell_id";
	public static final String FLD_GSM_LAC = "gsm_lac";
	//for TelephonyManager.PHONE_TYPE_CDMA
	public static final String FLD_BASE_STATION_ID = "base_station_id";
	public static final String FLD_BASE_STATION_LATITUDE = "base_station_latitude";
	public static final String FLD_BASE_STATION_LONGITUDE = "base_station_longitude";
	public static final String FLD_BASE_NETWORK_ID = "base_network_id";
	public static final String FLD_BASE_SYSTEM_ID = "base_system_id";
	
	public static final String CREATE_CELL_TABLE = String.format(
			"_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s TEXT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL",
			FLD_TIMESTAMP, FLD_PHONE_TYPE, FLD_GSM_CELL_ID, FLD_GSM_LAC, FLD_BASE_STATION_ID, FLD_BASE_STATION_LATITUDE, FLD_BASE_STATION_LONGITUDE, FLD_BASE_NETWORK_ID, FLD_BASE_SYSTEM_ID);

	protected boolean _isDump;
	protected boolean _isSend;

	public PipelineCell(MoSTApplication context) {
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

	public void onData(DataBundle b) {
		try {
			
			String phone_type = "";
			int gsm_cell_id = -1;
			int gsm_lac = -1;
			int base_station_id = -1;
			int base_station_latitude = -1;
			int base_station_longitude = -1;
			int base_network_id = -1;
			int base_system_id = -1;
	
			if (b.getInt(CellInput.KEY_PHONE_TYPE) == TelephonyManager.PHONE_TYPE_GSM) {
				phone_type = "PHONE_TYPE_GSM";
				gsm_cell_id = b.getInt(CellInput.KEY_GSM_CELL_ID);
				gsm_lac = b.getInt(CellInput.KEY_GSM_LAC);
			}
			if (b.getInt(CellInput.KEY_PHONE_TYPE) == TelephonyManager.PHONE_TYPE_CDMA) {
				phone_type = "PHONE_TYPE_CDMA";
				base_station_id = b.getInt(CellInput.KEY_BASE_STATION_ID);
				base_station_latitude = b.getInt(CellInput.KEY_BASE_STATION_LATITUDE);
				base_station_longitude = b.getInt(CellInput.KEY_BASE_STATION_LONGITUDE);
				base_network_id = b.getInt(CellInput.KEY_BASE_NETWORK_ID);
				base_system_id = b.getInt(CellInput.KEY_BASE_SYSTEM_ID);
			}
			if (b.getInt(CellInput.KEY_PHONE_TYPE) == TelephonyManager.PHONE_TYPE_NONE) {
				phone_type = "PHONE_TYPE_NONE";
			}
		
			if (_isDump) {
				ContentValues cv = new ContentValues();
				cv.put(FLD_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
				cv.put(FLD_PHONE_TYPE, phone_type);
				cv.put(FLD_GSM_CELL_ID, gsm_cell_id);
				cv.put(FLD_GSM_LAC, gsm_lac);
				cv.put(FLD_BASE_STATION_ID, base_station_id);
				cv.put(FLD_BASE_STATION_LATITUDE, base_station_latitude);
				cv.put(FLD_BASE_STATION_LONGITUDE, base_station_longitude);
				cv.put(FLD_BASE_NETWORK_ID, base_network_id);
				cv.put(FLD_BASE_SYSTEM_ID, base_system_id);

				getContext().getDbAdapter().storeData(TBL_CELL, cv, true);
			}
			if (_isSend) {
				Intent i = new Intent(KEY_ACTION);
				i.putExtra(KEY_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
				i.putExtra(KEY_PHONE_TYPE, phone_type);
				i.putExtra(KEY_GSM_CELL_ID, gsm_cell_id);
				i.putExtra(KEY_GSM_LAC, gsm_lac);
				i.putExtra(KEY_BASE_STATION_ID, base_station_id);
				i.putExtra(KEY_BASE_STATION_LATITUDE, base_station_latitude);
				i.putExtra(KEY_BASE_STATION_LONGITUDE, base_station_longitude);
				i.putExtra(KEY_BASE_NETWORK_ID, base_network_id);
				i.putExtra(KEY_BASE_SYSTEM_ID, base_system_id);
				getContext().sendBroadcast(i);
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.CELL;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.CELL);
		return result;
	}

}
