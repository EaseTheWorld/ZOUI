package dev.easetheworld.ui.zo;

import android.content.Context;
import android.graphics.PointF;
import android.view.ViewConfiguration;
import dev.easetheworld.recentbuffer.RecentBuffer;


public class StrokeTracker {
	
	private static final int MIN_POINTS_FOR_VECTOR = 3;
	private static final float MIN_COSINE_SQUARE_FOR_NEW_STROKE = (float)cosineSquare(90);
	
	private final float mMinLengthForVector;
	private final float mMinLengthForStroke;
	
	private final RecentBuffer<PointF> mPointBuffer;
	private final VectorF mV1;
	private final VectorF mV2;
	
	private final PointF mTurningPoint;
	private final VectorF mStrokeStart;
	
	public StrokeTracker(Context context) {
        mPointBuffer = new RecentBuffer<PointF>(MIN_POINTS_FOR_VECTOR * 2 - 1, PointF.class);
        mV1 = new VectorF();
        mV2 = new VectorF();
        
        mTurningPoint = new PointF();
        mStrokeStart = new VectorF();
        
		float touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMinLengthForStroke = touchSlop * touchSlop;
        mMinLengthForVector = mMinLengthForStroke / 16f;
	}
	
	private static final int STROKE_TURN = 0;
	public static final int STROKE_START = 1;
	public static final int STROKE_MOVE = 2;
	private int mState;
	
	private static final float COSINE_FOR_INVALID_VECTORS = 1f;
	private float mAngle;
	
	private int mPointIndex;
	
	public void addTouchDown(float x, float y) {
		mV1.clear();
		mV2.clear();
		
		mPointIndex = 0;
		mPointBuffer.clear();
		mPointBuffer.obtain().set(x, y);
		
		mTurningPoint.set(x, y);
		mState = STROKE_TURN;
	}
	
	public int addTouchMove(float x, float y) {
		mPointBuffer.obtain().set(x, y);
		
		PointF e = mPointBuffer.get(0);
		PointF m = mPointBuffer.get(MIN_POINTS_FOR_VECTOR - 1);
		PointF s = mPointBuffer.get((MIN_POINTS_FOR_VECTOR - 1) * 2);
//		android.util.Log.i("nora", "add x="+x+", y="+y+", index="+mPointIndex+", s="+(s==null?s:s.x+","+s.y)+", m="+(m==null?m:m.x+","+m.y)+", e="+(e==null?e:e.x+","+e.y));
		mPointIndex++;
		
		float cosSqr = COSINE_FOR_INVALID_VECTORS;
		mV1.checkAndSet(s, m, mMinLengthForVector);
		if (mV2.checkAndSet(m, e, mMinLengthForVector)) {
			// update angle
			cosSqr = mV1.cosineSquare(mV2);
//			android.util.Log.i("nora", "cosine="+cosSqr+" v1="+mV1+", v2="+mV2);
			if (cosSqr < MIN_COSINE_SQUARE_FOR_NEW_STROKE) {
//				android.util.Log.e("nora", "Turned");
				mTurningPoint.set(m.x, m.y);
				// remove the previous stroke part.
				mPointBuffer.removeSince(MIN_POINTS_FOR_VECTOR);
				mV1.clear();
				mState = STROKE_TURN;
			}
		}
		mAngle = cosSqr;
		
		switch(mState) {
		case STROKE_TURN:
			if (mStrokeStart.checkAndSet(mTurningPoint, e, mMinLengthForStroke)) { // long enough to be a stroke
//				android.util.Log.e("nora", "Started");
				mState = STROKE_START;
			}
			break;
		case STROKE_START:
			mState = STROKE_MOVE;
			break;
		}
		return mState;
	}
	
	public float getAngle() {
		return mAngle;
	}
	
	public int getState() {
		return mState;
	}
	
	public PointF getStrokeStartDirection() {
		return mStrokeStart;
	}
	
	private static class VectorF extends PointF {
		private float length;
		
		private VectorF() {
			super();
			clear();
		}
		
		private void set(float x, float y, float length) {
			set(x, y);
			this.length = length;
		}
		
		private void clear() {
			set(0, 0, 0);
		}
		
		private boolean checkAndSet(PointF p1, PointF p2, float minLength) {
			if (p1 == null || p2 == null)
				return false;
			float vx = p2.x - p1.x;
			float vy = p2.y - p1.y;
			float vl = vx * vx + vy * vy;
			if (vl > minLength) {
//				android.util.Log.w("nora", "    checkAndSet vx="+vx+", vy="+vy+", vl="+vl);
				set(vx, vy, vl);
				return true;
			} else {
//				android.util.Log.i("nora", "    checkAndSet vx="+vx+", vy="+vy+", vl="+vl);
				return false;
			}
		}
		
	    // keep the sign, square the magnitude
		private float cosineSquare(VectorF that) {
	    	if (this.length == 0 || that.length == 0)
	    		return COSINE_FOR_INVALID_VECTORS;
	    	float innerProduct = this.x * that.x + this.y * that.y;
	    	float result = (innerProduct * innerProduct) / (this.length * that.length);
	    	if (innerProduct < 0) // keep the sign even though its magnitude is squared.
	    		result = -result;
	    	return result;
		}

		@Override
		public String toString() {
			return x+", "+y+", length="+length;
		}
	}
    
    // keep the sign, square the magnitude
    private static double cosineSquare(double angdeg) {
    	double cosine = Math.cos(Math.toRadians(angdeg));
    	if (cosine > 0)
    		return cosine * cosine;
    	else
    		return -(cosine * cosine);
    }
}