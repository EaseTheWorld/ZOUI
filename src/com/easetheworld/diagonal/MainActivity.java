package com.easetheworld.diagonal;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import dev.easetheworld.ui.zo.StrokeGestureDetector;

public class MainActivity extends Activity {

    private StrokeGestureDetector mStrokeDetector;
    private TextView mTextView1;
    private TextView mTextViewMode;
    private PopupWindow mOverlayPopup;
    private ImageView mOverlay;
    
    private static final int MODE_STROKE_START_INC_HOLD = 0;
    private static final int MODE_CURVE_SMOOTH_MOVE_INC_BROKEN = 1;
    private static final int MODE_COUNT = 2;
    private int mMode;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView1 = (TextView)findViewById(android.R.id.text1);
        mTextView1.setOnTouchListener(mDragListener);
        
        mTextViewMode = (TextView)findViewById(android.R.id.text2);
        
        mOverlay= (ImageView) LayoutInflater.from(this).inflate(R.layout.gesture_overlay, null);
        mOverlayPopup = new PopupWindow(mOverlay);
        
        mStrokeDetector = new StrokeGestureDetector(this, mStrokeListener);
        
        updateMode(MODE_STROKE_START_INC_HOLD);
    }
	
	private View.OnTouchListener mDragListener = new View.OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (mStrokeDetector.onTouchEvent(event))
				return true;
			else
				return false;
		}
	};
	
	private int mCount = 0;
	
	private int mStrokeIncreaseMount;
	private int mStrokeSensitivityFactor;
	private int mStrokeSensitivity = 1;
	
	private void updateTextView() {
		if (++mStrokeSensitivityFactor >= mStrokeSensitivity)
			mStrokeSensitivityFactor = 0;
		if (mStrokeSensitivityFactor == 0) {
			mCount += mStrokeIncreaseMount;
			mTextView1.setText("Count : "+mCount);
		}
	}
	
	private void updateMode(int mode) {
		mCount = 0;
		mMode = mode;
		switch(mMode) {
		case MODE_STROKE_START_INC_HOLD:
			mStrokeDetector.setHoldEnabled(true);
    		mStrokeSensitivity = 1;
	    	mTextViewMode.setText("Mode Z : Zigzag and Hold");
			break;
		case MODE_CURVE_SMOOTH_MOVE_INC_BROKEN:
			mStrokeDetector.setHoldEnabled(false);
    		mStrokeSensitivity = 3;
	    	mTextViewMode.setText("Mode O : Circle and Reverse");
			break;
		}
	}
	
	private StrokeGestureDetector.BaseGestureDetector mStrokeListener = new StrokeGestureDetector.BaseGestureDetector() {
		
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
			case MODE_STROKE_START_INC_HOLD:
				updateTextView();
				break;
			}
			return false;
		}
		
		@Override
		public boolean onStrokeMove(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			android.util.Log.i("Stroke", "Move e1="+e1+", e2="+e2+", dx="+distanceX+", dy="+distanceY);
	    	switch(mMode) {
	    	case MODE_CURVE_SMOOTH_MOVE_INC_BROKEN:
				updateTextView();
	    		break;
	    	}
	    	showPopupOnScreen(mTextView1, e2);
			return false;
		}
		
		@Override
		public boolean onStrokeEnd(MotionEvent e1, MotionEvent e2) {
			android.util.Log.i("Stroke", "End "+e2.getX()+","+e2.getY()+" from "+e1.getX()+","+e1.getY());
	    	switch(mMode) {
	    	case MODE_CURVE_SMOOTH_MOVE_INC_BROKEN:
				setStrokeIncreaseMount(-mStrokeIncreaseMount);
	    		break;
	    	}
			return false;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			android.util.Log.i("Stroke", "SingleTapUp "+e);
	    	updateMode((mMode + 1) % MODE_COUNT);
			return false;
		}
		
		@Override
		public void onHold() {
			android.util.Log.i("Stroke", "Hold");
			switch(mMode) {
			case MODE_STROKE_START_INC_HOLD:
				setStrokeIncreaseMount(-mStrokeIncreaseMount);
				break;
			}
		}
	};
	
	private void setStrokeIncreaseMount(int value) {
		if (value > 0)
			mOverlay.setImageResource(android.R.drawable.btn_plus);
		else
			mOverlay.setImageResource(android.R.drawable.btn_minus);
		mOverlay.setAlpha(128);
		mStrokeIncreaseMount = value;
		mStrokeSensitivityFactor = 0;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private int[] mTempXY = null;
    
    private void showPopupOnScreen(View v, MotionEvent ev) {
		int popupWidth = mOverlay.getDrawable().getIntrinsicWidth();
		int popupHeight = mOverlay.getDrawable().getIntrinsicHeight();
		if (mTempXY == null) {
			mTempXY = new int[2];
    		v.getLocationOnScreen(mTempXY);
		}
		int left = mTempXY[0] + (int)ev.getX() - popupWidth / 2;
		int top = mTempXY[1] + (int)ev.getY() - popupHeight * 2;
    	if (mOverlayPopup.isShowing()) {
    		mOverlayPopup.update(left, top, -1, -1);
    	} else {
    		mOverlayPopup.setWidth(popupWidth);
    		mOverlayPopup.setHeight(popupHeight);
    		mOverlayPopup.showAtLocation(v, Gravity.NO_GRAVITY, left, top);
    	}
    }
    
    private void dismissPopupOnScreen() {
    	mOverlayPopup.dismiss();
    }
    
}