package com.easetheworld.diagonal;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import dev.easetheworld.strokegesturedetector.StrokeGestureDetector;

public class MainActivity extends Activity {

    private DirectionChangeDetector mGestureDetector;
    private StrokeGestureDetector mStrokeDetector;
    private TextView mTextView1;
    private TextView mTextView2;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView1 = (TextView)findViewById(android.R.id.text1);
        mTextView2 = (TextView)findViewById(android.R.id.text2);
        findViewById(android.R.id.icon1).setOnTouchListener(mDragListener);
        findViewById(android.R.id.icon2).setOnTouchListener(mDragListener);
        
        mGestureDetector = new DirectionChangeDetector(this, mTurningBackListener);
        mStrokeDetector = new StrokeGestureDetector(this, mStrokeListener);
    }
	
	private View mCurrentDragView;
	
	private View.OnTouchListener mDragListener = new View.OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			mCurrentDragView = v;
			switch(v.getId()) {
			case android.R.id.icon1:
				if (mGestureDetector.onTouchEvent(event))
					return true;
				break;
			case android.R.id.icon2:
				if (mStrokeDetector.onTouchEvent(event))
					return true;
				break;
			}
			return false;
		}
	};
	
	private int mCount1;
	private int mCount2;
	
	private DirectionChangeDetector.DirectionChangeListener mTurningBackListener = new DirectionChangeDetector.DirectionChangeListener() {
		@Override
		public void onDirectionChanged(double angleDegrees) {
			int count = 0;
			TextView tv = null;
			switch(mCurrentDragView.getId()) {
			case android.R.id.icon1:
				mCount1++;
				count = mCount1;
				tv = mTextView1;
				break;
			case android.R.id.icon2:
				mCount2++;
				count = mCount2;
				tv = mTextView2;
				break;
			}
			tv.setText("Turn Back : "+count);
		}

		@Override
		public void onDown() {
			int count = 0;
			TextView tv = null;
			switch(mCurrentDragView.getId()) {
			case android.R.id.icon1:
				mCount1 = 0;
				count = mCount1;
				tv = mTextView1;
				break;
			case android.R.id.icon2:
				mCount2 = 0;
				count = mCount2;
				tv = mTextView2;
				break;
			}
			tv.setText("Turn Back : "+count);
		}
	};
	
	private int mStrokeIncreaseMount = 1;
	
	private StrokeGestureDetector.OnStrokeGestureListener mStrokeListener = new StrokeGestureDetector.OnStrokeGestureListener() {
		
		@Override
		public void onDown(MotionEvent e) {
			android.util.Log.i("Stroke", "Down");
			mCount2 = 0;
			mStrokeIncreaseMount = 1;
			mTextView2.setText("Stroke : "+mCount2);
		}
		
		@Override
		public void onUp(MotionEvent e) {
			android.util.Log.i("Stroke", "Up");
			mTextView2.setText("Stroke : "+mCount2+ " Done.");
		}
		
		@Override
		public boolean onStrokeStart(MotionEvent e) {
			android.util.Log.i("Stroke", "Start "+e.getX()+","+e.getY());
			return false;
		}
		
		@Override
		public boolean onStrokeMove(MotionEvent e1, MotionEvent e2) {
			return false;
		}
		
		@Override
		public boolean onStrokeEnd(MotionEvent e1, MotionEvent e2) {
			android.util.Log.i("Stroke", "End "+e2.getX()+","+e2.getY()+" from "+e1.getX()+","+e1.getY());
			mCount2 += mStrokeIncreaseMount;
			mTextView2.setText("Stroke : "+mCount2);
			return false;
		}
		
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			android.util.Log.i("Stroke", "SingleTapUp "+e);
	    	Toast.makeText(MainActivity.this, "SingleTapUp "+e, Toast.LENGTH_SHORT).show();
			return false;
		}
		
		@Override
		public void onHold() {
			android.util.Log.i("Stroke", "Hold");
			mStrokeIncreaseMount = -mStrokeIncreaseMount;
		}
	};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void clickHandler(View v) {
    	Toast.makeText(this, "Button Clicked", Toast.LENGTH_SHORT).show();
    }
}