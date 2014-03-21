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

import java.util.Set;

import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.input.Input;
import org.most.input.InputBus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * This class models a generic pipeline.
 * 
 * For {@link Pipeline#onInit()}, {@link Pipeline#onActivated()},
 * {@link Pipeline#onDeactivated()} and {@link Pipeline#onFinalized()}
 * <strong>must</strong> call {@link Pipeline#checkNewState(State)} to make sure
 * that the state transition is valid, and at the end of each of those events
 * <strong>must</strong> call the <code>super.on*</code> method.
 * 
 * Whenever an Pipeline changes state, it broadcasts an {@link Intent} having:
 * <ul>
 * <li>{@link Intent#getAction()} set to {@link MoSTApplication#ACTION_PIPELINE}
 * </li>
 * <li>an extra field having key {@link Pipeline#EVENT_TYPE} containing the
 * {@link EventType};</li>
 * <li>an extra field having key {@link Pipeline#INPUT_TYPE} containing the
 * {@link Type} of the pipeline that changed state.</li>
 * </ul>
 * 
 * Please note that the above fields are not serialized because pipeline state
 * change is a relatively infrequent event, thus serialization does not cause
 * any performance issue.
 * 
 */
public abstract class Pipeline implements InputBus.Listener {

	public enum EventType {
		INITED, ACTIVATED, DEACTIVATED, FINALIZED
	}

	/**
	 * States an Pipeline can be in.
	 * 
	 */
	public static enum State {
		INVALID, INITED, ACTIVATED, DEACTIVATED, FINALIZED
	}

	/**
	 * Defines the type of the Pipeline. New Pipelines should extend this list.
	 * 
	 */
	public static enum Type {
		DUMMY, AUDIO_CLASSIFIER, ACTIVITY_RECOGNITION_COMPARE, ACCELEROMETER, ACCELEROMETER_CLASSIFIER, RAW_AUDIO, AVERAGE_ACCELEROMETER, APP_ON_SCREEN, APPS_NET_TRAFFIC, BATTERY, BLUETOOTH, CELL, CONNECTION_TYPE, DEVICE_NET_TRAFFIC, GOOGLE_ACTIVITY_RECOGNITION, GYROSCOPE, INSTALLED_APPS, LIGHT, LOCATION, MAGNETIC_FIELD, PHONE_CALL_DURATION, PHONE_CALL_EVENT, SYSTEM_STATS, WIFI_SCAN, TEST;

		/**
		 * Converts an integer to a valid Pipeline type.
		 * 
		 * @param value
		 *            The integer to convert
		 * @return The Type represented by <code>value</code>. If the conversion
		 *         fails, returns DUMMY.
		 */
		public static Type fromInt(int value) {
			switch (value) {
			case 1:
				return ACCELEROMETER;
			case 2:
				return RAW_AUDIO;
			case 3:
				return AVERAGE_ACCELEROMETER;
			case 4:
				return APP_ON_SCREEN;
			case 5:
				return BATTERY;
			case 6:
				return BLUETOOTH;
			case 7:
				return CELL;
			case 8:
				return GYROSCOPE;
			case 9:
				return INSTALLED_APPS;
			case 10:
				return LIGHT;
			case 11:
				return LOCATION;
			case 12:
				return MAGNETIC_FIELD;
			case 13:
				return PHONE_CALL_DURATION;
			case 14:
				return PHONE_CALL_EVENT;
			case 15:
				return ACCELEROMETER_CLASSIFIER;
			case 16:
				return SYSTEM_STATS;
			case 17:
				return WIFI_SCAN;
			case 18:
				return AUDIO_CLASSIFIER;
			case 19:
				return DEVICE_NET_TRAFFIC;
			case 20:
				return APPS_NET_TRAFFIC;
			case 21:
				return CONNECTION_TYPE;
			case 22:
				return GOOGLE_ACTIVITY_RECOGNITION;
			case 23:
				return ACTIVITY_RECOGNITION_COMPARE;
			case 99:
				return TEST;
			default:
				return DUMMY;
			}
		}

		/**
		 * Converts the Pipeline type to an integer.
		 * 
		 * @return
		 */
		public int toInt() {
			switch (this) {
			case ACCELEROMETER:
				return 1;
			case RAW_AUDIO:
				return 2;
			case AVERAGE_ACCELEROMETER:
				return 3;
			case APP_ON_SCREEN:
				return 4;
			case BATTERY:
				return 5;
			case BLUETOOTH:
				return 6;
			case CELL:
				return 7;
			case GYROSCOPE:
				return 8;
			case INSTALLED_APPS:
				return 9;
			case LIGHT:
				return 10;
			case LOCATION:
				return 11;
			case MAGNETIC_FIELD:
				return 12;
			case PHONE_CALL_DURATION:
				return 13;
			case PHONE_CALL_EVENT:
				return 14;
			case ACCELEROMETER_CLASSIFIER:
				return 15;
			case SYSTEM_STATS:
				return 16;
			case WIFI_SCAN:
				return 17;
			case AUDIO_CLASSIFIER:
				return 18;
			case DEVICE_NET_TRAFFIC:
				return 19;
			case APPS_NET_TRAFFIC:
				return 20;
			case CONNECTION_TYPE:
				return 21;
			case GOOGLE_ACTIVITY_RECOGNITION:
				return 22;
			case ACTIVITY_RECOGNITION_COMPARE:
				return 23;
			case TEST:
				return 99;
			default:
				return 0;
			}
		}
	}

	public static final String EVENT_TYPE = "org.most.pipeline.event_type";

	public static final String PIPELINE_TYPE = "org.most.pipeline.pipeline_type";

	public static final String KEY_TIMESTAMP = "timestamp";

	public static final String KEY_TYPE = "sensor_type";

	private static final Boolean DEBUG = true;

	private static final String TAG = Pipeline.class.getSimpleName();

	private MoSTApplication _context;
	private State _state;
	private InputStateChangeReceiver _inputStateChangeReceiver;
	private Boolean _active;
	private PipelineBus.SinglePipelineBus _bus;
	private PipelineQueue _pipelineQueue;
	private Thread _thread;

	public Pipeline(MoSTApplication context) {
		_context = context;
		_state = State.INVALID;
		_active = false;
		_bus = context.getPipelineBus().getBus(getType());
		_pipelineQueue = new PipelineQueue(this);
	}

	public Pipeline(MoSTApplication context, int bufferCapacity) {
		_context = context;
		_state = State.INVALID;
		_active = false;
		_bus = context.getPipelineBus().getBus(getType());
		_pipelineQueue = new PipelineQueue(this, bufferCapacity);
	}

	/**
	 * Checks that newState is a valid state to transition to, given the current
	 * state.
	 * 
	 * @param newState
	 *            The new state to which the Pipeline should switch.
	 * @throws IllegalStateException
	 *             if the new state is invalid
	 * @see Pipeline#getState()
	 */
	protected void checkNewState(State newState) {
		State currentState = getState();
		boolean invalidNewState = false;
		switch (currentState) {
		case INVALID:
			if (newState != State.INITED)
				invalidNewState = true;
			break;
		case INITED:
			if (newState != State.ACTIVATED && newState != State.FINALIZED)
				invalidNewState = true;
			break;
		case ACTIVATED:
			if (newState != State.DEACTIVATED)
				invalidNewState = true;
			break;
		case DEACTIVATED:
			if (newState != State.ACTIVATED && newState != State.FINALIZED)
				invalidNewState = true;
			break;
		case FINALIZED:
			invalidNewState = true;
			break;
		default:
			invalidNewState = true;
		}
		if (invalidNewState) {
			throw new IllegalStateException();
		}
	}

	/**
	 * Gets the {@link MoSTApplication} context of this pipeline.
	 * 
	 * @return The context of this pipeline.
	 */
	public MoSTApplication getContext() {
		return _context;
	}

	/**
	 * Gets the current state of this pipeline.
	 * 
	 * @return the state
	 */
	public State getState() {
		return _state;
	}

	/**
	 * Gets the PipelineQueue of this pipeline.
	 * 
	 * @return The PipelineQueue of this Pipeline.
	 */
	public InputBus.Listener getListener() {
		return _pipelineQueue;
	}

	/**
	 * Gets the type of this pipeline.
	 * 
	 * @return The type of this Pipeline.
	 */
	public abstract Type getType();

	/**
	 * Pause.
	 */
	public void onInit() {
		checkNewState(State.INITED);
		_state = State.INITED;
		Intent i = new Intent();
		i.setAction(MoSTApplication.ACTION_PIPELINE);
		i.putExtra(EVENT_TYPE, EventType.INITED);
		i.putExtra(PIPELINE_TYPE, getType());
		_context.sendBroadcast(i);
	}

	/**
	 * Activates the pipeline. The pipeline will send a broadcast intent with
	 * the following fields:
	 * <ul>
	 * <li>Action: {@link MoSTApplication#ACTION_PIPELINE};</li>
	 * <li>Extra: {@link EventType}: {@link EventType#ACTIVATED};</li>
	 * <li>Extra: {@link Pipeline#PIPELINE_TYPE}: the pipeline type returned by
	 * {@link Pipeline#getType()}</li>
	 */
	public boolean onActivate() {
		checkNewState(State.ACTIVATED);
		_state = State.ACTIVATED;
		Intent i = new Intent();
		i.setAction(MoSTApplication.ACTION_PIPELINE);
		i.putExtra(EVENT_TYPE, EventType.ACTIVATED);
		i.putExtra(PIPELINE_TYPE, getType());
		_context.sendBroadcast(i);
		_inputStateChangeReceiver = new InputStateChangeReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(MoSTApplication.ACTION_INPUT);
		getContext().registerReceiver(_inputStateChangeReceiver, intentFilter);
		_thread = new Thread(_pipelineQueue);
		_thread.setName("PipelineQueue of " + getClass().getSimpleName());
		_thread.start();
		_active = true;
		if (DEBUG) {
			Log.d(TAG, "Pipeline " + getType() + " activated");
		}
		return true;
	}

	/**
	 * Stops the Pipeline. The pipeline will send a broadcast intent with the
	 * following fields:
	 * <ul>
	 * <li>Action: {@link MoSTApplication#ACTION_PIPELINE};</li>
	 * <li>Extra: {@link EventType}: {@link EventType#DEACTIVATED};</li>
	 * <li>Extra: {@link Pipeline#PIPELINE_TYPE}: the pipeline type returned by
	 * {@link Pipeline#getType()}</li>
	 * </ul>
	 */
	public void onDeactivate() {
		checkNewState(State.DEACTIVATED);
		if (DEBUG) {
			Log.d(TAG, "Pipeline " + getType() + " deactivating");
		}

		// Unregister from notification of inputs.
		getContext().unregisterReceiver(_inputStateChangeReceiver);
		_inputStateChangeReceiver = null;

		// Stop the queue of incoming data.
		try {
			_thread.interrupt();
			_thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		_active = false;
		_state = State.DEACTIVATED;
		getContext().getDbAdapter().flushData();
		Intent i = new Intent();
		i.setAction(MoSTApplication.ACTION_PIPELINE);
		i.putExtra(EVENT_TYPE, EventType.DEACTIVATED);
		i.putExtra(PIPELINE_TYPE, getType());
		_context.sendBroadcast(i);
	}

	/**
	 * Finalize the Pipeline. The pipeline will send a broadcast intent with the
	 * following fields:
	 * <ul>
	 * <li>Action: {@link MoSTApplication#ACTION_PIPELINE};</li>
	 * <li>Extra: {@link EventType}: {@link EventType#FINALIZED};</li>
	 * <li>Extra: {@link Pipeline#PIPELINE_TYPE}: the pipeline type returned by
	 * {@link Pipeline#getType()}</li>
	 */
	public void onFinalize() {
		checkNewState(State.FINALIZED);
		_state = State.FINALIZED;
		Intent i = new Intent();
		i.setAction(MoSTApplication.ACTION_PIPELINE);
		i.putExtra(EVENT_TYPE, EventType.FINALIZED);
		i.putExtra(PIPELINE_TYPE, getType());
		_context.sendBroadcast(i);
	}

	/**
	 * Called whenever an Input this Pipeline connects to changes state.
	 * 
	 * @param i
	 *            the Intent sent by the Input. Refer to {@link Input} for
	 *            details about its content.
	 */
	public void onInputStateChange(Intent i) {

	}

	/**
	 * Checks if this pipeline is active.
	 * 
	 * @return <code>true</code> if this pipeline is active, else
	 *         <code>false</code>.
	 */
	public boolean isActive() {
		return _active;
	}

	/**
	 * Gets the {@link Input.Type}s that this pipeline needs to run.
	 * 
	 * @return The set of Input types that this pipeline needs.
	 */
	public abstract Set<Input.Type> getInputs();

	/**
	 * Posts a bundle on the bus associated to this Pipeline.
	 * 
	 * @param b
	 *            The bundle to post.
	 */
	protected void post(DataBundle b) {
		if (_active) {
			_bus.post(b);
		} else {
			b.release();
		}
	}

	class InputStateChangeReceiver extends BroadcastReceiver {

		/** The Constant TAG. */
		private final String TAG = InputStateChangeReceiver.class.getSimpleName();

		/** The Constant DEBUG. */
		private final static boolean DEBUG = true;

		public void onReceive(Context context, Intent intent) {
			if (DEBUG)
				Log.d(TAG, "onReceive: " + intent);
			if (MoSTApplication.ACTION_INPUT.equals(intent.getAction())) {
				Input.Type inputType = (Input.Type) intent.getSerializableExtra(Input.INPUT_TYPE);
				if (getInputs().contains(inputType)) {
					onInputStateChange(intent);
				}
			}
		}
	}
}