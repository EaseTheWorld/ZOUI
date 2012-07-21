package dev.easetheworld.strokegesturedetector;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class StrokeGestureDetector {
	
    public interface OnStrokeGestureListener {

        /**
         * Notified when a tap occurs with the down {@link MotionEvent}
         * that triggered it. This will be triggered immediately for
         * every down event. All other events should be preceded by this.
         *
         * @param e The down motion event.
         */
        void onDown(MotionEvent e);
        
		boolean onStrokeStart(MotionEvent e);
		
		boolean onStrokeMove(MotionEvent e, float distanceX, float distanceY);
        
		boolean onStrokeEnd(MotionEvent e1, MotionEvent e2);
		
		boolean onCurveSmooth(MotionEvent e, float distanceX, float distanceY, float cosineSquare);
		
		boolean onCurveBroken(MotionEvent e, float distanceX, float distanceY, float cosineSquare);
		
		void onHold();
		
        void onUp(MotionEvent e);

        /**
         * Notified when a tap occurs with the up {@link MotionEvent}
         * that triggered it.
         *
         * @param e The up motion event that completed the first tap
         * @return true if the event is consumed, else false
         */
        boolean onSingleTapUp(MotionEvent e);
    }
    
    // state
    private static final int STROKE_TURN = 0;
    private static final int STROKE = 1;
    
    private static final int CURVE_SMOOTH = 2;
    private static final int CURVE_BROKEN = 3;
    private int mCurrentState;
    private int mInitialState;
    
    // listener
    private final OnStrokeGestureListener mListener;
    
    // last position
    private float mLastMotionY;
    private float mLastMotionX;
    
    // touch threshold
    private int mBigSlopSquare; // stroke end-start
    private static final int SMALL_SLOP_DIVISOR = 32; // mBigSlopSquare / DIVISOR = mSmallSlopSquare;
    private int mSmallSlopSquare; // stroke hold, curve broken
    
    // single tap
    private boolean mIsSingleTap;
	
	// hold
    private boolean mIsHoldEnabled;
	private boolean mIsWaitingForHold;
    private static final int HOLD_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private final Handler mHoldHandler;

    // stroke
    private MotionEvent mStrokeStartEvent;
    private static final double MIN_ANGLE_DIFF_BETWEEN_STROKES = Math.toRadians(60);
	private double mThresholdCosineSquare;

    // curve
    private float mLastDistanceX;
    private float mLastDistanceY;
    
    /**
     * True if we are at a target API level of >= Froyo or the developer can
     * explicitly set it. If true, input events with > 1 pointer will be ignored
     * so we can work side by side with multitouch gesture detectors.
     */
    private boolean mIgnoreMultitouch;

    /**
     * Creates a GestureDetector with the supplied listener.
     * You may only use this constructor from a UI thread (this is the usual situation).
     * @see android.os.Handler#Handler()
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     *
     * @throws NullPointerException if {@code listener} is null.
     */
    public StrokeGestureDetector(Context context, OnStrokeGestureListener listener) {
        this(context, listener, context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.FROYO);
    }
    
    /**
     * Creates a GestureDetector with the supplied listener.
     * You may only use this constructor from a UI thread (this is the usual situation).
     * @see android.os.Handler#Handler()
     *
     * @param context the application's context
     * @param listener the listener invoked for all the callbacks, this must
     * not be null.
     * @param handler the handler to use
     * @param ignoreMultitouch whether events involving more than one pointer should
     * be ignored.
     *
     * @throws NullPointerException if {@code listener} is null.
     */
    public StrokeGestureDetector(Context context, OnStrokeGestureListener listener, boolean ignoreMultitouch) {
        mListener = listener;
        
	    mHoldHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				removeHoldMessage();
				mIsSingleTap = false;
				mCurrentState = mInitialState;
				mListener.onHold();
			}
	    };
	    
        init(context, ignoreMultitouch);
    }

    private void init(Context context, boolean ignoreMultitouch) {
        if (mListener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
        
        setHoldEnabled(true);
	    
        stroke();
        mIgnoreMultitouch = ignoreMultitouch;

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop();
        mBigSlopSquare = touchSlop * touchSlop;
		mSmallSlopSquare = mBigSlopSquare / SMALL_SLOP_DIVISOR;
        
		double cosine = Math.cos(MIN_ANGLE_DIFF_BETWEEN_STROKES);
		mThresholdCosineSquare = cosine * cosine;
    }
    
    // change gesture mode
    
    public void stroke() {
    	mInitialState = STROKE_TURN;
    }
    
    public void curve() {
    	mInitialState = CURVE_SMOOTH;
    }

    /**
     * Set whether hold is enabled, if this is enabled when a user
     * presses and holds down you get a hold event.
     *
     * @param enabled whether hold should be enabled.
     */
    public void setHoldEnabled(boolean enabled) {
        mIsHoldEnabled = enabled;
    }

    /**
     * @return true if hold is enabled, else false.
     */
    public boolean isHoldEnabled() {
        return mIsHoldEnabled;
    }

    /**
     * Analyzes the given motion event and if applicable triggers the
     * appropriate callbacks on the {@link OnStrokeGestureListener} supplied.
     *
     * @param ev The current motion event.
     * @return true if the {@link OnStrokeGestureListener} consumed the event,
     *              else false.
     */
    public boolean onTouchEvent(MotionEvent ev) {

        final int action = ev.getAction();
        final float y = ev.getY();
        final float x = ev.getX();

        boolean handled = false;

        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_POINTER_DOWN:
            if (mIgnoreMultitouch) {
                // Multitouch event - abort.
                cancel();
            }
            break;

        case MotionEvent.ACTION_POINTER_UP:
            // Ending a multitouch gesture and going back to 1 finger
            if (mIgnoreMultitouch && ev.getPointerCount() == 2) {
                int index = (((action & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT) == 0) ? 1 : 0;
                mLastMotionX = ev.getX(index);
                mLastMotionY = ev.getY(index);
            }
            break;

        case MotionEvent.ACTION_DOWN:
            mCurrentState = mInitialState;
        	switch(mCurrentState) {
        	case STROKE_TURN:
	            mIsWaitingForHold = false;
	            if (mIsHoldEnabled)
		            sendHoldMessage();
        		break;
        	case CURVE_SMOOTH:
	            mLastDistanceX = 0;
	            mLastDistanceY = 0;
        		break;
        	}
        	
            mLastMotionX = x;
            mLastMotionY = y;
            
            mIsSingleTap = true;
            
            mListener.onDown(ev);
            handled = true; // if ACTION_DOWN doesn't return true, ACTION_MOVE will not come.
            
            break;

        case MotionEvent.ACTION_MOVE:
            if (mIgnoreMultitouch && ev.getPointerCount() > 1) {
                break;
            }
            
            final float distanceX = x - mLastMotionX;
            final float distanceY = y - mLastMotionY;
            final float distance = magnitudeSquare(distanceX, distanceY);
            
//            android.util.Log.i("nora", "state="+mState+", cur="+x+","+y+", last="+mLastMotionX+","+mLastMotionY);
            switch(mCurrentState) {
            case STROKE_TURN:
            	if (distance > mBigSlopSquare) {
            		mCurrentState = STROKE;
		            if (mStrokeStartEvent != null)
		                mStrokeStartEvent.recycle();
		            mStrokeStartEvent = MotionEvent.obtain(ev);
            		handled = mListener.onStrokeStart(mStrokeStartEvent);
            	}
            	break;
            	
            case STROKE:
            	handled = mListener.onStrokeMove(ev, distanceX, distanceY);
				final float result = cosineSquare(x - mStrokeStartEvent.getX(), y - mStrokeStartEvent.getY(), distanceX, distanceY);
				if (result < mThresholdCosineSquare) {
					mCurrentState = STROKE_TURN;
					handled |= mListener.onStrokeEnd(mStrokeStartEvent, ev);
				}
				if (mIsHoldEnabled) {
					if (distance > mSmallSlopSquare) {
						if (mIsWaitingForHold) {
							removeHoldMessage();
						}
					} else {
						if (!mIsWaitingForHold) {
							sendHoldMessage();
						}
					}
				}
	            
	            mLastMotionX = x;
	            mLastMotionY = y;
	            
	            mIsSingleTap = false;
            	break;
            	
            case CURVE_SMOOTH:
//	        	android.util.Log.i("nora", "posistion now="+distance+", "+mBigSlopSquare+"/"+mSmallSlopSquare);
	            if (distance > mSmallSlopSquare) {
					final float result2 = cosineSquare(distanceX, distanceY, mLastDistanceX, mLastDistanceY);
	//	            android.util.Log.i("nora", "CosSqr "+result2+","+Math.toDegrees(Math.acos(Math.sqrt(result2)))+", now="+magnitudeSquare(curDistanceX, curDistanceY)+", last="+magnitudeSquare(mLastDistanceX, mLastDistanceY)+", slop="+mTouchSlopSquare+","+mHoldSlopSquare);
		            if (result2 < 0) {
//		            	android.util.Log.e("nora", "CosSqr "+result2+","+(int)Math.toDegrees(Math.acos(Math.sqrt(result2)))+", now="+distanceX+","+distanceY+", last="+mLastDistanceX+","+mLastDistanceY);
		            	mListener.onCurveBroken(ev, distanceX, distanceY, result2);
		            } else {
//		            	android.util.Log.i("nora", "CosSqr "+result2+","+(int)Math.toDegrees(Math.acos(Math.sqrt(result2)))+", now="+distanceX+","+distanceY+", last="+mLastDistanceX+","+mLastDistanceY);
		            	mListener.onCurveSmooth(ev, distanceX, distanceY, result2);
		            }
		            
		            mLastDistanceX = distanceX;
		            mLastDistanceY = distanceY;
	            
		            mLastMotionX = x;
		            mLastMotionY = y;
				}
				if (mIsHoldEnabled) {
					if (distance > mSmallSlopSquare) {
						if (mIsWaitingForHold) {
							removeHoldMessage();
						}
					} else {
						if (!mIsWaitingForHold) {
							sendHoldMessage();
						}
					}
				}
				mIsSingleTap = false;
            	break;
            }
            break;

        case MotionEvent.ACTION_UP:
        	removeHoldMessage();
        	if (mIsSingleTap) {
            	handled = mListener.onSingleTapUp(ev);
        	} else {
        		switch(mCurrentState) {
        		case STROKE:
					handled = mListener.onStrokeEnd(mStrokeStartEvent, ev);
        			break;
        		}
				mListener.onUp(ev);
        	}
            break;

        case MotionEvent.ACTION_CANCEL:
            cancel();
            break;
        }

        return handled;
    }

    private void cancel() {
    	removeHoldMessage();
    }
    
    private void sendHoldMessage() {
        mIsWaitingForHold = true;
		mHoldHandler.sendEmptyMessageDelayed(0, HOLD_TIMEOUT);
    }
    
    private void removeHoldMessage() {
        mIsWaitingForHold = false;
		mHoldHandler.removeMessages(0);
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
    
    public static class BaseGestureDetector implements OnStrokeGestureListener {


		@Override
		public void onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onStrokeStart(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onStrokeMove(MotionEvent e, float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public boolean onStrokeEnd(MotionEvent e1, MotionEvent e2) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onHold() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onUp(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onCurveSmooth(MotionEvent e, float distanceX,
				float distanceY, float cosineSquare) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onCurveBroken(MotionEvent e, float distanceX,
				float distanceY, float cosineSquare) {
			// TODO Auto-generated method stub
			return false;
		}
    }
}
