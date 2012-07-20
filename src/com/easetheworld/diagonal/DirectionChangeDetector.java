package com.easetheworld.diagonal;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

// wrapper of GestureDector to add onScrollEnd()
public class DirectionChangeDetector {
	
	private final GestureDetector mGestureDetector;
	private final DirectionGestureListener mGestureListener;
		
	private static final double MINIMUM_ANGLE_DEGREES_DIFFERENCE = 60;
	
	public static interface DirectionChangeListener {
		public void onDown();
		public void onDirectionChanged(double angleDegrees);
	}
	
	public DirectionChangeDetector(Context context, DirectionChangeListener listener) {
		mGestureListener = new DirectionGestureListener(listener, MINIMUM_ANGLE_DEGREES_DIFFERENCE);
		mGestureDetector = new GestureDetector(context, mGestureListener);
	}

	public boolean onTouchEvent(MotionEvent ev) {
		boolean ret = mGestureDetector.onTouchEvent(ev);
		if (!ret && ev.getActionMasked() == MotionEvent.ACTION_UP) {
			mGestureListener.onScrollEnd(ev);
		}
		return ret;
	}

	private static class DirectionGestureListener implements GestureDetector.OnGestureListener {
	
		private DirectionChangeListener mTurningBackListener;
		
		private float mStartX;
		private float mStartY;
		
		private double mThresholdCosineSquare;

		public DirectionGestureListener(DirectionChangeListener listener, double minAngleDegreesDiff) {
			super();
			mTurningBackListener = listener;
			
			double cosine = Math.cos(Math.toRadians(minAngleDegreesDiff));
			mThresholdCosineSquare = cosine * cosine;
		}

		@Override
		public boolean onDown(MotionEvent ev) {
			mLastLength = -1;
			mStartX = ev.getX();
			mStartY = ev.getY();
			mTurningBackListener.onDown();
			return true;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			float x = e2.getX();
			float y = e2.getY();
			float result = cosineSquare(x - mStartX, y - mStartY, -distanceX, -distanceY);
			if (result < mThresholdCosineSquare) {
				fireTurningBack(x, y);
			}
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return true; // consume here so onScrollEnd() will not be called.
		}

		public void onScrollEnd(MotionEvent e) {
			fireTurningBack(e.getX(), e.getY());
		}
		
		private static final int LENGTH_THRESHOLD_FACTOR = 5; // new length * threshold should be bigger than last length
		private double mLastLength;
		
		private void fireTurningBack(float x, float y) {
			float vx = x - mStartX;
			float vy = y - mStartY;
			float length = magnitudeSquare(vx, vy);
			if (length * LENGTH_THRESHOLD_FACTOR > mLastLength) { // long enough
				mTurningBackListener.onDirectionChanged(Math.toDegrees(Math.atan2(vy, vx)));
				mLastLength = length;
			}
			mStartX = x;
			mStartY = y;
		}

		private static float cosineSquare(float v1x, float v1y, float v2x, float v2y) {
			float innerProduct = v1x * v2x + v1y * v2y;
			float result = (innerProduct * innerProduct) / (magnitudeSquare(v1x, v1y) * magnitudeSquare(v2x, v2y));
			if (innerProduct < 0) // keep the sign even though its magnitude is squared.
				result = -result;
			return result;
		}
		
		private static float magnitudeSquare(float vx, float vy) {
			return vx * vx + vy * vy;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}
	}
}