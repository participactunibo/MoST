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

/**
 * Pipeline for testing purposes.
 */
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.input.AppOnScreenInput;
import org.most.input.BatteryInput;
import org.most.input.BluetoothScanInput;
import org.most.input.CellInput;
import org.most.input.ContinuousLocationInput;
import org.most.input.GyroscopeInput;
import org.most.input.Input;
import org.most.input.InstalledAppsInput;
import org.most.input.LightInput;
import org.most.input.MagneticFieldInput;
import org.most.input.PhoneCallInput;
import org.most.input.ProximityInput;
import org.most.input.SMSTimestampInput;
import org.most.input.StatisticsInput;
import org.most.input.WifiScanInput;

import android.content.pm.PackageInfo;
import android.net.wifi.ScanResult;
import android.telephony.TelephonyManager;
import android.util.Log;

public class TestPipeline extends Pipeline {

	private static final String TAG = TestPipeline.class.getSimpleName();

	public TestPipeline(MoSTApplication context) {
		super(context);
	}

	public void onData(DataBundle b) {
		// Test for AppOnScreenInput
		if (b.getInt(KEY_TYPE) == Input.Type.APPONSCREEN.toInt()) {
			Log.i(TAG,
					"App on screen: "
							+ b.getString(AppOnScreenInput.KEY_APPONSCREEN)
							+ " - Act: "
							+ b.getString(AppOnScreenInput.KEY_APPONSCREENACTIVITY));
		}
		// Test for WifiScanInput
		if (b.getInt(KEY_TYPE) == Input.Type.WIFISCAN.toInt()) {
			Log.i(TAG, "WiFi found");
			@SuppressWarnings("unchecked")
			List<ScanResult> result = (List<ScanResult>) b
					.getObject(WifiScanInput.KEY_WIFISCAN);
			for (ScanResult sr : result) {
				Log.i(TAG, String.format("%s %s cap: %s freq %d level %d",
						sr.BSSID, sr.SSID, sr.capabilities, sr.frequency,
						sr.level));
			}
		}
		// Test for WifiScanInput
		if (b.getInt(KEY_TYPE) == Input.Type.BLUETOOTHSCAN.toInt()) {
			String devaddress = b.getString(BluetoothScanInput.KEY_MAC);
			String devname = b.getString(BluetoothScanInput.KEY_NAME);
			Log.i(TAG, "Bluetooth device found: " + devname + " [" + devaddress
					+ "]");
		}
		if (b.getInt(KEY_TYPE) == Input.Type.CONTINUOUS_LOCATION.toInt()
				|| b.getInt(KEY_TYPE) == Input.Type.PERIODIC_LOCATION.toInt())
			Log.i(TAG,
					String.format(
							"New location for device: Lat %f, Long: %f, Acc: %f, Provider: %s",
							b.getDouble(ContinuousLocationInput.KEY_LATITUDE),
							b.getDouble(ContinuousLocationInput.KEY_LONGITUDE),
							b.getDouble(ContinuousLocationInput.KEY_ACCURACY),
							b.getString(ContinuousLocationInput.KEY_PROVIDER)));

		// Test for SMSTimestampInput
		if (b.getInt(KEY_TYPE) == Input.Type.SMSTIMESTAMP.toInt()) {
			Date date = new Date(
					b.getLong(SMSTimestampInput.KEY_TIMESTAMP) * 1000);
			if (b.getInt(SMSTimestampInput.KEY_SMS_TYPE) == SMSTimestampInput.SMS_TYPE_RECEIVED)
				Log.i(TAG, "New sms received at: " + date.toString());
			else
				Log.i(TAG, "New sms sent at: " + date.toString());
		}

		// Test for light input
		if (b.getInt(KEY_TYPE) == Input.Type.LIGHT.toInt())
			Log.i(TAG, "Light value " + b.getObject(LightInput.KEY_VALUE));

		// Test for proximity input
		if (b.getInt(KEY_TYPE) == Input.Type.PROXIMITY.toInt())
			Log.i(TAG,
					"Proximity value "
							+ b.getObject(ProximityInput.KEY_PROXIMITY));

		// Test for phone call input
		if (b.getInt(KEY_TYPE) == Input.Type.PHONECALL.toInt()) {
			if (b.getInt(PhoneCallInput.KEY_BUNDLE_TYPE) == PhoneCallInput.VAL_BUNDLE_TYPE_DURATION) {
				Date start = new Date(
						b.getLong(PhoneCallInput.KEY_CALL_START_TIMESTAMP));
				Date stop = new Date(
						b.getLong(PhoneCallInput.KEY_CALL_END_TIMESTAMP));

				if (b.getBoolean(PhoneCallInput.KEY_IS_INCOMING))
					Log.i(TAG,
							"New incoming call started at: " + start.toString()
									+ " and finished at: " + stop.toString());
				else
					Log.i(TAG,
							"New outgoing call started at: " + start.toString()
									+ " and finished at: " + stop.toString());
			}
		}

		// Test for battery input
		if (b.getInt(KEY_TYPE) == Input.Type.BATTERY.toInt()) {
			Log.i(TAG,
					String.format("New battery level: %s",
							b.getInt(BatteryInput.KEY_BATTERY_LEVEL)));
		}

		// Test for cell input
		if (b.getInt(KEY_TYPE) == Input.Type.CELL.toInt()) {

			if (b.getInt(CellInput.KEY_PHONE_TYPE) == TelephonyManager.PHONE_TYPE_GSM) {

				Log.i(TAG,
						String.format(
								"Cell details: type %s, id %s, location area code %s, primary scrambling code %s",
								"GSM",
								b.getDouble(BatteryInput.KEY_BATTERY_LEVEL),
								b.getInt(CellInput.KEY_GSM_CELL_ID),
								b.getInt(CellInput.KEY_GSM_LAC),
								b.getInt(CellInput.KEY_GSM_PSC)));
			}
			if (b.getInt(CellInput.KEY_PHONE_TYPE) == TelephonyManager.PHONE_TYPE_CDMA) {
				Log.i(TAG,
						String.format(
								"Cell details: type %s, id %s, station id %s, station latitude %s, station longitude %s, network id %s, system id %s",
								"CDMA",
								b.getInt(CellInput.KEY_BASE_STATION_ID),
								b.getInt(CellInput.KEY_BASE_STATION_LATITUDE),
								b.getInt(CellInput.KEY_BASE_STATION_LONGITUDE),
								b.getInt(CellInput.KEY_BASE_NETWORK_ID),
								b.getInt(CellInput.KEY_BASE_SYSTEM_ID)));
			}
			if (b.getInt(CellInput.KEY_PHONE_TYPE) == TelephonyManager.PHONE_TYPE_NONE) {
				Log.i(TAG, "No cell found.");
			}
		}

		// Test for installed apps input
		if (b.getInt(KEY_TYPE) == Input.Type.INSTALLED_APPS.toInt()) {
			@SuppressWarnings("unchecked")
			List<PackageInfo> installedApplications = (List<PackageInfo>) b
					.getObject(InstalledAppsInput.KEY_INSTALLED_APPS_LIST);
			String appsList = "";
			for (PackageInfo packageInfo : installedApplications) {
				appsList = packageInfo.toString() + " " + appsList;
			}
			Log.i(TAG, String.format("Installed apps: %s", appsList));
		}

		// Test for magnetic field input
		if (b.getInt(KEY_TYPE) == Input.Type.MAGNETICFIELD.toInt()) {
			float magX = b.getFloat(MagneticFieldInput.KEY_MAGNETIC_FIELD_X);
			float magY = b.getFloat(MagneticFieldInput.KEY_MAGNETIC_FIELD_X);
			float magZ = b.getFloat(MagneticFieldInput.KEY_MAGNETIC_FIELD_X);
			Log.i(TAG, String.format("Magnetic field values: x=%s y=%s z=%s",
					magX, magY, magZ));
		}

		// Test for gyroscope input
		if (b.getInt(KEY_TYPE) == Input.Type.GYROSCOPE.toInt()) {
			float x = b.getFloat(GyroscopeInput.KEY_ROTATION_X);
			float y = b.getFloat(GyroscopeInput.KEY_ROTATION_Y);
			float z = b.getFloat(GyroscopeInput.KEY_ROTATION_Z);
			Log.i(TAG,
					String.format("Gyroscope values: x=%s y=%s z=%s", x, y, z));
		}

		// Test for statistics input
		if (b.getInt(KEY_TYPE) == Input.Type.SYSTEM_STATS.toInt()) {
			Log.i(TAG,
					String.format(
							"Cpu statistics: freq=%s, total usage=%s, user usage=%s, nice usage=%s, system usage=%s, context switch=%s, boot time=%s, processes=%s,",
							b.getFloat(StatisticsInput.KEY_CPU_USER),
							b.getFloat(StatisticsInput.KEY_CPU_NICE),
							b.getFloat(StatisticsInput.KEY_CPU_SYSTEM),
							b.getLong(StatisticsInput.KEY_CONTEXT_SWITCH),
							b.getLong(StatisticsInput.KEY_BOOT_TIME),
							b.getLong(StatisticsInput.KEY_PROCESSES)));

		}

		b.release();
	}

	@Override
	public Pipeline.Type getType() {
		return Type.TEST;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> usedInputs = new HashSet<Input.Type>();
		// usedInputs.add(Input.Type.APPONSCREEN);
		// usedInputs.add(Input.Type.WIFISCAN);
		// usedInputs.add(Input.Type.BLUETOOTHSCAN);
		// usedInputs.add(Input.Type.CONTINUOUS_LOCATION);
		usedInputs.add(Input.Type.PERIODIC_LOCATION);
		// usedInputs.add(Input.Type.SMSTIMESTAMP);
		// usedInputs.add(Input.Type.LIGHT);
		// usedInputs.add(Input.Type.PROXIMITY);
		// usedInputs.add(Input.Type.PHONECALL);
		usedInputs.add(Input.Type.BATTERY);
		// usedInputs.add(Input.Type.CELL);
		// usedInputs.add(Input.Type.INSTALLED_APPS);
		// usedInputs.add(Input.Type.MAGNETICFIELD);
		// usedInputs.add(Input.Type.GYROSCOPE);
		// usedInputs.add(Input.Type.STATISTICS);
		return usedInputs;
	}

}
