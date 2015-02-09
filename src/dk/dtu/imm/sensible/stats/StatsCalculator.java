package dk.dtu.imm.sensible.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.movement.LocationProcessor;
import dk.dtu.imm.sensible.rest.LocationResponseEntity;

public class StatsCalculator {

	public static SpeedStats calculateSpeedStats(List<LocationResponseEntity> locs) {
		SpeedStats statsBean = new SpeedStats();
		for (int i = 1; i < locs.size(); i++) {
			double distInMeters = LocationProcessor.haversiveDist(locs.get(i), locs.get(i - 1));
			double timeInSecs = Math.abs(locs.get(i).getTime() - locs.get(i-1).getTime());
			if (timeInSecs > 0) {
				double speed = distInMeters / timeInSecs;
				if (speed > Constants.VEHICLE_SPEED) {
					statsBean.timeVehicle += timeInSecs;
					statsBean.distanceVehicle += distInMeters;
				} else if (speed > Constants.WALKING_SPEED) {
					statsBean.timeWalking += timeInSecs;
					statsBean.distanceWalking += distInMeters;
				} else {
					statsBean.timeStationary += timeInSecs;
				}
			}
		}
		return statsBean;
	}
	
	public static List<TimeAtLocation> findStaticLocs(List<LocationResponseEntity> locs) {
		ArrayList<TimeAtLocation> timeLocs = new ArrayList<TimeAtLocation>();
		for(int i = 1; i < locs.size(); i++) {
			if(locs.get(i).getLat() - locs.get(i-1).getLat() < Constants.WALKING_SPEED &&
			   locs.get(i).getLon() - locs.get(i-1).getLon() < Constants.WALKING_SPEED   ) {
				long duration = locs.get(i).getTime() - locs.get(i-1).getTime();
				if(duration > 1800) {
					timeLocs.add(new TimeAtLocation(locs.get(i-1).getTime(), 
							duration, locs.get(i).getLat(), locs.get(i).getLon()));
				}
				i++;
			}
		}
		return timeLocs;
	}
	
	public static List<TimeAtLocation> findSummaryStaticLocs(List<TimeAtLocation> timeLocs) {
		ArrayList<TimeAtLocation> totals = new ArrayList<TimeAtLocation>();
		for (int i = 0; i < timeLocs.size(); i++) {
			TimeAtLocation curr = timeLocs.get(i);
			if (curr != null) {
				float lat = (float) curr.lat;
				float lon = (float) curr.lon;
				long t = curr.duration;
				int c = 1;
				for (int j = i + 1; j < timeLocs.size(); j++) {
					TimeAtLocation next = timeLocs.get(j);
					if (next != null && LocationProcessor.haversiveDist(
							new LocationResponseEntity(0, (float) next.lat, (float) next.lon),
							new LocationResponseEntity(0, lat / c, lon / c)) < 150) {
						lat += next.lat;
						lon += next.lon;
						t += next.duration;
						c++;
						timeLocs.set(j, null);
					}
				}
				if (t >= 1800) {
					TimeAtLocation timeAtLocation = new TimeAtLocation(0, t, lat / c, lon / c);
					totals.add(timeAtLocation);
				}
			}
		}
		Collections.sort(totals, new Comparator<TimeAtLocation>() {
		
			@Override
			public int compare(TimeAtLocation lhs, TimeAtLocation rhs) {
				return (int) (rhs.duration - lhs.duration);
			}
		});
		return totals;
	}
	
}
