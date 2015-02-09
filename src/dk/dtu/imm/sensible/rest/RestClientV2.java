package dk.dtu.imm.sensible.rest;

import hirondelle.date4j.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Cipher;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.movement.LocationProcessor;
import dk.dtu.imm.sensible.utils.DateTimeUtils;
import dk.dtu.imm.sensible.utils.MathUtils;
import dk.dtu.imm.sensiblejournal.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources.NotFoundException;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

public class RestClientV2 {
	
	public static final String PREF_UID = "PREF_UID";
	private static final String BASE_URL = "https://curie.imm.dtu.dk/sensible_outbound/v1/";
	private static final String PREF_TOKEN_KEY = "TOKEN";
	private static final long MS_REQUEST_PERIOD = 1000 * Constants.SECS_ONE_DAY;

	private static RestClientV2 instance = null;
	
	private String token;
	private Context appContext;
	private CacheManager cacheManager;
	private Map<String, PublicProfileResponseEntity> publicProfileCache;
	private String[] names;
	
	private RestClientV2(Context appContext) {
		this.appContext = appContext; 
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		token = sharedPrefs.getString(PREF_TOKEN_KEY, "");
		cacheManager = new CacheManager(appContext);
		publicProfileCache = new HashMap<String, PublicProfileResponseEntity>();
		/*try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(appContext.getResources().openRawResource(R.raw.random_names)));
			names = new String[150];
			String line = reader.readLine();
			int c = 0;
			while (line != null) { 
				names[c++] = line;
				line = reader.readLine();
			}
		} catch (Exception e) {
			Log.e(Constants.APP_NAME, e.toString());
		}*/
	}
	
	public String getPublicNickname(String uid) {
		if(publicProfileCache.containsKey(uid)) {
			String public_nickname = publicProfileCache.get(uid).public_nickname;
			return public_nickname != null ? public_nickname : "";
		}
		return "";
		//return uid == null ? "" : names[Math.abs(uid.hashCode()) % names.length];
	}
		
	public PublicProfileResponseEntity fetchPublicProfile(String uid) throws Exception {
		Log.d(Constants.APP_NAME, String.format("getPublicProfile for %s", uid));
		synchronized(publicProfileCache) {
			if(publicProfileCache.containsKey(uid)) {
				return publicProfileCache.get(uid);
			}
		}
			
		try {
			String url = BASE_URL + "public_profile/?token={token}&friend_id={uid}";
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.setAccept(Collections.singletonList(new MediaType("application", "json")));
			HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpUtils.getNewHttpClient()));
			restTemplate.getMessageConverters().add(new GsonHttpMessageConverter());
			ResponseEntity<PublicProfileResponseEntity> responseEntity = 
					restTemplate.exchange(url, HttpMethod.GET, requestEntity,
							PublicProfileResponseEntity.class, token, uid);
			PublicProfileResponseEntity e = responseEntity.getBody();
			synchronized (publicProfileCache) {
				publicProfileCache.put(uid, e);
			}
			return e;
		} catch (RestClientException e) {
			Log.e(Constants.APP_NAME, e.toString());
			throw new Exception(e);
		}
	}

	public List<LocationResponseEntity> getLocations(DateTime date) throws Exception {
		return getLocations(date, date);
	}
	
	public List<LocationResponseEntity> getLocations(DateTime start, DateTime end) throws Exception {
		return getLocations(DateTimeUtils.toTimestamp(start.getStartOfDay()), 
				DateTimeUtils.toTimestamp(end.getEndOfDay()));
	}
	
	public List<BluetoothResponseEntity> getBluetooth(long start, long end) throws Exception {
		if (start > end) {
			throw new IllegalArgumentException("start must be >= end");
		}
		Log.d(Constants.APP_NAME, String.format("getBluetooth from %s to %s", 
				DateTimeUtils.timestampToDateTime(start), DateTimeUtils.timestampToDateTime(end)));
		try {
			long startWeek = (start / Constants.SECS_ONE_WEEK) * Constants.SECS_ONE_WEEK;
			long endWeek = (1 + end / Constants.SECS_ONE_WEEK) * Constants.SECS_ONE_WEEK;
			if(start < startWeek || end > endWeek + Constants.SECS_ONE_WEEK) {
				throw new AssertionError("error in week bucketing!"); 
			}
			String url = BASE_URL + "bluetooth/?token={token}&start={start}&end={end}";
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.setAccept(Collections.singletonList(new MediaType("application", "json")));
			HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpUtils.getNewHttpClient()));
			restTemplate.getMessageConverters().add(new GsonHttpMessageConverter());
			for (long t = startWeek; t < endWeek; t += Constants.SECS_ONE_WEEK) {
				if (System.currentTimeMillis() - cacheManager.getBluetoothLastChecked(t) > MS_REQUEST_PERIOD) {
					ResponseEntity<BluetoothResponseEntity[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
							BluetoothResponseEntity[].class, token, t, t + Constants.SECS_ONE_WEEK);
					BluetoothResponseEntity[] newBtFromServer = responseEntity.getBody();
					Log.d(Constants.APP_NAME, "received " + newBtFromServer.length + " from server");
					cacheManager.storeBluetooth(newBtFromServer);
					cacheManager.updateBluetoothLastChecked(t);
				}
			}
		} catch (RestClientException e) {
			Log.e(Constants.APP_NAME, e.toString());
			throw new Exception(e);
		}
		List<BluetoothResponseEntity> bt = cacheManager.retrieveBluetooth(start, end);
		Log.d(Constants.APP_NAME, "retrieved " + bt.size() + " from cache");
		return bt;
	}
	
	public HardwareResponseEntity getHardware() throws Exception {
		try {
			String url = BASE_URL + "hardware/?token={token}&limit=1";
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.setAccept(Collections.singletonList(new MediaType("application", "json")));
			HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpUtils.getNewHttpClient()));
			restTemplate.getMessageConverters().add(new GsonHttpMessageConverter());
			ResponseEntity<HardwareResponseEntity[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, 
					requestEntity, HardwareResponseEntity[].class, token);
			HardwareResponseEntity[] e = responseEntity.getBody();
			return e.length == 1 ? e[0] : null;
		} catch (RestClientException e) {
			Log.e(Constants.APP_NAME, e.toString());
			throw new Exception(e);
		}
	}

	public List<LocationResponseEntity> getLocations(long start, long end) throws Exception {
		if (start > end) {
			throw new IllegalArgumentException("start must be >= end");
		}
		Log.d(Constants.APP_NAME, String.format("getLocations from %s to %s", 
				DateTimeUtils.timestampToDateTime(start), DateTimeUtils.timestampToDateTime(end)));
		try {
			long startWeek = (start / Constants.SECS_ONE_WEEK) * Constants.SECS_ONE_WEEK;
			long endWeek = (1 + end / Constants.SECS_ONE_WEEK) * Constants.SECS_ONE_WEEK;
			if(start < startWeek || end > endWeek + Constants.SECS_ONE_WEEK) {
				throw new AssertionError("error in week bucketing!");
			}
			String url = BASE_URL + "location/?token={token}&start={start}&end={end}";
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.setAccept(Collections.singletonList(new MediaType("application", "json")));
			HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpUtils.getNewHttpClient()));
			restTemplate.getMessageConverters().add(new GsonHttpMessageConverter());
			for (long t = startWeek; t < endWeek; t += Constants.SECS_ONE_WEEK) {
				if (System.currentTimeMillis() - cacheManager.getLocationsLastChecked(t) > MS_REQUEST_PERIOD) {
					ResponseEntity<LocationResponseEntity[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
							LocationResponseEntity[].class, token, startWeek, endWeek);
					LocationResponseEntity[] newLocsFromServer = responseEntity.getBody();
					Log.d(Constants.APP_NAME, "received " + newLocsFromServer.length + " from server");
					cacheManager.storeLocations(newLocsFromServer);
					cacheManager.updateLocationsLastChecked(t);
				}
			}
		} catch (RestClientException e) {
			Log.e(Constants.APP_NAME, e.toString());
			throw new Exception(e);
		}
		List<LocationResponseEntity> locs = cacheManager.retrieveLocations(start, end);
		Log.d(Constants.APP_NAME, "retrieved " + locs.size() + " from cache");
		locs = LocationProcessor.process(locs);
		return locs;
	}
	
	public static RestClientV2 instance(Context appContext) {
		if(instance == null) {
			instance = new RestClientV2(appContext);
		}
		return instance;
	}

	public boolean doLogin(String username, String password) {
		boolean success = false;
		try {
			BigInteger m = new BigInteger("");
			BigInteger e = new BigInteger("");
			RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(m, e);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PublicKey pubKey = fact.generatePublic(publicKeySpec);
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			byte[] bytes = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", username, password).getBytes();
			byte[] cipherData = cipher.doFinal(bytes);
			String base64 = Base64.encodeToString(cipherData, Base64.NO_WRAP);

			String url = "https://curie.imm.dtu.dk/sensible_outbound/auth/mobile/";
			MultiValueMap<String, String> mvm = new LinkedMultiValueMap<String, String>();
			mvm.add("data", base64);
			RestTemplate restTemplate = new RestTemplate();
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			messageConverters.add(new FormHttpMessageConverter());
			messageConverters.add(new StringHttpMessageConverter());
			restTemplate.setMessageConverters(messageConverters);
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpUtils.getNewHttpClient()));
			String response = restTemplate.postForObject(url, mvm, String.class);
			JSONObject jsonObject = new JSONObject(response);
			if(jsonObject.has("token")) {
				token = jsonObject.getString("token");
				HardwareResponseEntity hardware = null;
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext);
				if(sharedPrefs.contains(PREF_UID) == false) {
					try {
						hardware = getHardware();
					} catch (Exception exc) {
						Log.e(Constants.APP_NAME, "Error retrieving hardware info: " + exc.toString());
					}
				}
				Editor editor = sharedPrefs.edit();
				editor.putString(PREF_TOKEN_KEY, token);
				if(hardware != null && hardware.uid != null) {
					editor.putString(PREF_UID, hardware.uid);
				}
				editor.commit();
				success = true;
			} else {
				Log.e(Constants.APP_NAME, "Failed login: " + jsonObject.getString("error"));
			}
		} catch (Exception e) {
			Log.e(Constants.APP_NAME, e.toString());
		}
		return success;
	}

	public boolean hasValidToken() {
		if(token == null || token.length() == 0) {
			return false;
		}
		boolean valid = true;
		try {
			String url = "https://curie.imm.dtu.dk/sensible_outbound/auth/mobile/validate_token/?token={token}";
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.setAccept(Collections.singletonList(new MediaType("application", "json")));
			HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpUtils.getNewHttpClient()));
			ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class, token);
			String result = responseEntity.getBody();
			JSONObject jsonObject = new JSONObject(result);
			valid = jsonObject.getBoolean("valid");
		} catch (Exception e) {
			Log.e(Constants.APP_NAME, e.toString());
		}
		return valid;
	}
}
