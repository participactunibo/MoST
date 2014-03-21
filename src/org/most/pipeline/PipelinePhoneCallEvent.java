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
import org.most.input.PhoneCallInput;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public class PipelinePhoneCallEvent extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelinePhoneCallEvent.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelinePhoneCallEvent.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	public static final String KEY_ACTION = "PipelinePhoneCallEvent";

	public static final String KEY_IS_START = "PipelinePhoneCallEvent.callStart";
	public static final String KEY_IS_INCOMING = "PipelinePhoneCallEvent.isIncoming";
	public static final String KEY_PHONE_NUMBER = "PipelinePhoneCallEvent.phoneNumber";

	public static final String TBL_PHONE_CALL_EVENT = "PHONE_CALL_EVENT";
	public static final String FLD_TIMESTAMP = "timestamp";
	public static final String FLD_IS_START = "is_start";
	public static final String FLD_IS_INCOMING = "is_incoming";
	public static final String FLD_PHONE_NUMBER = "phone_number";
	public static final String CREATE_PHONE_CALL_EVENT_TABLE = String.format(
			"_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s BOOLEAN NOT NULL, %s BOOLEAN NOT NULL, %s TEXT",
			FLD_TIMESTAMP, FLD_IS_START, FLD_IS_INCOMING, FLD_PHONE_NUMBER);

	protected boolean _isDump;
	protected boolean _isSend;

	public PipelinePhoneCallEvent(MoSTApplication context) {
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
			if (b.getInt(PhoneCallInput.KEY_BUNDLE_TYPE, -1) == PhoneCallInput.VAL_BUNDLE_TYPE_EVENT) {
				if (_isDump) {
					ContentValues cv = new ContentValues();
					cv.put(FLD_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
					cv.put(FLD_IS_START, b.getBoolean(PhoneCallInput.KEY_IS_START));
					cv.put(FLD_IS_INCOMING, b.getBoolean(PhoneCallInput.KEY_IS_INCOMING));
					cv.put(FLD_PHONE_NUMBER, b.getString(PhoneCallInput.KEY_PHONE_NUMBER));
					getContext().getDbAdapter().storeData(TBL_PHONE_CALL_EVENT, cv, true);
				}
				if (_isSend) {
					Intent i = new Intent(KEY_ACTION);
					i.putExtra(KEY_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
					i.putExtra(KEY_IS_START, b.getBoolean(PhoneCallInput.KEY_IS_START));
					i.putExtra(KEY_IS_INCOMING, b.getBoolean(PhoneCallInput.KEY_IS_INCOMING));
					i.putExtra(KEY_PHONE_NUMBER, b.getString(PhoneCallInput.KEY_PHONE_NUMBER));
					getContext().sendBroadcast(i);
				}
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.PHONE_CALL_EVENT;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.PHONECALL);
		return result;
	}

}
