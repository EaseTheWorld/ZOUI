package dev.easetheworld.ui.zo;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;

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
        
		boolean onStrokeStart(MotionEvent e, float directionX, float directionY);
		
		boolean onStrokeMove(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
		
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
    
    // gesture listener
    private final OnStrokeGestureListener mListener;
    
    // last position
    private float mLastMotionY;
    private float mLastMotionX;
    
    // single tap
    private boolean mIsSingleTap;

    // stroke
    private MotionEvent mStrokeStartEvent;
    
    private StrokeTracker mStrokeTracker;

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
	    
        init(context, ignoreMultitouch);
    }

    private void init(Context context, boolean ignoreMultitouch) {
        if (mListener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
	    
		mStrokeTracker = new StrokeTracker(context);
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

        final float y = ev.getY();
        final float x = ev.getX();

        boolean handled = false;
        
        switch (ev.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            mIsSingleTap = true;
            
            mListener.onDown(ev);
        	mStrokeTracker.addTouchDown(x, y);
            handled = true; // if ACTION_DOWN doesn't return true, ACTION_MOVE will not come.
            
        	break;
        case MotionEvent.ACTION_MOVE:
        	int state = mStrokeTracker.addTouchMove(x, y);
        	switch(state) {
        	case StrokeTracker.STROKE_START:
	            if (mStrokeStartEvent != null)
	                mStrokeStartEvent.recycle();
	            mStrokeStartEvent = MotionEvent.obtain(ev);
        		handled = mListener.onStrokeStart(mStrokeStartEvent, mStrokeTracker.getStrokeStartDirection().x, mStrokeTracker.getStrokeStartDirection().y);
        		break;
        	case StrokeTracker.STROKE_MOVE:
        		handled = mListener.onStrokeMove(mStrokeStartEvent, ev, x - mLastMotionX, y - mLastMotionY);
        		break;
        	}
	        if (mIsSingleTap && state == StrokeTracker.STROKE_START)
	        	mIsSingleTap = false;
        	break;
        case MotionEvent.ACTION_UP:
        	if (mIsSingleTap) {
            	handled = mListener.onSingleTapUp(ev);
        	} else {
				mListener.onUp(ev);
        	}
            break;
        }

        mLastMotionX = x;
        mLastMotionY = y;

        return handled;
    }
}