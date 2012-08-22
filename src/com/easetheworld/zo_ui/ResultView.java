package com.easetheworld.zo_ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.easetheworld.zo_ui.GestureAnalyzer.ResultItem;

    
public class ResultView extends View {
	private static final int[] COLORS = {Color.TRANSPARENT, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
	
	private ResultItem mCurItem;
	private ResultItem mPrevItem;
	private ResultItem mNextItem;
	private int mPosition;
	
	private Paint mLinePaint;
	private Paint mPointPaint;
	private Paint mStatePaint;
	private Paint mTextPaint;
	
	public ResultView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeWidth(1);
        mLinePaint.setColor(Color.BLACK);
        
		mPointPaint = new Paint();
        mPointPaint.setStyle(Paint.Style.FILL);
        mPointPaint.setColor(Color.BLACK);
        
		mStatePaint = new Paint();
        mStatePaint.setStyle(Paint.Style.FILL);
        
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setAlpha(0x80);
        mTextPaint.setTextSize(24);
	}
	
	public void setResultItem(int position, ResultItem prev, ResultItem cur, ResultItem next) {
		mPosition = position;
		mPrevItem = prev;
		mCurItem = cur;
		mNextItem = next;
		invalidate();
	}
	
	private static final float PADDING = 10f;
	private float getXFromValue(float fValue) {
		return (getWidth() - PADDING * 2) * fValue + PADDING;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		float curX = getXFromValue(mCurItem.fValue);
		float curY = getHeight() / 2;
		
		// state
		mStatePaint.setColor(COLORS[mCurItem.iValue]);
		canvas.drawRect(0, PADDING, PADDING, getHeight() - PADDING, mStatePaint);
		
		// index
		canvas.drawText(Integer.toString(mPosition), getWidth() / 2, getHeight() / 2, mTextPaint);
		
		// line
		if (mPrevItem != null)
			canvas.drawLine(getXFromValue(mPrevItem.fValue), curY - getHeight(), curX, curY, mLinePaint);
		if (mNextItem != null)
			canvas.drawLine(curX, curY, getXFromValue(mNextItem.fValue), curY + getHeight(), mLinePaint);
		
		// point
		canvas.drawCircle(curX, curY, PADDING/2, mPointPaint);
	}
}