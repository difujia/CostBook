package costbook.data.currency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.costs.R;

/**
 * One-shot service for updating currency database. This is asynchronous.
 */
public class CurrencyUpdateService extends IntentService {
	
	/*
	 * broadcast key
	 */
	public static final String	UPDATE_BROADCAST	= "com.costs.currencydatabase.CurrencyUpdateService.UPDATED";

	private static final String	TAG					= "CurrencyUpdateService";
	private static final String APP_ID				= "4f4ffe2d617e45619695cc7248255268";
	private static final String	API					= "http://openexchangerates.org/api/latest.json?app_id=" + APP_ID;

	public CurrencyUpdateService() {
		super("CurrencyUpdateService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = connMgr.getActiveNetworkInfo();
		if (info != null && info.isConnected()) {
			String json = fetchJSONString();
			if (json.length() > 0) {
				updateWithJSON(json);
			}
		} else {
			Log.w(TAG, "no internet");
		}
	}

	/*
	 * load data from network
	 */
	private String fetchJSONString() {
		Log.i(TAG, "start fetching");
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(API);

		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine status = response.getStatusLine();
			// OK
			if (status.getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.w(TAG, "http status " + status.getStatusCode());
			}
		} catch (IOException e) {
			Log.w(TAG, e.toString());
		} finally {
			Log.i(TAG, "finish fetching");
		}
		return builder.toString();
	}

	/*
	 * parse json and update database
	 */
	private void updateWithJSON(String json) {
		int update = 0;
		int addition = 0;
		try {
			JSONObject jsonRoot = new JSONObject(json);
			long timestamp = jsonRoot.getLong("timestamp");
			Log.i(TAG, "timestamp " + DateUtils.formatDateTime(this, timestamp, DateUtils.FORMAT_SHOW_TIME));
			JSONObject currencies = jsonRoot.getJSONObject("rates");
			Log.i(TAG, "num of entries " + currencies.length());
			Iterator<?> iterator = currencies.keys();
			CurrencyManager database = CurrencyManager.getInstance(this);
			while (iterator.hasNext()) {
				String country = (String) iterator.next();
				double rate = currencies.getDouble(country);
				int result = database.updateRateByCode(country, rate);
				// log update results
				if (result == 0) {
					addition++;
				} else {
					update += result;
				}
			}
			Log.i(TAG, "update " + update);
			Log.i(TAG, "addition " + addition);

			logAndNotify(timestamp);
		} catch (JSONException e) {
			Log.w(TAG, "failed to parse json string");
		}
	}

	/*
	 * save time stamp in SharedPreferences, send broadcast to notify any interested
	 */
	private void logAndNotify(long timestamp) {
		// save to SharedPreference
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		// API use seconds as timestamp unit
		sp.edit().putLong(getString(R.string.pref_key_currency_update_timestamp), timestamp * 1000).apply();
		// send broadcast
		Intent intent = new Intent(UPDATE_BROADCAST);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}

}
