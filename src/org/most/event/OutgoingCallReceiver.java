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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class OutgoingCallReceiver extends EventReceiver {

	private final String TAG = OutgoingCallReceiver.class.getSimpleName();

	@Override
	public IntentFilter getIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
		return filter;
	}
	
    @Override
    public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(null == bundle)
            	return;
            String phonenumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.i(TAG, "Outgoing Call to: " + phonenumber);
            //OutgoingCall -> vote to turn off microphone
            ((MoSTApplication) context.getApplicationContext()).getInputsArbiter().setEventVote(Input.Type.AUDIO, false);
    }
}