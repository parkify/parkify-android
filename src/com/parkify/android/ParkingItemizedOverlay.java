package com.parkify.android;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.parkify.android.R;

public class ParkingItemizedOverlay extends ItemizedOverlay {
	Drawable mDrawable;
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	
	private ParkingMapActivity angelHack;
	public ParkingItemizedOverlay(ParkingMapActivity angelHackIn, Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		angelHack = angelHackIn;
		mDrawable = defaultMarker;
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
	
	public void clear() {
		mOverlays.clear();
		populate();
	}
	
	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}
	@Override
    protected boolean onTap(int index) {
		super.onTap(index);
		
		angelHack.mapParking.removeView(angelHack.popParkingInfo);
		
		OverlayItem item = mOverlays.get(index);
		
		MapView.LayoutParams mapParams = new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT,
                item.getPoint(),
                mDrawable.getIntrinsicWidth()/2,
                -mDrawable.getIntrinsicHeight(),
                MapView.LayoutParams.LEFT);


		

		angelHack.mapParking.addView(angelHack.popParkingInfo, mapParams);
		TextView infoText = (TextView)angelHack.popParkingInfo.findViewById(R.id.info_text);
		
		angelHack.currentSpotID = Integer.parseInt(item.getSnippet());
		ParkingSpot spot = angelHack.mParkingSpots.get(angelHack.currentSpotID);
		infoText.setText(spot.infoString());
		
//       OverlayItem item = (OverlayItem) items.get(index);
//       AlertDialog.Builder dialog = new AlertDialog.Builder(context);
//       dialog.setTitle(item.getTitle());
//       dialog.setMessage(item.getSnippet());
//       dialog.show();
//       return true;
		return true;
    }
}
