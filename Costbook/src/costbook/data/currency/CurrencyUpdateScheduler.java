package costbook.data.currency;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.Log;

/**
 * Static receiver,listen to connectivity change and one-off command sent by MainActivity.
 * Schedule currency update event according to connectivity mode. (hourly or daily)
 */
public class CurrencyUpdateScheduler extends BroadcastReceiver {

	private static final String TAG = "CurrencyUpdateScheduler";

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo network = connMgr.getActiveNetworkInfo();
		if (network != null && network.isConnected()) {
			int type = network.getType();
			if (type == ConnectivityManager.TYPE_ETHERNET || type == ConnectivityManager.TYPE_WIFI) {
				scheduleWifi(context);
			} else if (type == ConnectivityManager.TYPE_MOBILE) {
				scheduleMobile(context);
			}
		}

	}

	/*
	 * repeating alarm with short interval (1 hour)
	 */
	private void scheduleWifi(Context context) {
		Log.i(TAG, "schedule wifi");
		Intent updater = new Intent(context, CurrencyUpdateService.class);
		PendingIntent recurringUpdate = PendingIntent.getService(context, 0, updater, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_HOUR, recurringUpdate);
	}
	
	/*
	 * repeating alarm with long interval (1 day)
	 */
	private void scheduleMobile(Context context) {
		Log.i(TAG, "schedule mobile");
		Intent updater = new Intent(context, CurrencyUpdateService.class);
		PendingIntent recurringUpdate = PendingIntent.getService(context, 0, updater, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_DAY, recurringUpdate);
	}

}
