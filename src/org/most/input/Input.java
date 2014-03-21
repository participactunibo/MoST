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

import org.most.DataBundle;
import org.most.DataBundlePool;
import org.most.MoSTApplication;
import org.most.input.InputBus.SingleInputBus;

import android.content.Intent;
import android.util.Log;

/**
 * This class models a generic sensor. The overridden methods of
 * {@link Input#onInit()}, {@link Input#onActivate()},
 * {@link Input#onDeactivate()} and {@link Input#onFinalize()}
 * <strong>must</strong> call {@link Input#checkNewState(State)} to make sure
 * that the state transition is valid, and <strong>must</strong> call the
 * <code>super.on*</code> method.
 * 
 * Whenever an Input changes state, it broadcasts an {@link Intent} having:
 * <ul>
 * <li>{@link Intent#getAction()} set to {@link MoSTApplication#ACTION_INPUT}</li>
 * <li>an extra field having key {@link Input#EVENT_TYPE} containing the
 * {@link EventType};</li>
 * <li>an extra field having key {@link Input#INPUT_TYPE} containing the
 * {@link Type} of the sensor that changed state.</li>
 * </ul>
 * 
 * Please note that the above fields are serialized because input state change
 * is a relatively infrequent event, thus serialization does not cause any
 * performance issue.
 * 
 */
public abstract class Input {
	
	private static final boolean DEBUG = true;
	private static final String TAG = Input.class.getSimpleName();

	public enum EventType {
		INITED, ACTIVATED, DEACTIVATED, FINALIZED
	}

	/**
	 * States an Input can be in.
	 * 
	 */
	public static enum State {
		INVALID, INITED, ACTIVATED, DEACTIVATED, FINALIZED;
	}

	/**
	 * Defines the type of the Input. New Inputs should extend this list.
	 * 
	 */
	public static enum Type {
		ACCELEROMETER, AUDIO, APPONSCREEN, BATTERY, BLUETOOTHSCAN, CELL, CONTINUOUS_FUSION_LOCATION, CONTINUOUS_LOCATION, GYROSCOPE, GOOGLE_ACTIVITY_RECOGNITION, INSTALLED_APPS, LIGHT, MAGNETICFIELD, PERIODIC_CONNECTION_TYPE, PERIODIC_FUSION_LOCATION, PERIODIC_GOOGLE_ACTIVITY_RECOGNITION, PERIODIC_LOCATION, PHONECALL, PROXIMITY, SMSTIMESTAMP, SYSTEM_STATS, WIFISCAN, NET_TRAFFIC, DUMMY;

		/**
		 * Converts an integer to a valid Input type.
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
				return AUDIO;
			case 3:
				return APPONSCREEN;
			case 4:
				return WIFISCAN;
			case 5:
				return BLUETOOTHSCAN;
			case 6:
				return CONTINUOUS_LOCATION;
			case 7:
				return SMSTIMESTAMP;
			case 8:
				return LIGHT;
			case 9:
				return PROXIMITY;
			case 10:
				return PHONECALL;
			case 11:
				return BATTERY;
			case 12:
				return CELL;
			case 13:
				return INSTALLED_APPS;
			case 14:
				return MAGNETICFIELD;
			case 15:
				return GYROSCOPE;
			case 16:
				return SYSTEM_STATS;
			case 17:
				return PERIODIC_LOCATION;
			case 18:
				return NET_TRAFFIC;
			case 19:
				return CONTINUOUS_FUSION_LOCATION;
			case 20:
				return PERIODIC_FUSION_LOCATION;
			case 21:
				return PERIODIC_CONNECTION_TYPE;
			case 22:
				return GOOGLE_ACTIVITY_RECOGNITION;
			case 23:
				return PERIODIC_GOOGLE_ACTIVITY_RECOGNITION;
			default:
				return DUMMY;
			}
		}

		/**
		 * Converts the Input type to an integer.
		 * 
		 * @return
		 */
		public int toInt() {
			switch (this) {
			case ACCELEROMETER:
				return 1;
			case AUDIO:
				return 2;
			case APPONSCREEN:
				return 3;
			case WIFISCAN:
				return 4;
			case BLUETOOTHSCAN:
				return 5;
			case CONTINUOUS_LOCATION:
				return 6;
			case SMSTIMESTAMP:
				return 7;
			case LIGHT:
				return 8;
			case PROXIMITY:
				return 9;
			case PHONECALL:
				return 10;
			case BATTERY:
				return 11;
			case CELL:
				return 12;
			case INSTALLED_APPS:
				return 13;
			case MAGNETICFIELD:
				return 14;
			case GYROSCOPE:
				return 15;
			case SYSTEM_STATS:
				return 16;
			case PERIODIC_LOCATION:
				return 17;
			case NET_TRAFFIC:
				return 18;
			case CONTINUOUS_FUSION_LOCATION:
				return 19;
			case PERIODIC_FUSION_LOCATION:
				return 20;
			case PERIODIC_CONNECTION_TYPE:
				return 21;
			case GOOGLE_ACTIVITY_RECOGNITION:
				return 22;
			case PERIODIC_GOOGLE_ACTIVITY_RECOGNITION:
				return 23;
			default:
				return 0;
			}
		}
	}

	public static final String EVENT_TYPE = "org.most.Input.event_type";

	public static final String INPUT_TYPE = "org.most.Input.input_type";

	public static final String KEY_TIMESTAMP = "timestamp";

	public static final String KEY_TYPE = "sensor_type";

	private MoSTApplication _context;
	private State _state;
	protected SingleInputBus _bus;
	protected DataBundlePool _bundlePool;

	public Input(MoSTApplication context) {
		_context = context;
		_state = State.INVALID;
		_bus = context.getInputBus().getBus(getType());
		_bundlePool = context.getDataBundlePool();
	}

	/**
	 * Checks that newState is a valid state to transition to, given the current
	 * state.
	 * 
	 * @param newState
	 *            The new state to which the Input should switch.
	 * @throws IllegalStateException
	 *             if the new state is invalid
	 * @see Input#getState()
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
	 * Gets the {@link MoSTApplication} context of this input.
	 * 
	 * @return The context of this Input.
	 */
	public MoSTApplication getContext() {
		return _context;
	}

	/**
	 * Gets the current state of this input.
	 * 
	 * @return the state
	 */
	public State getState() {
		return _state;
	}

	/**
	 * Gets the type of this input.
	 * 
	 * @return The input of this Input.
	 */
	public abstract Type getType();

	/**
	 * Pause.
	 */
	public void onInit() {
		checkNewState(State.INITED);
		_state = State.INITED;
		Intent i = new Intent();
		i.setAction(MoSTApplication.ACTION_INPUT);
		i.putExtra(EVENT_TYPE, EventType.INITED);
		i.putExtra(INPUT_TYPE, getType());
		_context.sendBroadcast(i);
		if (DEBUG) {
			Log.d(TAG, String.format("Input %s onInit", getType()));
		}
	}

	/**
	 * Activation.
	 */
	public boolean onActivate() {
		checkNewState(State.ACTIVATED);
		_state = State.ACTIVATED;
		Intent i = new Intent();
		i.setAction(MoSTApplication.ACTION_INPUT);
		i.putExtra(EVENT_TYPE, EventType.ACTIVATED);
		i.putExtra(INPUT_TYPE, getType());
		_context.sendBroadcast(i);
		if (DEBUG) {
			Log.d(TAG, String.format("Input %s onActivate", getType()));
		}
		return true;
	}

	/**
	 * Method used to start an Input instance.
	 */
	public void onDeactivate() {
		checkNewState(State.DEACTIVATED);
		_state = State.DEACTIVATED;
		Intent i = new Intent();
		i.setAction(MoSTApplication.ACTION_INPUT);
		i.putExtra(EVENT_TYPE, EventType.DEACTIVATED);
		i.putExtra(INPUT_TYPE, getType());
		_context.sendBroadcast(i);
		if (DEBUG) {
			Log.d(TAG, String.format("Input %s onDeactivate", getType()));
		}
	}

	/**
	 * Stop.
	 */
	public void onFinalize() {
		checkNewState(State.FINALIZED);
		_state = State.FINALIZED;
		Intent i = new Intent();
		i.setAction(MoSTApplication.ACTION_INPUT);
		i.putExtra(EVENT_TYPE, EventType.FINALIZED);
		i.putExtra(INPUT_TYPE, getType());
		_context.sendBroadcast(i);
		if (DEBUG) {
			Log.d(TAG, String.format("Input %s onFinalize", getType()));
		}
	}

	/**
	 * Posts a bundle on the bus associated to this Input.
	 * 
	 * @param b
	 *            The bundle to post.
	 */
	protected void post(DataBundle b) {
		if (getState().equals(State.ACTIVATED)) {
			_bus.post(b);
		} else {
			b.release();
		}
	}
	
	/**
	 * Methods that defines if Input requires WakeLock to run.
	 */
	public abstract boolean isWakeLockNeeded();
}