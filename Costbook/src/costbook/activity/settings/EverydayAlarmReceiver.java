package costbook.activity.settings;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.costs.R;

import costbook.activity.main.MainActivity;

/**
 * Create notification when alarm event is received.
 * This notification has PendingIntent navigating to MainActivity.
 */
public class EverydayAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		Intent toRecorderIntent = new Intent(context, MainActivity.class);
		toRecorderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		/*
		 * These three lines below allow intent to RecorderActivity, 
		 * however "up" and "back"action will leave this app (not expected)
		 */
//		Intent toRecorderIntent = new Intent(context, RecorderActivity.class);
//		Date today = Calendar.getInstance().getTime();
//		toRecorderIntent.putExtra(RecorderActivity.KEY_DATE, today);
		
		PendingIntent pIntent = PendingIntent.getActivity(context, 0, toRecorderIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		builder.setContentTitle(context.getString(R.string.notification_title))
			   .setTicker(context.getString(R.string.notification_title))
			   .setSmallIcon(R.drawable.ic_launcher)
			   .setContentIntent(pIntent)
			   .setAutoCancel(true);
		
		NotificationManager m = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		m.notify(0, builder.build());
	}
}
