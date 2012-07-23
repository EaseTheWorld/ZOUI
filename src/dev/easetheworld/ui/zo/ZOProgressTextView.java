package dev.easetheworld.ui.zo;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.easetheworld.diagonal.R;

public class ZOProgressTextView extends TextView {
	
    private static final int MODE_Z = 0;
    private static final int MODE_O = 1;
    private int mMode;
    
	private int mMax = 0;
	private float mDistanceSum = 0;
	private int mDistanceThreshold = 20;
    
    private PopupWindow mOverlayPopup;
    private ImageView mOverlay;
	
    private StrokeGestureDetector mStrokeDetector;
    
    private int[] mLocationXY = new int[2];

	public ZOProgressTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
        mStrokeDetector = new StrokeGestureDetector(getContext(), mStrokeListener);
        mOverlay = new ImageView(context);
        mOverlay.setImageResource(android.R.drawable.btn_plus);
        mOverlayPopup = new PopupWindow(mOverlay);
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ZOProgressTextView); 
        mMax = a.getInt(R.styleable.ZOProgressTextView_android_max, mMax);
        mDistanceThreshold = a.getDimensionPixelSize(R.styleable.ZOProgressTextView_android_spacing, mDistanceThreshold);
	}
	
	public void setMax(int max) {
		mMax = max;
	}
	
	public int getMax() {
		return mMax;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mStrokeDetector.onTouchEvent(event))
			return true;
		else
			return super.onTouchEvent(event);
	}
	
	private StrokeGestureDetector.OnStrokeGestureListener mStrokeListener = new StrokeGestureDetector.OnStrokeGestureListener() {
		
		@Override
		public void onDown(MotionEvent e) {
			android.util.Log.i("Stroke", "Down");
			setStrokeIncreaseMount(1);
	        setMode(MODE_Z);
		}
		
		@Override
		public void onUp(MotionEvent e) {
			android.util.Log.i("Stroke", "Up");
	    	dismissPopupOnScreen();
		}
		
		@Override
		public boolean onStrokeStart(MotionEvent e) {
			android.util.Log.i("Stroke", "Start "+e.getX()+","+e.getY());
			switch(mMode) {
			case MODE_Z:
				addProgress(mStrokeIncreaseMount);
				break;
			case MODE_O:
				mDistanceSum = 0;
				break;
			}
			return false;
		}
		
		@Override
		public boolean onStrokeMove(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//			android.util.Log.i("Stroke", "Move e1="+e1+", e2="+e2+", dx="+distanceX+", dy="+distanceY);
	    	switch(mMode) {
	    	case MODE_O:
	    		mDistanceSum += FloatMath.sqrt(distanceX * distanceX + distanceY * distanceY);
	    		int diff = (int)(mDistanceSum / mDistanceThreshold);
				addProgress(mStrokeIncreaseMount * diff);
				mDistanceSum = mDistanceSum % mDistanceThreshold;
	    		break;
	    	}
	    	showPopupOnScreen((int)e2.getX(), (int)e2.getY());
			return false;
		}
		
		@Override
		public boolean onStrokeEnd(MotionEvent e1, MotionEvent e2) {
			android.util.Log.i("Stroke", "End "+e2.getX()+","+e2.getY()+" from "+e1.getX()+","+e1.getY());
	    	switch(mMode) {
	    	case MODE_O:
				setStrokeIncreaseMount(-mStrokeIncreaseMount);
	    		break;
	    	}
			return false;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			android.util.Log.i("Stroke", "SingleTapUp "+e);
			return false;
		}
		
		@Override
		public boolean onHold(float x, float y) {
			android.util.Log.i("Stroke", "Hold");
			switch(mMode) {
			case MODE_Z:
				if (mStrokeDetector.isStroking())
					setStrokeIncreaseMount(-mStrokeIncreaseMount);
				else {
					setMode(MODE_O);
					showPopupOnScreen((int)x, (int)y);
				}
				return true;
			}
			return false;
		}
	};
	
	public int getProgress() {
		return mProgress;
	}
	
	public void setProgress(int progress) {
		mProgress = progress;
		setText(String.valueOf(mProgress));
	}
	
	private int mProgress = 0;
	
	private int mStrokeIncreaseMount;
	
	private void addProgress(int diff) {
		int progress = mProgress + diff;
		if (mMax > 0) {
			if (progress < 0)
				progress += mMax + 1;
			else if (progress > mMax)
				progress -= mMax + 1;
		}
		setProgress(progress);
	}
	
	private void setMode(int mode) {
		mMode = mode;
		switch(mMode) {
		case MODE_Z:
			mOverlay.setBackgroundColor(0xffffffaa);
			break;
		case MODE_O:
			mOverlay.setBackgroundColor(0xffaaaaff);
			break;
		}
	}
	
	private void setStrokeIncreaseMount(int value) {
		if (value > 0)
			mOverlay.setImageResource(android.R.drawable.btn_plus);
		else
			mOverlay.setImageResource(android.R.drawable.btn_minus);
		mStrokeIncreaseMount = value;
	}
	
    private void showPopupOnScreen(int x, int y) {
		int popupWidth = mOverlay.getDrawable().getIntrinsicWidth();
		int popupHeight = mOverlay.getDrawable().getIntrinsicHeight();
		getLocationOnScreen(mLocationXY);
		int left = mLocationXY[0] + x - popupWidth / 2;
		int top = mLocationXY[1] + y - popupHeight * 2;
    	if (mOverlayPopup.isShowing()) {
    		mOverlayPopup.update(left, top, -1, -1);
    	} else {
    		mOverlayPopup.setWidth(popupWidth);
    		mOverlayPopup.setHeight(popupHeight);
    		mOverlayPopup.showAtLocation(this, Gravity.NO_GRAVITY, left, top);
    	}
    }
    
    private void dismissPopupOnScreen() {
    	mOverlayPopup.dismiss();
    }
}
