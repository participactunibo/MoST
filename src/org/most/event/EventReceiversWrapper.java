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

import java.util.LinkedList;
import java.util.List;

import android.content.Context;

/**
 * @author acirri
 *
 */
public class EventReceiversWrapper {

	private Context _context;
	private List<EventReceiver> _receivers;
	
	public EventReceiversWrapper(Context context){
		_context = context;
		_receivers = new LinkedList<EventReceiver>();
		_receivers.add(new IncomingCallReceiver());
		_receivers.add(new OutgoingCallReceiver());
		_receivers.add(new SpeechRecognitionReceiver());
		_receivers.add(new ScreenOnReceiver());
		_receivers.add(new ScreenOffReceiver());
	}
	
	public void registerAllEventReceivers(){
		for(EventReceiver receiver: _receivers)
			_context.registerReceiver(receiver, receiver.getIntentFilter());
	}
	
	public void unregisterAllEventReceivers(){
		for(EventReceiver receiver: _receivers)
			_context.unregisterReceiver(receiver);
	}
}
