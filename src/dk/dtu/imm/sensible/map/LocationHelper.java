package dk.dtu.imm.sensible.map;

import android.location.Location;

import com.google.android.maps.GeoPoint;

import dk.dtu.imm.sensible.rest.LocationResponseEntity;

public class LocationHelper {

	public static GeoPoint createGeoPoint(double lat, double lon) {
		Location loc = new Location("");
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		return locationToGeoPoint(loc);
	}

	public static GeoPoint locationToGeoPoint(Location loc) {
		Double latitude = loc.getLatitude() * 1E6;
		Double longitude = loc.getLongitude() * 1E6;
		GeoPoint gp = new GeoPoint(latitude.intValue(), longitude.intValue());
		return gp;
	}

}
