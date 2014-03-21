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
import org.most.input.GyroscopeInput;
import org.most.input.Input;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

/**
 * This pipeline publishes 
 * 
 * @author gcardone
 *
 */
public class PipelineGyroscope extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineGyroscope.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineGyroscope.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	public static final String KEY_ACTION = "PipelineGyroscope";
	
	public static final String KEY_ROTATION_X = "PipelineGyroscope.rotation_x";
	public static final String KEY_ROTATION_Y = "PipelineGyroscope.rotation_y";
	public static final String KEY_ROTATION_Z = "PipelineGyroscope.rotation_z";

	public static final String TBL_GYROSCOPE = "GYROSCOPE";
	public static final String FLD_TIMESTAMP = "timestamp";
	public static final String FLD_ROTATION_X = "rotation_x";
	public static final String FLD_ROTATION_Y = "rotation_y";
	public static final String FLD_ROTATION_Z = "rotation_z";
	public static final String CREATE_GYROSCOPE_TABLE = String.format(
			"_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s REAL NOT NULL, %s REAL NOT NULL, %s INT NOT NULL",
			FLD_TIMESTAMP, FLD_ROTATION_X, FLD_ROTATION_Y, FLD_ROTATION_Z);

	protected boolean _isDump;
	protected boolean _isSend;

	public PipelineGyroscope(MoSTApplication context) {
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
			if (_isDump) {
				ContentValues cv = new ContentValues();
				cv.put(FLD_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
				cv.put(FLD_ROTATION_X, b.getFloat(GyroscopeInput.KEY_ROTATION_X));
				cv.put(FLD_ROTATION_Y, b.getFloat(GyroscopeInput.KEY_ROTATION_Y));
				cv.put(FLD_ROTATION_Z, b.getFloat(GyroscopeInput.KEY_ROTATION_Z));
				getContext().getDbAdapter().storeData(TBL_GYROSCOPE, cv);
			}
			if (_isSend) {
				Intent i = new Intent(KEY_ACTION);
				i.putExtra(KEY_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
				i.putExtra(KEY_ROTATION_X, b.getFloat(GyroscopeInput.KEY_ROTATION_X));
				i.putExtra(KEY_ROTATION_Y, b.getFloat(GyroscopeInput.KEY_ROTATION_Y));
				i.putExtra(KEY_ROTATION_Z, b.getFloat(GyroscopeInput.KEY_ROTATION_Z));
				getContext().sendBroadcast(i);
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.GYROSCOPE;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.GYROSCOPE);
		return result;
	}

}
