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
package org.most.persistence;

import java.io.File;

import org.most.MoSTApplication;
import org.most.pipeline.PipelineAccelerometer;
import org.most.pipeline.PipelineAccelerometerClassifier;
import org.most.pipeline.PipelineActivityRecognitionCompare;
import org.most.pipeline.PipelineAppOnScreen;
import org.most.pipeline.PipelineAppsNetTraffic;
import org.most.pipeline.PipelineBattery;
import org.most.pipeline.PipelineBluetooth;
import org.most.pipeline.PipelineCell;
import org.most.pipeline.PipelineConnectionType;
import org.most.pipeline.PipelineDeviceNetTraffic;
import org.most.pipeline.PipelineGoogleActivityRecognition;
import org.most.pipeline.PipelineGyroscope;
import org.most.pipeline.PipelineInstalledApps;
import org.most.pipeline.PipelineLight;
import org.most.pipeline.PipelineLocation;
import org.most.pipeline.PipelineMagneticField;
import org.most.pipeline.PipelinePhoneCallDuration;
import org.most.pipeline.PipelinePhoneCallEvent;
import org.most.pipeline.PipelineSystemStats;
import org.most.pipeline.PipelineWifiScan;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

	private static final String TAG = DBHelper.class.getSimpleName();

	private static final int DB_VERSION = 4;

	public DBHelper(Context context) {
		super(context.getApplicationContext(), context.getSharedPreferences(MoSTApplication.PREF_DB, Context.MODE_PRIVATE).getString(
				MoSTApplication.PREF_DB_NAME_KEY, MoSTApplication.PREF_DB_NAME_DEFAULT), null, DB_VERSION);
		Log.i(TAG, "Successfully created new instance of DBHelper");

	}

	public DBHelper(Context context, File path) {
		super(context.getApplicationContext(), getDBName(context, path), null, DB_VERSION);
		Log.i(TAG, "Successfully created new instance of DBHelper");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (oldVersion) {
		case 1:
		case 2:
		case 3:
			onCreate(db);
			break;

		default:
			break;
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTable(db, PipelineAppOnScreen.TBL_APP_ON_SCREEN, PipelineAppOnScreen.CREATE_APP_ON_SCREEN_TABLE);
		createTable(db, PipelineBattery.TBL_BATTERY, PipelineBattery.CREATE_BATTERY_TABLE);
		createTable(db, PipelineAccelerometer.TBL_ACCELEROMETER, PipelineAccelerometer.CREATE_ACCELEROMETER_TABLE);
		createTable(db, PipelineBluetooth.TBL_BLUETOOH, PipelineBluetooth.CREATE_BLUETOOTH_TABLE);
		createTable(db, PipelineGyroscope.TBL_GYROSCOPE, PipelineGyroscope.CREATE_GYROSCOPE_TABLE);
		createTable(db, PipelineInstalledApps.TBL_INSTALLED_APPS, PipelineInstalledApps.CREATE_INSTALLED_APPS_TABLE);
		createTable(db, PipelineLight.TBL_LIGHT, PipelineLight.CREATE_LIGHT_TABLE);
		createTable(db, PipelineLocation.TBL_LOCATION, PipelineLocation.CREATE_LOCATION_TABLE);
		createTable(db, PipelineMagneticField.TBL_MAGNETIC_FIELD, PipelineMagneticField.CREATE_MAGNETIC_FIELD_TABLE);
		createTable(db, PipelinePhoneCallDuration.TBL_PHONE_CALL_DURATION, PipelinePhoneCallDuration.CREATE_PHONE_CALL_DURATION_TABLE);
		createTable(db, PipelinePhoneCallEvent.TBL_PHONE_CALL_EVENT, PipelinePhoneCallEvent.CREATE_PHONE_CALL_EVENT_TABLE);
		createTable(db, PipelineSystemStats.TBL_SYSTEM_STATS, PipelineSystemStats.CREATE_SYSTEM_STATS_TABLE);
		createTable(db, PipelineAccelerometerClassifier.TBL_ACCELEROMETER_CLASSIFIER, PipelineAccelerometerClassifier.CREATE_ACCELEROMETER_CLASSIFIER_TABLE);
		createTable(db, PipelineWifiScan.TBL_WIFI_SCAN, PipelineWifiScan.CREATE_WIFI_SCAN_TABLE);
		createTable(db, PipelineCell.TBL_CELL, PipelineCell.CREATE_CELL_TABLE);
		createTable(db, PipelineDeviceNetTraffic.TBL_NET_TRAFFIC_DEVICE, PipelineDeviceNetTraffic.CREATE_DEVICE_NET_TRAFFIC_TABLE);
		createTable(db, PipelineAppsNetTraffic.TBL_NET_TRAFFIC_APPS, PipelineAppsNetTraffic.CREATE_APPS_NET_TRAFFIC_TABLE);
		createTable(db, PipelineConnectionType.TBL_CONNECTION_TYPE, PipelineConnectionType.CREATE_CONNECTION_TYPE_TABLE);
		createTable(db, PipelineGoogleActivityRecognition.TBL_GOOGLE_ACTIVITY_RECOGNITION, PipelineGoogleActivityRecognition.CREATE_GOOGLE_ACTIVITY_RECOGNITION_TABLE);
		createTable(db, PipelineActivityRecognitionCompare.TBL_ACTIVITY_RECOGNITION_COMPARE, PipelineActivityRecognitionCompare.CREATE_ACTIVITY_RECOGNITION_COMPARE_TABLE);
	}

	private void createTable(SQLiteDatabase db, String tableName, String createString) {
		db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s (%s)", tableName, createString));
	}

	public static String getDBName(Context context, File path) {
		return new File(path, context.getSharedPreferences(MoSTApplication.PREF_DB, Context.MODE_PRIVATE).getString(
				MoSTApplication.PREF_DB_NAME_KEY, MoSTApplication.PREF_DB_NAME_DEFAULT)).getPath();
	}
}
