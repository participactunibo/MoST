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

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class CellInput extends PeriodicInput {

	/** The Constant TAG. */
	@SuppressWarnings("unused")
	private final static String TAG = CellInput.class.getSimpleName();

	/**
	 * {@link SharedPreferences} key to set the cellular towers monitoring
	 * period.
	 */
	public final static String PREF_KEY_CELLINPUT_PERIOD = "CellInputPeriod";

	/**
	 * Default cellular towers monitor interval in milliseconds. Currently set
	 * to {@value #PREF_DEFAULT_CELLINPUT_PERIOD}.
	 */
	public final static int PREF_DEFAULT_CELLINPUT_PERIOD = 1000* 60 * 30;

	/**
	 * Key for the cellular tower type. Value can be
	 * TelephonyManager.PHONE_TYPE_GSM, TelephonyManager.PHONE_TYPE_CDMA or
	 * TelephonyManager.PHONE_TYPE_NONE.
	 */
	public static final String KEY_PHONE_TYPE = "CellInput.phoneType";

	/**
	 * Key for gsm cell id.
	 */
	public static final String KEY_GSM_CELL_ID = "CellInput.gsmCellId";

	/**
	 * Key for gsm location area code.
	 */
	public static final String KEY_GSM_LAC = "CellInput.gsmLocationAreaCode";

	/**
	 * Key for primary scrambling code for UMTS.
	 */
	public static final String KEY_GSM_PSC = "CellInput.gsmPrimaryScramblingCode";

	/**
	 * Key for cdma base station identification number.
	 */
	public static final String KEY_BASE_STATION_ID = "CellInput.cdmaBaseStationId";

	/**
	 * Key for cdma base station latitude in units of 0.25 seconds. NOTE:
	 * Latitude is a decimal number as specified in 3GPP2 C.S0005-A v6.0.
	 * (http://www.3gpp2.org/public_html/specs/C.S0005-A_v6.0.pdf) It is
	 * represented in units of 0.25 seconds and ranges from -1296000 to 1296000,
	 * both values inclusive (corresponding to a range of -90 to +90 degrees).
	 * Integer.MAX_VALUE is considered invalid value.
	 */
	public static final String KEY_BASE_STATION_LATITUDE = "CellInput.cdmaBaseStationLatitude";

	/**
	 * Key for cdma base station longitude in units of 0.25 seconds. NOTE:
	 * Longitude is a decimal number as specified in 3GPP2 C.S0005-A v6.0.
	 * (http://www.3gpp2.org/public_html/specs/C.S0005-A_v6.0.pdf) It is
	 * represented in units of 0.25 seconds and ranges from -2592000 to 2592000,
	 * both values inclusive (corresponding to a range of -180 to +180 degrees).
	 * Integer.MAX_VALUE is considered invalid value.
	 */
	public static final String KEY_BASE_STATION_LONGITUDE = "CellInput.cdmaBaseStationLongitude";

	/**
	 * Key for cdma network identification number.
	 */
	public static final String KEY_BASE_NETWORK_ID = "CellInput.cdmaNetworkId";

	/**
	 * Key for cdma system identification number.
	 */
	public static final String KEY_BASE_SYSTEM_ID = "CellInput.cdmaSystemId";

	TelephonyManager _telephonyManager;

	public CellInput(MoSTApplication context) {
		super(context, context.getSharedPreferences(MoSTApplication.PREF_INPUT, Context.MODE_PRIVATE).getInt(
				PREF_KEY_CELLINPUT_PERIOD, PREF_DEFAULT_CELLINPUT_PERIOD));
	}

	@Override
	public void onInit() {
		checkNewState(Input.State.INITED);
		_telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		super.onInit();
	}

	@Override
	public void onFinalize() {
		checkNewState(Input.State.FINALIZED);
		_telephonyManager = null;
		super.onFinalize();
	}

	@Override
	public void workToDo() {
		CellLocation cellLocation = _telephonyManager.getCellLocation();
		DataBundle b = _bundlePool.borrowBundle();
		if (cellLocation instanceof GsmCellLocation) {
			GsmCellLocation gsmLocation = (GsmCellLocation) cellLocation;
			b.putInt(KEY_GSM_CELL_ID, gsmLocation.getCid());
			b.putInt(KEY_GSM_LAC, gsmLocation.getLac());
			// gsmLocation.getPsc() require api 9
			// b.putInt(KEY_GSM_PSC, gsmLocation.getPsc());
			b.putInt(KEY_PHONE_TYPE, TelephonyManager.PHONE_TYPE_GSM);
		} else if (cellLocation instanceof CdmaCellLocation) {
			CdmaCellLocation cdmaLocation = (CdmaCellLocation) cellLocation;
			b.putInt(KEY_BASE_STATION_ID, cdmaLocation.getBaseStationId());
			b.putInt(KEY_BASE_STATION_LATITUDE, cdmaLocation.getBaseStationLatitude());
			b.putInt(KEY_BASE_STATION_LONGITUDE, cdmaLocation.getBaseStationLongitude());
			b.putInt(KEY_BASE_NETWORK_ID, cdmaLocation.getNetworkId());
			b.putInt(KEY_BASE_SYSTEM_ID, cdmaLocation.getSystemId());
			b.putInt(KEY_PHONE_TYPE, TelephonyManager.PHONE_TYPE_CDMA);
		} else {
			b.putInt(KEY_PHONE_TYPE, TelephonyManager.PHONE_TYPE_NONE);
		}

		b.putLong(Input.KEY_TIMESTAMP, System.currentTimeMillis());
		b.putInt(Input.KEY_TYPE, Input.Type.CELL.toInt());
		post(b);
		scheduleNextStart();
	}

	@Override
	public Type getType() {
		return Input.Type.CELL;
	}
}
