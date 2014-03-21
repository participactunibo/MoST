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
package org.most.event;

import org.most.MoSTApplication;
import org.most.input.Input;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.speech.RecognizerIntent;
import android.util.Log;

public class SpeechRecognitionReceiver extends EventReceiver {

	private final String TAG = SpeechRecognitionReceiver.class.getSimpleName();
	
	public final static String PREF_KEY_SPEECHRECOGNITION_OFF_TIMER_MS = "SpeechRecognitionOffTimerMs";
	public final static long PREF_DEFAULT_SPEECHRECOGNITION_OFF_TIMER_MS = 1*60*1000; //one minute
	
	public static final String INTENT_SPEECHRECOGNITION_END = "Intent.SpeechRecognitionEnd";
	public static final int INTENT_SPEECHRECOGNITION_END_REQUCODE = 52461;
	
	@Override
	public IntentFilter getIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		// XXX: readd this filter
//		filter.addAction(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
		filter.addAction(RecognizerIntent.ACTION_WEB_SEARCH);
//		filter.addAction(Intent.ACTION_VOICE_COMMAND);
//		filter.addAction(Intent.ACTION_ASSIST);
		filter.addAction(INTENT_SPEECHRECOGNITION_END);
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
		return filter;
	}

    @Override
    public void onReceive(Context context, Intent intent) {
    	MoSTApplication application = (MoSTApplication) context.getApplicationContext();
    	if(intent.getAction().equals(INTENT_SPEECHRECOGNITION_END)){
    		//End Speech recognition intent received -> vote to turn on microphone
    		application.getInputsArbiter().setEventVote(Input.Type.AUDIO, true);
	        Log.i(TAG, "End speech recognition intent received -> vote to turn on microphone");
    	}else{
	        //Speech recognition intent received -> vote to turn off microphone
	    	application.getInputsArbiter().setEventVote(Input.Type.AUDIO, false);
	        Log.i(TAG, "Speech recognition intent received -> vote to turn off microphone");
	        //set timer to restore microphone state after N seconds
	    	Intent i = new Intent();
			i.setAction(INTENT_SPEECHRECOGNITION_END);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, INTENT_SPEECHRECOGNITION_END_REQUCODE, i, PendingIntent.FLAG_CANCEL_CURRENT);
			AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			long next = context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getLong(PREF_KEY_SPEECHRECOGNITION_OFF_TIMER_MS, PREF_DEFAULT_SPEECHRECOGNITION_OFF_TIMER_MS);
			mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+next, pendingIntent);
    	}
    }

}
