package costbook.activity.main;

import java.util.Arrays;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.costs.R;

import costbook.data.cost.Cost;
import costbook.data.currency.CurrencyManager;
import costbook.data.currency.CurrencyManager.Converter;

/**
 * Custom ListView cell
 */
public class ListCostCellView extends LinearLayout {

	private ImageView	icon;
	private TextView	remarkText;
	private TextView	amountText;

	public ListCostCellView(Context context) {
		super(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.list_cell_cost, this, true);
		// find views
		icon = (ImageView) findViewById(R.id.list_cell_icon);
		remarkText = (TextView) findViewById(R.id.list_cell_remark);
		amountText = (TextView) findViewById(R.id.list_cell_amount);
	}

	/**
	 * @param cost
	 *            data to show in this cell
	 */
	public void setData(Cost cost) {
		// set icon
		String[] categories = getResources().getStringArray(R.array.category_values);
		int index = Arrays.asList(categories).indexOf(cost.getCategory());
		TypedArray images = getResources().obtainTypedArray(R.array.category_images);
		icon.setImageResource(images.getResourceId(index, -1));
		images.recycle();
		// set remark
		if (cost.getRemark().length() > 0) {
			remarkText.setText(cost.getRemark());
		} else {
			String[] entries = getResources().getStringArray(R.array.category_entries);
			remarkText.setText(entries[index]);
		}
		// set amount
		Converter c = CurrencyManager.getInstance(getContext()).getConverter();
		double amount = c.toPrimaryCurrency(cost);
		amountText.setText(String.format("%.2f", amount));
	}

}
