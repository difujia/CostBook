package costbook.activity.recorder;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;

import com.costs.R;

/**
 * Shown at the bottom of RecorderActivity. <br>
 * This is a simple calculator, delegates display to OnKeypadListener
 */
public class KeypadFragment extends Fragment implements OnClickListener {

	@SuppressWarnings("unused")
	private static final String	TAG	= "KeypadFragment";

	/**
	 * Implement this to display outputs of this calculator
	 */
	public interface OnKeypadListener {
		public void onOutput(double out);

		/**
		 * warn invalid input (i.e. division by 0, input longer than permitted digits)
		 */
		public void onInvalidInput();
	}

	private OnKeypadListener	callback;

	private static final int	NONE		= 0;
	private static final int	ADD			= 1;
	private static final int	SUBTRACT	= 2;
	private static final int	MULTIPLY	= 3;
	private static final int	DIVIDE		= 4;

	private String				integer		= "0";
	private String				decimal		= "";
	private int					precision	= 2;
	private int					pendingOP	= NONE;
	private double				memory		= Double.NaN;
	private boolean				dot			= false;
	private boolean				hasValue	= false;

	private GridLayout			grid;

	/*
	 * reference to highlighted button
	 */
	private Button				highlightBtn;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_keypad, container, false);
		grid = (GridLayout) rootView;
		grid.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
 
			@Override
			public void onGlobalLayout() {
				int cellWidth = grid.getWidth() / grid.getColumnCount();
				Button b;
				for (int i = 0; i < grid.getChildCount(); i++) {
					b = (Button) grid.getChildAt(i);
					b.setOnClickListener(KeypadFragment.this);
					b.setWidth(cellWidth);
				}				
				grid.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		if (savedInstanceState != null && savedInstanceState.containsKey("integer")) {
			integer = savedInstanceState.getString("integer");
			decimal = savedInstanceState.getString("decimal");
			precision = savedInstanceState.getInt("precision");
			pendingOP = savedInstanceState.getInt("pendingOP");
			memory = savedInstanceState.getDouble("memory");
			dot = savedInstanceState.getBoolean("dot");
			hasValue = savedInstanceState.getBoolean("hasValue");
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("integer", integer);
		outState.putString("decimal", decimal);
		outState.putInt("precision", precision);
		outState.putInt("pendingOP", pendingOP);
		outState.putDouble("memory", memory);
		outState.putBoolean("dot", dot);
		outState.putBoolean("hasValue", hasValue);
		super.onSaveInstanceState(outState);
	}
	
	/*
	 *  behaviours
	 */
	public void init(double initNum) {
		// programmatic reset and init action
		clearValue();
		pendingOP = NONE;
		memory = Double.NaN;
		String[] num = String.valueOf(initNum).split("\\.");
		integer = num[0];
		if (!num[1].equals("0")) {
			decimal = num[1];
			dot = true;
		}
		hasValue = true;
		
	}
	
	public void setOnKeypadListener(OnKeypadListener listener) {
		callback = listener;
	}
	
	public void setPrecision(int precision) {
		this.precision = precision;
	}

	@Override
	public void onClick(View v) {
		Button b = (Button) v;
		String tag = (String) b.getTag();
		
		/*
		 * dispatch to corresponding methods
		 */
		if ("number".equals(tag)) onNumberClick(b);
		else if ("operator".equals(tag)) onOperatorClick(b);
		else if ("clear".equals(tag)) onClearClick();
		else if ("dot".equals(tag)) onDotClick();
		else if ("calculate".equals(tag)) onCalculateClick();
	}

	/*
	 * calculator methods
	 */
	private void onClearClick() {
		double d = 0;
		init(d);
		output(d);
		cancelHighlight();
	}

	private void onNumberClick(Button b) {
		String clicked = b.getText().toString();
		if (dot) {
			if (decimal.length() < precision) {
				decimal = decimal + clicked;
			} else {
				warn();
				return;
			}
		} else {
			if ("0".equals(integer)) {
				integer = clicked;
			} else {
				integer = integer + clicked;
			}
		}
		hasValue = true;
		// output
		output(getValue());
		cancelHighlight();
	}

	private void onOperatorClick(Button b) {
		if (hasValue) {
			onCalculateClick();			
		}
		if (Double.isNaN(memory) ||
				(hasValue && pendingOP == NONE)) {
			memory = getValue();
		}
		clearValue();
		pendOperation(b.getText().toString());
	
		// set different background for active operator button
		cancelHighlight();
		highlightBtn = b;
		highlightBtn.setBackgroundResource(R.drawable.tint_border_rect_bg);

	}
	
	private void onCalculateClick() {
		if (pendingOP != NONE && !Double.isNaN(memory) && hasValue) {
			if (pendingOP == DIVIDE && getValue() == 0) {
				// invalid "divide by 0" operation
				warn();
			} else {
				// calculate pending operation and move result to memory
				memory = calculate();
				output(memory);
				clearValue();
				pendingOP = NONE;
				cancelHighlight();
			}
		}
	}

	private void onDotClick() {
		dot = true;
		cancelHighlight();
	}
	
	/*
	 *  helpers
	 */
	// convert concatenation of integer and decimal to double
	private double getValue() {
		String output;
		if (decimal.length() > 0) {
			output = integer + "." + decimal;
		} else {
			output = integer;
		}
		return Double.valueOf(output);
	}
	
	private double calculate() {
		double n = getValue();
		switch (pendingOP) {
		case ADD: return memory + n;
		case SUBTRACT: return memory - n;
		case MULTIPLY: return memory * n;
		case DIVIDE: return memory / n;
		
		// pendingOp == NONE, shouldn't reach
		default: return 0;
		}
	}
	
	private void pendOperation(String op) {
		if ("+".equals(op)) {
			pendingOP = ADD;
		} else if ("-".equals(op)) {
			pendingOP = SUBTRACT;
		} else if ("×".equals(op)) {
			pendingOP = MULTIPLY;
		} else if ("÷".equals(op)) {
			pendingOP = DIVIDE;
		}
	}
	
	private void clearValue() {
		integer = "0";
		decimal = "";
		dot = false;
		hasValue = false;
	}
	
	private void cancelHighlight() {
		if (highlightBtn != null) {
			highlightBtn.setBackgroundResource(0);
			highlightBtn = null;
		}
	}

	private void output(double out) {
		callback.onOutput(out);
	}

	private void warn() {
		callback.onInvalidInput();
	}

}
