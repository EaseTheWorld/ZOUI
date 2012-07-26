package dev.easetheworld.ui.zo;

import android.content.Context;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;

public class ZOTouchViewController {
	
	private static final String TAG = "ZOTouchViewController";
	
    private static final int MODE_Z = 0;
    private static final int MODE_O = 1;
    private int mMode;
    
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
	
	public ZOTouchViewController(Context context, Dispatcher dispatcher) {
		this(context, dispatcher, DEFAULT_DISTANCE_THRESHOLD);
	}
    
	public ZOTouchViewController(Context context, Dispatcher dispatcher, int distanceThreshold) {
		mDispatcher = dispatcher;
		
        mStrokeDetector = new StrokeGestureDetector(context, mStrokeListener);
        mOverlay = new ImageView(context);
        mOverlay.setImageResource(android.R.drawable.btn_plus);
        mOverlayPopup = new PopupWindow(mOverlay);
        
        mDistanceThreshold = distanceThreshold;
	}
	
	public static interface Dispatcher {
		void onDispatchZ(View v, int value);
		void onDispatchO(View v, int value);
	}
	
	private View mTouchedView;
	
	private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			mTouchedView = v;
			return mStrokeDetector.onTouchEvent(event);
		}
	};
	
	public void addView(View v) {
		v.setOnTouchListener(mOnTouchListener);
	}
	
	
	public void removeView(View v) {
		v.setOnTouchListener(null);
	}
	
	private StrokeGestureDetector.OnStrokeGestureListener mStrokeListener = new StrokeGestureDetector.OnStrokeGestureListener() {
		
		@Override
		public void onDown(MotionEvent e) {
			Log.i(TAG, "Down");
			setDirection(DIRECTION_FORWARD);
	        setMode(MODE_Z);
		}
		
		@Override
		public void onUp(MotionEvent e) {
			Log.i(TAG, "Up");
	    	dismissPopupOnScreen();
		}
		
		@Override
		public boolean onStrokeStart(MotionEvent e) {
			Log.i(TAG, "Start "+e.getX()+","+e.getY());
			switch(mMode) {
			case MODE_Z:
				mDispatcher.onDispatchZ(mTouchedView, mDirection);
				break;
			case MODE_O:
				mDistanceSum = 0;
				break;
			}
			return false;
		}
		
		@Override
		public boolean onStrokeMove(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//			Log.i(TAG, "Move e1="+e1+", e2="+e2+", dx="+distanceX+", dy="+distanceY);
	    	switch(mMode) {
	    	case MODE_O:
	    		mDistanceSum += FloatMath.sqrt(distanceX * distanceX + distanceY * distanceY);
	    		int diff = (int)(mDistanceSum / mDistanceThreshold);
				mDistanceSum = mDistanceSum % mDistanceThreshold;
				mDispatcher.onDispatchO(mTouchedView, mDirection * diff);
	    		break;
	    	}
	    	showPopupOnScreen((int)e2.getX(), (int)e2.getY());
			return false;
		}
		
		@Override
		public boolean onStrokeEnd(MotionEvent e1, MotionEvent e2) {
			Log.i(TAG, "End "+e2.getX()+","+e2.getY()+" from "+e1.getX()+","+e1.getY());
	    	switch(mMode) {
	    	case MODE_O:
	    		setDirection(-mDirection);
	    		break;
	    	}
			return false;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			Log.i(TAG, "SingleTapUp "+e);
			return false;
		}
		
		@Override
		public boolean onHold(float x, float y) {
			Log.i(TAG, "Hold "+x+", "+y);
			switch(mMode) {
			case MODE_Z:
				if (mStrokeDetector.isStroking())
		    		setDirection(-mDirection);
				else {
					setMode(MODE_O);
					showPopupOnScreen((int)x, (int)y);
				}
				return true;
			}
			return false;
		}
	};
	
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
		mTouchedView.getLocationOnScreen(mLocationXY);
		int left = mLocationXY[0] + x - popupWidth / 2;
		int top = mLocationXY[1] + y - popupHeight * 2;
    	if (mOverlayPopup.isShowing()) {
    		mOverlayPopup.update(left, top, -1, -1);
    	} else {
    		mOverlayPopup.setWidth(popupWidth);
    		mOverlayPopup.setHeight(popupHeight);
    		mOverlayPopup.showAtLocation(mTouchedView, Gravity.NO_GRAVITY, left, top);
    	}
    }
    
    private void dismissPopupOnScreen() {
    	mOverlayPopup.dismiss();
    }
}
