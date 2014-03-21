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
import org.most.input.MagneticFieldInput;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public class PipelineMagneticField extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineMagneticField.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineMagneticField.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	public static final String KEY_ACTION = "PipelineMagneticField";

	public static final String KEY_MAGNETIC_FIELD_X = "PipelineMagneticField.rotation_x";
	public static final String KEY_MAGNETIC_FIELD_Y = "PipelineMagneticField.rotation_y";
	public static final String KEY_MAGNETIC_FIELD_Z = "PipelineMagneticField.rotation_z";

	public static final String TBL_MAGNETIC_FIELD = "MAGNETIC_FIELD";
	public static final String FLD_TIMESTAMP = "timestamp";
	public static final String FLD_MAGNETIC_FIELD_X = "magnetic_field_x";
	public static final String FLD_MAGNETIC_FIELD_Y = "magnetic_field_y";
	public static final String FLD_MAGNETIC_FIELD_Z = "magnetic_field_z";
	public static final String CREATE_MAGNETIC_FIELD_TABLE = String.format(
			"_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s REAL NOT NULL, %s REAL NOT NULL, %s INT NOT NULL",
			FLD_TIMESTAMP, FLD_MAGNETIC_FIELD_X, FLD_MAGNETIC_FIELD_Y, FLD_MAGNETIC_FIELD_Z);

	protected boolean _isDump;
	protected boolean _isSend;

	public PipelineMagneticField(MoSTApplication context) {
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
				cv.put(FLD_MAGNETIC_FIELD_X, b.getFloat(MagneticFieldInput.KEY_MAGNETIC_FIELD_X));
				cv.put(FLD_MAGNETIC_FIELD_Y, b.getFloat(MagneticFieldInput.KEY_MAGNETIC_FIELD_Y));
				cv.put(FLD_MAGNETIC_FIELD_Z, b.getFloat(MagneticFieldInput.KEY_MAGNETIC_FIELD_Z));
				getContext().getDbAdapter().storeData(TBL_MAGNETIC_FIELD, cv);
			}
			if (_isSend) {
				Intent i = new Intent(KEY_ACTION);
				i.putExtra(KEY_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
				i.putExtra(KEY_MAGNETIC_FIELD_X, b.getFloat(MagneticFieldInput.KEY_MAGNETIC_FIELD_X));
				i.putExtra(KEY_MAGNETIC_FIELD_Y, b.getFloat(MagneticFieldInput.KEY_MAGNETIC_FIELD_Y));
				i.putExtra(KEY_MAGNETIC_FIELD_Z, b.getFloat(MagneticFieldInput.KEY_MAGNETIC_FIELD_Z));
				getContext().sendBroadcast(i);
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.MAGNETIC_FIELD;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.MAGNETICFIELD);
		return result;
	}

}
