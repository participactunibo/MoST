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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.most.pipeline.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MoSTService extends Service {
	
	private static final Logger logger = LoggerFactory.getLogger(MoSTService.class);

	public static final String START = "MostForeground";
	public static final String STOP = "MostStop";
	public static final String PING = "MostPing";
	public static final String PING_ACTION = "org.most.most.ping.action";
	public static final String KEY_PING_RESULT = "ping.result";
	public static final String KEY_PING_TIMESTAMP = "ping.timestamp";
	public static final String KEY_PIPELINE_TYPE = "PipelineType";

	private static final int NOTIFICATION_ID = 24;
	private static final String KEY_NOTIFICATION_INTENT_ACTIVITY = "MoST.notification.intent";

	public static final String PREF_KEY_STORE_STATE = "MoST.storeState";
	public static final boolean PREF_DEFAULT_STORE_STATE = false;
	private static final String TAG = "MoSTService";

	private AtomicInteger runningPipeline = new AtomicInteger(0);
	private Map<Pipeline.Type, Integer> listeners;

	ReentrantLock lock = new ReentrantLock();
	boolean firstRun;

	@Override
	public void onCreate() {
		super.onCreate();
		firstRun = true;

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.i(TAG, "Received onStartCommand");
		logger.info("Received onStartCommand");
		
		if(intent != null){
		
			logger.info("Intent action: {}", intent.getAction());
			
			if(intent.getExtras() != null){
				for (String key : intent.getExtras().keySet()) {
				    Object value = intent.getExtras().get(key);
				    logger.info("Intent content: key: {} value: {} class: {}.", key,  
				        value.toString(), value.getClass().getName());
				}
			}else{
				 logger.info("Intent doesn't contain extras.");
			}
		}else{
			logger.info("Received null intent.");
		}
		
		if (listeners == null) {
			listeners = new LinkedHashMap<Pipeline.Type, Integer>();
			logger.info("Created new listeners hashmap.");
		}

		if (firstRun) {
			
			logger.info("First run.");
			
			// restore sensing state
			if (getStoreStateEnabled()) {
				restoreState();
			}

			if ((intent != null && !PING.equals(intent.getAction())) || runningPipeline.get() > 0) {
				Log.d(TAG, "Starting MoSTService");
				firstRun = false;
				Class<?> startClass = MainActivity.class;
				String startIntent = null;
				if (intent != null && intent.getExtras() != null) {
					startIntent = intent.getExtras().getString(KEY_NOTIFICATION_INTENT_ACTIVITY);
				}
				if (startIntent != null) {
					try {
						startClass = this.getClassLoader().loadClass(startIntent);
						if (!Activity.class.isAssignableFrom(startClass)) {
							startClass = MainActivity.class;
						}
					} catch (ClassNotFoundException e) {
						startClass = MainActivity.class;
					}
				}
				Intent notificationIntent = new Intent(this, startClass);
				PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
				/*
				 * The following two statements are deprecated. However, better
				 * API is available only from API 11 (i.e. Android 3.0) onwards,
				 * thus we are stuck with those.
				 */
				Notification notification = new Notification(R.drawable.ic_stat_lightbulb, "Sensing started",
						System.currentTimeMillis());
				notification.setLatestEventInfo(this, "MoST running", "Collecting data", pendingIntent);
				startForeground(NOTIFICATION_ID, notification);
			}
			
		}

		if (intent != null) {

			if (START.equals(intent.getAction())) {
				Log.i(TAG, "Received start command");
				logger.info("Received start command for pipeline {}.", Pipeline.Type.fromInt(intent.getExtras().getInt(KEY_PIPELINE_TYPE)));
				startPipeline(Pipeline.Type.fromInt(intent.getExtras().getInt(KEY_PIPELINE_TYPE)));
			}

			if (STOP.equals(intent.getAction())) {
				Log.i(TAG, "Received stop command");
				logger.info("Received stop command for pipeline {}.", Pipeline.Type.fromInt(intent.getExtras().getInt(KEY_PIPELINE_TYPE)));
				stopPipeline(Pipeline.Type.fromInt(intent.getExtras().getInt(KEY_PIPELINE_TYPE)));

				if (runningPipeline.get() == 0) {
					Log.i(TAG, "Stopping MoSTService: no active pipeline");
					logger.info("Stopping MoSTService: no active pipeline.");
					stopForeground(true);
					stopSelf();
					return START_STICKY;
				}
			}

			if (PING.equals(intent.getAction())) {
				Log.i(TAG, "Received ping command");
				logger.info("Received ping command.");

				if (runningPipeline.get() > 0) {
					MoSTApplication ctx = (MoSTApplication) getApplicationContext();
					PipelineManager pipelineManager = ctx.getPipelineManager();
					LinkedList<Pipeline.Type> result = (LinkedList<Pipeline.Type>) pipelineManager
							.getPipelineByState(Pipeline.State.ACTIVATED);
					Intent i = new Intent();
					i.setAction(PING_ACTION);
					i.putExtra(KEY_PING_RESULT, result);
					i.putExtra(KEY_PING_TIMESTAMP, System.currentTimeMillis());
					sendBroadcast(i);
				} else {
					Intent i = new Intent();
					i.setAction(PING_ACTION);
					i.putExtra(KEY_PING_RESULT, new LinkedList<Pipeline.Type>());
					i.putExtra(KEY_PING_TIMESTAMP, System.currentTimeMillis());
					sendBroadcast(i);
				}
			}
		}
		
		Log.i(TAG, "Active Pipelines: " + runningPipeline.get());
		logger.info("Active Pipelines: {}", runningPipeline.get());
		
		for (Entry<Pipeline.Type, Integer> entry : listeners.entrySet()) {
			Log.i(TAG, "Pipeline: " + entry.getKey().toString() + " listeners: " + entry.getValue());
			logger.info("Pipelines: {} listeners: {}", entry.getKey().toString(), entry.getValue());
		}
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		logger.info("MostService onDestroy.");
		super.onDestroy();
	}

	private void updateState() {
		logger.info("Updating state.");
		lock.lock();
		try {
			MoSTApplication ctx = (MoSTApplication) getApplicationContext();
			PipelineManager pipelineManager = ctx.getPipelineManager();
			if (pipelineManager != null) {
				LinkedList<Pipeline.Type> result = (LinkedList<Pipeline.Type>) pipelineManager
						.getPipelineByState(Pipeline.State.ACTIVATED);
				MoSTState mostState = new MoSTState();
				mostState.setActivePipeline(result);
				mostState.setListeners(listeners);
				mostState.setRunningPipeline(runningPipeline);
				StateUtility.persistState(this, mostState);
				logger.info("State updated.");
			}
		} finally {
			lock.unlock();
		}
	}

	private void restoreState() {
		logger.info("Restoring state.");
		lock.lock();
		try {
			MoSTState mostState = StateUtility.loadState(this);
			if (mostState != null) {
				MoSTApplication ctx = (MoSTApplication) getApplicationContext();
				Controller ctrl = ctx.getController();
				for (Pipeline.Type pipeline : mostState.activePipeline) {
					ctrl.activatePipeline(pipeline);
				}
				listeners = mostState.listeners;
				runningPipeline = mostState.runningPipeline;
				logger.info("State restored.");
			}
		} finally {
			lock.unlock();
		}
	}

	private void startPipeline(Pipeline.Type pipeline) {
		MoSTApplication ctx = (MoSTApplication) getApplicationContext();
		Controller ctrl = ctx.getController();
		// per pipeline
		if (!listeners.containsKey(pipeline)) {
			// activate pipeline
			ctrl.activatePipeline(pipeline);
			listeners.put(pipeline, 1);
			// total++
			runningPipeline.incrementAndGet();
		} else {
			int num = listeners.get(pipeline);
			listeners.put(pipeline, ++num);
		}

		if (getStoreStateEnabled()) {
			updateState();
		}
	}

	private void stopPipeline(Pipeline.Type pipeline) {
		MoSTApplication ctx = (MoSTApplication) getApplicationContext();
		Controller ctrl = ctx.getController();
		if (!listeners.containsKey(pipeline)) {
			return;
		} else {
			int num = listeners.get(pipeline);
			if (num == 1) {
				ctrl.deactivatePipeline(pipeline);
				listeners.remove(pipeline);
				if (runningPipeline.decrementAndGet() < 0) {
					runningPipeline.set(0);
				}
			} else {
				listeners.put(pipeline, --num);
			}
		}

		if (getStoreStateEnabled()) {
			updateState();
		}
	}

	private boolean getStoreStateEnabled() {
		return getSharedPreferences(MoSTApplication.PREF_MOST_SERVICE, Context.MODE_PRIVATE).getBoolean(
				PREF_KEY_STORE_STATE, PREF_DEFAULT_STORE_STATE);
	}

}
