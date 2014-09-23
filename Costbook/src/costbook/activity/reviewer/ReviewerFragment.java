package costbook.activity.reviewer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.costs.R;

/**
 * Not finished yet. <br>
 * This Fragment is to show bar charts describing Cost data. This may need custom graphics.
 */
public class ReviewerFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootviView = inflater.inflate(R.layout.fragment_land, container, false);
		return rootviView;
	}
}
