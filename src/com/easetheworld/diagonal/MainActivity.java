package com.easetheworld.diagonal;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private DirectionChangeDetector mGestureDetector;
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
    }
	
	private View mCurrentDragView;
	
	private View.OnTouchListener mDragListener = new View.OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			mCurrentDragView = v;
			if (mGestureDetector.onTouchEvent(event))
				return true;
			else
				return false;
		}
	};
	
	private DirectionChangeDetector.DirectionChangeListener mTurningBackListener = new DirectionChangeDetector.DirectionChangeListener() {
		@Override
		public void onDirectionChanged(int count, double angleDegrees) {
			Log.i("nora", "count="+count+", angle="+angleDegrees);
			switch(mCurrentDragView.getId()) {
			case android.R.id.icon1:
				mTextView1.setText("Turn Back1 : "+count);
				break;
			case android.R.id.icon2:
				mTextView2.setText("Turn Back2 : "+count);
				break;
			}
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