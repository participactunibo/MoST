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

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.input.GoogleActivityRecognitionInput;
import org.most.input.Input;
import org.most.input.InputAccelerometer;
import org.most.persistence.DBAdapter;
import org.most.weka.Features;
import org.most.weka.WekaClassifier;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PipelineActivityRecognitionCompare extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineActivityRecognitionCompare.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineActivityRecognitionCompare.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;
	public static final String PREF_KEY_USER_ACTIVITY= "PipelineActivityRecognitionCompare.activity";
	public static final String PREF_DEFAULT_USER_ACTIVITY = "unknown";
	
	// intent
	public static final String KEY_ACTION = "PipelineActivityRecognitionCompare";
	
	public static final String KEY_USER_ACTIVITY = "PipelineActivityRecognitionCompare.userActivity";
	
	public final static String KEY_G_TIMESTAMP = "PipelineActivityRecognitionCompare.gTimestamp";
	public final static String KEY_G_CONFIDENCE = "PipelineActivityRecognitionCompare.gConfidence";
	public final static String KEY_G_RECOGNIZED_ACTIVITY = "PipelineActivityRecognitionCompare.gActivityRecognition";

	public final static String KEY_D_RECOGNIZED_ACTIVITY = "PipelineActivityRecognitionCompare.dActivityRecognition";
	public final static String KEY_D_TIMESTAMP = "PipelineActivityRecognitionCompare.dTimestamp";


	// persistence	
	
	public final static String FLD_TIMESTAMP = "TIMESTAMP";
	public final static String FLD_USER_ACTIVITY = "USER_ACTIVITY";
	
	public final static String FLD_G_TIMESTAMP = "G_TIMESTAMP";
	public final static String FLD_G_CONFIDENCE = "G_CONFIDENCE";
	public final static String FLD_G_RECOGNIZED_ACTIVITY = "G_RECOGNIZED_ACTIVITY";

	public static final String FLD_D_TIMESTAMP = "D_TIMESTAMP";
	public static final String FLD_D_VALUE = "D_RECOGNIZED_ACTIVITY";

	public final static String TBL_ACTIVITY_RECOGNITION_COMPARE = "ACTIVITY_RECOGNITION_COMPARE";

	public static final String CREATE_ACTIVITY_RECOGNITION_COMPARE_TABLE = String
			.format("_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s TEXT NOT NULL, %s INT NOT NULL, %s TEXT NOT NULL, %s INT NOT NULL, %s INT NOT NULL, %s TEXT NOT NULL",
					FLD_TIMESTAMP, FLD_USER_ACTIVITY, FLD_G_TIMESTAMP, FLD_G_RECOGNIZED_ACTIVITY, FLD_G_CONFIDENCE, FLD_D_TIMESTAMP,
					FLD_D_VALUE);

	private boolean _isDump;
	private boolean _isSend;
	private String _userActivity;
	private DBAdapter _dbAdapter;

	private ArrayList<Float> x;
	private ArrayList<Float> y;
	private ArrayList<Float> z;
	private Features f;
	private long startTime;
	private long lastGTimestamp;
	private long lastDTimestamp;

	String gActivityRecognition = "";
	int gConfidence = 0;
	long gTimestamp = 0;

	String dActivityRecognition = "";
	long dTimestamp = 0;

	public PipelineActivityRecognitionCompare(MoSTApplication context) {
		super(context);
	}

	@Override
	public void onInit() {
		x = new ArrayList<Float>();
		y = new ArrayList<Float>();
		z = new ArrayList<Float>();
		f = new Features();
		super.onInit();
	}

	@Override
	public boolean onActivate() {
		_isDump = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES,
				Context.MODE_PRIVATE).getBoolean(PREF_KEY_DUMP_TO_DB, PREF_DEFAULT_DUMP_TO_DB);
		_isSend = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES,
				Context.MODE_PRIVATE).getBoolean(PREF_KEY_SEND_INTENT, PREF_DEFAULT_SEND_INTENT);
		_userActivity = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES,
				Context.MODE_PRIVATE).getString(PREF_KEY_USER_ACTIVITY, PREF_DEFAULT_USER_ACTIVITY);
		_dbAdapter = getContext().getDbAdapter();
		Log.e(PipelineActivityRecognitionCompare.class.getSimpleName(), "Activated with " + _userActivity);
		return super.onActivate();
	}

	public synchronized void onData(DataBundle b) {
		try {

			if (b.getInt(KEY_TYPE) == Input.Type.GOOGLE_ACTIVITY_RECOGNITION.toInt()) {
				gActivityRecognition = b
						.getString(GoogleActivityRecognitionInput.KEY_RECOGNIZED_ACTIVITY);
				gConfidence = b.getInt(GoogleActivityRecognitionInput.KEY_CONFIDENCE);
				gTimestamp = b.getLong(Input.KEY_TIMESTAMP);
			}

			if (b.getInt(KEY_TYPE) == Input.Type.ACCELEROMETER.toInt()) {
				float[] values = b.getFloatArray(InputAccelerometer.KEY_ACCELERATIONS);
				long currentTime = System.currentTimeMillis();

				if ((x.size() == 0) && (y.size() == 0) && (z.size() == 0)) {
					startTime = currentTime;
				}
				if (currentTime < startTime + 2000) {
					x.add(values[0]);
					y.add(values[1]);
					z.add(values[2]);
				} else {
					f.calcolaX(x);
					f.calcolaY(y);
					f.calcolaZ(z);
					try {
						int result = (int) WekaClassifier.classify(f.getFeatures());
						if (result == 0f)
							dActivityRecognition = "staticoSulTavolo";
						else if (result == 1)
							dActivityRecognition = "staticoInTasca";
						else if (result == 2)
							dActivityRecognition = "camminando";
						else if (result == 3)
							dActivityRecognition = "correndo";
						x.removeAll(x);
						y.removeAll(y);
						z.removeAll(z);
						dTimestamp = System.currentTimeMillis();
					} catch (Exception e) {
					}
				}
			}

			if (dTimestamp != 0 && gTimestamp != 0) {

				if (Math.abs(gTimestamp - dTimestamp) < 4000) {

					if (lastGTimestamp != gTimestamp && lastDTimestamp != dTimestamp) {

						lastDTimestamp = dTimestamp;
						lastGTimestamp = gTimestamp;
						
						long now = System.currentTimeMillis();

						if (_isDump || _isSend) {

							if (_isDump) {
								ContentValues cv = new ContentValues();
								cv.put(FLD_USER_ACTIVITY, _userActivity);
								cv.put(FLD_TIMESTAMP, now);
								cv.put(FLD_G_TIMESTAMP, gTimestamp);
								cv.put(FLD_G_RECOGNIZED_ACTIVITY, gActivityRecognition);
								cv.put(FLD_G_CONFIDENCE, gConfidence);
								cv.put(FLD_D_TIMESTAMP, dTimestamp);
								cv.put(FLD_D_VALUE, dActivityRecognition);
								_dbAdapter.storeData(TBL_ACTIVITY_RECOGNITION_COMPARE, cv, true);
							}

							if (_isSend) {
								Intent i = new Intent(KEY_ACTION);
								i.putExtra(KEY_TIMESTAMP, now);
								i.putExtra(KEY_USER_ACTIVITY, _userActivity);
								i.putExtra(KEY_G_TIMESTAMP, gTimestamp);
								i.putExtra(KEY_G_RECOGNIZED_ACTIVITY, gActivityRecognition);
								i.putExtra(KEY_G_CONFIDENCE, gConfidence);
								i.putExtra(KEY_D_TIMESTAMP, dTimestamp);
								i.putExtra(KEY_D_RECOGNIZED_ACTIVITY, dActivityRecognition);
								 getContext().sendBroadcast(i);
							}
						}
					}
				}
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.ACTIVITY_RECOGNITION_COMPARE;
	}

	@Override
	public Set<Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.GOOGLE_ACTIVITY_RECOGNITION);
		result.add(Input.Type.ACCELEROMETER);
		return result;
	}

}
