package costbook.activity.recorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.costs.R;

import costbook.data.cost.Cost;

/**
 * Shown at the top of RecorderActivity. <br>
 * This fragment displays options for creation/modification of a cost record.
 * This may support "continuous" creation of cost records in the future.
 */
public class PanelFragment extends Fragment {

	public interface OnPanelListener {
		public void onRequestKeypad();

		public void onDismissKeypad();
	}

	@SuppressWarnings("unused")
	private static final String	TAG			= "PanelFragment";
	private static final String	KEY_RECORD	= "record";

	private TextView			dateText;
	private Spinner				spinner;
	private TextView			amountText;
	private GridView			grid;
	private EditText			remarkEdit;

	private Cost				record;
	private OnPanelListener		callback;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_panel, container, false);
		spinner = (Spinner) rootView.findViewById(R.id.panel_spinner_currency);
		amountText = (TextView) rootView.findViewById(R.id.panel_text_amount);
		grid = (GridView) rootView.findViewById(R.id.panel_grid_category);
		remarkEdit = (EditText) rootView.findViewById(R.id.panel_text_remark);
		dateText = (TextView) rootView.findViewById(R.id.panel_text_date);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_RECORD)) {
			record = (Cost) savedInstanceState.getSerializable(KEY_RECORD);
			Log.w(TAG, "resotred");
		}
		dateText.setText(DateUtils.formatDateTime(getActivity(), record.getDate().getTime(), DateUtils.FORMAT_ABBREV_MONTH));
		setupSpinner();
		setupAmount();
		setupGrid();
		setupRemarkEdit();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(KEY_RECORD, record);
		super.onSaveInstanceState(outState);
	}

	/*
	 * behaviours
	 */
	public void setRecord(Cost record) {
		this.record = record;
	}

	public Cost getRecord() {
		return record;
	}

	public void setOnPanelListener(OnPanelListener listener) {
		this.callback = listener;
	}

	public void setNewAmount(double amount) {
		amountText.setText(String.format("%.2f", amount));
		record.setAmount(amount);
	}

	public void warnInvalidInput() {
		Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.tilt);
		amountText.startAnimation(shake);
	}

	/*
	 * setup UI
	 */
	private void setupSpinner() {
		// retrieve currency settings
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		Set<String> currencyInUse = sp
				.getStringSet(getResources().getString(R.string.pref_key_currencies_in_use), null);
		// add the record'currency in case its currency is now not used, but
		// still need to access
		currencyInUse.add(record.getCurrency());

		// bind spinner with data set
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		adapter.addAll(currencyInUse);
		spinner.setAdapter(adapter);
		spinner.setSelection(adapter.getPosition(record.getCurrency()));
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long rowId) {
				record.setCurrency((String) spinner.getItemAtPosition(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

	}

	private void setupAmount() {
		amountText.setText(String.format("%.2f", record.getAmount()));
		amountText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dismissSoftKeyboard();
			}
		});
	}

	private void setupGrid() {
		TypedArray images = getResources().obtainTypedArray(R.array.category_images);
		String[] entries = getResources().getStringArray(R.array.category_entries);
		ArrayList<HashMap<String, Object>> items = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < images.length(); i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("image", images.getResourceId(i, -1));
			map.put("text", entries[i]);
			items.add(map);
		}
		images.recycle();
		GridCellAdapter adapter = new GridCellAdapter(getActivity(), items);
		grid.setAdapter(adapter);
		grid.setOnItemClickListener(adapter);
	}

	private void setupRemarkEdit() {
		if (record.getRemark().length() > 0) {
			remarkEdit.setText(record.getRemark());
		}
		remarkEdit.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					dismissSoftKeyboard();
					return true;
				}
				return false;
			}
		});
		remarkEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					callback.onDismissKeypad();
				}
			}
		});
		remarkEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				record.setRemark(s.toString());
			}
		});
	}

	// helpers
	private void dismissSoftKeyboard() {
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(remarkEdit.getWindowToken(), 0);
		remarkEdit.clearFocus();
		callback.onRequestKeypad();
	}

	/**
	 * For category GridView, also listen to click on the GridView.
	 */
	private class GridCellAdapter extends ArrayAdapter<HashMap<String, Object>> implements OnItemClickListener {

		List<HashMap<String, Object>>	dataList;
		String[]						cateValues	= getResources().getStringArray(R.array.category_values);

		public GridCellAdapter(Context context, List<HashMap<String, Object>> objects) {
			super(context, R.layout.grid_cell_category, objects);
			dataList = objects;
		}

		@Override
		public int getCount() {
			return dataList.size();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout container;
			if (convertView == null) {
				container = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.grid_cell_category,
						parent, false);
			} else {
				container = (LinearLayout) convertView;
			}
			
			String text = (String) dataList.get(position).get("text");
			((TextView) container.findViewById(R.id.grid_cell_text)).setText(text);
			int imageRes = (Integer) dataList.get(position).get("image");
			((ImageView) container.findViewById(R.id.grid_cell_image)).setImageResource(imageRes);
			
			/*
			 * highlight the one matching record's current category
			 */
			if (record.getCategory().equals(cateValues[position])) {
				container.setBackgroundColor(getResources().getColor(R.color.grid_cell_select));
			} else {
				container.setBackgroundColor(getResources().getColor(R.color.grid_cell_unselect));
			}
			return container;
		}

		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long rowId) {
			record.setCategory(cateValues[position]);
			this.notifyDataSetChanged();
			dismissSoftKeyboard();
		}
	}
}
