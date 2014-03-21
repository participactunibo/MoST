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
import org.most.input.StatisticsInput;
import org.most.persistence.DBAdapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public class PipelineSystemStats extends Pipeline {

	public static final String KEY_ACTION = "PipelineSystemStats";

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineSystemStats.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineSystemStats.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	public static final String KEY_CPU_FREQ = "PipelineSystemStats.cpuFreq";
	public static final String KEY_CPU_USER = "PipelineSystemStats.cpuUser";
	public static final String KEY_CPU_NICE = "PipelineSystemStats.cpuNice";
	public static final String KEY_CPU_IDLE = "PipelineSystemStats.cpuIdle";
	public static final String KEY_CPU_SYSTEM = "PipelineSystemStats.cpuSystem";
	public static final String KEY_CPU_IOWAIT = "PipelineSystemStats.cpuIOWait";
	public static final String KEY_CPU_HARDIRQ = "PipelineSystemStats.cpuHardIRQ";
	public static final String KEY_CPU_SOFTIRQ = "PipelineSystemStats.cpuSoftIRQ";
	public static final String KEY_CONTEXT_SWITCH = "PipelineSystemStats.contextSwitch";
	public static final String KEY_BOOT_TIME = "PipelineSystemStats.bootTime";
	public static final String KEY_PROCESSES = "PipelineSystemStats.processes";
	public static final String KEY_MEM_TOTAL = "PipelineSystemStats.memTotal";
	public static final String KEY_MEM_FREE = "PipelineSystemStats.memFree";
	public static final String KEY_MEM_ACTIVE = "PipelineSystemStats.memActive";
	public static final String KEY_MEM_INACTIVE = "PipelineSystemStats.memInactive";

	public final static String TBL_SYSTEM_STATS = "SYSTEM_STATS";
	public static final String FLD_CPU_FREQ = "CPU_FREQUENCY";
	public static final String FLD_CPU_USER = "CPU_USER";
	public static final String FLD_CPU_NICE = "CPU_NICED";
	public static final String FLD_CPU_IDLE = "CPU_IDLE";
	public static final String FLD_CPU_SYSTEM = "CPU_SYSTEM";
	public static final String FLD_CPU_IOWAIT = "CPU_IOWAIT";
	public static final String FLD_CPU_HARDIRQ = "CPU_HARDIRQ";
	public static final String FLD_CPU_SOFTIRQ = "CPU_SOFTIRQ";
	public static final String FLD_CONTEXT_SWITCH = "CONTEXT_SWITCHES";
	public static final String FLD_BOOT_TIME = "BOOT_TIME";
	public static final String FLD_PROCESSES = "PROCESSES";
	public static final String FLD_MEM_TOTAL = "MEM_TOTAL";
	public static final String FLD_MEM_FREE = "MEM_FREE";
	public static final String FLD_MEM_ACTIVE = "MEM_ACTIVE";
	public static final String FLD_MEM_INACTIVE = "MEM_INACTIVE";
	public final static String FLD_TIMESTAMP = "TIMESTAMP";

	public static final String CREATE_SYSTEM_STATS_TABLE = String
			.format("_ID INTEGER PRIMARY KEY, %s INT NOT NULL,"
					+ "%s REAL, %s INT, %s INT, %s INT, %s INT, %s INT, %s INT, %s INT, %s INT, %s INT, %s INT, %s INT, %s INT, "
					+ "%s INT, %s INT", FLD_TIMESTAMP, FLD_CPU_FREQ, FLD_CPU_USER, FLD_CPU_NICE,
					FLD_CPU_IDLE, FLD_CPU_SYSTEM, FLD_CPU_IOWAIT, FLD_CPU_HARDIRQ, FLD_CPU_SOFTIRQ,
					FLD_CONTEXT_SWITCH, FLD_BOOT_TIME, FLD_PROCESSES, FLD_MEM_TOTAL, FLD_MEM_FREE,
					FLD_MEM_ACTIVE, FLD_MEM_INACTIVE);

	private boolean _isDump;
	private boolean _isSend;
	private DBAdapter _dbAdapter;

	public PipelineSystemStats(MoSTApplication context) {
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
				float cpuFreq = b.getFloat(StatisticsInput.KEY_CPU_FREQ, 0);
				long user = b.getLong(StatisticsInput.KEY_CPU_USER, 0);
				long nice = b.getLong(StatisticsInput.KEY_CPU_NICE, 0);
				long system = b.getLong(StatisticsInput.KEY_CPU_SYSTEM, 0);
				long idle = b.getLong(StatisticsInput.KEY_CPU_IDLE, 0);
				long iowait = b.getLong(StatisticsInput.KEY_CPU_IOWAIT, 0);
				long hardirq = b.getLong(StatisticsInput.KEY_CPU_HARDIRQ, 0);
				long softirq = b.getLong(StatisticsInput.KEY_CPU_SOFTIRQ, 0);
				long cntxSwtchs = b.getLong(StatisticsInput.KEY_CONTEXT_SWITCH, 0);
				long boot = b.getLong(StatisticsInput.KEY_BOOT_TIME, 0);
				long processes = b.getLong(StatisticsInput.KEY_PROCESSES, 0);
				long memTotal = b.getLong(StatisticsInput.KEY_MEM_TOTAL, 0);
				long memFree = b.getLong(StatisticsInput.KEY_MEM_FREE, 0);
				long memActive = b.getLong(StatisticsInput.KEY_MEM_ACTIVE, 0);
				long memInactive = b.getLong(StatisticsInput.KEY_MEM_INACTIVE, 0);
				long timestamp = b.getLong(StatisticsInput.KEY_TIMESTAMP);

				if (_isDump) {
					ContentValues cv = new ContentValues();
					cv.put(KEY_TIMESTAMP, timestamp);
					cv.put(FLD_CPU_FREQ, cpuFreq);
					cv.put(FLD_CPU_USER, user);
					cv.put(FLD_CPU_NICE, nice);
					cv.put(FLD_CPU_SYSTEM, system);
					cv.put(FLD_CPU_IDLE, idle);
					cv.put(FLD_CPU_IOWAIT, iowait);
					cv.put(FLD_CPU_HARDIRQ, hardirq);
					cv.put(FLD_CPU_SOFTIRQ, softirq);
					cv.put(FLD_CONTEXT_SWITCH, cntxSwtchs);
					cv.put(FLD_BOOT_TIME, boot);
					cv.put(FLD_PROCESSES, processes);
					cv.put(FLD_MEM_TOTAL, memTotal);
					cv.put(FLD_MEM_FREE, memFree);
					cv.put(FLD_MEM_ACTIVE, memActive);
					cv.put(FLD_MEM_INACTIVE, memInactive);
					_dbAdapter.storeData(TBL_SYSTEM_STATS, cv, true);
				}

				if (_isSend) {
					Intent i = new Intent(KEY_ACTION);
					i.putExtra(FLD_TIMESTAMP, timestamp);
					i.putExtra(KEY_CPU_FREQ, cpuFreq);
					i.putExtra(KEY_CPU_USER, user);
					i.putExtra(KEY_CPU_NICE, nice);
					i.putExtra(KEY_CPU_SYSTEM, system);
					i.putExtra(KEY_CPU_IDLE, idle);
					i.putExtra(KEY_CPU_IOWAIT, iowait);
					i.putExtra(KEY_CPU_HARDIRQ, hardirq);
					i.putExtra(KEY_CPU_SOFTIRQ, softirq);
					i.putExtra(KEY_CONTEXT_SWITCH, cntxSwtchs);
					i.putExtra(KEY_BOOT_TIME, boot);
					i.putExtra(KEY_PROCESSES, processes);
					i.putExtra(KEY_MEM_TOTAL, memTotal);
					i.putExtra(KEY_MEM_FREE, memFree);
					i.putExtra(KEY_MEM_ACTIVE, memActive);
					i.putExtra(KEY_MEM_INACTIVE, memInactive);
					getContext().sendBroadcast(i);
				}
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.SYSTEM_STATS;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.SYSTEM_STATS);
		return result;
	}

}
