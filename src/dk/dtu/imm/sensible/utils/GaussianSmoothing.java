package dk.dtu.imm.sensible.utils;

import java.util.ArrayList;
import java.util.List;

import dk.dtu.imm.sensible.rest.LocationResponseEntity;

public class GaussianSmoothing {

	private static final int N = 3;
	private static final int RO = 400;

	public static List<LocationResponseEntity> smooth(List<LocationResponseEntity> locs) {
		ArrayList<LocationResponseEntity> result = new ArrayList<LocationResponseEntity>();
		for (int i = 0; i < N; i++) {
			result.add(locs.get(i));
		}
		for (int i = N; i < locs.size() - N; i++) {
			double sumLat = 0;
			double sumLon = 0;
			double sumW = 0;
			for (int j = i - N; j < i + N; j++) {
				double w = getW(locs.get(i).getTime(), locs.get(j).getTime());
				sumLat += w * locs.get(j).getLat();
				sumLon += w * locs.get(j).getLon();
				sumW += w;
			}
			result.add(new LocationResponseEntity(locs.get(i).getTime(), 
					(float) (sumLat / sumW), (float) (sumLon / sumW)));
		}
		for (int i = locs.size() - N; i < locs.size(); i++) {
			result.add(locs.get(i));
		}
		return result;
	}
	
	private static double getW(long t, long tj) {
		double x = -Math.pow(t-tj,2) / (2*RO*RO);
		double res = Math.exp(x);
		return res;
	}
}
