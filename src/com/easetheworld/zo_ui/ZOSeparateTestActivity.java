package com.easetheworld.zo_ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import dev.easetheworld.ui.zo.ZOTouchListener;

public class ZOSeparateTestActivity extends Activity {
	
	private static final int VALUE_MIN = 0;
	private static final int VALUE_MAX = 99;
	
	private int mValue1;
	private int mValue2;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zo_test);
        
        TextView text1 = (TextView)findViewById(android.R.id.text1);
        text1.setText("Z mode triggered when a stroke start or turn back. First stroke's direction decides the sign.");
        
        TextView btn1 = (TextView)findViewById(android.R.id.button1);
        btn1.setText("Z");
        
        btn1.setOnTouchListener(new ZOTouchListener(this, new ZOTouchListener.Dispatcher() {
			@Override
			public void onMove(int mode, View v, int value) {
				mValue1 = rotatedAdd(mValue1, value, VALUE_MIN, VALUE_MAX);
				((TextView)v).setText(String.valueOf(mValue1));
			}
			
			@Override
			public void onClick(View v) {
				Toast.makeText(v.getContext(), "View 1 Clicked.", Toast.LENGTH_SHORT).show();
				
			}

			@Override
			public void onDown(View v) {
				v.setBackgroundColor(0xffffcccc);
			}

			@Override
			public void onUp(View v) {
		        v.setBackgroundColor(Color.GRAY);
			}
		}).setMode(ZOTouchListener.MODE_Z));
        
        TextView text2 = (TextView)findViewById(android.R.id.text2);
        text2.setText("O mode triggered when a stroke is moving. To reverse the sign, move backward.");
        
        TextView btn2 = (TextView)findViewById(android.R.id.button2);
        btn2.setText("O");
        btn2.setOnTouchListener(new ZOTouchListener(this, new ZOTouchListener.Dispatcher() {
			@Override
			public void onMove(int mode, View v, int value) {
				mValue2 = rotatedAdd(mValue2, value, VALUE_MIN, VALUE_MAX);
				((TextView)v).setText(String.valueOf(mValue2));
			}
			
			@Override
			public void onClick(View v) {
				Toast.makeText(v.getContext(), "View 2 Clicked.", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onDown(View v) {
				v.setBackgroundColor(0xffccccff);
			}

			@Override
			public void onUp(View v) {
		        v.setBackgroundColor(Color.GRAY);
			}
		}).setMode(ZOTouchListener.MODE_O));
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