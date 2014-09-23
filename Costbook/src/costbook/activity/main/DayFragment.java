package costbook.activity.main;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.costs.R;

import costbook.activity.main.PullToFireListView.OnFireListener;
import costbook.activity.recorder.RecorderActivity;
import costbook.data.cost.Cost;
import costbook.data.cost.CostDatabase;
import costbook.data.currency.CurrencyManager;
import costbook.data.currency.CurrencyManager.Converter;

/**
 * Show by ViewPager. This fragment needs a single Date object to run.
 */
public class DayFragment extends ListFragment implements OnFireListener {

	@SuppressWarnings("unused")
	private static final String	TAG			= "DayFragment";

	/*
	 * used to save state
	 */
	private static final String	KEY_DATE	= "date";
	private Date				date;

	private TextView			dateTitleText;
	private TextView			daySumText;
	private FrameLayout			listContainer;

	private ArrayAdapter<Cost>	adapter;
	private BroadcastReceiver	receiver;

	/*
	 * convenient factory
	 */
	public static DayFragment newInstance(Date date) {
		DayFragment df = new DayFragment();
		df.date = date;
		return df;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// load view
		View rootView = inflater.inflate(R.layout.fragment_day, container, false);
		dateTitleText = (TextView) rootView.findViewById(R.id.day_text_date);
		daySumText = (TextView) rootView.findViewById(R.id.day_text_sum_day);
		listContainer = (FrameLayout) rootView.findViewById(R.id.list_container);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_DATE)) {
			date = (Date) savedInstanceState.getSerializable(KEY_DATE);
		}

		adapter = new CostArrayAdapter(getActivity());
		setListAdapter(adapter);

		// set OnFireListener on PullToFireListView
		((SimplePullToFireListView) getListView()).setOnFireListener(this);
		
		setupOnItemClick();
		setupOnItemLongClick();
		
//		setupLoader();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(KEY_DATE, date);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// load data
		List<Cost> data = CostDatabase.getInstance(getActivity()).getCostsByDate(date);
		updateUI(data);
		setupBroadcastReceiver();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
	}

	/* loader is not used
	   support library has a bug that when loader is used, 
	   neighbour fragment won't start off screen
	   this is a workaround, but loader is not being used now
	private void setupLoader() {
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				if (isAdded()) {
					getLoaderManager().initLoader(0, null, DayFragment.this);
				}
			}
		});
	}
	*/
	
	/*
	 * helpers
	 */
	private void setupBroadcastReceiver() {
		receiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Date receivedDate = (Date) intent.getSerializableExtra(CostDatabase.CHANGED_ON_DATE);
				if (date.equals(receivedDate)) {
					List<Cost> newData = CostDatabase.getInstance(getActivity()).getCostsByDate(date);
					updateUI(newData);
				}
			}
		};
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver,	new IntentFilter(CostDatabase.DATA_DID_CHANGE));
	}

	/*
	 * normal click to go to RecorderActivity
	 */
	private void setupOnItemClick() {
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int postion, long id) {
				Cost c = adapter.getItem(postion);
				intentToRecorder(c);
			}
		});
	}
	
	/*
	 * long click to delete
	 */
	private void setupOnItemLongClick() {
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long arg3) {
				ListCostCellView cell = new ListCostCellView(getActivity());
				final Cost toDelete = adapter.getItem(position);
				cell.setData(toDelete);
				
				/*
				 * confirmation dialog
				 */
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setView(cell)
				.setCancelable(true)
				.setTitle(R.string.alert_title_delete)
				.setNegativeButton(R.string.alert_title_no, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();						
					}
				})
				.setPositiveButton(R.string.alert_title_yes, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						CostDatabase.getInstance(getActivity()).deleteCost(toDelete);
						dialog.dismiss();
					}
				}).show();
				return true;
			}
		});
	}

	/*
	 * update the entire DayFragment, given the new data
	 */
	private void updateUI(List<Cost> newData) {
		// update list
		adapter.clear();
		if (newData.isEmpty()) {
			listContainer.setBackgroundResource(R.drawable.arrow_down_bitmap);
		} else {
			listContainer.setBackgroundResource(0);
			adapter.addAll(newData);			
		}
		
		// update sum text
		double sum = 0;
		Converter converter = CurrencyManager.getInstance(getActivity()).getConverter();
		for (Cost c : newData) {
			sum += converter.toPrimaryCurrency(c);
		}
		// get primary currency code
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String primary = prefs.getString(getResources().getString(R.string.pref_key_primary_currency), null);
		// use localised currency symbol
		NumberFormat nf = NumberFormat.getCurrencyInstance();
		nf.setCurrency(Currency.getInstance(primary));
		daySumText.setText(nf.format(sum));
		
		// update date text (may not need)
		String dateTitle = DateUtils.formatDateTime(getActivity(), date.getTime(), DateUtils.FORMAT_ABBREV_MONTH);
		dateTitleText.setText(dateTitle);
	}

	/*
	 * start RecorderActivity, cost can be null if wanting to create new cost
	 */
	private void intentToRecorder(Cost cost) {
		Intent intent = new Intent(getActivity(), RecorderActivity.class);
		intent.putExtra(RecorderActivity.KEY_DATE, date);
		if (cost != null) {
			intent.putExtra(RecorderActivity.KEY_RECORD, cost);
		}
		startActivity(intent);
		getActivity().overridePendingTransition(R.anim.fade_in, 0);
	}
	
	// OnFireListener callbacks for PullToFIreListView
	@Override
	public void onFire() {
		
		/*
		 * delay for better animation
		 */
		getListView().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				if (isAdded()) intentToRecorder(null);
			}
		}, 300);
		
		/*
		 * delay for better animation
		 */
		getListView().postDelayed(new Runnable() {

			@Override
			public void run() {
				if (isAdded()) ((SimplePullToFireListView) getListView()).onFireComplete();
			}
		}, 900);
	}
	
	/* loader callback, loader is not being used now
	@Override
	public Loader<List<Cost>> onCreateLoader(int arg0, Bundle arg1) {
		return new CostListLoader(getActivity(), date, null);
	}

	@Override
	public void onLoadFinished(Loader<List<Cost>> loader, List<Cost> data) {
		updateUI(data);
	}

	@Override
	public void onLoaderReset(Loader<List<Cost>> loader) {
		adapter.clear();
	}
	*/
	
	/**
	 * ListAdapter for ListView in DayFragment
	 */
	private class CostArrayAdapter extends ArrayAdapter<Cost> {

		public CostArrayAdapter(Context context) {
			super(context, R.layout.list_cell_cost);
		}
		
		public CostArrayAdapter(Context context, List<Cost> data) {
			super(context, R.layout.list_cell_cost, data);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Cost c = super.getItem(position);
			ListCostCellView cell;
			if (convertView == null) {
				cell = new ListCostCellView(getActivity());
				cell.setData(c);
			} else {
				cell = (ListCostCellView) convertView;
				cell.setData(c);
			}
			return cell;
		}
	}
}
