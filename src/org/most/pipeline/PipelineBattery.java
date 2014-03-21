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
import org.most.input.BatteryInput;
import org.most.input.Input;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

/**
 * This pipeline reports informations about the battery status. It supports
 * dumping on the MoST database and broadcasting intents.
 * 
 * Configuration:
 * <ul>
 * <li>{@link #PREF_KEY_DUMP_TO_DB} (boolean): if <code>true</code>, battery
 * status is written to MoST's DB. The DB stores the timestamp of the
 * measurement, the battery level, scale, voltage, plugged status, status, and
 * health. Default setting: {@value #PREF_DEFAULT_DUMP_TO_DB}.</li>
 * <li>{@link #PREF_KEY_SEND_INTENT} (boolean): if <code>true</code>, the
 * pipeline periodically broadcasts an {@link Intent}.</li>
 * </ul>
 * 
 * Intents broadcasted by this pipeline have the action set to
 * {@link #KEY_ACTION} and the following extra fields:
 * <ul>
 * <li>{@link Pipeline#KEY_TIMESTAMP} (long): timestamp of the measurement</li>
 * <li>{@link #KEY_BATTERY_LEVEL} (int): battery level, goest from 0 to
 * {@link #KEY_BATTERY_SCALE} (see next field)</li>
 * <li> {@link #KEY_BATTERY_SCALE} (int): maximum value of
 * {@link #KEY_BATTERY_LEVEL}</li>
 * <li>{@link #KEY_BATTERY_TEMPERATURE} (int): current battery temperature</li>
 * <li>{@link #KEY_BATTERY_VOLTAGE} (int): current battery voltage</li>
 * <li>{@link #KEY_BATTERY_PLUGGED} (int): battery plugged status, refer to
 * {@link BatteryManager#EXTRA_PLUGGED}</li>
 * <li>{@link #KEY_BATTERY_STATUS} (int): battery status, refer to
 * {@link BatteryManager#EXTRA_STATUS}</li>
 * <li>{@link #KEY_BATTERY_HEALTH} (int): battery health, refer to
 * {@link BatteryManager#EXTRA_HEALTH}</li>
 * </ul>
 * 
 * @author gcardone
 * 
 */
public class PipelineBattery extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineBattery.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineBattery.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	public static final String KEY_ACTION = "PipelineBattery";

	public static final String KEY_BATTERY_LEVEL = "PipelineBattery.level";
	public static final String KEY_BATTERY_SCALE = "PipelineBattery.scale";
	public static final String KEY_BATTERY_TEMPERATURE = "PipelineBattery.temperature";
	public static final String KEY_BATTERY_VOLTAGE = "PipelineBattery.voltage";
	public static final String KEY_BATTERY_PLUGGED = "PipelineBattery.plugged";
	public static final String KEY_BATTERY_STATUS = "PipelineBattery.status";
	public static final String KEY_BATTERY_HEALTH = "PipelineBattery.health";

	public static final String TBL_BATTERY = "BATTERY";
	public static final String FLD_TIMESTAMP = "timestamp";
	public static final String FLD_BATTERY_LEVEL = "level";
	public static final String FLD_BATTERY_SCALE = "scale";
	public static final String FLD_BATTERY_TEMPERATURE = "temperature";
	public static final String FLD_BATTERY_VOLTAGE = "voltage";
	public static final String FLD_BATTERY_PLUGGED = "plugged";
	public static final String FLD_BATTERY_STATUS = "status";
	public static final String FLD_BATTERY_HEALTH = "health";

	public static final String CREATE_BATTERY_TABLE = String
			.format("_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s INT NOT NULL",
					FLD_TIMESTAMP, FLD_BATTERY_LEVEL, FLD_BATTERY_SCALE, FLD_BATTERY_TEMPERATURE, FLD_BATTERY_VOLTAGE,
					FLD_BATTERY_PLUGGED, FLD_BATTERY_STATUS, FLD_BATTERY_HEALTH);

	protected boolean _isDump;
	protected boolean _isSend;

	public PipelineBattery(MoSTApplication context) {
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
				cv.put(FLD_BATTERY_LEVEL, b.getInt(BatteryInput.KEY_BATTERY_LEVEL));
				cv.put(FLD_BATTERY_SCALE, b.getInt(BatteryInput.KEY_BATTERY_SCALE));
				cv.put(FLD_BATTERY_TEMPERATURE, b.getInt(BatteryInput.KEY_BATTERY_TEMPERATURE));
				cv.put(FLD_BATTERY_VOLTAGE, b.getInt(BatteryInput.KEY_BATTERY_VOLTAGE));
				cv.put(FLD_BATTERY_PLUGGED, b.getInt(BatteryInput.KEY_BATTERY_PLUGGED));
				cv.put(FLD_BATTERY_STATUS, b.getInt(BatteryInput.KEY_BATTERY_STATUS));
				cv.put(FLD_BATTERY_HEALTH, b.getInt(BatteryInput.KEY_BATTERY_HEALTH));
				getContext().getDbAdapter().storeData(TBL_BATTERY, cv, true);
			}

			if (_isSend) {
				Intent i = new Intent(KEY_ACTION);
				i.putExtra(KEY_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
				i.putExtra(KEY_BATTERY_LEVEL, b.getInt(BatteryInput.KEY_BATTERY_LEVEL));
				i.putExtra(KEY_BATTERY_SCALE, b.getInt(BatteryInput.KEY_BATTERY_SCALE));
				i.putExtra(KEY_BATTERY_TEMPERATURE, b.getInt(BatteryInput.KEY_BATTERY_TEMPERATURE));
				i.putExtra(KEY_BATTERY_VOLTAGE, b.getInt(BatteryInput.KEY_BATTERY_VOLTAGE));
				i.putExtra(KEY_BATTERY_PLUGGED, b.getInt(BatteryInput.KEY_BATTERY_PLUGGED));
				i.putExtra(KEY_BATTERY_STATUS, b.getInt(BatteryInput.KEY_BATTERY_STATUS));
				i.putExtra(KEY_BATTERY_HEALTH, b.getInt(BatteryInput.KEY_BATTERY_HEALTH));
				getContext().sendBroadcast(i);
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.BATTERY;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.BATTERY);
		return result;
	}

}
