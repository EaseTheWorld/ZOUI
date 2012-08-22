package dev.easetheworld.ui.zo;

import android.content.Context;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;

public class ZOTouchListener implements View.OnTouchListener {
	
	private static final String TAG = "ZOTouchListener";
	
    private static final int MODE_ZO = 0;
    public static final int MODE_Z = 1;
    public static final int MODE_O = 2;
	private int mStartMode;
    private int mMode;
    
	private static final long THRESHOLD_START_MODE_O_INTERVAL = 150;
    
    private static final int DIRECTION_FORWARD = 1;
    private static final int DIRECTION_BACKWARD = -DIRECTION_FORWARD;
    private int mDirection;
    
	private float mDistanceSum;
	private int mDistanceThreshold;
	private static final int DEFAULT_DISTANCE_THRESHOLD = 20;
    
    private PopupWindow mOverlayPopup;
    private ImageView mOverlay;
	
    private StrokeGestureDetector mStrokeDetector;
    
    private int[] mLocationXY = new int[2];
    
	private Dispatcher mDispatcher;
	
	public ZOTouchListener(Context context, Dispatcher dispatcher) {
		this(context, dispatcher, DEFAULT_DISTANCE_THRESHOLD);
	}
    
	public ZOTouchListener(Context context, Dispatcher dispatcher, int distanceThreshold) {
		mDispatcher = dispatcher;
		
        mStrokeDetector = new StrokeGestureDetector(context, mStrokeListener);
        mOverlay = new ImageView(context);
        mOverlay.setImageResource(android.R.drawable.btn_plus);
        mOverlayPopup = new PopupWindow(mOverlay);
        
        mDistanceThreshold = distanceThreshold;
        
        mStartMode = MODE_ZO;
	}
	
	public ZOTouchListener setMode(int mode) {
		mStartMode = mode;
		return this;
	}
	
	public static interface Dispatcher {
		void onDown(View v);
		void onMove(int mode, View v, int value);
		void onUp(View v);
		void onClick(View v);
	}
	
	private View mMotionTarget;
	
	private long mDownTime;
	
	private StrokeGestureDetector.OnStrokeGestureListener mStrokeListener = new StrokeGestureDetector.OnStrokeGestureListener() {
		
		@Override
		public void onDown(MotionEvent e) {
			Log.i(TAG, "Down "+e);
			if (mStartMode == MODE_ZO) {
				mDownTime = e.getEventTime();
			} else {
				mDownTime = -1;
			}
			setModeInternal(mStartMode);
			setDirection(DIRECTION_FORWARD);
			mDispatcher.onDown(mMotionTarget);
		}
		
		@Override
		public void onUp(MotionEvent e) {
			Log.i(TAG, "Up "+e);
	    	dismissPopupOnScreen();
			mDispatcher.onUp(mMotionTarget);
		}
		
		@Override
		public boolean onStrokeStart(MotionEvent e, int index, float directionX, float directionY) {
			Log.i(TAG, "Start "+index+" "+e.getX()+","+e.getY()+", direction "+directionX+", "+directionY);
			if (index == 0 && mDownTime != -1) { // check first down time
				if ((e.getEventTime() - mDownTime) < THRESHOLD_START_MODE_O_INTERVAL)
					setModeInternal(MODE_Z);
				else
					setModeInternal(MODE_O);
				mDownTime = -1;
			}
			
			switch(mMode) {
			case MODE_Z:
				if (index == 0) {
					if (directionY > 0)
						setDirection(DIRECTION_FORWARD);
					else
						setDirection(DIRECTION_BACKWARD);
				}
				mDispatcher.onMove(mMode, mMotionTarget, mDirection);
				break;
			case MODE_O:
				mDistanceSum = 0;
		    	switch(mMode) {
		    	case MODE_O:
		    		if (index > 0)
			    		setDirection(-mDirection);
		    		break;
		    	}
				break;
			}
			return false;
		}
		
		@Override
		public boolean onStrokeMove(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//			Log.i(TAG, "Move e1="+e1+", e2="+e2+", distance "+distanceX+", "+distanceY);
	    	switch(mMode) {
	    	case MODE_O:
	    		mDistanceSum += FloatMath.sqrt(distanceX * distanceX + distanceY * distanceY);
	    		int diff = (int)(mDistanceSum / mDistanceThreshold);
				mDistanceSum = mDistanceSum % mDistanceThreshold;
				mDispatcher.onMove(mMode, mMotionTarget, mDirection * diff);
	    		break;
	    	}
	    	showPopupOnScreen((int)e2.getX(), (int)e2.getY());
			return false;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			Log.i(TAG, "SingleTapUp "+e);
			mDispatcher.onClick(mMotionTarget);
			return false;
		}
	};
	
	private void setModeInternal(int mode) {
		mMode = mode;
//		switch(mMode) {
//		case MODE_Z:
//			mOverlay.setBackgroundColor(0xffffffaa);
//			break;
//		case MODE_O:
//			mOverlay.setBackgroundColor(0xffaaaaff);
//			break;
//		}
	}
	
	private void setDirection(int direction) {
		if (direction > 0)
			mOverlay.setImageResource(android.R.drawable.btn_plus);
		else
			mOverlay.setImageResource(android.R.drawable.btn_minus);
		mDirection = direction;
	}
	
	
    private void showPopupOnScreen(int x, int y) {
		int popupWidth = mOverlay.getDrawable().getIntrinsicWidth();
		int popupHeight = mOverlay.getDrawable().getIntrinsicHeight();
		mMotionTarget.getLocationOnScreen(mLocationXY);
		int left = mLocationXY[0] + x - popupWidth / 2;
		int top = mLocationXY[1] + y - popupHeight * 2;
    	if (mOverlayPopup.isShowing()) {
    		mOverlayPopup.update(left, top, -1, -1);
    	} else {
    		mOverlayPopup.setWidth(popupWidth);
    		mOverlayPopup.setHeight(popupHeight);
    		mOverlayPopup.showAtLocation(mMotionTarget, Gravity.NO_GRAVITY, left, top);
    	}
    }
    
    private void dismissPopupOnScreen() {
    	mOverlayPopup.dismiss();
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN)
			mMotionTarget = v;
		boolean ret = mStrokeDetector.onTouchEvent(event);
		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
			mMotionTarget = null;
		return ret;
	}
}