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

import org.most.input.Input;
import org.most.pipeline.Pipeline;

import android.util.Log;

/**
 * DataBundle replaces Android Bundle Object, adding reference counting for
 * improved performance even when generating a few hundred objects per second,
 * by sidestepping the garbage collector. {@link Pipeline}s receiving a
 * DataBundle <b>must</b> call the {@link #release()} method when they do not
 * need anymore.
 * 
 * Please note that the DataBundle owns all its content: Pipelines and any other
 * object that receive a DataBundle can read its content, but they are forbidden
 * to modify its content in any way. In addition, they <b>must</b> call the
 * {@link #release()} method when they do not need the packet anymore. As soon
 * as {@link #release()} is called, all references to values bundled by the
 * DataBundle become invalid.
 * 
 * @author gcardone
 * 
 */
public class DataBundle {
	
	private static final String TAG = DataBundle.class.getSimpleName();

	/** The map that associates keys to values */
	private Map<String, Object> _map = null;

	/**
	 * Reference counter for this DataBundle. When it is zero, the Bundle can be
	 * returned to its {@link DataBundlePool} (see {@link #_databundlePool}.
	 */
	private int _refCount = 0;

	/** The {@link DataBundlePool} that owns this DataBundle. */
	private DataBundlePool _databundlePool = null;

	/**
	 * Instantiates a new data bundle.
	 * 
	 * @param dataBundlePoolManager
	 *            The {@link DataBundlePool} that owns the new DataBundle.
	 */
	public DataBundle(DataBundlePool dataBundlePoolManager) {
		_map = new HashMap<String, Object>();
		_refCount = 0;
		_databundlePool = dataBundlePoolManager;
	}

	/**
	 * Allocates a new byte array.
	 * 
	 * @param key
	 *            The key associated to the array.
	 * @param size
	 *            Size of the float array.
	 * @return The byte array.
	 */
	public synchronized byte[] allocateByteArray(String key, int size) {
		Object o = _map.get(key);
		boolean allocateNew = true;
		/*
		 * Allocate a new array if any of the following conditions is met: 1)
		 * there's no array allocated with the key "key" 2) there's an object
		 * associated to "key", but it is not a float[] 3) the object associated
		 * to "key" is a float[] but has the wrong size.
		 */
		allocateNew = (o == null);
		allocateNew = allocateNew || (!(o instanceof byte[]));
		allocateNew = allocateNew || (o instanceof byte[] && ((byte[]) o).length != size);
		if (allocateNew) {
			_map.put(key, new byte[size]);
		}
		return (byte[]) _map.get(key);
	}

	/**
	 * Allocates a new double array in the DataBundle. The returned array is
	 * already owned by the DataBundle. This method tries to reuse already
	 * allocated arrays, thus the returned array is not guaranteed to be
	 * zero-ed.
	 * 
	 * @param key
	 *            The key associated to the array.
	 * @param size
	 *            Size of the double array.
	 * @return The double array.
	 */
	public synchronized double[] allocateDoubleArray(String key, int size) {
		Object o = _map.get(key);
		boolean allocateNew = true;
		/*
		 * Allocate a new array if any of the following conditions is met: 1)
		 * there's no array allocated with the key "key" 2) there's an object
		 * associated to "key", but it is not a float[] 3) the object associated
		 * to "key" is a float[] but has the wrong size.
		 */
		allocateNew = (o == null);
		allocateNew = allocateNew || (!(o instanceof double[]));
		allocateNew = allocateNew || (o instanceof double[] && ((double[]) o).length != size);
		if (allocateNew) {
			_map.put(key, new double[size]);
		}
		return (double[]) _map.get(key);
	}

	/**
	 * Allocates a new float array in the DataBundle. The returned array is
	 * already owned by the DataBundle. This method tries to reuse already
	 * allocated arrays, thus the returned array is not guaranteed to be
	 * zero-ed.
	 * 
	 * @param key
	 *            The key associated to the array.
	 * @param size
	 *            Size of the float array.
	 * @return The float array.
	 */
	public synchronized float[] allocateFloatArray(String key, int size) {
		Object o = _map.get(key);
		boolean allocateNew = true;
		/*
		 * Allocate a new array if any of the following conditions is met: 1)
		 * there's no array allocated with the key "key" 2) there's an object
		 * associated to "key", but it is not a float[] 3) the object associated
		 * to "key" is a float[] but has the wrong size.
		 */
		allocateNew = (o == null);
		allocateNew = allocateNew || (!(o instanceof float[]));
		allocateNew = allocateNew || (o instanceof float[] && ((float[]) o).length != size);
		if (allocateNew) {
			_map.put(key, new float[size]);
		}
		return (float[]) _map.get(key);
	}

	/**
	 * Allocates a new short array.
	 * 
	 * @param key
	 *            The key associated to the array.
	 * @param size
	 *            Size of the float array.
	 * @return The byte array.
	 */
	public synchronized short[] allocateShortArray(String key, int size) {
		Object o = _map.get(key);
		boolean allocateNew = true;
		/*
		 * Allocate a new array if any of the following conditions is met: 1)
		 * there's no array allocated with the key "key" 2) there's an object
		 * associated to "key", but it is not a float[] 3) the object associated
		 * to "key" is a float[] but has the wrong size.
		 */
		allocateNew = (o == null);
		allocateNew = allocateNew || (!(o instanceof short[]));
		allocateNew = allocateNew || (o instanceof short[] && ((short[]) o).length != size);
		if (allocateNew) {
			_map.put(key, new short[size]);
		}
		return (short[]) _map.get(key);
	}

	/**
	 * Retrieves a boolean stored in this DataBundle. Calling this method is
	 * equivalent to call {@code getBoolean(key, true)}
	 * 
	 * @param key
	 *            The value to retrieve.
	 * @return The stored value or {@code true} if not found.
	 */
	public boolean getBoolean(String key) {
		return getBoolean(key, true);
	}

	/**
	 * Returns a boolean stored in this DataBundle, or {@code defaultValue} if
	 * not found.
	 * 
	 * @param key
	 *            The key of the value to retrieve.
	 * @param defaultValue
	 *            The defalt value to return if the key is not found.
	 * @return The stored value, or {@code defaultValue} if the value was not
	 *         found.
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		Object o = _map.get(key);
		if (o == null) {
			return defaultValue;
		}
		try {
			return (Boolean) o;
		} catch (ClassCastException e) {
			Log.e(TAG, String.format("getBoolean, found type is %s", o.getClass().getCanonicalName()));
			return defaultValue;
		}
	}

	/**
	 * Gets a byte array.
	 * 
	 * @param key
	 *            The key.
	 * @return The float array, null if not found.
	 */
	public synchronized byte[] getByteArray(String key) {
		Object o = _map.get(key);
		try {
			return (byte[]) o;
		} catch (ClassCastException e) {
			Log.e(TAG, String.format("getByteArray, found type is %s", o.getClass().getCanonicalName()));
			return null;
		}
	}

	/**
	 * Gets a double from this DataBundle. Calling this method is equivalent to
	 * call <code>getDouble(key, 0.0)</code>
	 * 
	 * @param key
	 *            The key.
	 * @return The stored float if found, otherwise 0.0.
	 */
	public synchronized double getDouble(String key) {
		return getDouble(key, 0.0);
	}

	/**
	 * Gets a double from this DataBundle.
	 * 
	 * @param key
	 *            The key.
	 * @param defaultValue
	 *            The default value to return if the key is not in this
	 *            DataBundle
	 * @return The stored double if found, otherwise the default value.
	 */
	public synchronized double getDouble(String key, double defaultValue) {
		Object o = _map.get(key);
		if (o == null) {
			return defaultValue;
		}
		try {
			return (Double) o;
		} catch (ClassCastException e) {
			Log.e(TAG, String.format("getDouble, found type is %s", o.getClass().getCanonicalName()));
			return defaultValue;
		}
	}

	/**
	 * Gets a double array.
	 * 
	 * @param key
	 *            The key.
	 * @return The double array or <code>null</code> if not found.
	 */
	public synchronized double[] getDoubleArray(String key) {
		Object o = _map.get(key);
		try {
			return (double[]) o;
		} catch (ClassCastException e) {
			Log.e(TAG, String.format("getDoubleArray, found type is %s", o.getClass().getCanonicalName()));
			return null;
		}
	}

	/**
	 * Gets a float from this DataBundle. Calling this method is equivalent to
	 * call <code>getFloat(key, 0.0f)</code>
	 * 
	 * @param key
	 *            The key.
	 * @return The stored float if found, otherwise 0.0f.
	 */
	public synchronized float getFloat(String key) {
		return getFloat(key, 0.0f);
	}

	/**
	 * Gets a float from this DataBundle.
	 * 
	 * @param key
	 *            The key.
	 * @param defaultValue
	 *            The default value to return if the key is not in this
	 *            DataBundle
	 * @return The stored float if found, otherwise the default value.
	 */
	public synchronized float getFloat(String key, float defaultValue) {
		Object o = _map.get(key);
		if (o == null) {
			return defaultValue;
		}
		try {
			return (Float) o;
		} catch (ClassCastException e) {
			Log.e(TAG, String.format("getFloat, found type is %s", o.getClass().getCanonicalName()));
			return defaultValue;
		}
	}

	/**
	 * Gets a float array.
	 * 
	 * @param key
	 *            The key.
	 * @return The float array or <code>null</code> if not found.
	 */
	public synchronized float[] getFloatArray(String key) {
		Object o = _map.get(key);
		try {
			return (float[]) o;
		} catch (ClassCastException e) {
			Log.e(TAG, String.format("getFloatArray, found type is %s", o.getClass().getCanonicalName()));
			return null;
		}
	}

	/**
	 * Gets an integer from this DataBundle. Calling this method is equivalent
	 * to call <code>getInt(key, 0)</code>
	 * 
	 * @param key
	 *            The key.
	 * @return The stored integer if found, otherwise 0.
	 */
	public synchronized int getInt(String key) {
		return getInt(key, 0);
	}

	/**
	 * Gets an integer from this DataBundle.
	 * 
	 * @param key
	 *            The key.
	 * @param defaultValue
	 *            The default value to return if the key is not in this
	 *            DataBundle
	 * @return The stored integer if found, otherwise the default value.
	 */
	public synchronized int getInt(String key, int defaultValue) {
		Object o = _map.get(key);
		if (o == null) {
			return defaultValue;
		}
		try {
			return (Integer) o;
		} catch (ClassCastException e) {
			Log.e(TAG, String.format("getInt, found type is %s", o.getClass().getCanonicalName()));
			return defaultValue;
		}
	}

	/**
	 * Gets a long.
	 * 
	 * @param key
	 *            The key.
	 * @return The long value, 0L if not found.
	 */
	public synchronized long getLong(String key) {
		return getLong(key, 0L);
	}

	/**
	 * Gets a long.
	 * 
	 * @param key
	 *            The key.
	 * @param defaultValue
	 *            the default value
	 * @return the long
	 */
	public synchronized long getLong(String key, long defaultValue) {
		Object o = _map.get(key);
		if (o == null) {
			return defaultValue;
		}
		try {
			return (Long) o;
		} catch (ClassCastException e) {
			Log.e(TAG, String.format("getLong, found type is %s", o.getClass().getCanonicalName()));
			return defaultValue;
		}
	}

	/**
	 * Returns a generic object stored in the DataBundle.
	 * 
	 * @param key
	 *            The key associated to the object.
	 * @return The stored object, or <code>null</code> if not found.
	 */
	public synchronized Object getObject(String key) {
		return _map.get(key);
	}

	/**
	 * Returns an object stored in the DataBundle, casted as requested.
	 * 
	 * @param key
	 *            The key associated to the object.
	 * @param clazz
	 *            The class to cast the object to.
	 * @return The requested object casted to clazz, or <code>null</code> if the
	 *         object is not found or the cast fails.
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T> T getObject(String key, Class<T> clazz) {
		Object o = _map.get(key);
		if (o == null) {
			return null;
		}
		try {
			return (T) o;
		} catch (ClassCastException e) {
			return null;
		}
	}

	/**
	 * Gets the reference counter value of this DataBundle.
	 * 
	 * @return The current value of the reference counter.
	 */
	public synchronized int getRefCount() {
		return _refCount;
	}

	/**
	 * Gets the short array.
	 * 
	 * @param key
	 *            The key.
	 * @return the float array
	 */
	public synchronized short[] getShortArray(String key) {
		Object o = _map.get(key);
		try {
			return (short[]) o;
		} catch (ClassCastException e) {
			return null;
		}
	}

	/**
	 * Gets a string.
	 * 
	 * @param key
	 *            The key.
	 * @return The string, <code>null</code> if not found.
	 */
	public synchronized String getString(String key) {
		Object o = _map.get(key);
		if (o == null) {
			return null;
		}
		try {
			return (String) o;
		} catch (ClassCastException e) {
			Log.e(TAG, String.format("getString, found type is %s", o.getClass().getCanonicalName()));
			return null;
		}
	}

	/**
	 * Puts a boolean in this DataBundle.
	 * 
	 * @param key
	 *            The key associated to this value.
	 * @param value
	 *            The value to store.
	 */
	public void putBoolean(String key, boolean value) {
		_map.put(key, value);
	}

	// byte[]
	/**
	 * Puts byte array. The array is not cloned.
	 * 
	 * @param key
	 *            The key.
	 * @param value
	 *            The value.
	 */
	public synchronized void putByteArray(String key, byte[] value) {
		_map.put(key, value);
	}

	/**
	 * Puts a double in this DataBundle.
	 * 
	 * @param key
	 *            The key.
	 * @param value
	 *            The value to store.
	 */
	public synchronized void putDouble(String key, double value) {
		_map.put(key, value);
	}

	/**
	 * Puts a double array. The array is not cloned, hence the caller
	 * <em>loses the ownership of the array</em>. {@link Input}s are encouraged
	 * to prefer {@link #allocateDoubleArray(String, int)} to this method.
	 * 
	 * @param key
	 *            The key.
	 * @param value
	 *            The value.
	 */
	public synchronized void putDoubleArray(String key, double[] value) {
		_map.put(key, value);
	}

	/**
	 * Puts a float in this DataBundle.
	 * 
	 * @param key
	 *            The key.
	 * @param value
	 *            The value to store.
	 */
	public synchronized void putFloat(String key, float value) {
		_map.put(key, value);
	}

	/**
	 * Puts a float array. The array is not cloned, hence the caller
	 * <em>loses the ownership of the array</em>. {@link Input}s are encouraged
	 * to prefer {@link #allocateFloatArray(String, int)} to this method.
	 * 
	 * @param key
	 *            The key.
	 * @param value
	 *            The value.
	 */
	public synchronized void putFloatArray(String key, float[] value) {
		_map.put(key, value);
	}

	/**
	 * Puts an integer in this DataBundle. Existing values are overwritten.
	 * 
	 * @param key
	 *            The key.
	 * @param value
	 *            The value.
	 */
	public synchronized void putInt(String key, int value) {
		_map.put(key, value);
	}

	// long
	/**
	 * Puts a long.
	 * 
	 * @param key
	 *            The key.
	 * @param value
	 *            the value
	 */
	public synchronized void putLong(String key, long value) {
		_map.put(key, value);
	}

	/**
	 * Puts a generic object in the DataBundle
	 * 
	 * @param key
	 *            The key associated to the object.
	 * @param o
	 *            The object to store.
	 */
	public synchronized void putObject(String key, Object o) {
		_map.put(key, o);
	}

	// short[]
	/**
	 * Put short array.
	 * 
	 * @param key
	 *            The key.
	 * @param value
	 *            the value
	 */
	public synchronized void putShortArray(String key, short[] value) {
		_map.put(key, value);
	}

	/**
	 * Puts a string in this DataBundle. Existing values are overwritten.
	 * 
	 * @param key
	 *            The key.
	 * @param value
	 *            The value to store.
	 */
	public synchronized void putString(String key, String value) {
		_map.put(key, value);
	}

	/**
	 * Releases the DataBundle. This method <b>must<b/> called by every Pipeline
	 * and any other object that receives a DataBundle.
	 */
	public synchronized void release() {
		if (_refCount <= 1) {
			_refCount = 0;
			_databundlePool.returnBundle(this);
		} else {
			_refCount--;
		}
	}

	/**
	 * Removes a field.
	 * 
	 * @param key
	 *            The key of the object to remove.
	 * @return The object that was removed, or null if it was not found.
	 */
	public synchronized Object remove(String key) {
		return _map.remove(key);
	}

	/**
	 * Sets the reference counter of this DataBundle. {@link Pipeline} usually
	 * do not need to call this method.
	 * 
	 * @param value
	 *            The new value of the reference counter.
	 */
	public synchronized void setRefCount(int value) {
		_refCount = value;
	}
}
