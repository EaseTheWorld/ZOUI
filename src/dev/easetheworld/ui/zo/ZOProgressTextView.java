package dev.easetheworld.ui.zo;

import android.content.Context;
import android.util.AttributeSet;
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
    private static final int MODE_COUNT = 2;
    private int mMode;
    
    private PopupWindow mOverlayPopup;
    private ImageView mOverlay;
	
    private StrokeGestureDetector mStrokeDetector;
    
    private int[] mLocationXY = new int[2];

	public ZOProgressTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
        mStrokeDetector = new StrokeGestureDetector(getContext(), mStrokeListener);
        mOverlay= (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.gesture_overlay, null);
        mOverlayPopup = new PopupWindow(mOverlay);
        
        setMode(MODE_Z);
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
				update();
				break;
			}
			return false;
		}
		
		@Override
		public boolean onStrokeMove(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//			android.util.Log.i("Stroke", "Move e1="+e1+", e2="+e2+", dx="+distanceX+", dy="+distanceY);
	    	switch(mMode) {
	    	case MODE_O:
				update();
	    		break;
	    	}
	    	showPopupOnScreen(e2);
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
	    	setMode((mMode + 1) % MODE_COUNT);
			return false;
		}
		
		@Override
		public void onHold() {
			android.util.Log.i("Stroke", "Hold");
			switch(mMode) {
			case MODE_Z:
				setStrokeIncreaseMount(-mStrokeIncreaseMount);
				break;
			}
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
	private int mStrokeSensitivityFactor;
	private int mStrokeSensitivity = 1;
	
	private void update() {
		if (++mStrokeSensitivityFactor >= mStrokeSensitivity)
			mStrokeSensitivityFactor = 0;
		if (mStrokeSensitivityFactor == 0)
			setProgress(mProgress + mStrokeIncreaseMount);
	}
	
	private void setMode(int mode) {
		mMode = mode;
		switch(mMode) {
		case MODE_Z:
			mStrokeDetector.setHoldEnabled(true);
    		mStrokeSensitivity = 1;
    		setBackgroundColor(0xffffaaaa);
			break;
		case MODE_O:
			mStrokeDetector.setHoldEnabled(false);
    		mStrokeSensitivity = 3;
    		setBackgroundColor(0xffaaaaff);
			break;
		}
	}
	
	private void setStrokeIncreaseMount(int value) {
		if (value > 0)
			mOverlay.setImageResource(android.R.drawable.btn_plus);
		else
			mOverlay.setImageResource(android.R.drawable.btn_minus);
		mOverlay.setAlpha(128);
		mStrokeIncreaseMount = value;
		mStrokeSensitivityFactor = 0;
	}
    
    private void showPopupOnScreen(MotionEvent ev) {
		int popupWidth = mOverlay.getDrawable().getIntrinsicWidth();
		int popupHeight = mOverlay.getDrawable().getIntrinsicHeight();
		getLocationOnScreen(mLocationXY);
		int left = mLocationXY[0] + (int)ev.getX() - popupWidth / 2;
		int top = mLocationXY[1] + (int)ev.getY() - popupHeight * 2;
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
