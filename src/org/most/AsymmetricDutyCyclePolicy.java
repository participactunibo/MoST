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
package org.most;

import org.most.input.Input;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * This class implements a simple duty cycle policy that periodically switched
 * on and off a sensor.
 * 
 * @author acirri
 */
public class AsymmetricDutyCyclePolicy implements IPowerPolicy {

	private final static String TAG = AsymmetricDutyCyclePolicy.class.getSimpleName();

	private Context _context;
	private PendingIntent _pendingIntent;
	private AsymetricDutyCyclePolicyBroadcastReceiver _receiver;
	private IntentFilter _filter;

	public static final String KEY_INPUT_TYPE = "Input.Type";
	public final static String PREF_KEY_DUTYCYCLEPOLICY_HIGH_PERIOD_MS = "DutyCyclePolicyHighPeriodMs";
	public final static String PREF_KEY_DUTYCYCLEPOLICY_LOW_PERIOD_MS = "DutyCyclePolicyLowPeriodMs";
	public final static long PREF_DEFAULT_ASYMETRIC_DUTYCYCLEPOLICY_HIGH_PERIOD_MS = 10 * 1000; // 10 seconds
	public final static long PREF_DEFAULT_ASYMETRIC_DUTYCYCLEPOLICY_LOW_PERIOD_MS = 2 * 60 * 1000; // 2 minutes

	public static final String BASE_INTENT_ACTION = "AsymetricDutyCyclePolicyIntent";

	private boolean isStarted = false;
	private long _highPeriod = 0L;
	private long _lowPeriod = 0L;

	public AsymmetricDutyCyclePolicy(Context context, Input.Type input) {
		_context = context;
		Intent i = new Intent();
		i.setAction(BASE_INTENT_ACTION + input.toInt());
		i.putExtra(KEY_INPUT_TYPE, input.toInt());
		_pendingIntent = PendingIntent.getBroadcast(_context, input.toInt(), i, 0);
		_receiver = new AsymetricDutyCyclePolicyBroadcastReceiver();
		_filter = new IntentFilter();
		_filter.addAction(BASE_INTENT_ACTION + input.toInt());
	}

	public synchronized void start() {
		_context.registerReceiver(_receiver, _filter);
		AlarmManager mgr = (AlarmManager) _context.getSystemService(Context.ALARM_SERVICE);
		_highPeriod = _context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getLong(
				PREF_KEY_DUTYCYCLEPOLICY_HIGH_PERIOD_MS, PREF_DEFAULT_ASYMETRIC_DUTYCYCLEPOLICY_HIGH_PERIOD_MS);
		_lowPeriod = _context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getLong(
				PREF_KEY_DUTYCYCLEPOLICY_LOW_PERIOD_MS, PREF_DEFAULT_ASYMETRIC_DUTYCYCLEPOLICY_LOW_PERIOD_MS);
		if(_receiver.getState()){
			mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + _highPeriod, _pendingIntent);
		}else{
			mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + _lowPeriod, _pendingIntent);
		}
		Log.i(TAG, "Power policy started");
		isStarted = true;
	}

	public synchronized void stop() {
		if (isStarted) {
			AlarmManager mgr = (AlarmManager) _context.getSystemService(Context.ALARM_SERVICE);
			mgr.cancel(_pendingIntent);
			_context.unregisterReceiver(_receiver);
			Log.i(TAG, "Power policy stopped");
			isStarted = false;
		}
	}

	public class AsymetricDutyCyclePolicyBroadcastReceiver extends BroadcastReceiver {

		private boolean _state;
		private AlarmManager mgr;
		
		public AsymetricDutyCyclePolicyBroadcastReceiver() {
			_state = true;
		}
		
		public boolean getState(){
			return _state;
		}
		
		public void setState(boolean state){
			_state = state;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			
			if(mgr == null){
				mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			}
			
			Input.Type inputType = Input.Type.fromInt(intent.getExtras().getInt(KEY_INPUT_TYPE));
			Log.i(TAG, "Timer expired for " + inputType.toString());
			_state = !_state;
			if(_state){
				mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + _highPeriod, _pendingIntent);
			}else{
				mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + _lowPeriod, _pendingIntent);
			}
			((MoSTApplication) context).getInputsArbiter().setPowerVote(inputType, _state);
		}
	}
}
