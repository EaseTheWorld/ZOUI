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
		
		boolean onStrokeMove(MotionEvent e);
        
		boolean onStrokeEnd(MotionEvent e1, MotionEvent e2);
		
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
    
    public static interface OnRotationGestureListener {
    	boolean onRotateStart(MotionEvent ev);
    	
    	boolean onRotateMove(MotionEvent ev, double angleRadian, int diff);
    	
    	boolean onRotateEnd(MotionEvent ev);
    }

    // state
    private static final int TURN = 0;
    private static final int STROKE = 1;
    
    private static final int ROTATE_CENTER = 2;
    private static final int ROTATE = 3;
    private int mState;
    
    private float mLastMotionY;
    private float mLastMotionX;

    // stroke
    private final OnStrokeGestureListener mListener;
    private int mTouchSlopSquare; // min distance for state change from TURN to STROKE
    private MotionEvent mStrokeStartEvent;
    private static final double MIN_ANGLE_DIFF_BETWEEN_STROKES = Math.toRadians(60);
	private double mThresholdCosineSquare;
	
	// hold
    private boolean mIsHoldEnabled;
    private int mHoldSlopSquare;
	private boolean mIsWaitingForHold;
    private static final int HOLD_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int HOLD_SLOP_DIVISOR = 32; // mTouchSlopSquare / DIVISOR = mHoldSlopSquare;
    private final Handler mHoldHandler;
    
    // single tap
    private boolean mIsSingleTap;
    
    // rotation
    private OnRotationGestureListener mRotationListener;
    private float mCenterX;
    private float mCenterY;
    private int mRotatePrevValue;
    private int mRotateResolution;
    private int mRotateResolutionHalf;
    private double mRotateScaleFactor;
    
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
				mState = TURN;
				mListener.onHold();
			}
	    };
	    
	    if (listener instanceof OnRotationGestureListener)
	    	setOnRotationListener((OnRotationGestureListener) listener);
	    
        init(context, ignoreMultitouch);
    }

    private void init(Context context, boolean ignoreMultitouch) {
        if (mListener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
	    
        setHoldEnabled(true);
        mIgnoreMultitouch = ignoreMultitouch;

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop();
        mTouchSlopSquare = touchSlop * touchSlop;
        
		mHoldSlopSquare = mTouchSlopSquare / HOLD_SLOP_DIVISOR;
        
		double cosine = Math.cos(MIN_ANGLE_DIFF_BETWEEN_STROKES);
		mThresholdCosineSquare = cosine * cosine;
    }
    
    // change gesture mode
    
    public void stroke() {
    	mState = TURN;
    }
    
    public void rotate(int resolution) {
    	removeHoldMessage();
    	
    	if (mRotationListener == null)
            throw new NullPointerException("OnRotationGestureListener must not be null");
    	
    	mRotateResolution = resolution;
    	mRotateResolutionHalf = resolution / 2;
    	mRotateScaleFactor = (double)resolution / (Math.PI * 2);
    	mState = ROTATE_CENTER;
    }
    
    public void setOnRotationListener(OnRotationGestureListener listener) {
    	mRotationListener = listener;
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
            mLastMotionX = x;
            mLastMotionY = y;
            
            mIsSingleTap = true;
            mIsWaitingForHold = false;
            mState = TURN;
            sendHoldMessage();
            
            mListener.onDown(ev);
            handled = true; // if ACTION_DOWN doesn't return true, ACTION_MOVE will not come.
            
            break;

        case MotionEvent.ACTION_MOVE:
            if (mIgnoreMultitouch && ev.getPointerCount() > 1) {
                break;
            }
            
            final float distanceX = x - mLastMotionX;
            final float distanceY = y - mLastMotionY;
            final float distance = distanceX * distanceX + distanceY * distanceY;
            
            android.util.Log.i("nora", "state="+mState+", cur="+x+","+y+", distance="+distance+"/"+mTouchSlopSquare+","+mHoldSlopSquare);
            switch(mState) {
            case TURN:
            	if (distance > mTouchSlopSquare) {
            		mState = STROKE;
		            if (mStrokeStartEvent != null)
		                mStrokeStartEvent.recycle();
		            mStrokeStartEvent = MotionEvent.obtain(ev);
            		handled = mListener.onStrokeStart(mStrokeStartEvent);
            	}
            	break;
            	
            case STROKE:
            	handled = mListener.onStrokeMove(ev);
				final float result = cosineSquare(x - mStrokeStartEvent.getX(), y - mStrokeStartEvent.getY(), distanceX, distanceY);
				if (result < mThresholdCosineSquare) {
					mState = TURN;
					handled |= mListener.onStrokeEnd(mStrokeStartEvent, ev);
				}
				if (mIsHoldEnabled) {
					if (distance > mHoldSlopSquare) {
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
            	
            case ROTATE_CENTER:
		    	mCenterX = x;
		    	mCenterY = y;
        		mRotatePrevValue = (int)(Math.atan2(y - mCenterY, x - mCenterX) * mRotateScaleFactor);
        		mRotationListener.onRotateStart(ev);
        		mState = ROTATE;
            	break;
            	
            case ROTATE:
            	double angle = Math.atan2(y - mCenterY, x - mCenterX);
    			int value = (int)(angle * mRotateScaleFactor);
//    			if (value != mRotatePrevValue) { // for smooth ui animation
    				int diff = value - mRotatePrevValue;
    				if (diff < -mRotateResolutionHalf)
    					diff += mRotateResolution;
    				else if (diff > mRotateResolutionHalf)
        				diff -= mRotateResolution;
        			handled = mRotationListener.onRotateMove(ev, angle, diff);
//    			}
    			mRotatePrevValue = value;
            	break;
            }
            
            break;

        case MotionEvent.ACTION_UP:
        	removeHoldMessage();
        	if (mIsSingleTap) {
            	handled = mListener.onSingleTapUp(ev);
        	} else {
        		if (mState == STROKE)
					handled = mListener.onStrokeEnd(mStrokeStartEvent, ev);
        		else if (mState == ROTATE)
					handled = mRotationListener.onRotateEnd(ev);
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
    
    public static class BaseGestureDetector implements OnStrokeGestureListener, OnRotationGestureListener {


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
		public boolean onStrokeMove(MotionEvent e) {
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
		public boolean onRotateStart(MotionEvent ev) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onRotateMove(MotionEvent ev, double angleRadian, int diff) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onRotateEnd(MotionEvent ev) {
			// TODO Auto-generated method stub
			return false;
		}
    }
}
