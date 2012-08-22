package com.easetheworld.zo_ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import dev.easetheworld.recentbuffer.RecentBuffer;
import dev.easetheworld.ui.zo.StrokeTracker;

public class GestureAnalyzer extends Activity {
	
	private TouchPaintView mTouchPaintView;
	private CheckBox mModeCheckBox;
	private ListView mResultList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gesture_analyzer);
        
        mTracker = new StrokeTracker(this);
        
        mTouchPaintView = (TouchPaintView)findViewById(R.id.touchPaintView);
        
        mResultList = (ListView)findViewById(android.R.id.list);
        mResultList.setDivider(null);
        mResultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mTouchPaintView.highlightPoint(position);
			}
		});
        
        mResultList.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				mTouchPaintView.highlightPoints(firstVisibleItem, firstVisibleItem + visibleItemCount - 1);
			}
		});
        
        mModeCheckBox = (CheckBox)findViewById(android.R.id.checkbox);
        mModeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setAnalyzeMode(isChecked);
			}
		});
        setAnalyzeMode(false);
    }
    
    private void setAnalyzeMode(boolean isAnalyzing) {
		mTouchPaintView.setIsAnalyzing(isAnalyzing);
		if (isAnalyzing) {
	        mResultList.setAdapter(new ResultAdapter(this, analyze(mTouchPaintView.getTouchData())));
//			mResultList.setVisibility(View.VISIBLE);
		} else {
//			mResultList.setVisibility(View.GONE);
			mResultList.setAdapter(null);
		}
    }
    
    private StrokeTracker mTracker;
    
    private ResultItem[] analyze(RecentBuffer<PointF> data) {
    	ResultItem[] results = new ResultItem[data.getSize()];
    	for (int i=0; i<data.getSize(); i++) {
    		PointF p = data.getFromOldest(i);
    		if (i == 0)
    			mTracker.addTouchDown(p.x, p.y);
    		else
	    		mTracker.addTouchMove(p.x, p.y);
    		int state = mTracker.getState();
    		float angle = mTracker.getCosineSquareAngle();
    		results[i] = new ResultItem((angle+1f)/2f, state);
    	}
    	return results;
    }
    
    public static class ResultItem {
    	float fValue; // line graph
    	int iValue; // state
    	ResultItem(float fValue, int iValue) {
    		this.fValue = fValue;
    		this.iValue = iValue;
    	}
    }
    
    private static class ResultAdapter extends ArrayAdapter<ResultItem> {
    	
    	private LayoutInflater mInflater;

		public ResultAdapter(Context context, ResultItem[] objects) {
			super(context, 0, objects);
			mInflater = LayoutInflater.from(context);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ResultView v = null;
			if (convertView == null) {
				v = (ResultView)mInflater.inflate(R.layout.result, parent, false);
			} else {
				v = (ResultView)convertView;
			}
			v.setResultItem(position, getItem(position-1), getItem(position), getItem(position+1));
			return v;
		}

		@Override
		public ResultItem getItem(int position) {
			if (position < 0 || position >= getCount())
				return null;
			else
				return super.getItem(position);
		}
    }
}