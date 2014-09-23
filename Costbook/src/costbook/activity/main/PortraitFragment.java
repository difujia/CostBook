package costbook.activity.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.costs.R;

import costbook.activity.settings.SettingsActivity;

/**
 * Used as main screen in portrait mode. This class initialises the calendar to midnight of
 * current day, create and assign required dates to current and neighbour WeekFragment.
 */
public class PortraitFragment extends Fragment {

	@SuppressWarnings("unused")
	private static final String		TAG	= "PortFragment";

	private Date					today;

	private ViewPager				weekPager;
	private InfinitePagerAdapter	weekPagerAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_port, container, false);	
		weekPager = (ViewPager) rootView.findViewById(R.id.port_pager_week);
		weekPager.setOffscreenPageLimit(2);
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// load action bar menu
		setHasOptionsMenu(true);
		// create dates of current week
		Calendar cal = new GregorianCalendar();
		// set time to the start of the day
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		// cal.setFirstDayOfWeek(Calendar.MONDAY);
		today = cal.getTime();
		cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
		Date[] currentWeek = new Date[7];
		for (int i = 0; i < currentWeek.length; i++) {
			currentWeek[i] = cal.getTime();
			cal.add(Calendar.DAY_OF_WEEK, 1);
		}
		ArrayList<Date[]> weekList = new ArrayList<Date[]>(3);
		weekList.add(WeekFragment.addDays(currentWeek, -7));
		weekList.add(currentWeek);
		weekList.add(WeekFragment.addDays(currentWeek, 7));
		weekPagerAdapter = new InfinitePagerAdapter(getChildFragmentManager(), weekList);
		weekPager.setAdapter(weekPagerAdapter);
		weekPager.setOnPageChangeListener(weekPagerAdapter);
		weekPager.setCurrentItem(InfinitePagerAdapter.PAGE_MIDDLE, false);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.main, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			startActivity(new Intent(getActivity(), SettingsActivity.class));
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * This adapter manages three WeekFragments, listens to page change events, asks WeekFragment to
	 * update themselves, and move these three pages to fake "infinity"
	 */
	private class InfinitePagerAdapter extends FragmentPagerAdapter implements OnPageChangeListener {

		private static final int	PAGE_MIDDLE			= 1;
		private int					selectedPageIndex	= 1;
		ArrayList<Date[]>			weekList;
		WeekFragment[]				fragList;

		/**
		 * @param fm Need childFragmentManager
		 * @param weekList List of 3 Date arrays, each array contains 7 days.
		 */
		public InfinitePagerAdapter(FragmentManager fm, ArrayList<Date[]> weekList) {
			super(fm);
			this.weekList = weekList;
			this.fragList = new WeekFragment[weekList.size()];
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return WeekFragment.newInstance(weekList.get(position), 6, false);
			case 1: {
				Date[] currentWeek = weekList.get(position);
				int initial = Arrays.asList(currentWeek).indexOf(today);
				return WeekFragment.newInstance(currentWeek, initial, true);
			}
			case 2:
				return WeekFragment.newInstance(weekList.get(position), 0, false);
			default:
				return null;
			}
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			/*
			 * hold references to instantiated fragments
			 */
			fragList[position] = (WeekFragment) super.instantiateItem(container, position);
			return fragList[position];
		}

		@Override
		public int getCount() {
			return weekList.size();
		}

		// onPageChangeListener methods
		@Override
		public void onPageScrollStateChanged(int arg0) {
			/*
			 * ask WeekFragment to update when pages have been to idle state
			 */
			if (arg0 == ViewPager.SCROLL_STATE_IDLE) {
				if (selectedPageIndex < PAGE_MIDDLE) {
					fragList[0].onPrevWeek();
					fragList[1].onPrevWeek();
					fragList[2].onPrevWeek();
				} else if (selectedPageIndex > PAGE_MIDDLE) {
					fragList[0].onNextWeek();
					fragList[1].onNextWeek();
					fragList[2].onNextWeek();
				}
				weekPager.setCurrentItem(PAGE_MIDDLE, false);
			}
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {}

		@Override
		public void onPageSelected(int arg0) {
			selectedPageIndex = arg0;
		}
	}
}
