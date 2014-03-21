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
public class DutyCyclePolicy implements IPowerPolicy {

	private final static String TAG = DutyCyclePolicy.class.getSimpleName();

	private Context _context;
	private PendingIntent _pendingIntent;
	private DutyCyclePolicyBroadcastReceiver _receiver;
	private IntentFilter _filter;

	public static final String KEY_INPUT_TYPE = "Input.Type";
	public final static String PREF_KEY_DUTYCYCLEPOLICY_PERIOD_MS = "DutyCyclePolicyPeriodMs";
	public final static long PREF_DEFAULT_DUTYCYCLEPOLICY_PERIOD_MS = 20 * 1000; // 20
																					// seconds

	public static final String BASE_INTENT_ACTION = "DutyCyclePolicyIntent";

	private boolean isStarted = false;

	public DutyCyclePolicy(Context context, Input.Type input) {
		_context = context;
		Intent i = new Intent();
		i.setAction(BASE_INTENT_ACTION + input.toInt());
		i.putExtra(KEY_INPUT_TYPE, input.toInt());
		_pendingIntent = PendingIntent.getBroadcast(_context, input.toInt(), i, 0);
		_receiver = new DutyCyclePolicyBroadcastReceiver();
		_filter = new IntentFilter();
		_filter.addAction(BASE_INTENT_ACTION + input.toInt());
	}

	public synchronized void start() {
		_context.registerReceiver(_receiver, _filter);
		AlarmManager mgr = (AlarmManager) _context.getSystemService(Context.ALARM_SERVICE);
		long period = _context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getLong(
				PREF_KEY_DUTYCYCLEPOLICY_PERIOD_MS, PREF_DEFAULT_DUTYCYCLEPOLICY_PERIOD_MS);
		mgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), period, _pendingIntent);
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

	public class DutyCyclePolicyBroadcastReceiver extends BroadcastReceiver {

		private boolean _state;

		public DutyCyclePolicyBroadcastReceiver() {
			_state = true;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			Input.Type inputType = Input.Type.fromInt(intent.getExtras().getInt(KEY_INPUT_TYPE));
			Log.i(TAG, "Timer expired for " + inputType.toString());
			_state = !_state;
			((MoSTApplication) context).getInputsArbiter().setPowerVote(inputType, _state);
		}
	}
}
