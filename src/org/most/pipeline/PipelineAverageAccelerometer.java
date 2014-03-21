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

import java.util.HashSet;
import java.util.Set;

import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.input.Input;
import org.most.input.InputAccelerometer;
import org.most.utils.LimitedLinkedList;

import android.util.Log;

public class PipelineAverageAccelerometer extends Pipeline {

	private LimitedLinkedList<Float> _x;
	private LimitedLinkedList<Float> _y;
	private LimitedLinkedList<Float> _z;
	private static final int WINDOW_SIZE = 200;
	private static final int WINDOW_SLIDE = 100;
	private int counter;
	
	public PipelineAverageAccelerometer(MoSTApplication context) {
		super(context);
	}
	
	@Override
	public boolean onActivate() {
		_x = new LimitedLinkedList<Float>(WINDOW_SIZE);
		_y = new LimitedLinkedList<Float>(WINDOW_SIZE);
		_z = new LimitedLinkedList<Float>(WINDOW_SIZE);
		counter = WINDOW_SLIDE;
		return super.onActivate();
	}
	
	@Override
	public void onDeactivate() {
		super.onDeactivate();
		_x.clear();
		_y.clear();
		_z.clear();
	}

	public void onData(DataBundle b) {
		float[] data = b.getFloatArray(InputAccelerometer.KEY_ACCELERATIONS);
		_x.add(data[0]);
		_y.add(data[1]);
		_z.add(data[2]);
		counter--;
		if (counter == 0) {
			counter = WINDOW_SLIDE;
			int listSize = _x.size();
			float avgx = 0;
			float avgy = 0;
			float avgz = 0;
			for (int i = 0; i < listSize; i++) {
				avgx += _x.get(i);
				avgy += _y.get(i);
				avgz += _z.get(i);
			}
			avgx = avgx / listSize;
			avgy = avgy / listSize;
			avgz = avgz / listSize;
			Log.d("AvgAccel", String.format("x = %f ; y = %f; z = %f", avgx, avgy, avgz));
		}
		b.release();
	}
	
	@Override
	public Pipeline.Type getType() {
		return Type.AVERAGE_ACCELEROMETER;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> usedInputs = new HashSet<Input.Type>();
		usedInputs.add(Input.Type.ACCELEROMETER);
		return usedInputs;
	}

}
