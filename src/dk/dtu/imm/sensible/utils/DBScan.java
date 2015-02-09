package dk.dtu.imm.sensible.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;

import dk.dtu.imm.sensible.movement.LocationProcessor;
import dk.dtu.imm.sensible.rest.LocationResponseEntity;
import dk.dtu.imm.sensible.stats.TimeAtLocation;

public class DBScan {

	public static final Integer NOISE = -1;

	public static void assignCluster(List<TimeAtLocation> locs, double eps1, int minPts) {
		// TODO: can it be done incrementally?
		for(TimeAtLocation t : locs) {
			t.clusterId = null;
		}
		int clusterLabel = 0;
		LinkedList<TimeAtLocation> stack = new LinkedList<TimeAtLocation>();
		for (int i = 0; i < locs.size(); i++) {
			TimeAtLocation curr = locs.get(i);
			if(curr.clusterId == null) {
				List<TimeAtLocation> neighbors = findNeighbors(locs, curr, eps1);
				if(neighbors.size() < minPts) {
					curr.clusterId = NOISE;
				} else {
					clusterLabel++;
					for(TimeAtLocation l : neighbors) {
						l.clusterId = clusterLabel;
						stack.push(l);
					}
					while(stack.isEmpty() == false) {
						TimeAtLocation l = stack.pop();
						List<TimeAtLocation> nabo = findNeighbors(locs, l, eps1);
						if(nabo.size() >= minPts) {
							for(TimeAtLocation n : nabo) {
								if(n.clusterId == null) {
									n.clusterId = clusterLabel;
									stack.push(n);
								}
							}
						}
					}
				}
			}
		}
	}

	private static List<TimeAtLocation> findNeighbors(List<TimeAtLocation> locs, TimeAtLocation curr, double eps1) {
		List<TimeAtLocation> result = new ArrayList<TimeAtLocation>();
		for (int i = 0; i < locs.size(); i++) {
			if(isNeighbor(locs.get(i), curr, eps1)) {
				result.add(locs.get(i));
			}
		}
		return result;
	}

	private static boolean isNeighbor(TimeAtLocation a, TimeAtLocation b, double eps1) {
		return LocationProcessor.haversiveDist(a, b) < eps1;
	}
}
