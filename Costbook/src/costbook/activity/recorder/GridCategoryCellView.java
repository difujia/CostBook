package costbook.activity.recorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.costs.R;

/**
 * Custom GridView cell
 */
public class GridCategoryCellView extends LinearLayout {

	private ImageView	imageView;
	private TextView	textView;

	public GridCategoryCellView(Context context) {
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.grid_cell_category, this, true);
		imageView = (ImageView) findViewById(R.id.grid_cell_image);
		textView = (TextView) findViewById(R.id.grid_cell_text);
	}

	/**
	 * @param text
	 *            Text shown below the image
	 * @param imageRes
	 *            Image resource id
	 */
	public void setData(String text, int imageRes) {
		textView.setText(text);
		imageView.setImageResource(imageRes);
	}
}
