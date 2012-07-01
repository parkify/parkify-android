package com.parkify.android;


import java.util.Date;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parkify.android.RangeSeekBar.OnRangeSeekBarChangeListener;
import com.parkify.android.R;

public class SpotInfoActivity extends Activity {
	
	ParkingSpot mSpot;
	
	TextView displayTime;
	TextView displayCost;
	TextView displayInfo;
	TextView displayDesc;
	
	long timeBegin;
	long timeEnd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		
		try {  
			mSpot = (ParkingSpot)intent.getExtras().get(ParkingMapActivity.EXTRA_SPOT);
        } catch(Exception e) {
        	Log.i(" Error at bundle " , e.toString());
    	} 
		
		
		setContentView(R.layout.spot_info);
		
		int a = 0;

		Date minDate = new Date();
		minDate.setMinutes(30*(minDate.getMinutes()/30)); //Intervals of 30min
		minDate.setSeconds(0);
		timeBegin = minDate.getTime();
		timeEnd = timeBegin + 6*30*60000; // 6*30min
		// create RangeSeekBar as Integer range between 20 and 75
		RangeSeekBar<Long> seekBar = new RangeSeekBar<Long>(timeBegin, timeEnd, 30*60000L, this);
		seekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Long>() {
		        //@Override
		        public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Long minValue, Long maxValue) {
		                // handle changed range values
		        		//textView.setText(String.format("User selected new range values: MIN=%d, MAX=%d", minValue, maxValue));
		        	timeBegin = minValue;
		        	timeEnd = maxValue;
		        	updateStrings();
		        }
		});

		displayTime = (TextView)findViewById(R.id.time_display);
		displayCost = (TextView)findViewById(R.id.cost_display);
		displayInfo = (TextView)findViewById(R.id.info_display);
		displayDesc = (TextView)findViewById(R.id.desc_display);
		
		displayTime.setTextSize(25);
		displayCost.setTextSize(25);
		displayInfo.setTextSize(20);
		displayDesc.setTextSize(15);
		
		
		ViewGroup layout = (ViewGroup) findViewById(R.id.time_bar_container);
		layout.addView(seekBar);

		updateStrings();		
	}
	
	public void updateStrings() {
		displayInfo.setText(mSpot.infoStringFull());
		displayDesc.setText(mSpot.mDesc);
		
		SimpleDateFormat ft = new SimpleDateFormat ("hh:mm a");
    	displayTime.setText(ft.format(new Date(timeBegin)) + " - " + ft.format(new Date(timeEnd)));
    	
    	float totalCost = mSpot.mPrice*(timeEnd-timeBegin)/(60*60000F);
    	displayCost.setText(String.format("Total Price: $%.2f", totalCost));    	
	}

}
