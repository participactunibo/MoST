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

import java.util.ArrayList;
import java.util.List;

import org.most.DataBundle;
import org.most.MoSTApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class InstalledAppsInput extends Input {

	private final static String TAG = InstalledAppsInput.class.getSimpleName();

	/**
	 * Key to access the list of installed apps.
	 */
	public final static String KEY_INSTALLED_APPS_LIST = "InstalledAppsInput.AppList";

	private PackageManager _packageManager;
	private InstalledAppReceiver _installedAppReceiver;

	/**
	 * @param context
	 */
	public InstalledAppsInput(MoSTApplication context) {
		super(context);
	}

	@Override
	public void onInit() {
		checkNewState(Input.State.INITED);
		_packageManager = getContext().getApplicationContext().getPackageManager();
		super.onInit();
	}

	@Override
	public boolean onActivate() {
		checkNewState(Input.State.ACTIVATED);
		super.onActivate();
		postInstalledApplications();
		_installedAppReceiver = new InstalledAppReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		intentFilter.addDataScheme("package");
		getContext().registerReceiver(_installedAppReceiver, intentFilter);
		return true;
	}

	@Override
	public void onDeactivate() {
		checkNewState(Input.State.DEACTIVATED);
		getContext().unregisterReceiver(_installedAppReceiver);
		_installedAppReceiver = null;
		super.onDeactivate();
	}

	@Override
	public void onFinalize() {
		checkNewState(Input.State.FINALIZED);
		_packageManager = null;
		super.onFinalize();
	}

	protected void postInstalledApplications() {
		Log.d(TAG, "Retrieving app state");
		List<PackageInfo> installedPkgs = new ArrayList<PackageInfo>(
				_packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS));
		DataBundle b = _bundlePool.borrowBundle();
		b.putObject(KEY_INSTALLED_APPS_LIST, installedPkgs);
		b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis());
		b.putInt(Input.KEY_TYPE, Input.Type.INSTALLED_APPS.toInt());
		post(b);
	}

	@Override
	public Type getType() {
		return Input.Type.INSTALLED_APPS;
	}

	@Override
	public boolean isWakeLockNeeded() {
		return false;
	}

	private class InstalledAppReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			postInstalledApplications();
		}

	}
}
