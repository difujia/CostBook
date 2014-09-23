package costbook.activity.main;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Derived from this <a href="https://github.com/erikwt/PullToRefresh-ListView">project</a> <br>
 * <br>
 * Generic pull-to-do something list view, the header view being pulled out is 100%
 * customisable. A couple of events are triggered at appropriate time to allow customisation of
 * header view, mainly for animation.<br>
 * <br>
 * Quick use: 
 * Create a layout xml as header container which has at least one child view (real header). 
 * Subclass this, customise the header in appropriate methods, see details in those methods.
 * Refresh graphic editor (eclipse), add the concrete subclass view in your layout.
 * Implement OnFireListener, set it on this view to listen to pull event.
 */
public abstract class PullToFireListView extends ListView {
	
	@SuppressWarnings("unused")
	private static final String TAG = "PullList";
	
	// adjust these numbers to control animation
	private static final float	PULL_RESISTANCE_WEAK			= 1.2f;
	private static final float  PULL_RESISTANCE_STRONG			= 7.5f;
	private static final int	BOUNCE_ANIMATION_DURATION		= 600;
	private static final int	BOUNCE_ANIMATION_DELAY			= 100;
	private static final float	BOUNCE_OVERSHOOT_TENSION		= 1.4f;

	private static enum State {
		PULL_TO_FIRE, 
		RELEASE_TO_FIRE, 
		FIRING
	}

	public interface OnFireListener {
		public void onFire();
	}

	private static int				measuredHeaderHeight;

	private boolean					bounceBackHeader;
	private boolean					lockScrollWhileFiring;
	private boolean					scrollbarEnabled;

	private float					previousY;
	private int						headerPadding;
	private boolean					hasResetHeader;
	private State					state;
	private ViewGroup				headerContainer;
	private View					header;
	private OnItemClickListener		onItemClickListener;
	private OnItemLongClickListener	onItemLongClickListener;
	private OnFireListener			onFireListener;
	
	private float mScrollStartY;
	private final int IDLE_DISTANCE = 5;

	public PullToFireListView(Context context) {
		super(context);
		init();
	}

	public PullToFireListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PullToFireListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
    @Override
    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener){
        this.onItemLongClickListener = onItemLongClickListener;
    }
    
    public void setOnFireListener(OnFireListener onFireListener) {
    	this.onFireListener = onFireListener;
    }
    
    public boolean isFiring() {
    	return state == State.FIRING;
    }
    
    public void setLockScrollWhileFIRING(boolean lockScrollWhileFiring){
        this.lockScrollWhileFiring= lockScrollWhileFiring;
    }
    
    public void setFiring() {
    	state = State.FIRING;
    	scrollTo(0, 0);
    	setHeaderPadding(0);
    }
    
    public void onFireComplete() {
    	state = State.PULL_TO_FIRE;
    	resetHeader();
    	//TODO: add event probably
    }

	private void init() {
		setVerticalFadingEdgeEnabled(false);
		headerContainer = onCreateHeaderContainer(LayoutInflater.from(getContext()), this);		
		header = headerContainer.findViewById(onFindHeaderView());
		addHeaderView(headerContainer);
		setState(State.PULL_TO_FIRE);
		scrollbarEnabled = isVerticalScrollBarEnabled();

		ViewTreeObserver vto = header.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new PTFOnGlobalLayoutListener());
		
		super.setOnItemClickListener(new PTFOnItemClickListener());
		super.setOnItemLongClickListener(new PTFOnItemLongClickListener());
	}
	
	
	private void setHeaderPadding(int newPadding) {
		headerPadding = newPadding;
		MarginLayoutParams params = (ViewGroup.MarginLayoutParams) header.getLayoutParams();
		params.setMargins(0, Math.round(newPadding), 0, 0);
		header.setLayoutParams(params);
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (lockScrollWhileFiring && (state == State.FIRING || getAnimation() != null && !getAnimation().hasEnded())) {
			return true;
		}
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (getFirstVisiblePosition() == 0) {
					previousY = event.getY();
				} else {
					previousY = -1;
				}
				
				// remember starting Y
				mScrollStartY = event.getY();
				break;
			case MotionEvent.ACTION_UP:
				if (previousY != -1 && (state == State.RELEASE_TO_FIRE || getFirstVisiblePosition() == 0)) {
					switch (state) {
						case RELEASE_TO_FIRE:
							setState(State.FIRING);
							bounceBackHeader();
				    		if (onFireListener != null) {
				    			onFireListener.onFire();
				    			onFireSuccess();
				    		} else {
				    			setState(State.PULL_TO_FIRE);
				    		}
							break;
						case PULL_TO_FIRE:
							resetHeader();
							onFireDiscard();
							break;
					}
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (previousY != -1 && getFirstVisiblePosition() == 0 && Math.abs(mScrollStartY-event.getY()) > IDLE_DISTANCE) {
					float y = event.getY();
					float diff = y - previousY;
					if (diff > 0) {
						if (headerPadding < 0) diff = diff / PULL_RESISTANCE_WEAK;
						else diff = diff / PULL_RESISTANCE_STRONG;
					}
					previousY = y;
					
					int newHeaderPadding = Math.max(Math.round(headerPadding + diff), -header.getHeight());
					if (newHeaderPadding != headerPadding && state != State.FIRING) {
						setHeaderPadding(newHeaderPadding);
						// report percentage of header shown
						float percent = (float) (newHeaderPadding + measuredHeaderHeight) / measuredHeaderHeight;
						onPulling(percent);
						// change state when header is completely revealed
                        if(state == State.PULL_TO_FIRE && headerPadding >= 0){
                            setState(State.RELEASE_TO_FIRE);
                        }else if(state == State.RELEASE_TO_FIRE && headerPadding < 0){
                            setState(State.PULL_TO_FIRE);

                        }

					}
				}
		}
		return super.onTouchEvent(event);
	}
	
	private void bounceBackHeader() {
		int yTranslate;
		if (state == State.FIRING) {
			yTranslate = header.getHeight() - headerContainer.getHeight();
		} else {
			yTranslate = -headerContainer.getHeight() - headerContainer.getTop() + getPaddingTop();
		}
		
        TranslateAnimation bounceAnim = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, 0,
                TranslateAnimation.ABSOLUTE, yTranslate);
        
        bounceAnim.setDuration(BOUNCE_ANIMATION_DURATION);
        bounceAnim.setFillEnabled(true);
        bounceAnim.setFillAfter(false);
        bounceAnim.setFillBefore(true);
        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_OVERSHOOT_TENSION));
        bounceAnim.setAnimationListener(new HeaderAnimationListener(yTranslate));

        startAnimation(bounceAnim);
	}
	
    private void resetHeader(){
        if(getFirstVisiblePosition() > 0){
            setHeaderPadding(-header.getHeight());
            setState(State.PULL_TO_FIRE);
            return;
        }

        if(getAnimation() != null && !getAnimation().hasEnded()){
            bounceBackHeader = true;
        }else{
            bounceBackHeader();
        }
    }

    private void setState(State state) {
    	this.state = state;
    }
    
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt){
        super.onScrollChanged(l, t, oldl, oldt);

        if(!hasResetHeader){
            if(measuredHeaderHeight > 0 && state != State.FIRING){
                setHeaderPadding(-measuredHeaderHeight);
            }

            hasResetHeader = true;
        }
    }
    
    private class HeaderAnimationListener implements AnimationListener{

        private int height, translation;
        private State stateAtAnimationStart;

        public HeaderAnimationListener(int translation){
            this.translation = translation;
        }

        @Override
        public void onAnimationStart(Animation animation){
            stateAtAnimationStart = state;

            ViewGroup.LayoutParams params = getLayoutParams();
            height = params.height;
            params.height = getHeight() - translation;
            setLayoutParams(params);

            if(scrollbarEnabled){
                setVerticalScrollBarEnabled(false);
            }
        }

        @Override
        public void onAnimationEnd(Animation animation){
            setHeaderPadding(stateAtAnimationStart == State.FIRING ? 0 : -measuredHeaderHeight - headerContainer.getTop());
            setSelection(0);

            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = height;
            setLayoutParams(params);

            if(scrollbarEnabled){
                setVerticalScrollBarEnabled(true);
            }

            if(bounceBackHeader){
                bounceBackHeader = false;

                postDelayed(new Runnable(){

                    @Override
                    public void run(){
                        resetHeader();
                    }
                }, BOUNCE_ANIMATION_DELAY);
            }else if(stateAtAnimationStart != State.FIRING){
                setState(State.PULL_TO_FIRE);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation){}
    }

    private class PTFOnGlobalLayoutListener implements OnGlobalLayoutListener{

		@Override
        public void onGlobalLayout(){
            int initialHeaderHeight = header.getHeight();

            if(initialHeaderHeight > 0){
                measuredHeaderHeight = initialHeaderHeight;

                if(measuredHeaderHeight > 0 && state != State.FIRING){
                    setHeaderPadding(-measuredHeaderHeight);
                    requestLayout();
                }
            }

            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

	private class PTFOnItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
			hasResetHeader = false;

			if (onItemClickListener != null && state == State.PULL_TO_FIRE && view != headerContainer) {
				// passing up onItemClick. correct position with the number of header views
				onItemClickListener.onItemClick(adapterView, view, position - getHeaderViewsCount(), id);
			}
		}
	}

	private class PTFOnItemLongClickListener implements OnItemLongClickListener {

		@Override
		public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
			hasResetHeader = false;

			if (onItemLongClickListener != null && state == State.PULL_TO_FIRE && view != headerContainer) {
				// passing up onItemLongClick. correct position with the number of header views
				return onItemLongClickListener.onItemLongClick(adapterView, view, position - getHeaderViewsCount(), id);
			}
			return false;
		}
	}

	// TODO: override below
	/**
	 * Called when creating the header container
	 * 
	 * @param inflater
	 * @param parent
	 * @return container ViewGroup that contains a header child
	 */
	protected abstract ViewGroup onCreateHeaderContainer(LayoutInflater inflater, ViewGroup parent);

	/**
	 * Called after creating the header container, the ListView will call findViewById() to get the header child
	 * 
	 * @return resource id of the header child inside header container, this id must be in the layout file of header container
	 */
	protected abstract int onFindHeaderView();

	/**
	 * Called continuously while the ListView is being pulled
	 * 
	 * @param percent
	 *            the percentage of header child revealed, 0-1 = partially revealed, >1 = fully revealed
	 */
	protected abstract void onPulling(float percent);

	/**
	 * Called when pull action is released, header is fully revealed, fire event is triggered
	 */
	protected abstract void onFireSuccess();

	/**
	 * Called when pull action is released, however header is not fully revealed, fire event is discarded
	 */
	protected abstract void onFireDiscard();
}