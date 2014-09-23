package costbook.activity.main;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.costs.R;


/**
 * Concrete class of PullToFireListView. This provides header view and animations.
 * See details in the abstract class
 */
public class SimplePullToFireListView extends PullToFireListView {

	private ImageView		cross;
	private RotateAnimation	rotate		= (RotateAnimation) AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
	private boolean			rotating	= false;

	public SimplePullToFireListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SimplePullToFireListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SimplePullToFireListView(Context context) {
		super(context);
	}

	@Override
	protected ViewGroup onCreateHeaderContainer(LayoutInflater inflater, ViewGroup parent) {
		LinearLayout headerContainer = (LinearLayout) inflater.inflate(R.layout.list_header, parent, false);
		cross = (ImageView) headerContainer.findViewById(R.id.cross);
		return headerContainer;
	}

	@Override
	protected int onFindHeaderView() {
		return R.id.cross;
	}

	@Override
	protected void onPulling(float percent) {
		if (percent < 1) {
			cross.setAlpha(percent * percent);
			if (rotating) {
				cross.clearAnimation();
				rotating = false;
			}
		} else {
			cross.setAlpha(1.0f);
			if (!rotating) {
				cross.startAnimation(rotate);
				rotating = true;
			}
		}
	}

	@Override
	protected void onFireSuccess() {
		cross.clearAnimation();
		rotating = false;
	}

	@Override
	protected void onFireDiscard() {
		cross.clearAnimation();
		rotating = false;
	}
}
