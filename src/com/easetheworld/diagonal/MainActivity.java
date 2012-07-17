package com.easetheworld.diagonal;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

    private DirectionGestureDetector mGestureDetector;
    private TextView mTextView;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(android.R.id.text1);
        
        mGestureDetector = new DirectionGestureDetector(this, mTurningBackListener);
    }
	
	private DirectionGestureDetector.DirectionChangeListener mTurningBackListener = new DirectionGestureDetector.DirectionChangeListener() {
		@Override
		public void onTurningBack(int count, float sx, float sy, float ex, float ey) {
			Log.i("nora", "count="+count+", x="+sx+"->"+ex+", y="+sy+"->"+ey);
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