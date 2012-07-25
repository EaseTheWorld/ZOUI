package dev.easetheworld.ui.zo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class SensitiveButton extends Button {

	public SensitiveButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	private boolean mIsClickLeft; // true : left, false : right
	private boolean mIsClickTop; // true : top, false : bottom
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			mIsClickLeft = event.getX() < getWidth() / 2;
			mIsClickTop = event.getY() < getHeight() / 2;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean performClick() {
		if (mOnSensitiveClickListener != null)
			mOnSensitiveClickListener.OnSensitiveClick(this, mIsClickLeft, mIsClickTop);
		return super.performClick();
	}

	private OnSensitiveClickListener mOnSensitiveClickListener;
	
	public void setOnSensitiveClickListener(OnSensitiveClickListener l) {
		mOnSensitiveClickListener = l;
	}
	
	public static interface OnSensitiveClickListener {
		void OnSensitiveClick(View v, boolean clickLeft, boolean clickTop);
	}
}
