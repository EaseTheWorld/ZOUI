package dev.easetheworld.ui.zo;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
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
		
		boolean onStrokeMove(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
        
		boolean onStrokeEnd(MotionEvent e1, MotionEvent e2);
		
		/**
		 * Notified when long pressed during touch down.
		 * @return true : change to stop state, false : keep current state
		 */
		boolean onHold(float x, float y);
		
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
    private static final int FIRST_STATE = STROKE_TURN;
    private int mState;
    
    // gesture listener
    private final OnStrokeGestureListener mListener;
    
    // last position
    private float mLastMotionY;
    private float mLastMotionX;
    
    // last distance
    private float mLastDistanceX;
    private float mLastDistanceY;
    
    // last move time. if this is too old, onHold will be fired.
    private long mLastMotionTime;
    
    // touch threshold
    private int mBigSlopSquare; // threshold for stroke start
    private static final int SMALL_SLOP_DIVISOR = 32; // mBigSlopSquare / DIVISOR = mSmallSlopSquare;
    private int mSmallSlopSquare; // threhold to recognize direction
    
    // single tap
    private boolean mIsSingleTap;
    private boolean mIsStroking;
	
	// hold
    private static final int HOLD_TIMEOUT = ViewConfiguration.getLongPressTimeout() / 2;
    private final Handler mHoldHandler;

    // stroke
    private MotionEvent mStrokeStartEvent;
    private static final double MIN_ANGLE_DIFF_BETWEEN_STROKES = Math.toRadians(60);
	private double mThresholdCosineSquare;
    
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
				final long now = SystemClock.uptimeMillis();
//				android.util.Log.i("nora", "handleMessage now="+now+", last="+mLastMotionTime+", diff="+(now - mLastMotionTime));
				if (now - mLastMotionTime > HOLD_TIMEOUT) {
					if (mListener.onHold(mLastMotionX, mLastMotionY)) {
						mIsSingleTap = false;
						mState = FIRST_STATE;
					}
				} else {
					sendHoldMessage();
				}
			}
	    };
	    
        init(context, ignoreMultitouch);
    }

    private void init(Context context, boolean ignoreMultitouch) {
        if (mListener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
	    
        mIgnoreMultitouch = ignoreMultitouch;

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop();
        mBigSlopSquare = touchSlop * touchSlop;
		mSmallSlopSquare = mBigSlopSquare / SMALL_SLOP_DIVISOR;
        
		double cosine = Math.cos(MIN_ANGLE_DIFF_BETWEEN_STROKES);
		mThresholdCosineSquare = cosine * cosine;
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
            mState = FIRST_STATE;
        	
            mLastMotionX = x;
            mLastMotionY = y;
            
            mLastMotionTime = ev.getEventTime();
            
            mLastDistanceX = 0;
            mLastDistanceY = 0;
        	
            sendHoldMessage();
            
            mIsSingleTap = true;
            mIsStroking = false;
            
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
            
//            android.util.Log.i("nora", "state="+mState+", distance="+distance+", timediff="+(ev.getEventTime() - mLastMotionTime));
            
            boolean updateLast = false;
            switch(mState) {
            case STROKE_TURN:
            	if (distance > mBigSlopSquare) {
            		mState = STROKE;
		            if (mStrokeStartEvent != null)
		                mStrokeStartEvent.recycle();
		            mStrokeStartEvent = MotionEvent.obtain(ev);
            		handled = mListener.onStrokeStart(mStrokeStartEvent);
            		
            		updateLast = true;
		            
		            removeHoldMessage(); // hold message could be sent in DOWN.
		            sendHoldMessage();
            	}
            	break;
            	
            case STROKE:
	            if (distance > mSmallSlopSquare) {
					final float result = cosineSquare(distanceX, distanceY, mLastDistanceX, mLastDistanceY);
		            if (result < mThresholdCosineSquare) {
						mState = STROKE_TURN;
						handled = mListener.onStrokeEnd(mStrokeStartEvent, ev);
		            } else {
		            	handled = mListener.onStrokeMove(mStrokeStartEvent, ev, distanceX, distanceY);
		            }
		            
            		updateLast = true;
				}
	            
	            mIsSingleTap = false;
	            mIsStroking = true;
            	break;
            }
            
            if (updateLast) {
	            mLastMotionX = x;
	            mLastMotionY = y;
	            
	            mLastMotionTime = ev.getEventTime();
	            
	            mLastDistanceX = distanceX;
	            mLastDistanceY = distanceY;
            }
            break;

        case MotionEvent.ACTION_UP:
        	removeHoldMessage();
        	if (mIsSingleTap) {
            	handled = mListener.onSingleTapUp(ev);
        	} else {
        		switch(mState) {
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
    
    public boolean isStroking() {
    	return mIsStroking;
    }

    private void cancel() {
    	removeHoldMessage();
    }
    
    private void sendHoldMessage() {
		mHoldHandler.sendEmptyMessageDelayed(0, HOLD_TIMEOUT);
    }
    
    private void removeHoldMessage() {
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
}