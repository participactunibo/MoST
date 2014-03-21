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

import java.util.HashMap;
import java.util.Map;

import org.most.input.AppOnScreenInput;
import org.most.input.BatteryInput;
import org.most.input.BluetoothScanInput;
import org.most.input.CellInput;
import org.most.input.ContinuousLocationInput;
import org.most.input.FusionLocationInput;
import org.most.input.GoogleActivityRecognitionInput;
import org.most.input.GyroscopeInput;
import org.most.input.Input;
import org.most.input.Input.State;
import org.most.input.InputAccelerometer;
import org.most.input.InputAudio;
import org.most.input.InstalledAppsInput;
import org.most.input.LightInput;
import org.most.input.MagneticFieldInput;
import org.most.input.NetTrafficInput;
import org.most.input.PeriodicConnectionTypeInput;
import org.most.input.PeriodicFusionLocationInput;
import org.most.input.PeriodicGoogleActivityRecognitionInput;
import org.most.input.PeriodicLocationInput;
import org.most.input.PhoneCallInput;
import org.most.input.ProximityInput;
import org.most.input.SMSTimestampInput;
import org.most.input.StatisticsInput;
import org.most.input.WifiScanInput;

import android.util.Log;

/**
 * This class manages inputs: it instantiates them, manages activation and
 * deactivation.
 * 
 */
public class InputManager {
	private static final String TAG = InputManager.class.getSimpleName();
	private final Map<Input.Type, Input> _inputs;
	private final MoSTApplication _context;
	private Map<Input.Type, IPowerPolicy> _powerPolicies;

	public InputManager(MoSTApplication context) {
		_context = context;
		_inputs = new HashMap<Input.Type, Input>();
		_powerPolicies = new HashMap<Input.Type, IPowerPolicy>();
	}

	/**
	 * Gets a input, if exists. If it doesn't, it creates one and inits it
	 * calling the {@link Input#onInit()} method.
	 * 
	 * @param inputType
	 *            Type of the input to get.
	 * @return The input.
	 */
	public Input getInput(Input.Type inputType) {
		Input result = _inputs.get(inputType);
		if (result != null) {
			return result;
		}
		switch (inputType) {
		case ACCELEROMETER:
			result = new InputAccelerometer(_context);
			break;
		case AUDIO:
			result = new InputAudio(_context);
			break;
		case APPONSCREEN:
			result = new AppOnScreenInput(_context);
			break;
		case WIFISCAN:
			result = new WifiScanInput(_context);
			break;
		case BLUETOOTHSCAN:
			result = new BluetoothScanInput(_context);
			break;
		case CONTINUOUS_LOCATION:
			result = new ContinuousLocationInput(_context);
			break;
		case SMSTIMESTAMP:
			result = new SMSTimestampInput(_context);
			break;
		case LIGHT:
			result = new LightInput(_context);
			break;
		case PROXIMITY:
			result = new ProximityInput(_context);
			break;
		case PHONECALL:
			result = new PhoneCallInput(_context);
			break;
		case BATTERY:
			result = new BatteryInput(_context);
			break;
		case CELL:
			result = new CellInput(_context);
			break;
		case INSTALLED_APPS:
			result = new InstalledAppsInput(_context);
			break;
		case MAGNETICFIELD:
			result = new MagneticFieldInput(_context);
			break;
		case GYROSCOPE:
			result = new GyroscopeInput(_context);
			break;
		case SYSTEM_STATS:
			result = new StatisticsInput(_context);
			break;
		case PERIODIC_LOCATION:
			result = new PeriodicLocationInput(_context);
			break;
		case NET_TRAFFIC:
			result = new NetTrafficInput(_context);
			break;
		case CONTINUOUS_FUSION_LOCATION:
			result = new FusionLocationInput(_context);
			break;
		case PERIODIC_FUSION_LOCATION:
			result = new PeriodicFusionLocationInput(_context);
			break;
		case PERIODIC_CONNECTION_TYPE:
			result = new PeriodicConnectionTypeInput(_context);
			break;
		case GOOGLE_ACTIVITY_RECOGNITION:
			result = new GoogleActivityRecognitionInput(_context);
			break;
		case PERIODIC_GOOGLE_ACTIVITY_RECOGNITION:
			result = new PeriodicGoogleActivityRecognitionInput(_context);
			break;
		default:
			throw new IllegalArgumentException();
		}
		_inputs.put(inputType, result);
		initInput(result);
		return result;
	}

	/**
	 * Checks if a given input type has been already instantiated.
	 * 
	 * @param inputType
	 *            The type of input to check.
	 * @return <code>true</code> if the input has been instantiated,
	 *         <code>false</code> otherwise.
	 */
	public boolean isInputAvailable(Input.Type inputType) {
		return _inputs.containsKey(inputType);
	}

	/**
	 * Initializes an input by calling the {@link Input#onInit()} method.
	 * 
	 * @param input
	 *            The input to init.
	 */
	protected void initInput(Input input) {
		if (input.getState() != Input.State.INVALID) {
			Log.i(TAG, String.format("Can not init input %s: current state: %s", input, input.getState()));
			return;
		}
		input.onInit();
	}

	/**
	 * Activates a input by calling the {@link Input#onActivate()} method and
	 * subscribing it to the bus of its inputs.
	 * 
	 * @param input
	 *            The input to init.
	 * @return true if the initialization was successful.
	 */
	protected boolean activateInput(Input input) {
		if (input.getState() != Input.State.INITED && input.getState() != Input.State.DEACTIVATED) {
			Log.i(TAG, String.format("Can not activate input %s: current state: %s", input, input.getState()));
			return false;
		}
		return input.onActivate();
	}

	/**
	 * Deactivates a input by calling the {@link Input#onDeactivate()} method.
	 * 
	 * @param input
	 *            The input to deactivate.
	 */
	protected void deactivateInput(Input input) {
		if (input.getState() != Input.State.ACTIVATED) {
			/*
			 * Do not deactivate inputs that are not active.
			 */
			Log.i(TAG, String.format("Can not deactivate input %s: current state: %s", input, input.getState()));
			return;
		}
		input.onDeactivate();
	}

	/**
	 * Finalizes (destroys) a input.
	 * 
	 * @param input
	 *            The input to finalize.
	 */
	protected void finalizeInput(Input input) {
		if (input.getState() != Input.State.INITED && input.getState() != Input.State.DEACTIVATED) {
			Log.i(TAG, String.format("Can not finalize input %s: current state: %s", input, input.getState()));
			return;
		}
		input.onFinalize();
		_inputs.remove(input.getType());
	}
	
	public void deactivateInput(Input.Type inputType) {
		if (isInputAvailable(inputType)) {
			Input input = getInput(inputType);
			deactivateInput(input);
		} else {
			Log.w(TAG, String.format("Can not deactivate unavailable input %s", inputType));
		}
	}
	
	public void activateInput(Input.Type inputType) {
		Input input = getInput(inputType);
		if (input == null) {
			Log.w(TAG, String.format("Unable to find input %s", inputType));
			return;
		}
		if (input.getState() == State.ACTIVATED) {
			Log.i(TAG, String.format("Input %s already active", inputType));
			return;
		}
		activateInput(input);
	}
	
	public IPowerPolicy getPowerPolicyForInput(Input.Type type){
		return _powerPolicies.get(type);
	}
	
	public void setPowerPolicyForInput(Input.Type type, IPowerPolicy policy){
		_powerPolicies.put(type, policy);
	}
	
	public void removePowerPolicyForInput(Input.Type type){
		if(_powerPolicies.containsKey(type))
			_powerPolicies.remove(type);
	}
}
