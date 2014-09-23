package costbook.activity.settings;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import com.costs.R;

/**
 * Derived from <a href="http://stackoverflow.com/questions/5533078/timepicker-in-preferencescreen">here</a><br>
 * Custom preference used to set up alarm time for everyday notification.
 */
public class TimePreference extends DialogPreference {

	private Calendar	calendar;
	private TimePicker	picker	= null;

	public TimePreference(Context context) {
		super(context, null);
	}

	public TimePreference(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.dialogPreferenceStyle);
		setPositiveButtonText(R.string.settings_set);
		setNegativeButtonText(R.string.settings_cancel);
		calendar = new GregorianCalendar();
	}

	public TimePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, android.R.attr.dialogPreferenceStyle);

		setPositiveButtonText(R.string.settings_set);
		setNegativeButtonText(R.string.settings_cancel);
		calendar = new GregorianCalendar();
	}

	@Override
	protected View onCreateDialogView() {
		picker = new TimePicker(getContext());
		return (picker);
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		picker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
		picker.setCurrentMinute(calendar.get(Calendar.MINUTE));
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			calendar.set(Calendar.HOUR_OF_DAY, picker.getCurrentHour());
			calendar.set(Calendar.MINUTE, picker.getCurrentMinute());

			setSummary(getSummary());
			if (callChangeListener(calendar.getTimeInMillis())) {
				persistLong(calendar.getTimeInMillis());
				notifyChanged();
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return (a.getString(index));
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		if (restoreValue) {
			if (defaultValue == null) {
				calendar.setTimeInMillis(getPersistedLong(System.currentTimeMillis()));
			} else {
				calendar.setTimeInMillis(Long.parseLong(getPersistedString((String) defaultValue)));
			}
		} else {
			if (defaultValue == null) {
				calendar.setTimeInMillis(System.currentTimeMillis());
			} else {
				calendar.setTimeInMillis(Long.parseLong((String) defaultValue));
			}
		}
		setSummary(getSummary());
	}

	@Override
	public CharSequence getSummary() {
		if (calendar == null) {
			return null;
		}
		return DateFormat.getTimeFormat(getContext()).format(new Date(calendar.getTimeInMillis()));
	}
}