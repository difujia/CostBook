package costbook.activity.recorder;

import java.util.Date;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.costs.R;

import costbook.activity.recorder.KeypadFragment.OnKeypadListener;
import costbook.activity.recorder.PanelFragment.OnPanelListener;
import costbook.data.cost.Cost;
import costbook.data.cost.CostDatabase;

/**
 * Create/modify cost record. Need at least a Date object to run.
 * This activity has responsibility for creating new Cost object.
 * This activity implements listeners for both keypad and panel fragment, transferring data between them.
 * When finishing, this activity take out the Cost object from PanelFragment, and save it to database.
 */
public class RecorderActivity extends FragmentActivity implements OnKeypadListener, OnPanelListener {
	public static final String	KEY_DATE	= "date";
	public static final String	KEY_RECORD	= "record";

	@SuppressWarnings("unused")
	private static final String	TAG			= "RecorderActivity";

	private Date				date;

	private KeypadFragment		keypad;
	private PanelFragment		panel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recorder);
		// Show the Up button in the action bar.
		setupActionBar();
		
		// retrieve extras
		Bundle extra = getIntent().getExtras();
		date = (Date) extra.getSerializable(KEY_DATE);
		Cost record;
		if (extra.containsKey(KEY_RECORD)) {
			record = (Cost) extra.getSerializable(KEY_RECORD);
		} else {
			record = new Cost();
			record.setAmount(0);
			record.setCategory(getString(R.string.category_default));
			// set to primary currency
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String currency = sp.getString(getResources().getString(R.string.pref_key_primary_currency), null);
			record.setCurrency(currency);
			record.setDate(date);
			record.setRemark("");
		}
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction trans = fm.beginTransaction();
		panel = (PanelFragment) fm.findFragmentByTag("panel");
		keypad = (KeypadFragment) fm.findFragmentByTag("keypad");
		if (panel == null ) {
			panel = new PanelFragment();
			panel.setRecord(record);
			trans.setCustomAnimations(R.anim.top_in, R.anim.top_out);
			trans.add(R.id.recorder_panel_container, panel, "panel");			
		}
		if (keypad == null) {
			keypad = new KeypadFragment();
			keypad.init(record.getAmount());
			trans.setCustomAnimations(R.anim.bottom_in, R.anim.bottom_out);
			trans.add(R.id.recorder_keypad_container, keypad, "keypad");			
		}
		if (!trans.isEmpty()) trans.commit();
		panel.setOnPanelListener(this);
		keypad.setOnKeypadListener(this);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recorder, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// This ID represents the Home or Up button. In the case of this
				// activity, the Up button is shown. Use NavUtils to allow users
				// to navigate up one level in the application structure. For
				// more details, see the Navigation pattern on Android Design:
				//
				// http://developer.android.com/design/patterns/navigation.html#up-vs-back
				//
				Intent intent = NavUtils.getParentActivityIntent(this);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				NavUtils.navigateUpTo(this, intent);
				return true;
			case R.id.finish:
				CostDatabase.getInstance(this).putCost(panel.getRecord());
				finish();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		// this doesn't work
		onRequestKeypad();
		super.onBackPressed();
	}

	// keypad listener methods
	@Override
	public void onOutput(double out) {
		panel.setNewAmount(out);
	}

	@Override
	public void onInvalidInput() {
		panel.warnInvalidInput();
	}

	// panel listener
	@Override
	public void onRequestKeypad() {
		
		/*
		 * show KeypadFragment at the bottom
		 */
		if (keypad.isHidden()) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.setCustomAnimations(R.anim.bottom_in, R.anim.bottom_out);
			trans.show(keypad).commit();
		}
	}

	@Override
	public void onDismissKeypad() {
		
		/*
		 * hide KeypadFragment when system's soft keyboard comes out
		 */
		if (!keypad.isHidden()) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			trans.setCustomAnimations(R.anim.bottom_in, R.anim.bottom_out);
			trans.hide(keypad).commit();
		}
	}
}
