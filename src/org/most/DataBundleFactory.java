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

import org.apache.commons.pool.BasePoolableObjectFactory;

/**
 * A factory for creating DataBundle objects.
 * 
 */
public class DataBundleFactory extends BasePoolableObjectFactory<DataBundle> {

	/** The _data bundle pool. */
	private DataBundlePool _dataBundlePool = null;

	/**
	 * Instantiates a new data bundle factory.
	 * 
	 * @param dataBundlePool
	 *            the data bundle pool
	 */
	public DataBundleFactory(DataBundlePool dataBundlePool) {
		_dataBundlePool = dataBundlePool;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.pool.BasePoolableObjectFactory#makeObject()
	 */
	@Override
	public DataBundle makeObject() throws Exception {
		return new DataBundle(_dataBundlePool);
	}

}
