package costbook.deprecated;

import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import costbook.data.cost.Cost;
import costbook.data.cost.CostDatabase;

/**
 * Load all cost records from database matching given Date (or Date range)
 * 
 * @deprecated Loader has problem working with ViewPager in support library, 
 * and may cause screen to "blink" when switch WeekFragment.
 */
public class CostListLoader extends AsyncTaskLoader<List<Cost>> {

	private static final String	TAG	= "CostListLoader";
	private Date				mStart;
	private Date				mEnd;
	private List<Cost>			mData;
	private BroadcastReceiver	mObserver;

	public CostListLoader(Context context, Date start, Date end) {
		super(context);
		this.mStart = start;
		this.mEnd = end;
		onContentChanged();
	}

	@Override
	public List<Cost> loadInBackground() {
		// Log.i(TAG, "day " + mStart.toString() + " load in background");
		if (mEnd == null) {
			return CostDatabase.getInstance(getContext()).getCostsByDate(mStart);
		} else {
			return CostDatabase.getInstance(getContext()).getCostsBetween(mStart, mEnd);
		}
	}

	@Override
	public void deliverResult(List<Cost> data) {
		// Log.i(TAG, "day " + mStart.toString() + " deliver result");
		if (isReset()) {
			return;
		}

		mData = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		// Begin monitoring the underlying data source.
		if (mObserver == null) {
			mObserver = new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {
					Date receivedDate = (Date) intent.getSerializableExtra(CostDatabase.CHANGED_ON_DATE);
					boolean needReload = false;
					if (mEnd == null && receivedDate.equals(mStart)) {
						needReload = true;
					} else if (mEnd != null && receivedDate.compareTo(mStart) >= 0 && receivedDate.compareTo(mEnd) <= 0) {
						needReload = true;
					}
					if (needReload) {
						Log.d(TAG, "data changed");
						onContentChanged();
					}
				}
			};
			LocalBroadcastManager.getInstance(getContext()).registerReceiver(mObserver,	new IntentFilter(CostDatabase.DATA_DID_CHANGE));
		}
		
//		Log.i(TAG, "day " + mStart.toString() + " start loading");
		if (mData != null) {
			// Deliver previously loaded data immediately.
			deliverResult(mData);
		}
		if (takeContentChanged() || mData == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
//		Log.i(TAG, "day " + mStart.toString() + " is reset");
		// release
		if (mData != null || mStart != null || mEnd != null) {
			mData = null;
			mStart = null;
			mEnd = null;
		}

		if (mObserver != null) {
			LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mObserver);
			mObserver = null;
		}
	}
}
