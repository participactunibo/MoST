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
import org.most.input.InputBus;
import org.most.input.InputBus.SingleInputBus;
import org.most.pipeline.Pipeline;

import android.util.Log;

/**
 * One stop shop for activating and deactivating {@link Pipeline}s. It takes
 * care of instantiating them, wiring them to Inputs, starting inputs if needed
 * and shutting them down when no Pipeline are attached to them.
 * 
 * @author gcardone
 * 
 */
public class Controller {

	private final String TAG = Controller.class.getSimpleName();

	private final MoSTApplication _context;
	private final InputBus _inputBus;
	private final PipelineManager _pipelineManager;

	public Controller(MoSTApplication context) {
		_context = context;
		_inputBus = _context.getInputBus();
		_pipelineManager = _context.getPipelineManager();
	}

	/**
	 * Activates a {@link Pipeline}. All the {@link Input}s needed by the
	 * pipeline will be automatically instantiated and inited. If for any reason
	 * an Input can not be inited (e.g., setting up a pipeline that needs access
	 * to the microphone while a call is in place), then its instatiation will
	 * be deferred until it is available again.
	 * 
	 * @param pipelineType
	 *            Pipeline to activate.
	 * @return <code>true</code> if the Pipeline was already available or was
	 *         successfully loaded and inited, <code>false</code> otherwise.
	 */
	public boolean activatePipeline(Pipeline.Type pipelineType) {
		Pipeline pipeline = _pipelineManager.getPipeline(pipelineType);
		if (pipeline == null) {
			Log.w(TAG, String.format("Unable to find pipeline %s", pipelineType));
			return false;
		}
		if (pipeline.getState() == Pipeline.State.ACTIVATED) {
			Log.i(TAG, String.format("Pipeline %s is already active", pipelineType));
			return true;
		}
		for (Input.Type inputType : pipeline.getInputs()) {
			_context.getInputsArbiter().setSensingVote(inputType, true);
		}
		_pipelineManager.activatePipeline(pipeline);
		return true;
	}

	/**
	 * Deactivates a {@link Pipeline}. All {@link Input}s that it uses are shut
	 * down, too, unless they are being currently used by another Pipeline.
	 * 
	 * @param pipelineType
	 *            The pipeline to deactivate.
	 * @return Always returns <code>true</code>.
	 */
	public boolean deactivatePipeline(Pipeline.Type pipelineType) {
		if (_pipelineManager.isPipelineAvailable(pipelineType)) {
			Pipeline pipeline = _pipelineManager.getPipeline(pipelineType);
			_pipelineManager.deactivatePipeline(pipeline);
			/*
			 * Deactivate unused inputs.
			 */
			for (Input.Type inputType : pipeline.getInputs()) {
				SingleInputBus singleInputBus = _inputBus.getBus(inputType);
				if (singleInputBus.getListenerCount() == 0) {
					_context.getInputsArbiter().setSensingVote(inputType, false);
				}
			}
		}
		return true;
	}
}
