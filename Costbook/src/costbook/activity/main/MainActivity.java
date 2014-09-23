package costbook.activity.main;

import java.util.Arrays;
import java.util.Currency;
import java.util.HashSet;
import java.util.Locale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.costs.R;

/**
 * This class only does important initialisation.
 * All other functions are delegated to fragments.
 */
public class MainActivity extends FragmentActivity {
	private static final String	TAG				= "MainActivity";
	private static final String	ONE_TIME_FIRE	= "com.costs.main.ONE_TIME_FIRE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/*
		 * initialisations
		 */
		setupDefaultPreference();
		setupAlarmForCurrencyUpdate();
	}

	/*
	 * schedule alarm for the first launch
	 */
	private void setupAlarmForCurrencyUpdate() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		if (sp.getBoolean(ONE_TIME_FIRE, true)) {
			sendBroadcast(new Intent(ONE_TIME_FIRE));
			Editor editor = sp.edit();
			editor.putBoolean(ONE_TIME_FIRE, false).apply();
			Log.i(TAG, "scheduled recursive currency update");
		}
	}

	/*
	 * set default value for SharedPreferences, also set default currency based on Locale
	 */
	private void setupDefaultPreference() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		if (!sp.contains(getString(R.string.pref_key_primary_currency))) {
			Log.i(TAG, "set default!");
			PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

			// try to set default primary currency depending on Locale
			String localCurrency = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
			String[] availableCurrencies = getResources().getStringArray(R.array.currency_codes);

			// if local currency is in currency list, set it. Otherwise leave the default
			if (Arrays.asList(availableCurrencies).contains(localCurrency)) {
				HashSet<String> currencyInUse = new HashSet<String>();
				currencyInUse.add(localCurrency);

				SharedPreferences.Editor editor = sp.edit();
				editor.putString(getString(R.string.pref_key_primary_currency), localCurrency)
						.putStringSet(getString(R.string.pref_key_currencies_in_use), currencyInUse).apply();
				Log.i(TAG, "set to local currency " + localCurrency);
			}
		}
	}
}
