package dk.dtu.imm.sensible.rest;

import com.google.android.maps.GeoPoint;

import dk.dtu.imm.sensible.map.LocationHelper;

public class LocationResponseEntity {

	private GeoPoint gp;

	// do not change var names, or Gson won't work
	private long timestamp;
	private Location location;

	public LocationResponseEntity() {
		location = new Location();
	}
	
	public LocationResponseEntity(long timestamp, float lat, float lon) {
		this();
		this.timestamp = timestamp;
		this.location.latitude = lat;
		this.location.longitude = lon;
	}

	public LocationResponseEntity(long t, float lat, float lon, float acc) {
		this(t, lat, lon);
		this.location.accuracy = acc;
	}

	public static class Location {
		public float latitude;
		public float longitude;
		public float accuracy;
		
		@Override
		public String toString() {
			return "Location [lat=" + latitude + ", lon="
					+ longitude + ", acc=" + accuracy + "]";
		}
	}

	@Override
	public String toString() {
		return "LocationResponseEntity [timestamp=" + timestamp + ", location="
				+ location + "]";
	} 
	
	public long getTime() {
		return timestamp;
	}
	
	public float getLat() {
		return location.latitude;
	}
	
	public float getLon() {
		return location.longitude;
	}
	
	public float getAcc() {
		return location.accuracy;
	}
	
	public GeoPoint getGeoPoint() {
		if(gp == null) {
			gp = LocationHelper.createGeoPoint(getLat(), getLon());
		}
		return gp;
	}
}
