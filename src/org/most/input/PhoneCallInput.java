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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.most.DataBundle;
import org.most.MoSTApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * This Input notifies the start time and end time of incoming and outgoing
 * calls. Whenever a call ends emits a {@link DataBundle} containing:
 * <ul>
 * <li>{@link Input#KEY_TYPE} (int): type of the sensor, convert it to a
 * {@link Input#Type} using {@link Input#Type.fromInt()}. Set to
 * {@link Input.Type#PHONECALL}.</li>
 * <li> {@link Input#KEY_TIMESTAMP} (long): the time the DataBundle was
 * generated.</li>
 * <li>{@link #KEY_BUNDLE_TYPE} (int): set to {@link #VAL_BUNDLE_TYPE_DURATION}
 * (value: {@value #VAL_BUNDLE_TYPE_DURATION})</li>
 * <li>{@link #KEY_PHONE_NUMBER} (string): the phone number of the phone
 * conversation partner</li>
 * <li>{@link #KEY_CALL_START_TIMESTAMP} (long): the time the call started.</li>
 * <li>{@link #KEY_CALL_END_TIMESTAMP} (long): the time the call ended.</li>
 * <li> {@value #KEY_IS_INCOMING} (boolean): true if the call was incoming, false
 * if it was outgoing.</li>
 * </ul>
 * 
 * Whenever a call starts or ends emits a DataBundle containing:
 * <ul>
 * <li>{@link Input#KEY_TYPE} (int): type of the sensor, convert it to a
 * {@link Input#Type} using {@link Input#Type.fromInt()}. Set to
 * {@link Input.Type#PHONECALL}.</li>
 * <li> {@link Input#KEY_TIMESTAMP} (long): the time the DataBundle was
 * generated.</li>
 * <li>{@link #KEY_BUNDLE_TYPE} (int): set to {@link #VAL_BUNDLE_TYPE_EVENT}
 * (value: {@value #VAL_BUNDLE_TYPE_EVENT})</li>
 * <li>{@link #KEY_PHONE_NUMBER} (string): the phone number of the phone
 * conversation partner</li>
 * <li>{@link #KEY_IS_START} (boolean) <code>true</code> if the event is related
 * to a call starting, <code>false</code> otherwise.
 * <li> {@value #KEY_IS_INCOMING} (boolean): <code>true</code> if the call was
 * incoming, <code>false</code> if it was outgoing.</li>
 * </ul>
 * 
 * This input does not support any configuration.
 * 
 * @author acirri
 * @author gcardone
 * 
 */
public class PhoneCallInput extends Input {

	private final static boolean DEBUG = true;

	private final static String TAG = PhoneCallInput.class.getSimpleName();

	public static final String KEY_BUNDLE_TYPE = "PhoneCallInput.BundleType";
	public static final int VAL_BUNDLE_TYPE_EVENT = 1;
	public static final int VAL_BUNDLE_TYPE_DURATION = 2;
	public static final String KEY_IS_START = "PhoneCallInput.isStart";
	public static final String KEY_CALL_START_TIMESTAMP = "PhoneCallInput.startCallTimestamp";
	public static final String KEY_CALL_END_TIMESTAMP = "PhoneCallInput.stopCallTimestamp";
	public static final String KEY_IS_INCOMING = "PhoneCallInput.isIncomingCall";
	public static final String KEY_PHONE_NUMBER = "PhoneCallInput.phoneNumber";

	private IntentFilter _filter;
	private CallBrodcastReceiver _callBroadcastReceiver;
	private PhoneStateListener _phoneStateListener;
	private ReentrantLock _lock;
	private Condition _phoneNumberCondition;
	private boolean _isPhoneNumberReady;

	public PhoneCallInput(MoSTApplication context) {
		super(context);
	}

	@Override
	public void onInit() {
		checkNewState(Input.State.INITED);
		_filter = new IntentFilter();
		_filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		_filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
		_filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		_callBroadcastReceiver = new CallBrodcastReceiver();
		_phoneStateListener = new CallInputPhoneStateListener();
		super.onInit();
	}

	@Override
	public boolean onActivate() {
		checkNewState(Input.State.ACTIVATED);
		getContext().registerReceiver(_callBroadcastReceiver, _filter);
		_lock = new ReentrantLock();
		_phoneNumberCondition = _lock.newCondition();
		_isPhoneNumberReady = false;
		TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		return super.onActivate();
	}

	@Override
	public void onDeactivate() {
		checkNewState(Input.State.DEACTIVATED);
		getContext().unregisterReceiver(_callBroadcastReceiver);
		TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_NONE);
		super.onDeactivate();
	}

	@Override
	public void onFinalize() {
		checkNewState(Input.State.FINALIZED);
		_filter = null;
		_callBroadcastReceiver = null;
		super.onFinalize();
	}

	@Override
	public Type getType() {
		return Input.Type.PHONECALL;
	}

	class CallInputPhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				_lock.lock();
				try {
					_callBroadcastReceiver._phoneNumber = incomingNumber;
					_isPhoneNumberReady = true;
					_phoneNumberCondition.signal();
				} finally {
					_lock.unlock();
				}
			}
		}
	}

	class CallBrodcastReceiver extends BroadcastReceiver {

		long _startCall;
		long _endCall;
		boolean _incomingCall;
		String _phoneNumber;

		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle bundle = intent.getExtras();
			if (null == bundle)
				return;
			// incoming call
			if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
				String state = bundle.getString(TelephonyManager.EXTRA_STATE);
				if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
					if (DEBUG) {
						Log.d(TAG, "Phone call: incoming start");
					}

					_startCall = System.currentTimeMillis();
					_incomingCall = true;
					// wait for the phone listener to retrieve the incoming phone number
					_lock.lock();
					try {
						while (_isPhoneNumberReady == false) {
							try {
								_phoneNumberCondition.await(1, TimeUnit.SECONDS);
							} catch (InterruptedException e) {
								e.printStackTrace();
								break;
							}
							_isPhoneNumberReady = true;
						}
						_isPhoneNumberReady = false;
					} finally {
						_lock.unlock();
					}
					// notify the incoming call start event
					DataBundle b = _bundlePool.borrowBundle();
					b.putInt(Input.KEY_TYPE, Input.Type.PHONECALL.toInt());
					b.putLong(Input.KEY_TIMESTAMP, _startCall);
					b.putInt(KEY_BUNDLE_TYPE, VAL_BUNDLE_TYPE_EVENT);
					b.putBoolean(KEY_IS_INCOMING, _incomingCall);
					b.putBoolean(KEY_IS_START, true);
					b.putString(KEY_PHONE_NUMBER, _phoneNumber);
					post(b);
				}
				if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
					_endCall = System.currentTimeMillis();

					if (DEBUG) {
						if (_incomingCall) {
							Log.d(TAG, "Phone call: incoming end");
						} else {
							Log.d(TAG, "Phone call: outgoing end");
						}
					}

					// notify phone call duration
					DataBundle b = _bundlePool.borrowBundle();
					b.putInt(KEY_BUNDLE_TYPE, VAL_BUNDLE_TYPE_DURATION);
					b.putLong(KEY_CALL_START_TIMESTAMP, _startCall);
					b.putLong(KEY_CALL_END_TIMESTAMP, _endCall);
					b.putBoolean(KEY_IS_INCOMING, _incomingCall);
					b.putInt(Input.KEY_TYPE, Input.Type.PHONECALL.toInt());
					b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis());
					b.putString(KEY_PHONE_NUMBER, _phoneNumber);
					post(b);

					// notify the end call event
					b = _bundlePool.borrowBundle();
					b.putInt(Input.KEY_TYPE, Input.Type.PHONECALL.toInt());
					b.putLong(Input.KEY_TIMESTAMP, _endCall);
					b.putInt(KEY_BUNDLE_TYPE, VAL_BUNDLE_TYPE_EVENT);
					b.putBoolean(KEY_IS_INCOMING, _incomingCall);
					b.putBoolean(KEY_IS_START, false);
					b.putString(KEY_PHONE_NUMBER, _phoneNumber);
					post(b);
				}
			} else if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				_startCall = System.currentTimeMillis();
				_incomingCall = false;
				_phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

				if (DEBUG) {
					Log.d(TAG, "Phone call: outgoing start");
				}

				// notify the outgoing call start event
				DataBundle b = _bundlePool.borrowBundle();
				b.putInt(Input.KEY_TYPE, Input.Type.PHONECALL.toInt());
				b.putLong(Input.KEY_TIMESTAMP, _startCall);
				b.putInt(KEY_BUNDLE_TYPE, VAL_BUNDLE_TYPE_EVENT);
				b.putBoolean(KEY_IS_INCOMING, _incomingCall);
				b.putBoolean(KEY_IS_START, true);
				b.putString(KEY_PHONE_NUMBER, _phoneNumber);
				post(b);
			}

		}
	}

	@Override
	public boolean isWakeLockNeeded() {
		return false;
	}
}
