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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.most.DataBundle;

/**
 * Bus that dispatches data from {@link Pipeline} to users.
 * 
 */
public class PipelineBus {

	protected Map<Pipeline.Type, SinglePipelineBus> _buses;

	public PipelineBus() {
		_buses = new HashMap<Pipeline.Type, SinglePipelineBus>();
		for (Pipeline.Type type : Pipeline.Type.values()) {
			_buses.put(type, new SinglePipelineBus());
		}
	}

	public void addListener(Pipeline.Type inputType, Listener listener) {
		_buses.get(inputType).addListener(listener);
	}

	public void removeListener(Pipeline.Type inputType, Listener listener) {
		_buses.get(inputType).removeListener(listener);
	}

	public SinglePipelineBus getBus(Pipeline.Type inputType) {
		return _buses.get(inputType);
	}

	public interface Listener {

		public void onData(DataBundle b);
	}

	public static class SinglePipelineBus {

		private Collection<Listener> _listeners;
		private int _listenerCount;

		public SinglePipelineBus() {
			_listeners = new ArrayList<PipelineBus.Listener>();
		}

		public synchronized void addListener(Listener listener) {
			if (!_listeners.contains(listener)) {
				_listeners.add(listener);
				_listenerCount++;
			}
		}

		public synchronized void removeListener(Listener listener) {
			if (_listeners.contains(listener)) {
				_listeners.add(listener);
				_listenerCount--;
			}
		}

		public synchronized void post(DataBundle b) {
			b.setRefCount(_listenerCount);
			for (Listener listener : _listeners) {
				listener.onData(b);
			}
		}
	}
}
