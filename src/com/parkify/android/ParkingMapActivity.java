package com.parkify.android;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.parkify.android.R;

public class ParkingMapActivity extends MapActivity {
	MapView map=null;
	List<Overlay> mapOverlays;
	Drawable drawFreeParkingPin;
	Drawable drawTakenParkingPin;
	ParkingItemizedOverlay freeParkingOverlay;
	ParkingItemizedOverlay takenParkingOverlay;
	View popup;
	
	int currentSpotID = -1;
	
	boolean bSateliteView = false;
	int maxStreetZoom;
	
	
	public final static String EXTRA_MESSAGE = "com.sl.angelhack.MESSAGE";
	private static final String MAP = "MAPMAPMAP";
	public final static String EXTRA_SPOT = "com.sl.angelhack.SPOT";
	
	//Parking spots:
	HashMap<Integer, ParkingSpot> mParkingSpots;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        map = (MapView)findViewById(R.id.mapview);
        map.getController().setCenter(getPoint(37.872679, -122.266797));
        map.getController().setZoom(17);
        map.setBuiltInZoomControls(true);
        
        ZoomButtonsController zoomButton = map.getZoomButtonsController();
        OnZoomListener listener = new OnZoomListener() {
        	   @Override
        	   public void onVisibilityChanged(boolean arg0) {
        	    // TODO Auto-generated method stub

        	   }
        	   @Override
        	   public void onZoom(boolean arg0) {
        		   if(arg0) { //zoom in
            		   int prevZoomLevel = map.getZoomLevel();
            		   if (prevZoomLevel < map.getMaxZoomLevel()) {
            			   map.getController().zoomIn();
            			   handleStreetSatelite(map.getZoomLevel());
            			   map.invalidate();
            		   }
        			   
        		   } else { //zoom out
        			   int prevZoomLevel = map.getZoomLevel();
        			   if (prevZoomLevel > 1) {
        				   map.getController().zoomOut();
            			   handleStreetSatelite(map.getZoomLevel());
            			   map.invalidate();
        			   }
        		   }
        	   }
        	  };
		zoomButton.setOnZoomListener(listener);
        
        ImageButton btn=(ImageButton)findViewById(R.id.address_button);
        btn.setBackgroundColor(Color.TRANSPARENT);
        
        mapOverlays = map.getOverlays();
        drawFreeParkingPin = this.getResources().getDrawable(R.drawable.parking_icon_free);
        drawTakenParkingPin = this.getResources().getDrawable(R.drawable.parking_icon_taken);
        freeParkingOverlay = new ParkingItemizedOverlay(this, drawFreeParkingPin);
        takenParkingOverlay = new ParkingItemizedOverlay(this, drawTakenParkingPin);
        
        
        //map.setStreetView(true);
        bSateliteView = false;
        map.setSatellite(bSateliteView);
        maxStreetZoom = map.getMaxZoomLevel();
        
        
        mapOverlays.add(takenParkingOverlay);
        mapOverlays.add(freeParkingOverlay);
    	
        // To be loaded from server...
        
        mParkingSpots = new HashMap<Integer, ParkingSpot>();
        //mParkingSpots.put(0, new ParkingSpot(0, 37.872708,-122.266824, "Mike's Bike", 1, 5.00F, "408-421-1194", "A Fantastic Spot!", true));
        //mParkingSpots.put(1, new ParkingSpot(1, 37.872681,-122.266818, "Mike's Bike", 2, 5.01F, "408-421-1194", "A Fantastic Spot!", true));
        // ... To be loaded from server
        
        UpdateParkingOverlay();

        
        popup = getLayoutInflater().inflate(R.layout.spot_popup, map, false);
        //Use MapView.LayoutParams to position the popup with respect to GeoPoint in the ItemizedOverlay< OverlayItem >::onTap method. Popup will scroll automatically (without any additional code) when user scrolls the map. Basically popup gets tied to a GeoPoint, if user zooms, popup's position gets adjusted automatically.
   
        MapView.LayoutParams mapParams = new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                new GeoPoint(0,0),//mParkingSpots.get(0).getGeoPoint(),
                                drawFreeParkingPin.getIntrinsicWidth()/2,
                                -drawFreeParkingPin.getIntrinsicHeight(),
                                MapView.LayoutParams.LEFT);
        
        
        
        map.addView(popup, mapParams);
        TextView infoText = (TextView)findViewById(R.id.info_text);
        infoText.setText("HAHA :3");
        
        /** Updating from server */
        
        Intent intent = new Intent(this, DownloadService.class);
		// Create a new Messenger for the communication back
		Messenger messenger = new Messenger(handler);
		intent.putExtra("MESSENGER", messenger);
		intent.setData(Uri.parse("http://swooplot.herokuapp.com/parking_spots.html"));
		intent.putExtra("urlpath", "http://swooplot.herokuapp.com/parking_spots.html");
		startService(intent);
		
		map.getOverlays().add(new TouchOverlay());
    }
    
    @Override
    protected boolean isRouteDisplayed() {
    	return false;
    }
    
    private GeoPoint getPoint(double lat, double lon) {
    	return (new GeoPoint((int)(lat*1000000.0),
    			(int)(lon*1000000.0)));
    }
    
    private void UpdateParkingOverlay() {
    	
    	freeParkingOverlay.clear();
    	takenParkingOverlay.clear();
    	
    	for (Iterator<ParkingSpot> iterPark = mParkingSpots.values().iterator();
    			iterPark.hasNext();) {
    		ParkingSpot spot = iterPark.next();
    		OverlayItem overlayItem = spot.makeOverlayItem();
    		if(spot.isFree()) {
        		freeParkingOverlay.addOverlay(overlayItem);
    		} else {
    			takenParkingOverlay.addOverlay(overlayItem);
    		}
    	}
    	map.invalidate();
    }
    
    public void CheckSpot(View view) {
    	if (currentSpotID < 0) {
    		return;
    	}
    	Intent intent = new Intent(this, SpotInfoActivity.class);    	
    	intent.putExtra(EXTRA_SPOT, mParkingSpots.get(currentSpotID));
    	startActivity(intent);
    }
    
    public void navigateButtonPressed(View view) {
    	EditText editAddress = (EditText)findViewById(R.id.address_bar);
    	String strAddress = editAddress.getText().toString();
    	if(strAddress.length() == 0) {
    		return;
    	}
    	Log.i(MAP, String.format("Actually clicked something."));
    	Geocoder geoCoder = new Geocoder(this, Locale.getDefault());    
        try {
            List<Address> addresses = geoCoder.getFromLocationName(
            		strAddress , 5);
            if (addresses.size() > 0) {
            	Log.i(MAP, String.format("Actually found something."));
                GeoPoint p = new GeoPoint(
                        (int) (addresses.get(0).getLatitude() * 1E6), 
                        (int) (addresses.get(0).getLongitude() * 1E6));
                map.getController().animateTo(p);    
                map.invalidate();
            }    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /** Called when the user selects the "PARK ME NOW" button */
    public void parkMeNow(View view) {
    	Toast.makeText(ParkingMapActivity.this,
				"Feature coming soon!", Toast.LENGTH_SHORT)
				.show();
    }
    
    
    
    private Handler handler = new Handler() {
		public void handleMessage(Message message) {
			Object json = message.obj;
			Gson gson = new Gson();
			if (message.arg1 == RESULT_OK) {
				
				Log.i(MAP, json.toString());
				Type collectionType = new TypeToken<ArrayList<ParkingSpot>>(){}.getType();
				ArrayList<ParkingSpot> parkingIn = gson.fromJson(json.toString(), collectionType);
				
		        mParkingSpots.clear();
		        for (int i=0; i<parkingIn.size(); i++) {
		        	mParkingSpots.put(parkingIn.get(i).mID, parkingIn.get(i));
		        }
		        
		        UpdateParkingOverlay();
		        
				Toast.makeText(ParkingMapActivity.this,
						"Fresh Data", Toast.LENGTH_LONG)
						.show();
			} else {
				Toast.makeText(ParkingMapActivity.this, "Can't reach server...",
						Toast.LENGTH_LONG).show();
			}
 
		};
	};
	
	
	private class TouchOverlay extends com.google.android.maps.Overlay {
        int lastZoomLevel = -1;

        @Override
        public boolean onTouchEvent(MotionEvent event, MapView mapview) {
            if (event.getAction() == 1) {
                if (lastZoomLevel == -1)
                    lastZoomLevel = map.getZoomLevel();

                if (map.getZoomLevel() != lastZoomLevel) {
                	handleStreetSatelite(map.getZoomLevel());
                    lastZoomLevel = map.getZoomLevel();
                }
            }
            return false;
        }
    }

    public void handleStreetSatelite(int zoomLevel) {
        //reloadMapData(); //act on zoom level change event here
//    	Toast.makeText(AngelHackActivity.this,
//				String.format(":%d", zoomLevel), Toast.LENGTH_SHORT)
//				.show();
    	
        
    	if (bSateliteView) {
    		//switch if level < maxStreetZoom    		
    		if (zoomLevel < maxStreetZoom) {
    			bSateliteView = false;
    			map.setSatellite(bSateliteView);
    		} else {
    			;
    		}
    	} else {
    		//switch if level >= maxStreetZoom
    		if (zoomLevel >= maxStreetZoom) {
    			bSateliteView = true;
    			map.setSatellite(bSateliteView);
    		} else {
    			;
    		}
    	}
    }
	
    
}
