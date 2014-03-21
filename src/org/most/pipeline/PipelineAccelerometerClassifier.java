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
import java.util.HashSet;
import java.util.Set;

import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.input.Input;
import org.most.input.InputAccelerometer;
import org.most.weka.Features;
import org.most.weka.WekaClassifier;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PipelineAccelerometerClassifier extends Pipeline {
	
	private static final String TAG = PipelineAccelerometerClassifier.class.getSimpleName();

	private ArrayList<Float> x;
	private ArrayList<Float> y;
	private ArrayList<Float> z;
	private Features f;
	private long startTime;
	
	public static final String KEY_ACTION = "PipelineAccelerometerClassifier";
	public static final String KEY_VALUE = "PipelineAccelerometerClassifier.value";
	
	
	public static final String PREF_KEY_DUMP_TO_DB = "PipelineAccelerometerClassifier.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineAccelerometerClassifier.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;
	
	public static final String TBL_ACCELEROMETER_CLASSIFIER = "ACCELEROMETER_CLASSIFIER";
	public static final String FLD_TIMESTAMP = "timestamp";
	public static final String FLD_VALUE = "value";

	public static final String CREATE_ACCELEROMETER_CLASSIFIER_TABLE = String.format(
			"_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s TEXT NULL", FLD_TIMESTAMP, FLD_VALUE);

	private static final int LIMIT_OUTPUT = 16; // 2 per min
	
	protected boolean _isDump;
	protected boolean _isSend;
	protected int count;
	
	public PipelineAccelerometerClassifier(MoSTApplication context) {
		this(context, true);
	}
	
	public PipelineAccelerometerClassifier(MoSTApplication context, boolean dump) {
		super(context, 50);
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
		checkNewState(State.ACTIVATED);
		_isDump = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES, Context.MODE_PRIVATE).getBoolean(
				PREF_KEY_DUMP_TO_DB, PREF_DEFAULT_DUMP_TO_DB);
		_isSend = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES, Context.MODE_PRIVATE).getBoolean(
				PREF_KEY_SEND_INTENT, PREF_DEFAULT_SEND_INTENT);
		return super.onActivate();
	}

	public void onData(DataBundle b) {
		
		float[] values = b.getFloatArray(InputAccelerometer.KEY_ACCELERATIONS);
        long currentTime = System.currentTimeMillis();
		b.release();
		String accClass = "";

		if((x.size()==0)&&(y.size()==0)&&(z.size()==0)){
			startTime=currentTime;
		}
		if(currentTime<startTime+2000)
		{
			x.add(values[0]);
			y.add(values[1]);
			z.add(values[2]);
		}
		else
		{
            f.calcolaX(x);
            f.calcolaY(y);
            f.calcolaZ(z);
            try {
				int result = (int) WekaClassifier.classify(f.getFeatures());
				if(result == 0f)
				   accClass = "staticoSulTavolo";
				else if(result == 1)
				   accClass="staticoInTasca";
				else if(result == 2)
				   accClass="camminando";
				else if(result == 3)
				   accClass = "correndo";
				x.removeAll(x);
				y.removeAll(y);
				z.removeAll(z);
			} catch (Exception e) {
			}
            
            
            if((++count % LIMIT_OUTPUT) == 0){
            	
	        	if (_isDump) {
					ContentValues cv = new ContentValues();
					cv.put(FLD_TIMESTAMP, System.currentTimeMillis());
					cv.put(FLD_VALUE, accClass);
					getContext().getDbAdapter().storeData(TBL_ACCELEROMETER_CLASSIFIER, cv, true);
				}
	
	        	if (_isSend) {
	            Intent i = new Intent();
	            i.setAction(KEY_ACTION);
	            i.putExtra(KEY_VALUE, accClass);
	            getContext().sendBroadcast(i);
	        	}
	        
	            Log.i(TAG, "Result " + accClass);
	            
	            count = 1;
            }
		}
	}

	@Override
	public Set<Input.Type> getInputs() {
		Set<Input.Type> result = new HashSet<Input.Type>();
		result.add(Input.Type.ACCELEROMETER);
		return result;
	}

	@Override
	public Type getType() {
		return Type.ACCELEROMETER_CLASSIFIER;
	}


}
