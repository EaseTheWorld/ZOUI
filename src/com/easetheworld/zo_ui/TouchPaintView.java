package com.easetheworld.zo_ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import dev.easetheworld.recentbuffer.RecentBuffer;

public class TouchPaintView extends View {
	
	private boolean mIsAnalyzing;
	
	private static final int MAX_COUNT = 500;
    
    private Path    mPath;
    private Paint   mLinePaint;
    private Paint   mHighlightLinePaint;
    private Paint   mHighlightPointPaint;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    
	private RecentBuffer<PointF> mData = new RecentBuffer<PointF>(MAX_COUNT, PointF.class);

	public TouchPaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		setIsAnalyzing(false);
		
        mPath = new Path();
        
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setColor(Color.RED);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeWidth(1);
        
        mHighlightLinePaint = new Paint(mLinePaint);
        
        mHighlightPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHighlightPointPaint.setStyle(Paint.Style.FILL);
        mHighlightPointPaint.setColor(Color.GREEN);
        
        mGestureDetector = new GestureDetector(context, mGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
	}
	
    private float mTranslateX;
    private float mTranslateY;
	private float mScaleFactor;
	
	private float getAdjustedX(float x) {
		float originalWidth = getWidth();
		float scaledWidth = originalWidth * mScaleFactor;
		float targetLeft = 0;
		float targetRight = getWidth();
		x = Math.min(x, targetLeft);
		x = Math.max(x, targetRight - scaledWidth);
		return x;
	}
	
	private float getAdjustedY(float y) {
		float originalHeight = getHeight();
		float scaledHeight = originalHeight * mScaleFactor;
		float targetTop = 0;
		float targetBottom = getHeight();
		y = Math.min(y, targetTop);
		y = Math.max(y, targetBottom - scaledHeight);
		return y;
	}
	
	private static final float MIN_SCALE_FACTOR = 1.0f;
	private static final float MAX_SCALE_FACTOR = 10.0f;
	
	private float getAdjustedScale(float s) {
		s = Math.max(s, MIN_SCALE_FACTOR);
		s = Math.min(s, MAX_SCALE_FACTOR);
		return s;
	}
	
	public boolean movePointToCenter(int index) {
		PointF p = mData.getFromOldest(index);
		return move(p.x, p.y, getWidth() / 2, getHeight() / 2);
	}
	
	// x, y is original axis base. (from touch event)
	public boolean move(float x1, float y1, float x2, float y2) {
		return translate(x2 - mScaleFactor * x1, y2 - mScaleFactor * y1);
	}
	
	// x, y is original axis base. (from touch event)
	public boolean translateBy(float x, float y) {
		return translate(mTranslateX + x, mTranslateY + y);
	}
	
	// x, y is original axis base. (from touch event)
	public boolean translate(float x, float y) {
		x = getAdjustedX(x);
		y = getAdjustedY(y);
		if (mTranslateX == x && mTranslateY == y) {
			return false;
		}
		mTranslateX = x;
		mTranslateY = y;
		invalidate();
		return true;
	}
	
	// cx, cy is original axis base. (from touch event)
	public boolean scaleBy(float s, float cx, float cy) {
		float newS = getAdjustedScale(mScaleFactor * s);
		if (mScaleFactor == newS) {
			return false;
		}
		s = newS / mScaleFactor;
		mScaleFactor = newS;
		mTranslateX = getAdjustedX(s * (mTranslateX - cx) + cx);
		mTranslateY = getAdjustedY(s * (mTranslateY - cy) + cy);
		invalidate();
		return true;
	}
	
	private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			translateBy(-distanceX, -distanceY);
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return super.onSingleTapUp(e);
		}
	};
	
	private ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
		
		private final float SCALE_FACTOR_SENSITIVITY1 = 1f - 0.01f;
		private final float SCALE_FACTOR_SENSITIVITY2 = 1f + 0.01f;

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			final float s = detector.getScaleFactor();
			if (s > SCALE_FACTOR_SENSITIVITY1 && s < SCALE_FACTOR_SENSITIVITY2)
				return false;
			else {
				scaleBy(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
				return true;
			}
		}
	};
	
	public void setIsAnalyzing(boolean isAnalyzing) {
		if (mIsAnalyzing != isAnalyzing) {
			mIsAnalyzing = isAnalyzing;
			if (isAnalyzing) {
				mTranslateX = 0f;
				mTranslateY = 0f;
				mScaleFactor = 1f;
				mLinePaint.setAlpha(0x20);
			} else {
		        clearTouchData();
		        mPath.reset();
				mLinePaint.setAlpha(0xff);
			}
			invalidate();
		}
	}
    
    @Override
    protected void onDraw(Canvas canvas) {
    	if (mIsAnalyzing) {
			canvas.translate(mTranslateX, mTranslateY);
    		canvas.scale(mScaleFactor, mScaleFactor);
	        canvas.drawPath(mPath, mLinePaint);
    		canvas.drawRect(0, 0, getWidth(), getHeight(), mLinePaint);
			for (int i=mStartPoint; i<mEndPoint; i++) {
				PointF p1 = mData.getFromOldest(i);
				PointF p2 = mData.getFromOldest(i+1);
				canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mHighlightLinePaint);
			}
    		if (mHighlightPoint >= 0) {
				PointF p = mData.getFromOldest(mHighlightPoint);
		        mHighlightPointPaint.setAlpha(0x80);
				canvas.drawCircle(p.x, p.y, 2, mHighlightPointPaint);
		        mHighlightPointPaint.setAlpha(0xff);
				canvas.drawCircle(p.x, p.y, 1, mHighlightPointPaint);
    		}
    	} else {
	        canvas.drawPath(mPath, mLinePaint);
    	}
    }
    
//    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//    
//    private void drawGridText(Canvas canvas) {
//    	mTextPaint.setTextAlign(Paint.Align.CENTER);
//    	for (int x=0; x<480; x+=100) {
//	    	for (int y=0; y<700; y+=100) {
//		        canvas.drawText(x+","+y, x, y, mTextPaint);
//		        canvas.drawCircle(x, y, 4, mHighlightPointPaint);
//	    	}
//    	}
//    }
//    
//    private void drawGridLine(Canvas canvas) {
//    	for (int x=0; x<480; x+=100)
//	        canvas.drawLine(x, -700, x, 700, mTextPaint);
//    	for (int y=0; y<700; y+=100)
//	        canvas.drawLine(-480, y, 480, y, mTextPaint);
//    }
    
    private void touch_down(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        
        clearTouchData();
        addTouchData(x, y);
    }
    private void touch_move(float x, float y) {
        mPath.lineTo(x, y);
        
        addTouchData(x, y);
    }
    private void touch_up(float x, float y) {
    }
    
    private int mHighlightPoint;
    
    public void highlightPoint(int pos) {
    	mHighlightPoint = pos;
    	invalidate();
    }
    
    private int mStartPoint;
    private int mEndPoint;
    
    public void highlightPoints(int start, int end) {
    	if (!mIsAnalyzing) return;
    	if (mStartPoint != start || mEndPoint != end) {
    		mStartPoint = start;
    		mEndPoint = end;
    		mHighlightPoint = -1;
    		float sumX = 0f;
    		float sumY = 0f;
    		for (int i=start; i<=end; i++) {
    			PointF p = mData.getFromOldest(i);
    			sumX += p.x;
    			sumY += p.y;
    		}
    		sumX /= (end - start + 1);
    		sumY /= (end - start + 1);
    		move(sumX, sumY, getWidth() / 2, getHeight() / 2);
	    	invalidate();
    	}
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
    	
    	if (mIsAnalyzing) {
    		mScaleGestureDetector.onTouchEvent(event);
			return mGestureDetector.onTouchEvent(event);
    	} else {
	        switch (event.getActionMasked()) {
	            case MotionEvent.ACTION_DOWN:
	                touch_down(x, y);
	                invalidate();
	                break;
	            case MotionEvent.ACTION_MOVE:
	                touch_move(x, y);
	                invalidate();
	                break;
	            case MotionEvent.ACTION_UP:
	                touch_up(x, y);
	                invalidate();
	                break;
	        }
	        return true;
    	}
    }
    
    private void clearTouchData() {
    	mData.clear();
    }
    
    private void addTouchData(float x, float y) {
    	mData.obtain().set(x, y);
    }
    
    public RecentBuffer<PointF> getTouchData() {
    	return mData;
    }
}