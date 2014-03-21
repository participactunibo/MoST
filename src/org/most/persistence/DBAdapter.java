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
package org.most.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

/**
 * This class abstracts access to the MoST database. Accessing class <b>must</b>
 * call the {@link DBAdapter#open()} method when using the database (i.e., if
 * accessing the private {@link SQLiteDatabase} variable) and <b>must</b> call
 * the {@link DBAdapter#close()} method when done.
 * 
 * Data is stored in a cache before being written to the database: this allows
 * to quickly handle even large amounts of data (e.g., accelerometer) while reducing
 * the number of accesses to persistent storage, which in turn saves battery.
 * 
 * @author gcardone
 * @author acirri
 * 
 */
public class DBAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(DBAdapter.class);

	private static final String TAG = DBAdapter.class.getSimpleName();

	private final static ReentrantLock _dbLock = new ReentrantLock();

	private static final String FLD_TABLE = "DBAdapter.TableName.skrtwrd";

	private final static int DATA_TO_WRITE_DB = 1000;

	private Context _context;
	private DBHelper _dbHelper;
	private SQLiteDatabase _db;
	private LinkedBlockingQueue<ContentValues> _cachedData;
	private BlockingQueue<LinkedBlockingQueue<ContentValues>> _dataToDump;
	private AtomicBoolean _runningDump = new AtomicBoolean(false);
	private int _warningPrintCount;

	private static DBAdapter sDBAdapterInstance;

	public static synchronized DBAdapter getInstance(Context context) {
		if (sDBAdapterInstance == null) {
			sDBAdapterInstance = new DBAdapter(context);
			Log.i(TAG, "Successfully created new instance of DBAdapter");
		}
		return sDBAdapterInstance;
	}

	private DBAdapter(Context context) {
		if (context == null) {
			throw new IllegalArgumentException();
		}
		_context = context;
		_cachedData = new LinkedBlockingQueue<ContentValues>(
				DATA_TO_WRITE_DB * 2);
		_dataToDump = new LinkedBlockingQueue<LinkedBlockingQueue<ContentValues>>();
		_warningPrintCount = 0;
		File externalDir = _context.getExternalFilesDir(null);
		if (externalDir == null) {
			Log.e(TAG, "external storage is not available");
			logger.error("external storage is not available");
			_dbLock.unlock();
			throw new IllegalStateException();
		}
		_dbHelper = new DBHelper(_context, externalDir);
	}

	public DBAdapter open() {
		Log.i(TAG, "Opening DBAdapter");
		_dbLock.lock();
		Log.i(TAG, "DBAdapter successfully locked");
		try {
			_db = _dbHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			_dbLock.unlock();
			Log.e(TAG, "Unable to open MoST db", e);
			logger.error("Unable to open MoST d", e);
			return null;
		}
		return this;
	}

	public void close() {
		Log.i(TAG, "Closing DBAdapter");
		try {
			_dbHelper.close();
			Log.i(TAG, "DBAdapter successfully closed");
		} finally {
			_dbLock.unlock();
			Log.i(TAG, "DBAdapter successfully unlocked");
		}
	}

	/**
	 * Stores data in a cache to be written to the database (without flushing).
	 * 
	 * @param table
	 *            Table to store data into.
	 * @param data
	 *            Data to store
	 */
	public synchronized void storeData(String table, ContentValues data) {
		storeData(table, data, false);
	}

	/**
	 * Stores data in a cache to be written to the database.
	 * 
	 * @param table
	 *            Table to store data into.
	 * @param data
	 *            Data to store
	 * @param forceFlush
	 *            If <code>false</code>, the data is stored in a local buffer
	 *            before being written on permanent storage to save battery. If
	 *            <code>true</code>, all cached data is immediately written to
	 *            the DB.
	 */
	public synchronized void storeData(String table, ContentValues data,
			boolean forceFlush) {
		// Log.d(TAG, "Storing data to table " + table);
		data.put(FLD_TABLE, table);
		int dataSize = _cachedData.size();
		synchronized (_cachedData) {
			if (!_cachedData.offer(data)) {
				Log.e(TAG,
						"The database buffer is overflowing, consider increasing DATA_TO_WRITE_DB");
				logger.error("The database buffer is overflowing, consider increasing DATA_TO_WRITE_DB.");
			} else {
				dataSize++;
			}
		}

		if (forceFlush || dataSize >= DATA_TO_WRITE_DB) {
			if (!_runningDump.getAndSet(true)) {
				asyncFlushData();
			} else {
				if (dataSize >= DATA_TO_WRITE_DB + DATA_TO_WRITE_DB / 10) {
					// print a warning every 100 overflowing pending
					// ContentValues
					_warningPrintCount = (_warningPrintCount + 1) % 100;
					if (_warningPrintCount == 1) {
						Log.w(TAG,
								"The database is busy and the buffer is full. Consider increasing DATA_TO_WRITE_DB");
						logger.warn("The database is busy and the buffer is full. Consider increasing DATA_TO_WRITE_DB.");
					}
				}
			}
		}
		// Log.d(TAG, "Storing data completed");
	}

	private void updateList() {
		synchronized (_cachedData) {
			_dataToDump.add(_cachedData);
			_cachedData = new LinkedBlockingQueue<ContentValues>();
		}
	}

	/**
	 * Flushes all currently buffered data to the database. This method
	 * <em>does not</em> require to acquire the DB lock beforehand.
	 */
	public void flushData() {
		Log.d(TAG, "Flushing MoST db.");
		logger.info("Flushing MoST db.");
		_runningDump.set(true);
		long start = System.currentTimeMillis();
		if (open() == null) {
			Log.e(TAG, "Unable to write data to db.");
			logger.error("Unable to write data to db.");
			_runningDump.set(false);
			return;
		}
		updateList();
		int linesCount = 0;
		_db.beginTransaction();
		try {
			LinkedBlockingQueue<ContentValues> list;
			while ((list = _dataToDump.poll()) != null) {
				for (ContentValues data : list) {
					String tableName = data.getAsString(FLD_TABLE);
					data.remove(FLD_TABLE);
					_db.insert(tableName, null, data);
					linesCount++;
				}
			}
			// commit on successful write
			_db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			_db.endTransaction();
			close();
			start = System.currentTimeMillis() - start;
			Log.i(TAG, "DB write time: " + start + "ms for " + linesCount
					+ " entries.");
			_runningDump.set(false);
		}
	}

	public void asyncFlushData() {

		Runnable task = new Runnable() {
			public void run() {
				flushData();
			}
		};
		new Thread(task, "MoST DB flush").start();
	}

	public List<ContentValues> getFIFOTuples(String table, int num) {
		Log.d(TAG, "Trying to get tuples from table " + table);
		if (open() == null) {
			Log.e(TAG, "Unable to open DB for getFIFOTuples");
			logger.error("Unable to open DB for getFIFOTuples");
			return new ArrayList<ContentValues>();
		}
		Cursor result = null;
		try {
			List<ContentValues> retVal = null;
			if (num <= 0) {
				result = _db.query(table, null, null, null, null, null, "_ID",
						null);
				retVal = new LinkedList<ContentValues>();
			} else {
				result = _db.query(table, null, null, null, null, null, "_ID",
						num + "");
				retVal = new ArrayList<ContentValues>(num);
			}
			ContentValues map;
			if (result.moveToFirst()) {
				do {
					map = new ContentValues();
					DatabaseUtils.cursorRowToContentValues(result, map);
					retVal.add(map);
				} while (result.moveToNext());
			}
			Log.d(TAG, "Got " + retVal.size() + "tuples from table " + table);
			return retVal;
		} catch (Exception e) {
			Log.e(TAG, "Exception in getFIFOTuples.", e);
			logger.error("Exception in getFIFOTuples.", e);
			return new ArrayList<ContentValues>();
		} finally {
			try {
				result.close();
			} catch (Exception e) {
				Log.e(TAG, "Exception closing cursor.", e);
				logger.error("Exception closing cursor.", e);
			}
			close();
		}
	}

	public int deleteTuples(String table, Collection<Long> ids) {
		Log.d(TAG, "Trying to delete tuples from table " + table);
		logger.info("Trying to delete tuples from table {}.", table);
		if (open() == null) {
			Log.e(TAG, "Unable to lock database to delete tuples");
			logger.error("Unable to lock database to delete tuples");
			return -1;
		}
		try {
			String args = StringUtils.join(ids, ",");
			int deleted = _db.delete(table, "_ID IN (" + args + ")", null);
			Log.i(TAG,
					String.format("IDs to delete: %d - IDS deleted: %d",
							ids.size(), deleted));
			logger.info("Successfully deleted {} tuples from table {}.",
					deleted, table);
			return deleted;
		} finally {
			close();
		}
	}

}
