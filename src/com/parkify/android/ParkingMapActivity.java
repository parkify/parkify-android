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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Displays a map filled with parking spaces. This is the main activity used to
 * find available parking spaces.
 */
public class ParkingMapActivity extends MapActivity {
	Drawable drawFreeParkingPin;
	Drawable drawTakenParkingPin;
	MapView mapParking=null;
	List<Overlay> overlayMap;
	ParkingItemizedOverlay overlayFreeParking;
	ParkingItemizedOverlay overlayTakenParking;
	View popParkingInfo;
	MyLocationOverlay myLocationOverlay;
	Geocoder geoCoder;
	
	int currentSpotID = -1;
	
	boolean bSateliteView = false;
	int maxStreetZoom;
	
	
	public final static String EXTRA_MESSAGE = "com.parkify.android.MESSAGE";
	private static final String MAP = "MAPMAPMAP";
	public final static String EXTRA_SPOT = "com.parkify.android.SPOT";
	
	//Parking spots:
	HashMap<Integer, ParkingSpot> mParkingSpots;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mapParking = (MapView)findViewById(R.id.mapview);
        mapParking.getController().setCenter(getPoint(37.872679, -122.266797));
        mapParking.getController().setZoom(17);
        mapParking.setBuiltInZoomControls(true);
        
        geoCoder = new Geocoder(this, Locale.getDefault());
        
        ZoomButtonsController zoomButton = mapParking.getZoomButtonsController();
        OnZoomListener listener = new OnZoomListener() {
        	   //@Override
        	   public void onVisibilityChanged(boolean arg0) {
        	    // TODO Auto-generated method stub

        	   }
        	   //@Override
        	   public void onZoom(boolean arg0) {
        		   if(arg0) { //zoom in
            		   int prevZoomLevel = mapParking.getZoomLevel();
            		   if (prevZoomLevel < mapParking.getMaxZoomLevel()) {
            			   mapParking.getController().zoomIn();
            			   handleStreetSatelite(mapParking.getZoomLevel());
            			   mapParking.invalidate();
            		   }
        			   
        		   } else { //zoom out
        			   int prevZoomLevel = mapParking.getZoomLevel();
        			   if (prevZoomLevel > 1) {
        				   mapParking.getController().zoomOut();
            			   handleStreetSatelite(mapParking.getZoomLevel());
            			   mapParking.invalidate();
        			   }
        		   }
        	   }
        	  };
		zoomButton.setOnZoomListener(listener);
        
        ImageButton btn=(ImageButton)findViewById(R.id.address_button);
        btn.setBackgroundColor(Color.TRANSPARENT);
        
        overlayMap = mapParking.getOverlays();
        drawFreeParkingPin = this.getResources().getDrawable(R.drawable.parking_icon_free);
        drawTakenParkingPin = this.getResources().getDrawable(R.drawable.parking_icon_taken);
        overlayFreeParking = new ParkingItemizedOverlay(this, drawFreeParkingPin);
        overlayTakenParking = new ParkingItemizedOverlay(this, drawTakenParkingPin);
        
        
        //map.setStreetView(true);
        bSateliteView = false;
        mapParking.setSatellite(bSateliteView);
        maxStreetZoom = mapParking.getMaxZoomLevel();
        
        myLocationOverlay = new MyLocationOverlay(this, mapParking);
        myLocationOverlay.enableMyLocation();
        

        overlayMap.add(myLocationOverlay);
        overlayMap.add(overlayTakenParking);
        overlayMap.add(overlayFreeParking);
        
    	
        // To be loaded from server...
        
        mParkingSpots = new HashMap<Integer, ParkingSpot>();
        //mParkingSpots.put(0, new ParkingSpot(0, 37.872708,-122.266824, "Mike's Bike", 1, 5.00F, "408-421-1194", "A Fantastic Spot!", true));
        //mParkingSpots.put(1, new ParkingSpot(1, 37.872681,-122.266818, "Mike's Bike", 2, 5.01F, "408-421-1194", "A Fantastic Spot!", true));
        // ... To be loaded from server
        
        UpdateParkingOverlay();

        
        popParkingInfo = getLayoutInflater().inflate(R.layout.spot_popup, mapParking, false);
        //Use MapView.LayoutParams to position the popup with respect to GeoPoint in the ItemizedOverlay< OverlayItem >::onTap method. Popup will scroll automatically (without any additional code) when user scrolls the map. Basically popup gets tied to a GeoPoint, if user zooms, popup's position gets adjusted automatically.
   
        MapView.LayoutParams mapParams = new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                new GeoPoint(0,0),//mParkingSpots.get(0).getGeoPoint(),
                                drawFreeParkingPin.getIntrinsicWidth()/2,
                                -drawFreeParkingPin.getIntrinsicHeight(),
                                MapView.LayoutParams.LEFT);
        
        
        
        mapParking.addView(popParkingInfo, mapParams);
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
		
		mapParking.getOverlays().add(new TouchOverlay());
		
		
		final EditText etAddressBar = (EditText) findViewById(R.id.address_bar);
		etAddressBar.setOnEditorActionListener(new TextView.OnEditorActionListener() { 
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) { 
		    	Log.i(MAP, String.format("Action: %d", actionId));
		        if (actionId == EditorInfo.IME_ACTION_DONE) { 
		        	String strAddress = etAddressBar.getText().toString();
		        	if(strAddress.length() == 0) {
		        		return false;
		        	}
		        	Log.i(MAP, String.format("Actually clicked something."));
		        	try {
		                List<Address> addresses = geoCoder.getFromLocationName(
		                		strAddress , 5);
		                if (addresses.size() > 0) {
		                	Log.i(MAP, String.format("Actually found something."));
		                    GeoPoint p = new GeoPoint(
		                            (int) (addresses.get(0).getLatitude() * 1E6), 
		                            (int) (addresses.get(0).getLongitude() * 1E6));
		                    mapParking.getController().animateTo(p);    
		                    mapParking.invalidate();
		                }    
		            } catch (IOException e) {
		                e.printStackTrace();
		            }
		            
		        } 
		        return false; 
		    } 
		}); 
		
		
		
		
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
    	
    	overlayFreeParking.clear();
    	overlayTakenParking.clear();
    	
    	for (Iterator<ParkingSpot> iterPark = mParkingSpots.values().iterator();
    			iterPark.hasNext();) {
    		ParkingSpot spot = iterPark.next();
    		OverlayItem overlayItem = spot.makeOverlayItem();
    		if(spot.isFree()) {
        		overlayFreeParking.addOverlay(overlayItem);
    		} else {
    			overlayTakenParking.addOverlay(overlayItem);
    		}
    	}
    	mapParking.invalidate();
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
    	
    	GeoPoint myLocation = myLocationOverlay.getMyLocation();
    	
    	if (myLocation != null) {
    		mapParking.getController().animateTo(myLocation);    
            mapParking.invalidate();
    	} else {
    		Toast.makeText(ParkingMapActivity.this,
    				"Unable to find current location.", Toast.LENGTH_SHORT)
    				.show();
    	}
    	/*
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
                mapParking.getController().animateTo(p);    
                mapParking.invalidate();
            }    
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
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
                    lastZoomLevel = mapParking.getZoomLevel();

                if (mapParking.getZoomLevel() != lastZoomLevel) {
                	handleStreetSatelite(mapParking.getZoomLevel());
                    lastZoomLevel = mapParking.getZoomLevel();
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
    			mapParking.setSatellite(bSateliteView);
    		} else {
    			;
    		}
    	} else {
    		//switch if level >= maxStreetZoom
    		if (zoomLevel >= maxStreetZoom) {
    			bSateliteView = true;
    			mapParking.setSatellite(bSateliteView);
    		} else {
    			;
    		}
    	}
    }
	
    
}
