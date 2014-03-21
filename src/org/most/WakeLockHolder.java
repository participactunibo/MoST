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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * This class is the application-wide wrapper that acquires and releases the
 * {@link WakeLock}.
 * 
 */
public class WakeLockHolder {
	
	private static final Logger logger = LoggerFactory.getLogger(WakeLockHolder.class);
	
	private final static String TAG = WakeLockHolder.class.getSimpleName();

	/** The Constant WAKE_LOCK_NAME. */
	private final static String WAKE_LOCK_NAME = "MOST_WAKELOCK";

	/** The _context. */
	private Context _context = null;

	/** The _wake lock. */
	private WakeLock _wakeLock = null;

	private List<WakeLockListener> _listeners;
	
	private AtomicInteger _count;

	public WakeLockHolder(Context context) {
		_context = context;
		_listeners = new ArrayList<WakeLockHolder.WakeLockListener>();
		_count = new AtomicInteger(0);
	}

	/**
	 * Acquires the wake lock. After the acquisition, all registered listeners
	 * are notified via {@link WakeLockListener#onPostAcquire()}.
	 */
	public synchronized void acquireWL() {
		if (_wakeLock == null) {
			PowerManager pm = (PowerManager) _context
					.getSystemService(Context.POWER_SERVICE);
			_wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					WAKE_LOCK_NAME);
		}

		if (!_wakeLock.isHeld()) {
			_wakeLock.acquire();
			Log.i(TAG, "WakeLock acquired");
			logger.info("WakeLock acquired.");
			for (WakeLockListener listener : _listeners) {
				listener.onPostAcquire();
			}
		}
		
		_count.incrementAndGet();
	}

	/**
	 * Releases the wake lock. Just before the wake lock is released, all
	 * registered listeners are notified via
	 * {@link WakeLockListener#onBeforeRelease()}.
	 */
	public synchronized void releaseWL() {
		if (_wakeLock != null && _wakeLock.isHeld()) {
			_count.decrementAndGet();
			if(_count.get() == 0){
				for (WakeLockListener listener : _listeners) {
					listener.onBeforeRelease();
				}
				_wakeLock.release();
				Log.i(TAG, "WakeLock released");
				logger.info("WakeLock released.");
			} 
		}
	}

	/**
	 * Checks if the wake lock is acquired.
	 * 
	 * @return true, if wake lock is acquired
	 */
	public synchronized boolean isAcquired() {
		if (_wakeLock == null)
			return false;
		return _wakeLock.isHeld();
	}

	/**
	 * Adds a listener. Multiple add of the same listener are ignored.
	 * 
	 * @param listener
	 *            Listener to be added.
	 */
	public void addListener(WakeLockListener listener) {
		if (null != listener && !_listeners.contains(listener)) {
			_listeners.add(listener);
		}
	}

	/**
	 * Removes a listener.
	 * 
	 * @param listener
	 *            Listener to remove.
	 */
	public void removeListener(WakeLockListener listener) {
		_listeners.remove(listener);
	}

	/**
	 * Classes implementing this listener are notified after the Wake Lock is
	 * acquired and before it is released.
	 * 
	 */
	public interface WakeLockListener {

		/**
		 * Called after the wake lock has been acquired.
		 */
		public void onPostAcquire();

		/**
		 * Called just before the wake lock is going to be released.
		 */
		public void onBeforeRelease();
	}
}
