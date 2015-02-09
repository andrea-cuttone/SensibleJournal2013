package dk.dtu.imm.sensible.movement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import android.util.Log;
import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.rest.LocationResponseEntity;
import dk.dtu.imm.sensible.stats.StatsCalculator;
import dk.dtu.imm.sensible.stats.TimeAtLocation;
import dk.dtu.imm.sensible.utils.MathUtils;
import dk.dtu.imm.sensible.utils.STDBScan;
import dk.dtu.imm.sensible.utils.STDBScan.ClusteredLocation;

public class LocationProcessor {

	private static List<LocationResponseEntity> clusterTime(List<LocationResponseEntity> locs) {
		ArrayList<LocationResponseEntity> timeClustered = new ArrayList<LocationResponseEntity>();
		if (locs.size() > 0) {
			float lat = locs.get(0).getLat();
			float lon = locs.get(0).getLon();
			long timestamp = locs.get(0).getTime();
			long c = 1;
			for (int i = 1; i < locs.size(); i++) {
				if (locs.get(i).getTime() - timestamp < 600) {
					lat += locs.get(i).getLat();
					lon += locs.get(i).getLon();
					c++;
				} else {
					LocationResponseEntity l = new LocationResponseEntity(timestamp, lat / c, lon / c);
					timeClustered.add(l);
					c = 1;
					lat = locs.get(i).getLat();
					lon = locs.get(i).getLon();
					timestamp = locs.get(i).getTime();
				}
			}
			LocationResponseEntity l = new LocationResponseEntity(timestamp, lat / c, lon / c);
			timeClustered.add(l);
		}
		return timeClustered;
	}
	
	public static double getSpeed(LocationResponseEntity l1, LocationResponseEntity l2) {
		double distInMeters = haversiveDist(l1, l2);
		double timeInSecs = Math.abs(l1.getTime() - l2.getTime());
		return timeInSecs > 0 ? distInMeters / timeInSecs : Double.MAX_VALUE;
	}
	
	private static List<LocationResponseEntity> removeDuplicates(List<LocationResponseEntity> locs) {
		LinkedHashMap<Long, LocationResponseEntity> map = new LinkedHashMap<Long, LocationResponseEntity>();
		for(LocationResponseEntity e : locs) {
			map.put(e.getTime(), e);
		}
		return new ArrayList<LocationResponseEntity>(map.values());
	}
	
	private static List<LocationResponseEntity> removeInvalid(List<LocationResponseEntity> locs) {
		List<LocationResponseEntity> result = new ArrayList<LocationResponseEntity>();
 		Iterator<LocationResponseEntity> iterator = locs.iterator();
 		LocationResponseEntity prev = iterator.next();
 		while(iterator.hasNext()) {
 			LocationResponseEntity curr = iterator.next();
 			double speed = getSpeed(prev, curr);
 			if(curr.getAcc() < 100 && speed < 33 /* ~120 km/h */)  {
 				result.add(curr);
 				prev = curr;
 			} 
 		}
 		return result;
	}
	
	public static List<LocationResponseEntity> removeNoiseByTripletsSpeed(List<LocationResponseEntity> locs) {
		List<LocationResponseEntity> result = new ArrayList<LocationResponseEntity>();
		int prev = 0;
		for (int curr = 1; curr < locs.size(); curr++) {
			boolean doAdd = true;
			double prev2CurrSpeed = getSpeed(locs.get(prev), locs.get(curr));
			int next = curr + 1;
			if (next < locs.size()) {
				double prev2NextSpeed = getSpeed(locs.get(prev), locs.get(next));
				if (prev2CurrSpeed > Constants.WALKING_SPEED * 2 && prev2NextSpeed < Constants.WALKING_SPEED) {
					doAdd = false;
				}
			}
			if (doAdd) {
				result.add(locs.get(curr));
				prev = curr;
			}
		}
		return result;
	}

	public static double haversiveDist(LocationResponseEntity l1, LocationResponseEntity l2) {
		final long R = 6371000;
		double dLat = (l2.getLat() - l1.getLat()) * Math.PI / 180.0f;
		double dLon = (l2.getLon() - l1.getLon()) * Math.PI / 180.0f;
		double lat1 = l1.getLat() * Math.PI / 180.0f;
		double lat2 = l2.getLat() * Math.PI / 180.0f;
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + 
					Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = R * c;
		return d; // meters
	}
	
	public static double haversiveDist(TimeAtLocation a, TimeAtLocation b) {
		LocationResponseEntity l1 = new LocationResponseEntity(a.start, (float) a.lat, (float) a.lon);
		LocationResponseEntity l2 = new LocationResponseEntity(b.start, (float) b.lat, (float) b.lon);
		return haversiveDist(l1, l2);
	}

	private static List<LocationResponseEntity> clusterSpace(List<LocationResponseEntity> locs) {
		List<LocationResponseEntity> clusteredSpace = new ArrayList<LocationResponseEntity>();
		if (locs.size() > 0) {
			float lat = locs.get(0).getLat();
			float lon = locs.get(0).getLon();
			long timestamp = locs.get(0).getTime();
			long c = 1;
			for (int i = 1; i < locs.size(); i++) {
				if (getSpeed(locs.get(i), locs.get(i - 1)) < Constants.WALKING_SPEED) {
					lat += locs.get(i).getLat();
					lon += locs.get(i).getLon();
					c++;
				} else {
					clusteredSpace.add(new LocationResponseEntity(timestamp, lat / c, lon / c));
					clusteredSpace.add(new LocationResponseEntity(locs.get(i - 1).getTime(), lat / c, lon / c));
					c = 1;
					lat = locs.get(i).getLat();
					lon = locs.get(i).getLon();
					timestamp = locs.get(i).getTime();
				}
			}
			clusteredSpace.add(new LocationResponseEntity(timestamp, lat / c, lon / c));
			clusteredSpace.add(new LocationResponseEntity(locs.get(locs.size() - 1).getTime(), lat / c, lon / c));
		}
		return clusteredSpace;
	}

	public static List<LocationResponseEntity> interpolateLocs(List<LocationResponseEntity> locs) {
		final long DELTAT = 120;
		List<LocationResponseEntity> interpolated = new ArrayList<LocationResponseEntity>();
		for (int i = 1; i < locs.size(); i++) {
			long t1 = locs.get(i - 1).getTime();
			long t2 = locs.get(i).getTime();
			if (t2 - t1 > DELTAT) {
				for (long t = t1; t < t2; t += DELTAT) {
					double lat1 = locs.get(i - 1).getLat();
					double lat2 = locs.get(i).getLat();
					double newlat = MathUtils.interpolateXY(t, t1, t2, lat1, lat2);
					double lon1 = locs.get(i - 1).getLon();
					double lon2 = locs.get(i).getLon();
					double newlon = MathUtils.interpolateXY(t, t1, t2, lon1, lon2);
					LocationResponseEntity l = new LocationResponseEntity(t, (float) newlat, (float) newlon);
					interpolated.add(l);
				}
			} else {
				interpolated.add(locs.get(i));
			}
		}
		Log.d(Constants.APP_NAME, "interpolated: " + interpolated.size());
		return interpolated;
	}

	public static List<LocationResponseEntity> process(List<LocationResponseEntity> locs) {
		if(locs.size() > 0) {
			locs = LocationProcessor.removeDuplicates(locs);
			Log.d(Constants.APP_NAME, "after remove dup: " + locs.size());
			locs = LocationProcessor.removeInvalid(locs);
			Log.d(Constants.APP_NAME, "after remove invalid: " + locs.size());
			locs = LocationProcessor.clusterTime(locs);
			Log.d(Constants.APP_NAME, "after clustertime: " + locs.size());
//			locs = LocationProcessor.removeNoiseByDBScan(locs);
//			Log.d(Constants.APP_NAME, "after dbscan: " + locs.size());
			locs = LocationProcessor.removeNoiseByTripletsSpeed(locs);
			Log.d(Constants.APP_NAME, "after remove jumps: " + locs.size());
			locs = LocationProcessor.clusterSpace(locs);
			Log.d(Constants.APP_NAME, "after clusterspace: " + locs.size());
		}
		return locs;
	}

	public static List<LocationResponseEntity> removeNoiseByDBScan(List<LocationResponseEntity> locs) {
		List<ClusteredLocation> clustered = STDBScan.cluster(locs, 5000, 1200, 0, 1);
		List<LocationResponseEntity> noiseRemoved = new ArrayList<LocationResponseEntity>();
		for(ClusteredLocation l : clustered) {
			if(l.label != STDBScan.NOISE) {
				noiseRemoved.add(l.location);
			}
		}
		locs = noiseRemoved;
		return locs;
	}
}
