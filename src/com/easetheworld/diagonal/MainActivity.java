package com.easetheworld.diagonal;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends Activity {

    private GestureDetector mGestureDetector;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mGestureDetector = new GestureDetector(this, new DirectionChangeListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
      if (mGestureDetector.onTouchEvent(event))
        return true;
      else
        return false;
    }
    
    public void clickHandler(View v) {
    	
    }
    
    private static class DirectionChangeListener extends GestureDetector.SimpleOnGestureListener{
    	// E -> S -> W -> N
    	// odd number are diagonal
    	// 3rd bit is sign bit (except sign bit they are same direction)
    	private static final int DIR_NONE = -1;
    	private static final int DIR_E = 0;
    	private static final int DIR_SE = 1;
    	private static final int DIR_S = 2;
    	private static final int DIR_SW = 3;
    	private static final int DIR_W = 4;
    	private static final int DIR_NW = 5;
    	private static final int DIR_N = 6;
    	private static final int DIR_NE = 7;
    	private int mCurDirection = -1;
    	
    	// if dy/dx is diagonal, it should be A/B < dy/dx < B/A
    	private int DIAGONAL_FACTOR_A = 2;
    	private int DIAGONAL_FACTOR_B = 3;
    	private float mAverageDistanceX;
    	private float mAverageDistanceY;
    	
    	private static final int STATE_FIRST_DRAGGING = 0;
    	private static final int STATE_DRAGGING_SAME = 1;
    	private static final int STATE_CHANGING = 2;
    	private static final int STATE_DRAGGING_OPPOSITE = 3;
    	private int mState;
    	private int mPrevResult;
    	
    	@Override
    	public boolean onDown(MotionEvent ev) {
    		Log.d("onDownd",ev.toString()+", time="+ev.getDownTime()+", "+ev.getEventTime());
    		mCurDirection = DIR_NONE;
    		mPrevResult = SAME_DIRECTION;
    		setState(STATE_FIRST_DRAGGING);
    		return true;
    	}
    	@Override
    	public boolean onSingleTapUp(MotionEvent ev) {
    		Log.d("onSingleTapUp",ev.toString());
    		return true;
    	}
    	@Override
    	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//    		Log.d("onScroll","e1="+e1);
//    		Log.d("onScroll","e2="+e2);
    		distanceX = -distanceX;
    		distanceY = -distanceY;
    		// update average
    		if (mState == STATE_FIRST_DRAGGING) {
	    		mAverageDistanceX = e2.getX() - e1.getX();
	    		mAverageDistanceY = e2.getY() - e1.getY();
    		}
    		// compare current and average
    		int result = compareDirection(distanceX, distanceY, mAverageDistanceX, mAverageDistanceY);
    		Log.d("onScroll", "result="+result+", avg dx="+mAverageDistanceX+", dy="+mAverageDistanceY+", cur dx="+distanceX+", dy="+distanceY+", time="+(e2.getEventTime()-e2.getDownTime()));
    		if (mPrevResult == SAME_DIRECTION && result == OPPOSITE_DIRECTION) {
	    		Log.e("onScroll", "Changed1");
	    		mPrevResult = result;
    		} else if (mPrevResult == OPPOSITE_DIRECTION && result == SAME_DIRECTION) {
	    		Log.e("onScroll", "Changed2");
	    		mPrevResult = result;
    		}
//    		switch(result) {
//    		case SAME_DIRECTION:
//    			if (mState == STATE_CHANGING)
//    				setState(STATE_DRAGGING_SAME);
//    			break;
//    		}
//    		if (result == DIFFERENT_DIRECTION) {
//    			setState(STATE_CHANGING);
//    		} else {
//    			if (result == SAME_DIRECTION)
//					setState(STATE_DRAGGING_SAME);
//    			else
//					setState(STATE_DRAGGING_OPPOSITE);
//    		}
    		return true;
    	}
    	
    	private void setState(int state) {
    		if (mState != state) {
	    		mState = state;
	    		Log.w("nora", "State "+mState);
    		}
    	}
    	
    	private static final int SAME_DIRECTION = 1;
    	private static final int OPPOSITE_DIRECTION = -1;
    	private static final int DIFFERENT_DIRECTION = 0;
    	private static int compareDirection(float dX1, float dY1, float dX2, float dY2) {
    		// get cosine square from inner product
    		double innerProduct = dX1*dX2 + dY1*dY2;
    		double cosineSquare = (innerProduct * innerProduct) / ((dX1*dX1 + dY1*dY1) * (dX2*dX2 + dY2*dY2));
//    		Log.i("nora", "cosineSquare = "+cosineSquare+", inner="+innerProduct);
    		if (cosineSquare > 0.75) { // less than 30 degree
    			if (innerProduct > 0)
    				return SAME_DIRECTION ;
    			else
    				return OPPOSITE_DIRECTION;
    		} else
	    		return DIFFERENT_DIRECTION;
    	}
    	
    	public boolean onDirectionChange(int oldDirection, int newDirection) {
    		return true;
    	}
/*    	
    	@Override
    	public void onShowPress(MotionEvent ev) {
    		Log.d("onShowPress",ev.toString());
    	}
    	@Override
    	public void onLongPress(MotionEvent ev) {
    		Log.d("onLongPress",ev.toString());
    	}
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    		Log.d("onFling","vx="+velocityX+", vy="+velocityY);
    		return true;
    	}
    	*/
    }
}
