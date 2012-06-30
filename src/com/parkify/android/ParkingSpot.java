package com.parkify.android;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public 
class ParkingSpot implements java.io.Serializable {
	private static final long serialVersionUID = 7526475895622776137L;
	public int mID;
	public double mLat;
	public double mLong;
	public String mCompanyName;
	public int mLocalID;
	public float mPrice;
	public String mPhoneNumber;
	public String mDesc;
	boolean mFree; //temp
	
	ParkingSpot(int inID, double inLat, double inLong) {
		this(inID, inLat, inLong, "Nobody", -1, 0.00F, "408-421-1194", "Just for testing :3", true);
	}
	
	ParkingSpot(int inID,
		double inLat,
		double inLong,
		String inCompanyName,
		int inLocalID,
		float inPrice,
		String inPhoneNumber,
		String inDesc, boolean free) {
		if(inLat > 90 || inLat < 0 || inLong < -180 || inLong > 180) {
			//Error Location
			inLat = 0;
			inLong = 0;
		}
		
		mID = inID;
		mLat = inLat;
		mLong = inLong;
		mCompanyName = inCompanyName;
		mLocalID = inLocalID;
		mPrice = inPrice;
		mPhoneNumber = inPhoneNumber;
		mDesc =  inDesc;
		mFree = free;
	}
	
	
	
	GeoPoint getGeoPoint() {
		return (new GeoPoint((int)(mLat*1000000.0),
    			(int)(mLong*1000000.0)));
	}
	public OverlayItem makeOverlayItem() {
		return new OverlayItem(this.getGeoPoint(), "", String.format("%d", mID));
	}
	
	public String infoString() {
		if (isFree())
		{
			return String.format("%s #%d | $%.2f/hr\n%s to Book!", mCompanyName, mLocalID, mPrice, mPhoneNumber);
		} else {
			return String.format("%s #%d | $%.2f/hr\nSpot is taken.", mCompanyName, mLocalID, mPrice);
		}
		//return String.format("%s Spot #%d | Price: $%.2f/hr\nCALL %s to Book Now!", mCompanyName, mLocalID, mPrice, mPhoneNumber);
		
		//return String.format("%s #%d | $%.2f/hr\nCALL %s to Book Now!", mCompanyName, mLocalID, mPrice, mPhoneNumber);
		//return String.format("%s #%d\n$%.2f/hr", mCompanyName, mLocalID, mPrice);
	}
	
	public String infoStringFull() {
		if (isFree())
		{
			return String.format("%s Spot #%d | Price: $%.2f/hr\nCALL %s to Book Now!", mCompanyName, mLocalID, mPrice, mPhoneNumber);
		} else {
			return String.format("%s Spot #%d | Price: $%.2f/hr\nUnfortunately, this spot is taken.", mCompanyName, mLocalID, mPrice);
		}
		
		//return String.format("%s #%d | $%.2f/hr\n%s to Book!", mCompanyName, mLocalID, mPrice, mPhoneNumber);
		//return String.format("%s #%d | $%.2f/hr\nCALL %s to Book Now!", mCompanyName, mLocalID, mPrice, mPhoneNumber);
		//return String.format("%s #%d\n$%.2f/hr", mCompanyName, mLocalID, mPrice);
	}
	
	public boolean isFree() {
		return mFree;
	}
	
	@Override
	public String toString() {
		return "ParkingSpot [mID=" + mID + "mLat=" + mLat + "mLong=" + mLong + "mCompanyName=" + mCompanyName + "mLocalID=" + 
	mLocalID + "mPrice=" + mPrice + "mPhoneNumber=" + mPhoneNumber + "mDesc=" + mDesc + "mFree=" + mFree + "]";
	
	}
}