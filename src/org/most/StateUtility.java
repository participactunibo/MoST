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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;

public class StateUtility {

	private static final String FILENAME = "MoST.state";

	public static synchronized boolean persistState(Context context, MoSTState state) {
		
		try {
			FileOutputStream fileOutputStream = context.openFileOutput(
					FILENAME, Context.MODE_PRIVATE);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(
					fileOutputStream);
			objectOutputStream.writeObject(state);
			objectOutputStream.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static synchronized MoSTState loadState(Context context) {
		
		MoSTState result = null;
		try {
			File file = new File(context.getFilesDir(), FILENAME);
			if(file.exists()){
				FileInputStream fileInputStream = context.openFileInput(FILENAME);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				Object obj = objectInputStream.readObject();
				objectInputStream.close();
				if (obj instanceof MoSTState) {
					result = (MoSTState) obj;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
		
	}
	
	public static synchronized boolean deleteState(Context context){
		
		boolean result = false;
		try {
			result = context.deleteFile(FILENAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	
}
