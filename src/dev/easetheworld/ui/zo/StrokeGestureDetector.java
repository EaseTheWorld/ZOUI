package dev.easetheworld.ui.zo;

import android.content.Context;
import android.view.MotionEvent;

public class StrokeGestureDetector {
	
    public static interface OnStrokeGestureListener {

        /**
         * Notified when a tap occurs with the down {@link MotionEvent} that triggered it.
         * This will be triggered immediately for every down event.
         * All other events should be preceded by this.
         *
         * @param e The down motion event.
         */
        void onDown(MotionEvent e);
        
		/**
		 * Notified when a stroke starts with {@link MotionEvent}.
		 * 
		 * @param e The motion event that starts this stroke.
		 * @param index stroke index.
		 * @param directionX x-direction of this stroke
		 * @param directionY y-direction of this stroke
		 * 
		 * @return true if the event is consumed, else false 
		 */
		boolean onStrokeStart(MotionEvent e, int index, float directionX, float directionY);
		
		/**
		 * Notified when a stroke moves with the initial on stroke start {@link MotionEvent} and the current move {@link MotionEvent}.
		 * The distance in x and y is also supplied for convenience.
		 * 
		 * @param e1 The first down motion event that started this stroke.
		 * @param e2 The motion event that triggered the current onStrokeMove.
		 * @param distanceX The distance along the X axis that has been scrolled since the last call to onStrokeMove. This is NOT the distance between e1 and e2.
		 * @param distanceY The distance along the Y axis that has been scrolled since the last call to onStrokeMove. This is NOT the distance between e1 and e2.
		 * 
		 * @return true if the event is consumed, else false 
		 */
		boolean onStrokeMove(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
		
        /**
         * Notified when a up {@link MotionEvent} occurs.
         * This will be triggered immediately for every up event except onSingleTapUp returns true.
         *
         * @param e The up motion event.
         */
        void onUp(MotionEvent e);

        /**
         * Notified when a tap occurs with the up {@link MotionEvent} that triggered it.
         * If a stroke is already started, onSingleTapUp will not be called.
         *
         * @param e The up motion event that completed the first tap.
         * @return true if the event is consumed, else false. If true, onUp will not be called.
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
     * @param handler the handler to use
     *
     * @throws NullPointerException if {@code listener} is null.
     */
    public StrokeGestureDetector(Context context, OnStrokeGestureListener listener) {
        if (listener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
        mListener = listener;
        
		mStrokeTracker = new StrokeTracker(context);
    }
    
    private int mStrokeIndex;

    /**
     * Analyzes the given motion event and if applicable triggers the
     * appropriate callbacks on the {@link OnStrokeGestureListener} supplied.
     *
     * @param ev The current motion event.
     * @return true if the {@link OnStrokeGestureListener} consumed the event, else false.
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
        	mStrokeIndex = 0;
            handled = true; // if ACTION_DOWN doesn't return true, ACTION_MOVE will not come.
            
        	break;
        case MotionEvent.ACTION_MOVE:
        	int state = mStrokeTracker.addTouchMove(x, y);
        	switch(state) {
        	case StrokeTracker.STROKE_START:
	            if (mStrokeStartEvent != null)
	                mStrokeStartEvent.recycle();
	            mStrokeStartEvent = MotionEvent.obtain(ev);
        		handled = mListener.onStrokeStart(mStrokeStartEvent, mStrokeIndex, mStrokeTracker.getStrokeStartDirection().x, mStrokeTracker.getStrokeStartDirection().y);
	        	mStrokeIndex++;
        		break;
        	case StrokeTracker.STROKE_MOVE:
        		handled = mListener.onStrokeMove(mStrokeStartEvent, ev, x - mLastMotionX, y - mLastMotionY);
        		break;
        	}
	        if (mIsSingleTap && state == StrokeTracker.STROKE_START)
	        	mIsSingleTap = false;
        	break;
        case MotionEvent.ACTION_UP:
        	if (mIsSingleTap)
            	handled = mListener.onSingleTapUp(ev);
        	if (!handled)
				mListener.onUp(ev);
            break;
        }

        mLastMotionX = x;
        mLastMotionY = y;

        return handled;
    }
}