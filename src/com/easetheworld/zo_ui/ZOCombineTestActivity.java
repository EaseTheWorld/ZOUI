package com.easetheworld.zo_ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import dev.easetheworld.ui.zo.ZOTouchListener;

public class ZOCombineTestActivity extends Activity {
	
	private static final int VALUE_MIN = 0;
	private static final int VALUE_MAX = 99;
	
	private int mValue1;
	private int mValue2;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zo_test);
        
        TextView text1 = (TextView)findViewById(android.R.id.text1);
        text1.setText("Long touch at start will set mode to O, otherwise Z.");
        
        TextView btn1 = (TextView)findViewById(android.R.id.button1);
        btn1.setText("Z O");
        
        findViewById(android.R.id.text2).setVisibility(View.GONE);
        
        TextView btn2 = (TextView)findViewById(android.R.id.button2);
        btn2.setText("Z O");
        
        ZOTouchListener listener = new ZOTouchListener(this, new ZOTouchListener.Dispatcher() {
        	private int mMode;
			@Override
			public void onMove(int mode, View v, int value) {
				switch(v.getId()) {
				case android.R.id.button1:
					mValue1 = rotatedAdd(mValue1, value, VALUE_MIN, VALUE_MAX);
					((TextView)v).setText(String.valueOf(mValue1));
					break;
				case android.R.id.button2:
					mValue2 = rotatedAdd(mValue2, value, VALUE_MIN, VALUE_MAX);
					((TextView)v).setText(String.valueOf(mValue2));
					break;
				}
				if (mode != mMode) {
					if (mode == ZOTouchListener.MODE_Z)
						v.setBackgroundColor(0xffffcccc);
					else
						v.setBackgroundColor(0xffccccff);
					mMode = mode;
				}
			}
			
			@Override
			public void onClick(View v) {
				switch(v.getId()) {
				case android.R.id.button1:
					Toast.makeText(v.getContext(), "View 1 Clicked.", Toast.LENGTH_SHORT).show();
					break;
				case android.R.id.button2:
					Toast.makeText(v.getContext(), "View 2 Clicked.", Toast.LENGTH_SHORT).show();
					break;
				}
			}

			@Override
			public void onDown(View v) {
				mMode = -1;
		        v.setBackgroundColor(Color.DKGRAY);
			}

			@Override
			public void onUp(View v) {
		        v.setBackgroundColor(Color.GRAY);
			}
		});
        
        btn1.setOnTouchListener(listener);
        btn2.setOnTouchListener(listener);
    }
	
	private static int rotatedAdd(int a, int b, int min, int max) {
		a += b;
		if (a < min)
			a += (max - min + 1);
		else if (a > max)
			a -= (max - min + 1);
		return a;	
	}
}