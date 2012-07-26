package com.easetheworld.diagonal;

import dev.easetheworld.ui.zo.ZOTouchViewController;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private TextView mTextView1;
	private TextView mTextView2;
	
	private static final int VALUE_MIN = 0;
	private static final int VALUE_MAX = 29;
	private int mValue1;
	private int mValue2;
	
	private ZOTouchViewController mZOController;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mTextView1 = (TextView)findViewById(android.R.id.text1);
        mTextView2 = (TextView)findViewById(android.R.id.text2);
        
        mZOController = new ZOTouchViewController(this, mZOListener, 120);
        
        mZOController.addView(mTextView1);
        mZOController.addView(mTextView2);
    }
	
	private ZOTouchViewController.Listener mZOListener = new ZOTouchViewController.Listener() {
		
		@Override
		public void onMove(int mode, View v, int value) {
			switch(v.getId()) {
			case android.R.id.text1:
				mValue1 = rotatedAdd(mValue1, value, VALUE_MIN, VALUE_MAX);
				mTextView1.setText(String.valueOf(mValue1));
				break;
			case android.R.id.text2:
				mValue2 = rotatedAdd(mValue2, value, VALUE_MIN, VALUE_MAX);
				mTextView2.setText(String.valueOf(mValue2));
				break;
			}
		}

		@Override
		public void onClick(View v) {
			Toast.makeText(v.getContext(), "Clicked.", Toast.LENGTH_SHORT).show();
		}
	};
	
	private static int rotatedAdd(int a, int b, int min, int max) {
		a += b;
		if (a < min)
			a += (max - min + 1);
		else if (a > max)
			a -= (max - min + 1);
		return a;	
	}
}