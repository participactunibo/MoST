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
package org.most.pipeline;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.most.DataBundle;
import org.most.MoSTApplication;
import org.most.input.Input;
import org.most.input.InstalledAppsInput;
import org.most.persistence.DBAdapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

public class PipelineInstalledApps extends Pipeline {

	public static final String PREF_KEY_DUMP_TO_DB = "PipelineInstalledApps.DumpToDB";
	public static final boolean PREF_DEFAULT_DUMP_TO_DB = true;
	public static final String PREF_KEY_SEND_INTENT = "PipelineInstalledApps.SendIntent";
	public static final boolean PREF_DEFAULT_SEND_INTENT = false;

	public static final String KEY_ACTION = "PipelineInstalledApps.Action";

	public static final String KEY_INSTALLED_APPS = "PipelineInstalledApps.InstalledApps";

	public static final String TBL_INSTALLED_APPS = "INSTALLED_APPS";
	public static final String FLD_TIMESTAMP = "timestamp";
	public static final String FLD_PACKAGE_NAME = "pkg_name";
	public static final String FLD_VERSION_CODE = "version_code";
	public static final String FLD_VERSION_NAME = "version_name";
	public static final String FLD_REQ_PERMISSIONS = "requested_permissions";
	public static final String CREATE_INSTALLED_APPS_TABLE = String.format(
			"_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s TEXT NOT NULL, %s INT, %s TEXT, %s TEXT", FLD_TIMESTAMP,
			FLD_PACKAGE_NAME, FLD_VERSION_CODE, FLD_VERSION_NAME, FLD_REQ_PERMISSIONS);

	protected boolean _isDump;
	protected boolean _isSend;

	public PipelineInstalledApps(MoSTApplication context) {
		super(context);
	}

	@Override
	public boolean onActivate() {
		checkNewState(State.ACTIVATED);
		_isDump = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES, Context.MODE_PRIVATE).getBoolean(
				PREF_KEY_DUMP_TO_DB, PREF_DEFAULT_DUMP_TO_DB);
		_isSend = getContext().getSharedPreferences(MoSTApplication.PREF_PIPELINES, Context.MODE_PRIVATE).getBoolean(
				PREF_KEY_SEND_INTENT, PREF_DEFAULT_SEND_INTENT);
		return super.onActivate();
	}

	public void onData(DataBundle b) {
		try {
			if (_isDump) {
				@SuppressWarnings("unchecked")
				List<PackageInfo> installedApps = (List<PackageInfo>) b
						.getObject(InstalledAppsInput.KEY_INSTALLED_APPS_LIST);
				DBAdapter dbAdapter = getContext().getDbAdapter();
				for (PackageInfo pi : installedApps) {
					ContentValues cv = new ContentValues();
					cv.put(FLD_TIMESTAMP, b.getLong(Input.KEY_TIMESTAMP));
					cv.put(FLD_PACKAGE_NAME, pi.packageName);
					cv.put(FLD_VERSION_NAME, pi.versionName);
					cv.put(FLD_VERSION_CODE, pi.versionCode);
					String[] permissions = pi.requestedPermissions;
					if (permissions != null) {
						Arrays.sort(permissions);
						cv.put(FLD_REQ_PERMISSIONS, StringUtils.join(permissions, ','));
					}else{
						cv.put(FLD_REQ_PERMISSIONS, "");
					}
					dbAdapter.storeData(TBL_INSTALLED_APPS, cv);
				}
				getContext().getDbAdapter().flushData();
			}

			if (_isSend) {
				Intent i = new Intent(KEY_ACTION);
				@SuppressWarnings("unchecked")
				List<PackageInfo> installedApps = (List<PackageInfo>) b
						.getObject(InstalledAppsInput.KEY_INSTALLED_APPS_LIST);
				PackageInfo[] arrayInstalledApps = installedApps.toArray(new PackageInfo[installedApps.size()]);
				i.putExtra(KEY_INSTALLED_APPS, arrayInstalledApps);
				i.putExtra(KEY_TIMESTAMP, Input.KEY_TIMESTAMP);
				getContext().sendBroadcast(i);
			}
		} finally {
			b.release();
		}
	}

	@Override
	public Type getType() {
		return Type.INSTALLED_APPS;
	}

	@Override
	public Set<org.most.input.Input.Type> getInputs() {
		Set<Input.Type> result = new TreeSet<Input.Type>();
		result.add(Input.Type.INSTALLED_APPS);
		return result;
	}

}
