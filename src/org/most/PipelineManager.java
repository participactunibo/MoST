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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.most.input.Input;
import org.most.input.InputBus;
import org.most.pipeline.Pipeline;
import org.most.pipeline.PipelineAccelerometer;
import org.most.pipeline.PipelineAccelerometerClassifier;
import org.most.pipeline.PipelineActivityRecognitionCompare;
import org.most.pipeline.PipelineAppOnScreen;
import org.most.pipeline.PipelineAppsNetTraffic;
import org.most.pipeline.PipelineAverageAccelerometer;
import org.most.pipeline.PipelineBattery;
import org.most.pipeline.PipelineBluetooth;
import org.most.pipeline.PipelineCell;
import org.most.pipeline.PipelineConnectionType;
import org.most.pipeline.PipelineDeviceNetTraffic;
import org.most.pipeline.PipelineGoogleActivityRecognition;
import org.most.pipeline.PipelineGyroscope;
import org.most.pipeline.PipelineInstalledApps;
import org.most.pipeline.PipelineLight;
import org.most.pipeline.PipelineLocation;
import org.most.pipeline.PipelineMagneticField;
import org.most.pipeline.PipelinePhoneCallDuration;
import org.most.pipeline.PipelinePhoneCallEvent;
import org.most.pipeline.PipelineRawAudio;
import org.most.pipeline.PipelineSystemStats;
import org.most.pipeline.PipelineWifiScan;
import org.most.pipeline.TestPipeline;

import android.util.Log;

/**
 * Class that manages processing {@link Pipeline}s.
 * 
 */
public class PipelineManager {

	private static final String TAG = PipelineManager.class.getSimpleName();
	private final MoSTApplication _context;
	private final Map<Pipeline.Type, Pipeline> _pipelines;

	public PipelineManager(MoSTApplication context) {
		_context = context;
		_pipelines = new HashMap<Pipeline.Type, Pipeline>();
	}

	/**
	 * Gets a pipeline, if exists. If it doesn't, it creates one and inits it
	 * calling the {@link Pipeline#onInit()} method.
	 * 
	 * @param pipelineType
	 *            Type of the pipeline to get.
	 * @return The pipeline.
	 */
	public Pipeline getPipeline(Pipeline.Type pipelineType) {
		Pipeline result = _pipelines.get(pipelineType);
		if (result != null) {
			return result;
		}
		switch (pipelineType) {
		case AVERAGE_ACCELEROMETER:
			result = new PipelineAverageAccelerometer(_context);
			break;
		case ACCELEROMETER:
			result = new PipelineAccelerometer(_context, true);
			break;
		case RAW_AUDIO:
			result = new PipelineRawAudio(_context);
			break;
		case APP_ON_SCREEN:
			result = new PipelineAppOnScreen(_context);
			break;
		case BATTERY:
			result = new PipelineBattery(_context);
			break;
		case BLUETOOTH:
			result = new PipelineBluetooth(_context);
			break;
		case CELL:
			result = new PipelineCell(_context);
			break;
		case GYROSCOPE:
			result = new PipelineGyroscope(_context);
			break;
		case INSTALLED_APPS:
			result = new PipelineInstalledApps(_context);
			break;
		case LIGHT:
			result = new PipelineLight(_context);
			break;
		case LOCATION:
			result = new PipelineLocation(_context);
			break;
		case MAGNETIC_FIELD:
			result = new PipelineMagneticField(_context);
			break;
		case PHONE_CALL_DURATION:
			result = new PipelinePhoneCallDuration(_context);
			break;
		case PHONE_CALL_EVENT:
			result = new PipelinePhoneCallEvent(_context);
			break;
		case TEST:
			result = new TestPipeline(_context);
			break;
		case ACCELEROMETER_CLASSIFIER:
			result = new PipelineAccelerometerClassifier(_context);
			break;
		case SYSTEM_STATS:
			result = new PipelineSystemStats(_context);
			break;
		case WIFI_SCAN:
			result = new PipelineWifiScan(_context);
			break;
		case DEVICE_NET_TRAFFIC:
			result = new PipelineDeviceNetTraffic(_context);
			break;
		case APPS_NET_TRAFFIC:
			result = new PipelineAppsNetTraffic(_context);
			break;
		case CONNECTION_TYPE:
			result = new PipelineConnectionType(_context);
			break;
		case GOOGLE_ACTIVITY_RECOGNITION:
			result = new PipelineGoogleActivityRecognition(_context);
			break;
		case ACTIVITY_RECOGNITION_COMPARE:
			result = new PipelineActivityRecognitionCompare(_context);
			break;
		default:
			throw new IllegalArgumentException();
		}
		_pipelines.put(pipelineType, result);
		initPipeline(result);
		return result;
	}

	/**
	 * Checks if a given pipeline was already loaded.
	 * 
	 * @param pipelineType
	 *            The type of the pipeline to look for.
	 * @return True if the pipeline has been instantiated. However, there are no
	 *         guarantees about its state.
	 */
	public boolean isPipelineAvailable(Pipeline.Type pipelineType) {
		return _pipelines.containsKey(pipelineType);
	}

	/**
	 * Initializes a pipeline by calling the {@link Pipeline#onInit()} method.
	 * 
	 * @param pipeline
	 *            The pipeline to init.
	 */
	protected void initPipeline(Pipeline pipeline) {
		if (pipeline.getState() != Pipeline.State.INVALID) {
			Log.i(TAG, String.format(
					"Can not init pipeline %s: current state: %s", pipeline,
					pipeline.getState()));
			return;
		}
		pipeline.onInit();
	}

	/**
	 * Activates a pipeline by calling the {@link Pipeline#onActivate()} method
	 * and subscribing it to the bus of its inputs.
	 * 
	 * @param pipeline
	 *            The pipeline to init.
	 */
	protected void activatePipeline(Pipeline pipeline) {
		if (pipeline.getState() != Pipeline.State.INITED
				&& pipeline.getState() != Pipeline.State.DEACTIVATED) {
			Log.i(TAG, String.format(
					"Can not activate pipeline %s: current state: %s",
					pipeline, pipeline.getState()));
			return;
		}
		InputBus inputBus = _context.getInputBus();
		for (Input.Type inputType : pipeline.getInputs()) {
			inputBus.addListener(inputType, pipeline.getListener());
		}
		pipeline.onActivate();
	}

	/**
	 * Deactivates a pipeline by calling the {@link Pipeline#onDeactivate()}
	 * method.
	 * 
	 * @param pipeline
	 *            The pipeline to deactivate.
	 */
	protected void deactivatePipeline(Pipeline pipeline) {
		if (pipeline.getState() != Pipeline.State.ACTIVATED) {
			/*
			 * Do not deactivate pipelines that are not active.
			 */
			Log.i(TAG, String.format(
					"Can not deactivate pipeline %s: current state: %s",
					pipeline, pipeline.getState()));
			return;
		}
		InputBus inputBus = _context.getInputBus();
		for (Input.Type inputType : pipeline.getInputs()) {
			inputBus.removeListener(inputType, pipeline.getListener());
		}
		pipeline.onDeactivate();
	}

	/**
	 * Finalizes (destroys) a pipeline.
	 * 
	 * @param pipeline
	 *            The pipeline to finalize.
	 */
	protected void finalizePipeline(Pipeline pipeline) {
		if (pipeline.getState() != Pipeline.State.INITED
				&& pipeline.getState() != Pipeline.State.DEACTIVATED) {
			Log.i(TAG, String.format(
					"Can not finalize pipeline %s: current state: %s",
					pipeline, pipeline.getState()));
			return;
		}
		pipeline.onFinalize();
		_pipelines.remove(pipeline.getType());
	}
	
	public List<Pipeline.Type> getPipelineByState(Pipeline.State state) {
		List<Pipeline.Type> result = new LinkedList<Pipeline.Type>();
		for (Map.Entry<Pipeline.Type, Pipeline> entry : _pipelines.entrySet()) {
			if(state == entry.getValue().getState()){
				result.add(entry.getKey());
			}
		}
		return result;
	}
}
