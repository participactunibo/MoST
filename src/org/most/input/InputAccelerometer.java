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
import org.most.MoSTApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Class that wraps an accelerometer. When posting on a bus, it sends a
 * {@link DataBundle} containing (key, type, description)
 * 
 * <ul>
 * <li>{@link Input#KEY_TYPE} (int): type of the sensor, convert it to a
 * {@link Input#Type} using {@link Input#Type.fromInt()}.</li>
 * <li> {@link Input#KEY_TIMESTAMP} (long): the timestamp of the sensed
 * acceleration, in nanoseconds.</li>
 * <li> {@link InputAccelerometer#KEY_ACCELERATIONS} (float[3]): array of the
 * accelerations over X, Y and Z as floats.
 * </ul>
 * 
 * This input supports the accelerometer rate to be configured. The
 * {@link SharedPreferences} name to use is {@link MoSTApplication#PREF_INPUT},
 * the key value to use is {@link #PREF_KEY_ACCELEROMETER_RATE} . Its default
 * value is {@link #PREF_DEFAULT_ACCELEROMETER_RATE}. Valid values are those
 * defined by {@link SensorManager}:
 * <ul>
 * <li>{@link SensorManager#SENSOR_DELAY_UI};</li>
 * <li>{@link SensorManager#SENSOR_DELAY_NORMAL};</li>
 * <li>{@link SensorManager#SENSOR_DELAY_GAME};</li>
 * <li>{@link SensorManager#SENSOR_DELAY_FASTEST}.</li>
 * </ul>
 * 
 * For example:
 * 
 * <pre>
 * {
 * 	Editor editor = context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).edit();
 * 	editor.putInt(PREF_KEY_ACCELEROMETER_RATE, SensorManager.SENSOR_DELAY_GAME);
 * 	editor.apply();
 * }
 * </pre>
 * 
 * 
 * @author gcardone
 * @author acirri
 */
public class InputAccelerometer extends Input implements SensorEventListener {
	/** The Constant DEBUG. */
	private final static boolean DEBUG = false;

	/** The Constant TAG. */
	private final static String TAG = InputAccelerometer.class.getSimpleName();

	/** The _sensor manager. */
	private SensorManager _sensorManager = null;

	/** The _sensor. */
	private Sensor _sensor = null;

	/** The _sensor rate. */
	private int _sensorRate = 0;

	public final static String KEY_ACCELERATIONS = "InputAccelerometer.Accelerations";

	public final static String PREF_KEY_ACCELEROMETER_RATE = "InputAccelerometer.Rate";
	public final static int PREF_DEFAULT_ACCELEROMETER_RATE = SensorManager.SENSOR_DELAY_FASTEST;

	/**
	 * Return a new instance of AccelerometerInput.
	 * 
	 * @param context
	 *            MoSTApplication context.
	 */
	public InputAccelerometer(MoSTApplication context) {
		super(context);
		_sensorRate = getContext().getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getInt(
				PREF_KEY_ACCELEROMETER_RATE, PREF_DEFAULT_ACCELEROMETER_RATE);
	}

	@Override
	public void onInit() {
		checkNewState(State.INITED);

		if (_sensorManager == null) {
			_sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		}
		if (_sensor == null) {
			_sensor = _sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}

		if (DEBUG)
			Log.d(TAG, "onInit()");

		super.onInit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unibo.mobilesensingframework.input.IInput#start()
	 */
	@Override
	public boolean onActivate() {
		checkNewState(State.ACTIVATED);
		if (DEBUG)
			Log.d(TAG, "onActivate()");
		_sensorRate = getContext().getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getInt(
				PREF_KEY_ACCELEROMETER_RATE, PREF_DEFAULT_ACCELEROMETER_RATE);
		boolean registrationSuccessful = _sensorManager.registerListener(this, _sensor, _sensorRate);
		if (registrationSuccessful) {
			return super.onActivate();
		} else {
			return false;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unibo.mobilesensingframework.input.Input#pause()
	 */
	public void onDeactivate() {
		checkNewState(State.DEACTIVATED);
		_sensorManager.unregisterListener(this, _sensor);

		if (DEBUG)
			Log.d(TAG, "onDeactivate()");
		super.onDeactivate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unibo.mobilesensingframework.input.IInput#stop()
	 */
	@Override
	public void onFinalize() {
		checkNewState(State.FINALIZED);
		if (_sensorManager != null) {
			_sensorManager = null;
		}
		if (_sensor != null) {
			_sensor = null;
		}

		if (DEBUG)
			Log.d(TAG, "onFinalize()");

		super.onFinalize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.hardware.SensorEventListener#onAccuracyChanged(android.hardware
	 * .Sensor, int)
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.hardware.SensorEventListener#onSensorChanged(android.hardware
	 * .SensorEvent)
	 */
	public void onSensorChanged(SensorEvent event) {
		if (DEBUG) {
			Log.v(TAG, "Received accelerometer data");
		}

		if (!getState().equals(Input.State.ACTIVATED)) {
			return;
		}

		DataBundle b = _bundlePool.borrowBundle();
		b.allocateFloatArray(KEY_ACCELERATIONS, 3);
		float[] data = b.getFloatArray(KEY_ACCELERATIONS);
		data[0] = event.values[0];
		data[1] = event.values[1];
		data[2] = event.values[2];

		b.putLong(Input.KEY_TIMESTAMP, event.timestamp);
		b.putInt(Input.KEY_TYPE, Input.Type.ACCELEROMETER.toInt());

		post(b);
	}

	/**
	 * Gets the sensor rate.
	 * 
	 * @return the _sensor rate
	 */
	public int getSensorRate() {
		return _sensorRate;
	}

	/**
	 * Sets the sensor rate.
	 * 
	 * @param sensorRate
	 *            the new _sensor rate
	 */
	public void setSensorRate(int sensorRate) {
		this._sensorRate = sensorRate;
		_sensorManager.unregisterListener(this, _sensor);
		_sensorManager.registerListener(this, _sensor, _sensorRate);
	}

	@Override
	public Type getType() {
		return Input.Type.ACCELEROMETER;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof InputAccelerometer)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return getType().hashCode();
	}
	
	@Override
	public boolean isWakeLockNeeded() {
		return true;
	}
}
