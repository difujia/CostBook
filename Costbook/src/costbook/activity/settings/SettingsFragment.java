package costbook.activity.settings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.costs.R;

import costbook.data.currency.CurrencyUpdateService;

/**
 * Listen to preference changes and do appropriate job. <br>
 * These include adjust certain preference according to another one, 
 * and setup alarm for everyday notification.
 */
public class SettingsFragment extends PreferenceFragment {

	private static final String			TAG				= "SettingsFragment";

	private ListPreference				primaryCurrencyP;
	private MultiSelectListPreference	currencyInUseP;
	private CheckBoxPreference			alarmSwitchP;
	private TimePreference				alarmTimeP;
	
	private SharedPreferences 			defaultSP;
	
	/*
	 * receive currency update event for real-time update if the fragment is on screen
	 */
	private BroadcastReceiver			receiver;
	
	private TextView 					footer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		defaultSP = PreferenceManager.getDefaultSharedPreferences(getActivity());

		primaryCurrencyP = (ListPreference) findPreference(getString(R.string.pref_key_primary_currency));
		currencyInUseP = (MultiSelectListPreference) findPreference(getString(R.string.pref_key_currencies_in_use));
		alarmSwitchP = (CheckBoxPreference) findPreference(getString(R.string.pref_key_alarm_switch));
		alarmTimeP = (TimePreference) findPreference(getString(R.string.pref_key_alarm_time));

		primaryCurrencyP.setOnPreferenceChangeListener(new PrimaryCurrencyChangeListener());
		currencyInUseP.setOnPreferenceChangeListener(new CurrencyInUseChangeListener());
		alarmSwitchP.setOnPreferenceChangeListener(new AlarmSwitchChangeListener());
		alarmTimeP.setOnPreferenceChangeListener(new AlarmTimeChangeListener());
		adjustPreferences(currencyInUseP.getValues());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate a layout containing footer
		View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
		footer = (TextView) rootView.findViewById(R.id.settings_footer);
		return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		setupReceiverForFooter();
		updateFooter();
	}
	
	@Override
	public void onPause() {
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
		super.onPause();
	}

	/*
	 * a couple of listeners to preference changes
	 */
	private class PrimaryCurrencyChangeListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			String newPrimary = (String) newValue;
			
			/*
			 * update summary
			 */
			primaryCurrencyP.setSummary(newPrimary);
			return true;
		}

	}

	/*
	 * CurrencyInUse preference doesn't allow empty selection.
	 */
	private class CurrencyInUseChangeListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			@SuppressWarnings("unchecked")
			Set<String> currencyInUse = (Set<String>) newValue;

			/*
			 * if newValue is empty, show error dialog
			 */
			if (currencyInUse.isEmpty()) {

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(getString(R.string.settings_error_empty_selection))
						.setIcon(R.drawable.ic_action_error)
						.setPositiveButton(R.string.settings_discard, new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int arg1) {
								dialog.dismiss();
							}
						}).show();
				return false;
			}

			// if not empty
			adjustPreferences(currencyInUse);
			return true;
		}

	}

	/*
	 * setup/stop alarm for everyday notification
	 */
	private class AlarmSwitchChangeListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if ((Boolean) newValue) {
				// alarm is on
				long time = defaultSP.getLong(alarmTimeP.getKey(), 0);
				Calendar cal = new GregorianCalendar();
				cal.setTimeInMillis(time);
				scheduleAlarmEveryday(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
			} else {
				// alarm is off
				stopAlarmEveryday();
			}
			return true;
		}
	}
	
	/*
	 * reschedule on new alarm time
	 */
	private class AlarmTimeChangeListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			long newTime = (Long) newValue;
			Calendar cal = new GregorianCalendar();
			cal.setTimeInMillis(newTime);
			scheduleAlarmEveryday(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
			return true;
		}
		
	}

	/*
	 * helper methods
	 */
	private void adjustPreferences(Set<String> currencyInUse) {
		
		/*
		 * change primary currency entries according to currencyInUse
		 */
		
		// first reverse the Set for consistent order
		List<String> toReverse = new ArrayList<String>(currencyInUse);
		Collections.reverse(toReverse);

		String[] entries = toReverse.toArray(new String[currencyInUse.size()]);
		primaryCurrencyP.setEntries(entries);
		primaryCurrencyP.setEntryValues(entries);
		String oldPrimary = primaryCurrencyP.getValue();
		if (!currencyInUse.contains(oldPrimary)) primaryCurrencyP.setValue(entries[0]);

		/*
		 * update summary
		 */
		primaryCurrencyP.setSummary(primaryCurrencyP.getValue());
		currencyInUseP.setSummary(currencyInUse.toString());
	}

	private void scheduleAlarmEveryday(int hour, int minute) {
		Intent toReceiver = new Intent(getActivity(), EverydayAlarmReceiver.class);
		PendingIntent pIntent = PendingIntent.getBroadcast(getActivity(), 0, toReceiver,
				PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarm = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
		Calendar cal = new GregorianCalendar();
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pIntent);
		Log.i(TAG, "schedule alarm at " + cal.getTime().toString());
	}

	private void stopAlarmEveryday() {
		Intent toReceiver = new Intent(getActivity(), EverydayAlarmReceiver.class);
		PendingIntent pIntent = PendingIntent.getBroadcast(getActivity(), 0, toReceiver,
				PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarm = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pIntent);
		Log.i(TAG, "alarm stoped");
	}
	
	/*
	 * read time stamp in SharedPreference, show time elapse at the bottom of screen.
	 */
	private void updateFooter() {
		long timestamp = defaultSP.getLong(getString(R.string.pref_key_currency_update_timestamp), 0);
		String info = getString(R.string.settings_update_info) + " ";
		if (timestamp > 0) {
			CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(timestamp,
										System.currentTimeMillis(), 
										DateUtils.MINUTE_IN_MILLIS, 
										DateUtils.FORMAT_ABBREV_RELATIVE);
			info += relativeTime;
		} else {
			info += getString(R.string.settings_never);
		}
		footer.setText(info);
	}
	
	/*
	 * listen to CurrencyUpdateService broadcast,
	 * this allows real-time update in case Settings are on screen.
	 */
	private void setupReceiverForFooter() {
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateFooter();
			}
		};
		IntentFilter filter = new IntentFilter(CurrencyUpdateService.UPDATE_BROADCAST);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
	}

}
