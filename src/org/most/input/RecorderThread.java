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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Looper;
import android.util.Log;

/**
 * Recording thread that receives data from the microphone and packs it in
 * {@link DataBundle}s.
 */
public class RecorderThread extends Thread {

	/** The Constant DEBUG. */
	private final static boolean DEBUG = false;

	/** The Constant TAG. */
	private final static String TAG = RecorderThread.class.getSimpleName();

	/** The Constant CHANNEL_CONFIGURATION. */
	private final static int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_IN_DEFAULT;

	/** The Constant ENCODING. */
	private final static int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	/** The _recorder. */
	private AudioRecord _recorder = null;

	/** The _buffer size. */
	private int _bufferSize = 0;

	/** The _recording. */
	public AtomicBoolean _recording;

	private InputAudio _input;

	/**
	 * Instantiates a new recorder thread.
	 * 
	 * @param context
	 *            the context
	 */
	public RecorderThread(MoSTApplication context, InputAudio input) {

		super("MoST InputAudio Recorder Thread");

		SharedPreferences sp = context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE);
		int sampleRate = sp.getInt(InputAudio.PREF_KEY_SAMPLE_RATE, InputAudio.PREF_DEFAULT_SAMPLE_RATE);

		_bufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIGURATION, ENCODING) * 8;
		_recorder = new AudioRecord(AudioSource.MIC, sampleRate, CHANNEL_CONFIGURATION, ENCODING, _bufferSize);
		_recording = new AtomicBoolean(false);
		_input = input;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		_recording.set(true);
		Looper.prepare();
		if (DEBUG)
			Log.d(TAG, "Start Recording");
		_recorder.startRecording();
		while (_recording.get()) {

			DataBundle b = _input._bundlePool.borrowBundle();
			int dataSize = _recorder.read(b.allocateShortArray(InputAudio.KEY_AUDIODATA, _bufferSize), 0, _bufferSize);

			b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis() * 100000L);
			b.putInt(InputAudio.KEY_AUDIODATA_LENGTH, dataSize);
			b.putInt(Input.KEY_TYPE, Input.Type.AUDIO.toInt());
			if (DEBUG)
				Log.d(TAG, "Read data from microphone");
			_input.post(b);

		}
		// TODO: is this stop() really necessary?
		_recorder.stop();
		Looper.myLooper().quit();
		_recorder.release();
		if (DEBUG)
			Log.d(TAG, "Recorder released");
	}

	public void stopRecorder() {
		_recording.set(false);
	}
}
