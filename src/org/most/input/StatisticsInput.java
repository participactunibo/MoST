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
package org.most.input;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.most.DataBundle;
import org.most.MoSTApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class StatisticsInput extends PeriodicInput {

	private final static String TAG = StatisticsInput.class.getSimpleName();

	/**
	 * {@link SharedPreferences} key to set the statistics monitoring period.
	 */
	public final static String PREF_KEY_STATISTICS_PERIOD = "StatisticsInputPediodMs";

	/**
	 * Default statistics monitoring interval in milliseconds. Currently set to
	 * {@value #PREF_DEFAULT_STATISTICS_PERIOD}.
	 */
	public final static int PREF_DEFAULT_STATISTICS_PERIOD = 1000 * 60 * 60;

	private static final String MEMINFO_PATH = "/proc/meminfo";

	private static final String CPUINFO_PATH = "/proc/cpuinfo";

	private static final String STAT_PATH = "/proc/stat";

	public static final String KEY_CPU_FREQ = "StatisticsInput.cpuFreq";

	public static final String KEY_CPU_USER = "StatisticsInput.cpuUser";

	public static final String KEY_CPU_NICE = "StatisticsInput.cpuNice";

	public static final String KEY_CPU_IDLE = "StatisticsInput.cpuIdle";

	public static final String KEY_CPU_SYSTEM = "StatisticsInput.cpuSystem";

	public static final String KEY_CPU_IOWAIT = "StatisticsInput.cpuIOWait";

	public static final String KEY_CPU_HARDIRQ = "StatisticsInput.cpuHardIRQ";

	public static final String KEY_CPU_SOFTIRQ = "StatisticsInput.cpuSoftIRQ";

	public static final String KEY_CONTEXT_SWITCH = "StatisticsInput.contextSwitch";

	public static final String KEY_BOOT_TIME = "StatisticsInput.bootTime";

	public static final String KEY_PROCESSES = "StatisticsInput.processes";

	public static final String KEY_MEM_TOTAL = "StatisticsInput.memTotal";

	public static final String KEY_MEM_FREE = "StatisticsInput.memFree";

	public static final String KEY_MEM_ACTIVE = "StatisticsInput.memActive";

	public static final String KEY_MEM_INACTIVE = "StatisticsInput.memInactive";

	/**
	 * @param context
	 */
	public StatisticsInput(MoSTApplication context) {
		super(context, context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getInt(
				PREF_KEY_STATISTICS_PERIOD, PREF_DEFAULT_STATISTICS_PERIOD));
	}

	@Override
	public void workToDo() {
		DataBundle b = _bundlePool.borrowBundle();
		parseCpuStat(b);
		parseMemInfo(b);
		b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis());
		b.putInt(Input.KEY_TYPE, Input.Type.SYSTEM_STATS.toInt());
		post(b);
		scheduleNextStart();
	}

	@Override
	public Type getType() {
		return Input.Type.SYSTEM_STATS;
	}

	public void parseCpuStat(DataBundle b) {

		BufferedReader reader = null;
		String line = null;

		float cpuFreq = -1.0f;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(CPUINFO_PATH)));

			Pattern p = Pattern.compile("BogoMIPS\\s+:\\s*([0-9]+\\.?[0-9]*).*", Pattern.CASE_INSENSITIVE);
			while ((line = reader.readLine()) != null) {
				Matcher m = p.matcher(line);
				if (m.matches()) {
					cpuFreq = Float.parseFloat(m.group(1));
					break;
				}
			}
			reader.close();
		} catch (IOException e) {
			Log.w(TAG, "Exception parsing /proc/cpuinfo");
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
		b.putFloat(KEY_CPU_FREQ, cpuFreq);

		long user = -1;
		long nice = -1;
		long system = -1;
		long idle = -1;
		long iowait = -1;
		long hardirq = -1;
		long softirq = -1;

		try {

			reader = new BufferedReader(new InputStreamReader(new FileInputStream(STAT_PATH)));

			while ((line = reader.readLine()) != null) {
				line = line.replaceAll("\\s+", " ");
				String[] tokens = line.split(" ");
				if (tokens[0].equals("cpu")) {

					user = Long.parseLong(tokens[1]);
					nice = Long.parseLong(tokens[2]);
					system = Long.parseLong(tokens[3]);
					idle = Long.parseLong(tokens[4]);
					iowait = Long.parseLong(tokens[5]);
					hardirq = Long.parseLong(tokens[6]);
					softirq = Long.parseLong(tokens[7]);

				} else if (tokens[0].equals("ctxt")) {
					b.putLong(KEY_CONTEXT_SWITCH, Long.valueOf(tokens[1]));
				} else if (tokens[0].equals("btime")) {
					b.putLong(KEY_BOOT_TIME, Long.valueOf(tokens[1]));
				} else if (tokens[0].equals("processes")) {
					b.putLong(KEY_PROCESSES, Long.valueOf(tokens[1]));
				}

			}

		} catch (IOException e) {
			Log.e(TAG, "Could not read /proc/stat file", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}

		b.putLong(KEY_CPU_USER, user);
		b.putLong(KEY_CPU_NICE, nice);
		b.putLong(KEY_CPU_SYSTEM, system);
		b.putLong(KEY_CPU_IDLE, idle);
		b.putLong(KEY_CPU_IOWAIT, iowait);
		b.putLong(KEY_CPU_HARDIRQ, hardirq);
		b.putLong(KEY_CPU_SOFTIRQ, softirq);
	}

	public void parseMemInfo(DataBundle b) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(MEMINFO_PATH)));
			Pattern p = Pattern.compile("([^:]+):\\s+([\\d]+).*");
			String line;
			while ((line = reader.readLine()) != null) {
				Matcher m = p.matcher(line);
				if (m.matches()) {
					String key = m.group(1);
					long value = Long.parseLong(m.group(2));
					if ("MemTotal".equals(key)) {
						b.putLong(KEY_MEM_TOTAL, value);
					} else if ("MemFree".equals(key)) {
						b.putLong(KEY_MEM_FREE, value);
					} else if ("Active".equals(key)) {
						b.putLong(KEY_MEM_ACTIVE, value);
					} else if ("Inactive".equals(key)) {
						b.putLong(KEY_MEM_INACTIVE, value);
					}
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "Exception parsing /proc/meminfo");
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
