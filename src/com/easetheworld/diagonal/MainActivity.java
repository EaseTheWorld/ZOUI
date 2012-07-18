package com.easetheworld.diagonal;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

    private DirectionChangeDetector mGestureDetector;
    private TextView mTextView;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(android.R.id.text1);
        
        mGestureDetector = new DirectionChangeDetector(this, mTurningBackListener);
    }
	
	private DirectionChangeDetector.DirectionChangeListener mTurningBackListener = new DirectionChangeDetector.DirectionChangeListener() {
		@Override
		public void onDirectionChanged(int count, double angleDegrees) {
			Log.i("nora", "count="+count+", angle="+angleDegrees);
			mTextView.setText("Turn Back "+count);
		}
	};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
      if (mGestureDetector.onTouchEvent(event))
        return true;
      else
        return super.onTouchEvent(event);
    }
    
    public void clickHandler(View v) {
    	
    }
}