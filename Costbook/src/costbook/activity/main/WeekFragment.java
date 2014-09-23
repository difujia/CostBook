package costbook.activity.main;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.costs.R;
import com.viewpagerindicator.TabPageIndicator;

import costbook.data.cost.Cost;
import costbook.data.cost.CostDatabase;
import costbook.data.currency.CurrencyManager;
import costbook.data.currency.CurrencyManager.Converter;

/**
 * This fragment contains a ViewPager where  7 DayFragments are shown.
 * This fragment updates itself on demand.
 */
public class WeekFragment extends Fragment {

	@SuppressWarnings("unused")
	private static final String	TAG				= "WeekFragment";
	private Date[]				week;
	private int					initialDayPosition;
	private boolean				isMiddle;

	private static final String	KEY_WEEK		= "week";
	private static final String	KEY_INITIAL		= "initial";
	private static final String	KEY_IS_MIDDLE	= "isMiddle";

	private TextView			weekRangeText;
	private TextView			weekSumText;
	private TextView			weekAvgText;
	private ViewPager			dayPager;
	private TabPageIndicator	dayPagerIndicator;

	private BroadcastReceiver	receiver;

	/*
	 * convenient factory
	 */
	public static WeekFragment newInstance(Date[] dates, int initialDay, boolean isMiddle) {
		WeekFragment wf = new WeekFragment();
		wf.week = dates;
		wf.initialDayPosition = initialDay;
		wf.isMiddle = isMiddle;
		return wf;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_week, container, false);
		weekRangeText = (TextView) rootView.findViewById(R.id.week_text_range);
		weekSumText = (TextView) rootView.findViewById(R.id.week_text_sum);
		weekAvgText = (TextView) rootView.findViewById(R.id.week_text_avg);
		dayPager = (ViewPager) rootView.findViewById(R.id.week_pager_day);
		dayPagerIndicator = (TabPageIndicator) rootView.findViewById(R.id.week_indicator_day);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_WEEK)) {
			initialDayPosition = savedInstanceState.getInt(KEY_INITIAL);
			isMiddle = savedInstanceState.getBoolean(KEY_IS_MIDDLE);
			long[] convertedWeek = savedInstanceState.getLongArray(KEY_WEEK);
			week = new Date[convertedWeek.length];
			for (int i = 0; i < convertedWeek.length; i++) {
				week[i] = new Date(convertedWeek[i]);
			}
//			Log.d(TAG, "week state restored");
		}
		
		dayPager.setAdapter(new DayPagerAdapter(getChildFragmentManager()));
		dayPagerIndicator.setViewPager(dayPager);
		dayPagerIndicator.setCurrentItem(initialDayPosition);
		updateUI();
//		setupLoader();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateUI();
		setupBroadcastReceiver();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		long[] convertedWeek = new long[week.length];
		for (int i = 0; i < week.length; i++) {
			convertedWeek[i] = week[i].getTime();
		}
		outState.putLongArray(KEY_WEEK, convertedWeek);
		outState.putInt(KEY_INITIAL, initialDayPosition);
		outState.putBoolean(KEY_IS_MIDDLE, isMiddle);
		super.onSaveInstanceState(outState);
	}

	/* Loader has problem working with ViewPager in support library. It's deprecated.
	private void setupLoader() {
		new Handler().post(new Runnable() {
			
			@Override
			public void run() {
				getLoaderManager().initLoader(0, null, WeekFragment.this);
			}
		});
	}
	*/
		
	/*
	 * update itself
	 */
	public void onNextWeek() {
		moveWeekByDays(7, 0);
	}

	public void onPrevWeek() {
		moveWeekByDays(-7, 6);
	}
	
	/*
	 * register broadcast receiver
	 */
	private void setupBroadcastReceiver() {
		receiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Date receivedDate = (Date) intent.getSerializableExtra(CostDatabase.CHANGED_ON_DATE);
				// update UI if changes happen in this week
				if (receivedDate.compareTo(week[0]) >= 0 && receivedDate.compareTo(week[6]) <= 0) {
					updateUI();
				}
			}
		};
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,	new IntentFilter(CostDatabase.DATA_DID_CHANGE));
	}

	/*
	 * utility used to move Date, this creates new Date array
	 */
	public static Date[] addDays(Date[] dates, int days) {
		int length = dates.length;
		Calendar cal = Calendar.getInstance();
		Date[] newDates = new Date[length];
		for (int i = 0; i < length; i++) {
			cal.setTime(dates[i]);
			cal.add(Calendar.DATE, days);
			newDates[i] = cal.getTime();
		}
		return newDates;
	}
	
	private void moveWeekByDays(int numOfDays, int newPos) {
		week = addDays(week, numOfDays);
		
		/*
		 * update DayFragments by creating new ones, this may be a bad idea
		 */
		dayPager.setAdapter(new DayPagerAdapter(getChildFragmentManager()));
		
		if (isMiddle) {
			dayPagerIndicator.setCurrentItem(newPos);
		} else {
			dayPagerIndicator.setCurrentItem(initialDayPosition);
		}
//		getLoaderManager().restartLoader(0, null, this);
		updateUI();
	}

	private void updateUI() {
		String range = DateUtils.formatDateRange(getActivity(), week[0].getTime(), week[6].getTime() + 1000,
				DateUtils.FORMAT_ABBREV_MONTH);
		weekRangeText.setText(range);

		/*
		 * update week statistics (week sum and past average)
		 */
		Converter converter = CurrencyManager.getInstance(getActivity()).getConverter();

		// sum all items of current week
		List<Cost> weekData = CostDatabase.getInstance(getActivity()).getCostsBetween(week[0], week[6]);
		double weekSum = 0;
		for (Cost c : weekData) {
			weekSum += converter.toPrimaryCurrency(c);
		}

		//TODO: add to preference
		// average the sum of past 4 weeks, this would be configurable
		Date[] pastWeek = addDays(week, -7);
		Date end = pastWeek[6];
		pastWeek = addDays(week, -7 * 4);
		Date start = pastWeek[0];
		List<Cost> monthData = CostDatabase.getInstance(getActivity()).getCostsBetween(start, end);
		double monthSum = 0;
		for (Cost c : monthData) {
			monthSum += converter.toPrimaryCurrency(c);
		}
		double average = monthSum / 4;
		
		// get primary currency code
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String primary = prefs.getString(getResources().getString(R.string.pref_key_primary_currency), null);
		// use localised currency symbol
		NumberFormat nf = NumberFormat.getCurrencyInstance();
		nf.setCurrency(Currency.getInstance(primary));
		weekSumText.setText(String.format("Week sum: %s", nf.format(weekSum)));
		weekAvgText.setText(String.format("Past average: %s", nf.format(average)));
	}
	
	/* loader callbacks, not being used
	@Override
	public Loader<List<Cost>> onCreateLoader(int arg0, Bundle arg1) {
		return new CostListLoader(getActivity(), week[0], week[6]);
	}

	@Override
	public void onLoadFinished(Loader<List<Cost>> loader, List<Cost> data) {
		double sum = 0;
		Converter converter = CurrencyManager.getInstance(getActivity()).getConverter();
		for (Cost c : data) {
			sum += converter.toPrimaryCurrency(c);
		}
		weekSumText.setText(String.valueOf(sum));
	}

	@Override
	public void onLoaderReset(Loader<List<Cost>> arg0) {}
	*/
	
	private class DayPagerAdapter extends FragmentStatePagerAdapter {

		public DayPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
//			Log.i(TAG, "create day page " + week[position].toString());
			return DayFragment.newInstance(week[position]);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			SimpleDateFormat sdf = new SimpleDateFormat("EE", Locale.getDefault());
			return sdf.format(week[position]);
		}

		@Override
		public int getCount() {
			return week.length;
		}
		
		@Override
		public int getItemPosition(Object object) {
			/*
			 * this doesn't update DayFragments properly, I don't know why.
			 */
			return POSITION_NONE;
		}
	}
}
