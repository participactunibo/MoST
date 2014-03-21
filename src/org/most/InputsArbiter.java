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

import android.os.PowerManager.WakeLock;

/**
 * This class is a proxy that limits activation/deactivation of inputs. There
 * are four voters that can vote if an {@link Input} should be active or not:
 * 
 * <ul>
 * <li>Sensing, i.e., pipelines that need a given input to work;</li>
 * <li>Power, i.e., duty cycling policies (e.g., {@link DutyCyclePolicy} class
 * or {@link IPowerPolicy}) that can shut down an input too save power;</li>
 * <li>Event, i.e., external events that may require the an Input to stop (e.g.,
 * locking screen, incoming phone call);</li>
 * <li>User, i.e., the user wants to pause an input for privacy reasons.</li>
 * </ul>
 * 
 * If all the voters agree, the InputsArbiter acquires the {@link WakeLock} (via
 * {@link WakeLockHolder}) and starts the input. If any of the voters wants to
 * shutdown an Input, the Input is deactivated and if there are no other Inputs
 * running then the WakeLock is released.
 * 
 * @author acirri
 * 
 */
public class InputsArbiter {

	private Map<Input.Type, Boolean> _sensingVotes;
	private Map<Input.Type, Boolean> _userVotes;
	private Map<Input.Type, Boolean> _eventVotes;
	private Map<Input.Type, Boolean> _powerVotes;

	private final MoSTApplication _context;

	public InputsArbiter(MoSTApplication _context) {
		this._context = _context;
		// getting the actual state of WakeLock.
		// NOTE now disabled for debug
		// _wakeLockVote = _context.getWakeLockHolder().isAcquired();
		_sensingVotes = new HashMap<Input.Type, Boolean>();
		_userVotes = new HashMap<Input.Type, Boolean>();
		_eventVotes = new HashMap<Input.Type, Boolean>();
		_powerVotes = new HashMap<Input.Type, Boolean>();
	}

	public void setUserVote(Input.Type type, boolean vote) {
		_userVotes.put(type, vote);
		evaluation(type);
	}

	public void setEventVote(Input.Type type, boolean vote) {
		_eventVotes.put(type, vote);
		evaluation(type);
	}

	public void setPowerVote(Input.Type type, boolean vote) {
		_powerVotes.put(type, vote);
		evaluation(type);
	}

	public void setSensingVote(Input.Type type, boolean vote) {
		_sensingVotes.put(type, vote);
		evaluation(type);
		if (_context.getInputManager().getPowerPolicyForInput(type) != null) {
			if (vote)
				_context.getInputManager().getPowerPolicyForInput(type).start();
			else
				_context.getInputManager().getPowerPolicyForInput(type).stop();
		}
	}

	private void evaluation(Input.Type type) {
		// if there is no vote for a specific Input Type, the default value
		// is true.
		boolean sensingVote = _sensingVotes.containsKey(type) ? _sensingVotes.get(type) : false;
		boolean userVote = _userVotes.containsKey(type) ? _userVotes.get(type) : true;
		boolean eventVote = _eventVotes.containsKey(type) ? _eventVotes.get(type) : true;
		boolean powerVote = _powerVotes.containsKey(type) ? _powerVotes.get(type) : true;
		if (sensingVote && userVote && eventVote && powerVote) {
			if(_context.getInputManager().getInput(type).isWakeLockNeeded())
				_context.getWakeLockHolder().acquireWL();
			_context.getInputManager().activateInput(type);
		} else if (_context.getInputManager().isInputAvailable(type)) {
			_context.getInputManager().deactivateInput(type);
			if(_context.getInputManager().getInput(type).isWakeLockNeeded())
				_context.getWakeLockHolder().releaseWL();
		}
	}

}
