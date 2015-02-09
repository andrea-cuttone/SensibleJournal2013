package dk.dtu.imm.sensible.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import dk.dtu.imm.sensible.Constants;

public class CacheManager extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 4;
	private static final String DATABASE_NAME = "sensible_journal_cache.db";
	//private static final String DATABASE_NAME = "/storage/sdcard0/bt_generated.db";

	private static final String TABLE_LOCS = "LOCS";
	private static final String KEY_TIMESTAMP = "TIMESTAMP";
	private static final String KEY_LAT = "LAT";
	private static final String KEY_LON = "LON";
	private static final String KEY_ACC = "ACC";
	
	private static final String TABLE_BT = "BT";
	private static final String KEY_SENSIBLE_USER_ID = "KEY_SENSIBLE_USER_ID";
	
	private static final String TABLE_CACHED_LOCS_WEEKS = "CACHED_LOCS_WEEKS";
	private static final String TABLE_CACHED_BT_WEEKS = "CACHED_BT_WEEKS";
	private static final String KEY_WEEK_START = "WEEK_START";
	private static final String KEY_LAST_CHECKED = "LAST_CHECKED";
	
	
	public CacheManager(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	private void dumpDB() {
		File f = new File("/data/data/dk.dtu.imm.sensiblejournal/databases/sensible_journal_cache.db");
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(f);
			fos = new FileOutputStream("/mnt/sdcard/sensible_journal_cache.db");
			while (true) {
				int i = fis.read();
				if (i != -1) {
					fos.write(i);
				} else {
					break;
				}
			}
			fos.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fos.close();
				fis.close();
			} catch (IOException ioe) {
			}
		}
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_LOCS + "(" + KEY_TIMESTAMP + " LONG PRIMARY KEY, " 
				+ KEY_LAT + " FLOAT, " + KEY_LON + " FLOAT, "  + KEY_ACC + " FLOAT)");
		db.execSQL("CREATE TABLE " + TABLE_CACHED_LOCS_WEEKS + "(" + KEY_WEEK_START + " LONG PRIMARY KEY, " 
				+ KEY_LAST_CHECKED + " LONG)");
		db.execSQL("CREATE TABLE " + TABLE_BT + "(" + KEY_TIMESTAMP + " LONG, " 
				+ KEY_SENSIBLE_USER_ID + " TEXT, PRIMARY KEY(" + KEY_TIMESTAMP + ", " + KEY_SENSIBLE_USER_ID + "))");
		db.execSQL("CREATE TABLE " + TABLE_CACHED_BT_WEEKS + "(" + KEY_WEEK_START + " LONG PRIMARY KEY, " 
				+ KEY_LAST_CHECKED + " LONG)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHED_LOCS_WEEKS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_BT);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHED_BT_WEEKS);
		onCreate(db);
	}
	
	public List<LocationResponseEntity> retrieveLocations(long from, long to) {
		SQLiteDatabase db = this.getReadableDatabase();
	    Cursor cursor = db.query(TABLE_LOCS, new String [] {KEY_TIMESTAMP, KEY_LAT, KEY_LON, KEY_ACC}, 
	    		KEY_TIMESTAMP + ">= ? AND " + KEY_TIMESTAMP + " <= ?", 
	    		new String[] { Long.toString(from), Long.toString(to) }, null, null, KEY_TIMESTAMP);
	    List<LocationResponseEntity> locs = new ArrayList<LocationResponseEntity>();
	    cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	      long timestamp = cursor.getLong(0);
	      float lat = cursor.getFloat(1);
	      float lon = cursor.getFloat(2);
	      float acc = cursor.getFloat(3);
	      locs.add(new LocationResponseEntity(timestamp, lat, lon, acc));
	      cursor.moveToNext();
	    }
	    cursor.close();
	    return locs;
	}
	
	public void storeLocations(LocationResponseEntity[] locs) {
		if(locs.length == 0) {
			return;
		}
		Log.d(Constants.APP_NAME, "starting caching " + locs.length + " locs...");
		ContentValues cv = new ContentValues();
		SQLiteDatabase db = this.getWritableDatabase();
		db.beginTransaction();
		try {

			for (int i = 0; i < locs.length; i++) {
				db.delete(TABLE_LOCS, KEY_TIMESTAMP + " = ?", new String [] {Long.toString(locs[i].getTime())});
				cv.put(KEY_TIMESTAMP, locs[i].getTime());
				cv.put(KEY_LAT, locs[i].getLat());
				cv.put(KEY_LON, locs[i].getLon());
				cv.put(KEY_ACC, locs[i].getAcc());
				db.insert(TABLE_LOCS, null, cv);
			}
			db.setTransactionSuccessful();
		} catch (SQLiteException exc) {
			Log.e(Constants.APP_NAME, exc.getMessage());
			throw exc;
		} finally {
			db.endTransaction();
		}
		Log.d(Constants.APP_NAME, "done caching " + locs.length + " locs");
	}
	
	public long getLocationsLastChecked(long weekStart) {
		SQLiteDatabase db = this.getReadableDatabase();
	    Cursor cursor = db.query(TABLE_CACHED_LOCS_WEEKS, new String [] {KEY_LAST_CHECKED}, 
	    		KEY_WEEK_START + "= ?", new String[] { Long.toString(weekStart) }, null, null, null);
	    
	    long lastChecked = 0;
	    if(cursor.getCount() == 1) {
	    	cursor.moveToFirst();
	    	lastChecked = cursor.getLong(0);
	    }
	    cursor.close();
	    return lastChecked;
	}
	
	public void updateLocationsLastChecked(long weekStart) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.beginTransaction();
		try {
			db.delete(TABLE_CACHED_LOCS_WEEKS, KEY_WEEK_START + " = ?", new String [] {Long.toString(weekStart)});
			ContentValues cv = new ContentValues();
			cv.put(KEY_WEEK_START, weekStart);
			cv.put(KEY_LAST_CHECKED, System.currentTimeMillis());
			db.insert(TABLE_CACHED_LOCS_WEEKS, null, cv);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	public List<BluetoothResponseEntity> retrieveBluetooth(long from, long to) {
		SQLiteDatabase db = this.getReadableDatabase();
	    Cursor cursor = db.query(TABLE_BT, new String [] {KEY_TIMESTAMP, KEY_SENSIBLE_USER_ID}, 
	    		KEY_TIMESTAMP + ">= ? AND " + KEY_TIMESTAMP + " <= ?", 
	    		new String[] { Long.toString(from), Long.toString(to) }, null, null, KEY_TIMESTAMP);
	    List<BluetoothResponseEntity> bt = new ArrayList<BluetoothResponseEntity>();
	    cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	      long timestamp = cursor.getLong(0);
	      String uid = cursor.getString(1);
	      bt.add(new BluetoothResponseEntity(timestamp, uid));
	      cursor.moveToNext();
	    }
	    cursor.close();
	    return bt;
	}
	
	public void storeBluetooth(BluetoothResponseEntity[] bt) {
		if(bt.length == 0) {
			return;
		}
		Log.d(Constants.APP_NAME, "starting caching " + bt.length + " bt...");
		ContentValues cv = new ContentValues();
		SQLiteDatabase db = this.getWritableDatabase();
		long cnt = 0;
		db.beginTransaction();
		try {
			for (int i = 0; i < bt.length; i++) {
				for (int j = 0; j < bt[i].devices.length; j++) {
					String sensible_user_id = bt[i].devices[j].sensible_user_id;
					if(sensible_user_id != null) {
						db.delete(TABLE_BT, KEY_TIMESTAMP + " = ? AND " + KEY_SENSIBLE_USER_ID + " = ?", 
								new String [] {Long.toString(bt[i].timestamp), sensible_user_id });
						cv.put(KEY_TIMESTAMP, bt[i].timestamp);
						cv.put(KEY_SENSIBLE_USER_ID, sensible_user_id);
						db.insert(TABLE_BT, null, cv);
						cnt++;
					}
				}
			}
			db.setTransactionSuccessful();
		} catch (SQLiteException exc) {
			Log.e(Constants.APP_NAME, exc.getMessage());
			throw exc;
		} finally {
			db.endTransaction();
		}
		Log.d(Constants.APP_NAME, "done caching " + cnt + " BT");
	}
	
	public long getBluetoothLastChecked(long weekStart) {
		SQLiteDatabase db = this.getReadableDatabase();
	    Cursor cursor = db.query(TABLE_CACHED_BT_WEEKS, new String [] {KEY_LAST_CHECKED}, 
	    		KEY_WEEK_START + "= ?", new String[] { Long.toString(weekStart) }, null, null, null);
	    
	    long lastChecked = 0;
	    if(cursor.getCount() == 1) {
	    	cursor.moveToFirst();
	    	lastChecked = cursor.getLong(0);
	    }
	    cursor.close();
	    return lastChecked;
	}

	public void updateBluetoothLastChecked(long weekStart) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.beginTransaction();
		try {
			db.delete(TABLE_CACHED_BT_WEEKS, KEY_WEEK_START + " = ?", new String [] {Long.toString(weekStart)});
			ContentValues cv = new ContentValues();
			cv.put(KEY_WEEK_START, weekStart);
			cv.put(KEY_LAST_CHECKED, System.currentTimeMillis());
			db.insert(TABLE_CACHED_BT_WEEKS, null, cv);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

}
