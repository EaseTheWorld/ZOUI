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
import dev.easetheworld.strokegesturedetector.StrokeGestureDetector;

public class MainActivity extends Activity {

    private StrokeGestureDetector mStrokeDetector;
    private TextView mTextView1;
    private TextView mTextViewMode;
    private PopupWindow mOverlayPopup;
    private ImageView mOverlay;
    
    private static final int MODE_STROKE_START_INC_HOLD = 0;
    private static final int MODE_CURVE_SMOOTH_MOVE_INC_BROKEN = 1;
    private static final int MODE_COUNT = 2;
    private int mMode = MODE_STROKE_START_INC_HOLD;
    
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
        updateTextView();
        updateMode();
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
	
	private int mStrokeIncreaseMount = 1;
	
	private void updateTextView() {
		mTextView1.setText("Stroke("+(mStrokeIncreaseMount > 0 ? "+"+mStrokeIncreaseMount : "" + mStrokeIncreaseMount)+") : "+mCount);
	}
	
	private void updateMode() {
    	mTextViewMode.setText("Mode="+mMode);
	}
	
	private StrokeGestureDetector.BaseGestureDetector mStrokeListener = new StrokeGestureDetector.BaseGestureDetector() {
		
		@Override
		public void onDown(MotionEvent e) {
			android.util.Log.i("Stroke", "Down");
			mStrokeIncreaseMount = 1;
		}
		
		@Override
		public void onUp(MotionEvent e) {
			android.util.Log.i("Stroke", "Up");
			mTextView1.setText("Stroke : "+mCount+ " Done.");
		}
		
		@Override
		public boolean onStrokeStart(MotionEvent e) {
			android.util.Log.i("Stroke", "Start "+e.getX()+","+e.getY());
			switch(mMode) {
			case MODE_STROKE_START_INC_HOLD:
				mCount += mStrokeIncreaseMount;
				updateTextView();
				break;
			}
			return false;
		}
		
		@Override
		public boolean onStrokeMove(MotionEvent e, float distanceX, float distanceY) {
//			android.util.Log.i("Stroke", "Move "+distance);
			return false;
		}
		
		@Override
		public boolean onStrokeEnd(MotionEvent e1, MotionEvent e2) {
			android.util.Log.i("Stroke", "End "+e2.getX()+","+e2.getY()+" from "+e1.getX()+","+e1.getY());
			return false;
		}
		
		
		
		@Override
		public boolean onCurveSmooth(MotionEvent e, float distanceX, float distanceY, float cosineSquare) {
	    	switch(mMode) {
	    	case MODE_CURVE_SMOOTH_MOVE_INC_BROKEN:
				mCount += mStrokeIncreaseMount;
				updateTextView();
	    		break;
	    	}
			return super.onCurveSmooth(e, distanceX, distanceY, cosineSquare);
		}

		@Override
		public boolean onCurveBroken(MotionEvent e, float distanceX, float distanceY, float cosineSquare) {
			android.util.Log.i("Curve", "Broken "+e+" "+(distanceX*distanceX+distanceY*distanceY)+", cos="+cosineSquare);
	    	switch(mMode) {
	    	case MODE_CURVE_SMOOTH_MOVE_INC_BROKEN:
				mStrokeIncreaseMount = -mStrokeIncreaseMount;
				updateTextView();
	    		break;
	    	}
			return super.onCurveBroken(e, distanceX, distanceY, cosineSquare);
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			android.util.Log.i("Stroke", "SingleTapUp "+e);
			mCount = 0;
	    	mMode = (mMode + 1) % MODE_COUNT;
	    	switch(mMode) {
	    	case MODE_STROKE_START_INC_HOLD:
	    		mStrokeDetector.stroke();
	    		break;
	    	case MODE_CURVE_SMOOTH_MOVE_INC_BROKEN:
	    		mStrokeDetector.curve();
	    		break;
	    	}
	    	updateTextView();
	    	updateMode();
			return false;
		}
		
		@Override
		public void onHold() {
			android.util.Log.i("Stroke", "Hold");
			switch(mMode) {
			case MODE_STROKE_START_INC_HOLD:
				mStrokeIncreaseMount = -mStrokeIncreaseMount;
				updateTextView();
				break;
			}
		}
	};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private void showPopupOnScreen(View v, MotionEvent ev) {
    	v.getLocationOnScreen(mTempXY);
    	int popupWidth = mOverlay.getDrawable().getIntrinsicWidth() * 4;
    	int popupHeight = mOverlay.getDrawable().getIntrinsicHeight() * 4;
    	mOverlayPopup.setWidth(popupWidth);
    	mOverlayPopup.setHeight(popupHeight);
    	mOverlayPopup.showAtLocation(v, Gravity.NO_GRAVITY, mTempXY[0] + (int)ev.getX() - popupWidth / 2, mTempXY[1] + (int)ev.getY() - popupHeight / 2);
    }
    
    private void dismissPopupOnScreen() {
    	mOverlayPopup.dismiss();
    }
    
    private int[] mTempXY = new int[2];
    private void dumpViewLocation(View v) {
    	v.getLocationInWindow(mTempXY);
    	android.util.Log.i("nora", "--getLocationInWindow "+mTempXY[0]+","+mTempXY[1]);
    	v.getLocationOnScreen(mTempXY);
    	android.util.Log.i("nora", "  getLocationOnScreen "+mTempXY[0]+","+mTempXY[1]);
    	android.util.Log.i("nora", "  getLeft="+v.getLeft()+", getTop="+v.getTop());
    }
}