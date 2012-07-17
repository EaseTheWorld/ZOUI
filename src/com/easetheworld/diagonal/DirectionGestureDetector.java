package com.easetheworld.diagonal;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

// wrapper of GestureDector to add onFinished()
public class DirectionGestureDetector {
	
	private final GestureDetector mGestureDetector;
	private final DirectionGestureListener mGestureListener;
	
	public static interface DirectionChangeListener {
		public void onTurningBack(int count, float sx, float sy, float ex, float ey);
	}
	
	public DirectionGestureDetector(Context context, DirectionChangeListener listener) {
		mGestureListener = new DirectionGestureListener(listener);
		mGestureDetector = new GestureDetector(context, mGestureListener);
	}

	public boolean onTouchEvent(MotionEvent ev) {
		boolean ret = mGestureDetector.onTouchEvent(ev);
		if (!ret && ev.getActionMasked() == MotionEvent.ACTION_UP) {
			mGestureListener.onScrollEnd(ev);
		}
		return ret;
	}

	public static class DirectionGestureListener extends GestureDetector.SimpleOnGestureListener {
	
		private DirectionChangeListener mTurningBackListener;
		
		private float mStartX;
		private float mStartY;
		private int mTurningCount;

		public DirectionGestureListener(DirectionChangeListener listener) {
			super();
			mTurningBackListener = listener;
		}

		@Override
		public boolean onDown(MotionEvent ev) {
			mTurningCount = 0;
			mLastLength = 0;
			mStartX = ev.getX();
			mStartY = ev.getY();
			fireTurningBack(mStartX, mStartY, true);
			return true;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			float curX = e2.getX();
			float curY = e2.getY();
			double result = innerProduct(curX - mStartX, curY - mStartY, -distanceX, -distanceY);
			if (result < 0) { // angle is negative, which means direction is opposite.
				fireTurningBack(curX, curY, false);
				mStartX = curX;
				mStartY = curY;
			}
			return true;
		}
		
		public void onScrollEnd(MotionEvent e) {
			fireTurningBack(e.getX(), e.getY(), false);
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return true; // consume here so onScrollEnd() will not be called.
		}
		
		private static final int LENGTH_THRESHOLD_FACTOR = 5; // new length * threshold should be bigger than last length
		private double mLastLength;
		
		private void fireTurningBack(float curX, float curY, boolean force) {
			double length = distance(mStartX, mStartY, curX, curY);
			if (force || (length * LENGTH_THRESHOLD_FACTOR > mLastLength)) { // long enough
				mTurningBackListener.onTurningBack(mTurningCount++, mStartX, mStartY, curX, curY);
				mLastLength = length;
			}
		}

		// Math util. Inner product between 2 vectors
		private static double innerProduct(float dX1, float dY1, float dX2, float dY2) {
			return dX1*dX2 + dY1*dY2;
		}
		
		// Math util. Distance square between 2 points
		private static double distance(float sx, float sy, float ex, float ey) {
			float dx = ex - sx;
			float dy = ey - sy;
			return (dx * dx) + (dy * dy);
		}
	}
}