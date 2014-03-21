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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;

import org.most.MoSTApplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public abstract class PeriodicInput extends Input {
	
	public static final String BASE_INTENT_ACTION = "PeriodicInput";

	private PendingIntent _pendingIntent;
	private BroadcastReceiver _receiver;
	private IntentFilter _filter;
	
	protected Timer _timer;
	protected int _period;
	/**
	 * Time when this periodic input was last started.
	 */
	protected Date _lastStart;


	/**
	 * Creates a PeriodicInput
	 * @param context {@link MoSTApplication} context
	 * @param period Period of the input in milliseconds.
	 */
	public PeriodicInput(MoSTApplication context, int period) {
		super(context);
		_period = period;
		Calendar lastStart = GregorianCalendar.getInstance();
		lastStart.set(Calendar.YEAR, 1970);
		_lastStart = lastStart.getTime();
		
		Intent i = new Intent();
		i.setAction(BASE_INTENT_ACTION + getType().toInt());
		_pendingIntent = PendingIntent.getBroadcast(context, 1000+getType().toInt(), i, 0);
		_receiver = new PeriodicInputBroadcastReceiver();
		_filter = new IntentFilter();
		_filter.addAction(BASE_INTENT_ACTION + getType().toInt());
	}
	
	protected void scheduleNextStart() {
		if (getState() == State.ACTIVATED) {
			AlarmManager mgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
			Calendar now = GregorianCalendar.getInstance();
			now.add(Calendar.MILLISECOND, _period);
			Date nextStart = now.getTime();
			mgr.set(AlarmManager.RTC_WAKEUP, nextStart.getTime(), _pendingIntent); 
		}
	}

	@Override
	public boolean onActivate() {
		checkNewState(State.ACTIVATED);
		getContext().registerReceiver(_receiver, _filter);
		AlarmManager mgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
		Calendar now = GregorianCalendar.getInstance();
		Calendar nextScheduledTime = GregorianCalendar.getInstance();
		nextScheduledTime.setTime(_lastStart);
		nextScheduledTime.add(Calendar.MILLISECOND, (int) _period);
		Date nextStart = null;
		if (now.after(nextScheduledTime)) {
			nextStart = new Date();
		} else {
			nextStart = nextScheduledTime.getTime();
		}

		mgr.set(AlarmManager.RTC_WAKEUP, nextStart.getTime(), _pendingIntent); 
		
		return super.onActivate();
	}
	

	@Override
	public void onDeactivate() {
		checkNewState(State.DEACTIVATED);
		AlarmManager mgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
		mgr.cancel(_pendingIntent);
		getContext().unregisterReceiver(_receiver);
		super.onDeactivate();
	}

	/**
	 * Method that define the actions to do every time that the timer expires.
	 */
	public abstract void workToDo();
	
	@Override
	public boolean isWakeLockNeeded() {
		return false;
	}
	
	
	private class PeriodicInputBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			workToDo();
		}
		
	}

}
