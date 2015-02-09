package dk.dtu.imm.sensible.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;

import dk.dtu.imm.sensible.movement.LocationProcessor;
import dk.dtu.imm.sensible.rest.LocationResponseEntity;

public class STDBScan {

	/*public static class Cluster extends ArrayList<LocationResponseEntity> {
		
		private int label;

		public Cluster(int label) {
			this.label = label;
		}
	}*/
	
	public static final Integer NOISE = -1;

	public static class ClusteredLocation {
		
		public Integer label;
		public LocationResponseEntity location;

		public ClusteredLocation(LocationResponseEntity e) {
			this.label = null;
			this.location = e;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((location == null) ? 0 : new Long(location.getTime()).hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClusteredLocation other = (ClusteredLocation) obj;
			if (location == null) {
				if (other.location != null)
					return false;
			} else if (location.getTime() != (other.location.getTime()))
				return false;
			return true;
		}
	}

	
	
	public static List<ClusteredLocation> cluster(List<LocationResponseEntity> locs, double eps1, double eps2, double deltaeps, int minPts) {
		int clusterLabel = 0;
		ArrayList<ClusteredLocation> clustered = new ArrayList<ClusteredLocation>();
		LinkedList<ClusteredLocation> stack = new LinkedList<ClusteredLocation>();
		for(LocationResponseEntity l : locs) {
			clustered.add(new ClusteredLocation(l));
		}
		for (int i = 0; i < clustered.size(); i++) {
			ClusteredLocation curr = clustered.get(i);
			if(curr.label == null) {
				List<ClusteredLocation> neighbors = findNeighbors(clustered, curr, eps1, eps2);
				if(neighbors.size() < minPts) {
					curr.label = NOISE;
				} else {
					clusterLabel++;
					for(ClusteredLocation l : neighbors) {
						l.label = clusterLabel;
						stack.push(l);
					}
					while(stack.isEmpty() == false) {
						ClusteredLocation l = stack.pop();
						List<ClusteredLocation> nabo = findNeighbors(clustered, l, eps1, eps2);
						if(nabo.size() >= minPts) {
							for(ClusteredLocation n : nabo) {
								if(n.label == null /*&& deltaeps */) {
									n.label = clusterLabel;
									stack.push(n);
								}
							}
						}
					}
				}
			}
		}
		return clustered;
	}

	private static List<ClusteredLocation> findNeighbors(List<ClusteredLocation> locs, ClusteredLocation curr, double eps1, double eps2) {
		List<ClusteredLocation> result = new ArrayList<ClusteredLocation>();
		for (int i = 0; i < locs.size(); i++) {
			if(locs.get(i) != curr) {
				if(isNeighbor(locs.get(i).location, curr.location, eps1, eps2)) {
					result.add(locs.get(i));
				}
			}
		}
		return result;
	}

	private static boolean isNeighbor(LocationResponseEntity a, LocationResponseEntity b, double eps1, double eps2) {
		return LocationProcessor.haversiveDist(a, b) < eps1 && Math.abs(a.getTime() - b.getTime()) < eps2;
	}
}
