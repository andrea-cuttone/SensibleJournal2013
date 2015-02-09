package dk.dtu.imm.sensible.components;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.rest.HttpUtils;
import dk.dtu.imm.sensible.rest.RestClientV2;

public class UILogger extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "sensible_journal.db";
	private static final String TABLE_UI_EVENTS = "UI_EVENTS";
	private static final String KEY_TIMESTAMP = "TIMESTAMP";
	private static final String KEY_EVENT = "EVENT";

	private static UILogger instance;

	private String userid;
	private long lastAttempt;
//	private NetworkInfo wifi;

	private UILogger(Context appContext) {
		super(appContext, DATABASE_NAME, null, DATABASE_VERSION);
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		userid = sharedPrefs.getString(RestClientV2.PREF_UID, null);
//		ConnectivityManager connManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//		wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		lastAttempt = 0;
	}

	public static UILogger instance(Context appContext) {
		if (instance == null) {
			instance = new UILogger(appContext);
		}
		return instance;
	}

	public void logEvent(String event) {
		Log.d("UILogger", event);
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		long now = System.currentTimeMillis();
		values.put(KEY_TIMESTAMP, now);
		values.put(KEY_EVENT, event);
		db.insert(TABLE_UI_EVENTS, null, values);
		db.close();
		// TODO: need new permission
		if (userid != null /*&& wifi.isConnected()*/ && System.currentTimeMillis() - lastAttempt > 60000 * 60) {
			lastAttempt = System.currentTimeMillis();
			new PostTask().execute();
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_UI_EVENTS + "(" + KEY_TIMESTAMP + " LONG PRIMARY KEY," + KEY_EVENT + " TEXT)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_UI_EVENTS);
		onCreate(db);
	}
	
	class PostTask extends AsyncTask<Void, Void, Void> {

		private static final String TOKEN = "MO5wFYSXdvyGNRRU";
		private static final String DATARECEIVER_URL = "http://sensiblejournal.appspot.com/datareceiver";

		@Override
		protected Void doInBackground(Void... params) {
			try {
				long latestTimestamp = getLatestTimestamp(userid);
				SQLiteDatabase db = getReadableDatabase();
				String sql = String.format("SELECT %s, %s FROM %s WHERE %s > %d ORDER BY %s LIMIT 1000",
						KEY_TIMESTAMP, KEY_EVENT, TABLE_UI_EVENTS, KEY_TIMESTAMP, latestTimestamp, KEY_TIMESTAMP);
				Cursor cursor = db.rawQuery(sql, null);
				cursor.moveToFirst();
				JsonArray ja = new JsonArray();
				while (!cursor.isAfterLast()) {
					long timestamp = cursor.getLong(0);
					String event = cursor.getString(1);
					JsonObject jo = new JsonObject();
					jo.addProperty("USERID", userid);
					jo.addProperty(KEY_TIMESTAMP, timestamp);
					jo.addProperty(KEY_EVENT, event);
					ja.add(jo);
					cursor.moveToNext();
				}
				cursor.close();
				db.close();
				postUsageData(ja);
				Log.d(Constants.APP_NAME, String.format("Posted %d usage stats", ja.size()));
			} catch (Exception e) {
				Log.e(Constants.APP_NAME, "error contacting datareceiver: " + e.toString());
				cancel(true);
			}
			return null;
		}

		private long getLatestTimestamp(String userid) throws Exception {
			try {
				String url = DATARECEIVER_URL + "?token={token}&userid={userid}";
				HttpHeaders requestHeaders = new HttpHeaders();
				HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
				RestTemplate restTemplate = new RestTemplate();
				restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
				ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class, TOKEN, userid);
				String result = responseEntity.getBody();
				return Long.parseLong(result);
			} catch (Exception e) {
				Log.e(Constants.APP_NAME, e.toString());
				throw e;
			}
		}

		private void postUsageData(JsonArray ja) throws Exception {
			String url = DATARECEIVER_URL;
			MultiValueMap<String, String> mvm = new LinkedMultiValueMap<String, String>();
			mvm.add("token", TOKEN);
			mvm.add("data", ja.toString());
			RestTemplate restTemplate = new RestTemplate();
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			messageConverters.add(new FormHttpMessageConverter());
			messageConverters.add(new StringHttpMessageConverter());
			restTemplate.setMessageConverters(messageConverters);
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpUtils.getNewHttpClient()));
			restTemplate.postForObject(url, mvm, String.class);
		}
	}

}
