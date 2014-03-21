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

import org.most.DataBundle;
import org.most.MoSTApplication;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * This Input wraps the microphone. It records 44100, 16bit PCM raw audio. When
 * posting on a bus, it sends {@link DataBundle} containing:
 * <ul>
 * <li>{@link Input#KEY_TYPE} (int): type of the sensor, convert it to a
 * {@link Input#Type} using {@link Input#Type.fromInt()}.</li>
 * <li> {@link Input#KEY_TIMESTAMP} (long): the timestamp of the sensed audio, in
 * nanoseconds.</li>
 * <li> {@link InputAudio#KEY_AUDIODATA} (short[]): array containing raw audio.</li>
 * <li> {@link InputAudio#KEY_AUDIODATA_LENGTH} (int): the number of shorts that
 * the KEY_AUDIODATA array contains.</li>
 * </ul>
 * 
 * This input supports sample rate to be configured. The
 * {@link SharedPreferences} name to use is {@link MoSTApplication#PREF_INPUT},
 * the key value to use is {@link #PREF_KEY_SAMPLE_RATE}. Its default value is
 * {@link #PREF_DEFAULT_SAMPLE_RATE} ({@value #PREF_DEFAULT_SAMPLE_RATE}). For
 * example:
 * 
 * <pre>
 * {@code
 * Editor editor = context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).edit();
 * editor.putInt(PREF_KEY_SAMPLE_RATE, 8000);
 * editor.apply();
 * }
 * </pre>
 * 
 */
public class InputAudio extends Input {

	/**
	 * {@link SharedPreferences} key to set the audio sample rate. The sample
	 * rate expressed in Hertz. 44100Hz is currently the only rate that is
	 * guaranteed to work on all devices, but other rates such as 22050, 16000,
	 * and 11025 may work on some devices.
	 */
	public static final String PREF_KEY_SAMPLE_RATE = "InputAudioSampleRate";
	/**
	 * The default sample rate in Hertz. The default value is set to
	 * {@value #PREF_DEFAULT_SAMPLE_RATE} because it is guaranteed to work on
	 * all devices. Other lower rates may work on some devices.
	 */
	public static final int PREF_DEFAULT_SAMPLE_RATE = 44100;

	/** The Constant DEBUG. */
	@SuppressWarnings("unused")
	private final static boolean DEBUG = true;

	/** The Constant TAG. */
	private final static String TAG = InputAudio.class.getSimpleName();

	/**
	 * Key to access in the {@link DataBundle} the array of short containing
	 * audio data.
	 */
	public final static String KEY_AUDIODATA = "audio_data";
	/**
	 * Key to access in the {@link DataBundle} the number of shorts that the
	 * {@link #KEY_AUDIODATA} array contains.
	 */
	public final static String KEY_AUDIODATA_LENGTH = "audio_data_len";

	/** The _recorder. */
	private RecorderThread _recorder = null;

	public InputAudio(MoSTApplication context) {
		super(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unibo.mobilesensingframework.input.Input#resume()
	 */
	@Override
	public boolean onActivate() {
		checkNewState(State.ACTIVATED);
		_recorder = new RecorderThread(getContext(), this);
		_recorder.start();

		return super.onActivate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unibo.mobilesensingframework.input.Input#pause()
	 */
	@Override
	public void onDeactivate() {
		checkNewState(State.DEACTIVATED);
		_recorder.stopRecorder();
		try {
			_recorder.join();
		} catch (InterruptedException e) {
			Log.e(TAG, "Error while stopping InputAudio: " + e.getMessage());
			e.printStackTrace();
		}

		super.onDeactivate();
	}

	@Override
	public Type getType() {
		return Type.AUDIO;
	}

	@Override
	public boolean isWakeLockNeeded() {
		return true;
	}
}
