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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

import org.most.DataBundle;
import org.most.MoSTApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.TrafficStats;

public class NetTrafficInput extends PeriodicInput {

	@SuppressWarnings("unused")
	private final static String TAG = NetTrafficInput.class.getSimpleName();

	/**
	 * {@link SharedPreferences} key to set the statistics monitoring period.
	 */
	public final static String PREF_KEY_NET_TRAFFIC_PERIOD = "trafficInputPediodMs";

	/**
	 * Default net traffic monitoring interval in milliseconds. Currently set to
	 * {@value #PREF_DEFAULT_NET_TRAFFIC_PERIOD}.
	 */
	public final static int PREF_DEFAULT_NET_TRAFFIC_PERIOD = 1000 * 60 * 60 * 3;

	public final static String KEY_TX_TOT_DEVICE_NET_TRAFFIC = "NetTrafficInput.TxDeviceNetTraffic";
	public final static String KEY_RX_TOT_DEVICE_NET_TRAFFIC = "NetTrafficInput.RxDeviceNetTraffic";
	public final static String KEY_APPS_NET_TRAFFIC_LIST = "NetTrafficInput.AppsNetTrafficList";

	/**
	 * @param context
	 */
	public NetTrafficInput(MoSTApplication context) {
		super(context, context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getInt(
				PREF_KEY_NET_TRAFFIC_PERIOD, PREF_DEFAULT_NET_TRAFFIC_PERIOD));
	}

	@Override
	public void workToDo() {
		DataBundle b = _bundlePool.borrowBundle();
		b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis());
		b.putInt(Input.KEY_TYPE, Input.Type.NET_TRAFFIC.toInt());

		TrafficSnapshot snapshot = new TrafficSnapshot(getContext());
		
		b.putLong(KEY_TX_TOT_DEVICE_NET_TRAFFIC, snapshot.device.tx);
		b.putLong(KEY_RX_TOT_DEVICE_NET_TRAFFIC, snapshot.device.rx);

		LinkedList<TrafficRecord> list = new LinkedList<NetTrafficInput.TrafficRecord>();
		list.addAll(snapshot.apps.values());
		
		b.putObject(KEY_APPS_NET_TRAFFIC_LIST, list);

		post(b);
		scheduleNextStart();
	}

	@Override
	public Type getType() {
		return Input.Type.NET_TRAFFIC;
	}
	
	private class TrafficSnapshot {
		TrafficRecord device = null;
		HashMap<Integer, TrafficRecord> apps = new HashMap<Integer, TrafficRecord>();

		TrafficSnapshot(Context ctxt) {
			device = new TrafficRecord();

			HashMap<Integer, String> appNames = new HashMap<Integer, String>();

			for (ApplicationInfo app : ctxt.getPackageManager().getInstalledApplications(0)) {
				appNames.put(app.uid, app.packageName);
			}

			for (Integer uid : appNames.keySet()) {
				apps.put(uid, new TrafficRecord(uid, appNames.get(uid)));
			}
		}
	}

	public static class TrafficRecord implements Serializable{
		
		private static final long serialVersionUID = -281165549284978466L;
		
		long tx = 0;
		long rx = 0;
		String tag = null;

		TrafficRecord() {
			tx = TrafficStats.getTotalTxBytes();
			rx = TrafficStats.getTotalRxBytes();
		}

		TrafficRecord(int uid, String tag) {
			tx = TrafficStats.getUidTxBytes(uid);
			rx = TrafficStats.getUidRxBytes(uid);
			this.tag = tag;
		}

		public long getTx() {
			return tx;
		}

		public void setTx(long tx) {
			this.tx = tx;
		}

		public long getRx() {
			return rx;
		}

		public void setRx(long rx) {
			this.rx = rx;
		}

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}
		
		
	}
}
