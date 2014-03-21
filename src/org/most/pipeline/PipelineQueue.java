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

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.most.DataBundle;
import org.most.input.InputBus.Listener;

import android.util.Log;

/**
 * An optionally bounded buffering queue associated to a {@link Pipeline}. It
 * receives data with the {@link #onData(DataBundle)} method and forwards by
 * calling {@link Pipeline#onData(DataBundle)}. The {@link #onData(DataBundle)}
 * method never blocks. If the queue is bounded and full, incoming
 * {@link DataBundle} will be dropped.
 * 
 * @author acirri
 * @author gcardone
 * 
 */
public class PipelineQueue implements Runnable, Listener {

	private static final String TAG = PipelineQueue.class.getSimpleName();

	/**
	 * Buffer queue.
	 */
	private LinkedBlockingQueue<DataBundle> _queue;
	/**
	 * Pipeline that owns this instance.
	 */
	private Pipeline _pipeline;
	private AtomicBoolean _running;

	/**
	 * Constructs a new queue.
	 * 
	 * @param p
	 *            Pipeline associated to this PipelineQueue.
	 * @param capacity
	 *            Maximum capacity.
	 */
	public PipelineQueue(Pipeline p, int capacity) {
		super();
		_queue = new LinkedBlockingQueue<DataBundle>(capacity);
		_pipeline = p;
		_running = new AtomicBoolean();
	}

	/**
	 * Constructs a new queue.
	 * 
	 * @param p
	 *            Pipeline associated to this PipelineQueue.
	 */
	public PipelineQueue(Pipeline p) {
		super();
		_queue = new LinkedBlockingQueue<DataBundle>();
		_pipeline = p;
		_running = new AtomicBoolean();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		_running.set(true);
		while (_running.get()) {
			DataBundle data;
			try {
				data = _queue.take();
				_pipeline.onData(data);
			} catch (InterruptedException e) {
				_running.set(false);
			}
		}
		Log.d(TAG, "PipelineQueue " + _pipeline.getType() + " terminating");
		if (!_queue.isEmpty()) {
			for (Iterator<DataBundle> iterator = _queue.iterator(); iterator.hasNext();) {
				DataBundle d = iterator.next();
				d.release();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.most.input.InputBus.Listener#isActive()
	 */
	public boolean isActive() {
		return _pipeline.isActive();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.most.input.InputBus.Listener#onData(org.most.DataBundle)
	 */
	public void onData(DataBundle b) {
		if (isActive()) {
			if (_queue.offer(b) == false) {
				Log.w(TAG, "Dropped DataBundle. Pipeline " + _pipeline.getType()
						+ " is not able to process data in real time.");
				b.release();
			}
		} else {
			Log.e(TAG, "PipelineQueue " + _pipeline.getType() + ": received DataBundle for a non-active Pipeline");
			b.release();
		}
	}

}
