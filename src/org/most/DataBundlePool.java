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

import org.apache.commons.pool.impl.StackObjectPool;

import android.util.Log;

/**
 * This class represents a pool of {@link DataBundle}, that can be reused to
 * minimize the memory and CPU cost of the application.
 * 
 */
public class DataBundlePool {

	/** The Constant DEBUG. */
	private final static boolean DEBUG = false;

	/** The Constant TAG. */
	private final static String TAG = DataBundlePool.class.getCanonicalName();

	/** The _pool. */
	private StackObjectPool<DataBundle> _pool = null;

	/**
	 * Instantiates a new DataBundlePool.
	 */
	public DataBundlePool() {
		_pool = new StackObjectPool<DataBundle>(new DataBundleFactory(this));
		// this.setWhenExhaustedAction(WHEN_EXHAUSTED_GROW);
	}

	/**
	 * Gets a new {@link DataBundle} from the pool.
	 * 
	 * @return a new {@link DataBundle}
	 */
	public DataBundle borrowBundle() {
		if (DEBUG)
			Log.d(TAG, "Borrowed DataBundle, active: "
					+ (_pool.getNumActive() + 1));
		try {
			return _pool.borrowObject();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a new {@link DataBundle} <em>to</em> to pool.
	 * 
	 * @param b
	 *            The DataBundle to return to the pool.
	 */
	public void returnBundle(DataBundle b) {

		try {
			b.setRefCount(0);
			_pool.returnObject(b);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (DEBUG)
			Log.d(TAG, "Returned DataBundle, active: " + _pool.getNumActive());
	}

}
