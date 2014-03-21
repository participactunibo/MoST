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
package org.most.input;

import java.util.concurrent.atomic.AtomicBoolean;

import org.most.DataBundle;
import org.most.MoSTApplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * This Input periodically scans nearby devices. Each scan inquiry is about 12
 * seconds long, therefore the period of this Input should not be shorter than
 * 12 seconds. Every time a device is discovered, the input emits a
 * {@link DataBundle} containing:
 * <ul>
 * <li>{@link Input#KEY_TYPE} (int): type of the sensor, convert it to a
 * {@link Input#Type} using {@link Input#Type.fromInt()}. Set to
 * {@link Input.Type#BLUETOOTHSCAN}.</li>
 * <li> {@link Input#KEY_TIMESTAMP} (long): timestamp in milliseconds.</li>
 * <li>{@link #KEY_NAME} (String): &quot;friendly name&quot; of the discovered
 * device</li>
 * <li>{@link #KEY_MAC} (String): MAC address of the device.</li>
 * <li>{@link #KEY_DEVICECLASS} (int): scanned device Bluetooth class (see
 * {@link BluetoothClass.Device})</li>
 * <li>{@link #KEY_DEVICEMAJORCLASS} (int): scanned device Bluetooth major class
 * (see {@link BluetoothClass.Device.Major})</li>
 * </ul>
 * 
 * This input supports the app monitoring rate to be configured. The
 * {@link SharedPreferences} name to use is {@link MoSTApplication#PREF_INPUT},
 * the key value to use is {@link #PREF_KEY_BLUETOOTH_SCANPERIOD_MS} . Its
 * default value is {@link #PREF_DEFAULT_BLUETOOTH_SCANPERIOD_MS} (
 * {@value #PREF_DEFAULT_BLUETOOTH_SCANPERIOD_MS} milliseconds). For example:
 * 
 * <pre>
 * {
 * 	Editor editor = context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).edit();
 * 	editor.putInt(PREF_KEY_BLUETOOTH_SCANPERIOD_MS, 60000);
 * 	editor.apply();
 * }
 * </pre>
 * 
 * In addition, this Input can forcefully switch on the Bluetooth interface to
 * start the scan; in this case, the interface is switched off as soon as the
 * inquiry scan ends. Please note that this behavior is intrusive to the user
 * experience and should be avoided, if possible. By default, the Bluetooth
 * interface is <em>not</em> automatically switched on. The
 * {@link SharedPreferences} name to use is {@link MoSTApplication#PREF_INPUT},
 * the key value to use is {@link #PREF_KEY_BLUETOOTH_SWITCH_ON_INTERFACE} . Its
 * default value is {@link #PREF_DEFAULT_BLUETOOTH_SWITCH_ON_INTERFACE}.
 * 
 * <pre>
 * {
 * 	Editor editor = context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).edit();
 * 	editor.putBoolean(PREF_KEY_BLUETOOTH_SWITCH_ON_INTERFACE, true);
 * 	editor.apply();
 * }
 * </pre>
 * 
 * @author gcardone
 * @author acirri
 * 
 */
public class BluetoothScanInput extends PeriodicInput {

	/** The Constant DEBUG. */
	@SuppressWarnings("unused")
	private final static boolean DEBUG = true;

	/** The Constant TAG. */
	private final static String TAG = BluetoothScanInput.class.getSimpleName();

	public final static String KEY_MAC = "BluetoothScanInput.MAC";
	public final static String KEY_NAME = "BluetoothScanInput.FriendlyName";
	public final static String KEY_DEVICECLASS = "BluetoothScanInput.DeviceClass";
	public final static String KEY_DEVICEMAJORCLASS = "BluetoothScanInput.DeviceMajorClass";

	public final static String PREF_KEY_BLUETOOTH_SCANPERIOD_MS = "BlueToothScanInputPeriodMs";
	public final static int PREF_DEFAULT_BLUETOOTH_SCANPERIOD_MS = 60000 * 30;
	public final static String PREF_KEY_BLUETOOTH_SWITCH_ON_INTERFACE = "BlueToothScanInputSwitchOnInterface";
	public final static boolean PREF_DEFAULT_BLUETOOTH_SWITCH_ON_INTERFACE = false;

	BluetoothAdapter _bluetoothAdapter;
	boolean _forceOnInterface;
	boolean _shouldShutDownInterface;
	BluetoothEndDiscoveryReceiver _endDiscoveryReceiver;
	BluetoothStateReceiver _stateReceiver;
	BluetoothDeviceReceiver _bluetoothDeviceReceiver;
	AtomicBoolean _scanRunning;
	AtomicBoolean _running;

	/**
	 * Builds a new Bluetooth scan input.
	 * 
	 * @param context
	 *            Reference {@link MoSTApplication} context.
	 */
	public BluetoothScanInput(MoSTApplication context) {
		super(context, context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getInt(
				PREF_KEY_BLUETOOTH_SCANPERIOD_MS, PREF_DEFAULT_BLUETOOTH_SCANPERIOD_MS));
	}

	@Override
	public void onInit() {
		checkNewState(Input.State.INITED);
		_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (_bluetoothAdapter == null) {
			Log.e(TAG, " Device does not support Bluetooth");
		}
		_scanRunning = new AtomicBoolean(false);
		_running = new AtomicBoolean(false);
		super.onInit();
	}

	@Override
	public boolean onActivate() {
		checkNewState(Input.State.ACTIVATED);
		_forceOnInterface = getContext().getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE)
				.getBoolean(PREF_KEY_BLUETOOTH_SWITCH_ON_INTERFACE, PREF_DEFAULT_BLUETOOTH_SWITCH_ON_INTERFACE);
		return super.onActivate();
	}

	@Override
	public void onDeactivate() {
		checkNewState(Input.State.DEACTIVATED);
		unregisterReceivers();
		_scanRunning.set(false);
		_running.set(false);
		super.onDeactivate();
	}

	@Override
	public void workToDo() {
		if (_running.getAndSet(true)) {
			Log.w(TAG, "Bluetooth scan already running.");
			return;
		}
		if (_scanRunning.get()) {
			Log.w(TAG, "Bluetooth scan already running.");
			return;
		}
		if (_bluetoothAdapter != null) {
			_shouldShutDownInterface = false;

			if (!_bluetoothAdapter.isEnabled()) {
				if (_forceOnInterface) {
					_stateReceiver = new BluetoothStateReceiver();
					IntentFilter stateFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
					getContext().registerReceiver(_stateReceiver, stateFilter);
					if (_bluetoothAdapter.enable()) {
						_shouldShutDownInterface = true;
					} else {
						Log.e(TAG, "Error while enabling the bluetooth interface.");
						_running.set(false);
						scheduleNextStart();
					}
				} else {
					Log.w(TAG, "Cannot start bluetooth scanning: interface disabled");
					_running.set(false);
					scheduleNextStart();
				}
			} else {
				_shouldShutDownInterface = false;
				startDiscovery();
			}
		} else {
			Log.e(TAG, "Bluetooth Adapter is null, Bluetooth not supported on this platform.");
			_running.set(false);
			scheduleNextStart();
		}
	}

	/**
	 * Starts the bluetooth inquiry scan. Multiple starts are ignored.
	 */
	private void startDiscovery() {
		if (_scanRunning.getAndSet(true)) {
			Log.w(TAG, "Bluetooth discovery already started, aborting this scan request.");
		}
		_endDiscoveryReceiver = new BluetoothEndDiscoveryReceiver();
		IntentFilter endDiscovery = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		getContext().registerReceiver(_endDiscoveryReceiver, endDiscovery);

		_bluetoothDeviceReceiver = new BluetoothDeviceReceiver();
		IntentFilter deviceFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		getContext().registerReceiver(_bluetoothDeviceReceiver, deviceFilter);

		boolean successfulStart = _bluetoothAdapter.startDiscovery();
		if (successfulStart) {
			Log.i(TAG, "Bluetooth scan started succesfully");
		} else {
			unregisterReceivers();
			Log.e(TAG, "Bluetooth scan failed.");
			_running.set(false);
			_scanRunning.set(false);
			scheduleNextStart();
		}
	}

	/**
	 * Unregisters all the {@link BroadcastReceiver}s usedto detect Bluetooth
	 * interface state changes.
	 */
	private void unregisterReceivers() {
		if (_bluetoothDeviceReceiver != null) {
			getContext().unregisterReceiver(_bluetoothDeviceReceiver);
			_bluetoothDeviceReceiver = null;
		}
		if (_endDiscoveryReceiver != null) {
			getContext().unregisterReceiver(_endDiscoveryReceiver);
			_endDiscoveryReceiver = null;
		}
		if (_stateReceiver != null) {
			getContext().unregisterReceiver(_stateReceiver);
			_stateReceiver = null;
		}
	}

	@Override
	public Type getType() {
		return Input.Type.BLUETOOTHSCAN;
	}

	/**
	 * Detects new Bluetooth devices and accordingly creates a
	 * {@link DataBundle}.
	 * 
	 */
	class BluetoothDeviceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				DataBundle b = _bundlePool.borrowBundle();
				b.putInt(Input.KEY_TYPE, Input.Type.BLUETOOTHSCAN.toInt());
				b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis());
				b.putString(KEY_MAC, device.getAddress());
				b.putString(KEY_NAME, device.getName());
				b.putInt(KEY_DEVICEMAJORCLASS, device.getBluetoothClass().getMajorDeviceClass());
				b.putInt(KEY_DEVICECLASS, device.getBluetoothClass().getDeviceClass());
				post(b);
			}
		}
	}

	class BluetoothStateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			int bluetoothState = _bluetoothAdapter.getState();
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) && bluetoothState == BluetoothAdapter.STATE_ON) {
				/*
				 * Apparently some Android devices need a pause before starting
				 * the bluetooth inquiry scan. We use a Thread to wait a second
				 * before scanning for Bluetooth devices.
				 */
				new Thread(new Runnable() {
					public void run() {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						startDiscovery();
					}
				}, "BluetoothStatInquiryScan").start();
			}
		}
	}

	class BluetoothEndDiscoveryReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				Log.i(TAG, "Bluetooth scan ended");
				unregisterReceivers();
				if (_shouldShutDownInterface) {
					_bluetoothAdapter.disable();
				}
				_scanRunning.set(false);
				_running.set(false);
				
				scheduleNextStart();
			}
		}

	}
}
