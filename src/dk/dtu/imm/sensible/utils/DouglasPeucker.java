package dk.dtu.imm.sensible.utils;

import java.util.ArrayList;
import java.util.List;

import dk.dtu.imm.sensible.rest.LocationResponseEntity;

public class DouglasPeucker {

	private static double getPointLineDist(LocationResponseEntity loc, 
			LocationResponseEntity a, LocationResponseEntity b) {
        double pointX = loc.getLon();
        double pointY = loc.getLat();
        double aX = a.getLon();
        double aY = a.getLat();
        double bX = b.getLon();
        double bY = b.getLat();
        if(Math.abs(bX - aX) > 0) {
        	double slope = (bY - aY) / (bX - aX);
        	double intercept = aY - (slope * aX);
        	double result = Math.abs(slope * pointX - pointY + intercept) / Math.sqrt(Math.pow(slope, 2) + 1);
        	return result;
        } else {
        	return Math.abs(pointX - aX);
        }
    }

    public static List<LocationResponseEntity> douglasPeucker(List<LocationResponseEntity> points, double epsilon) {
        int maxIndex = 0;
        double maxDistance = 0;
        for (int i = 1; i < points.size(); i++) {
            double d = getPointLineDist(points.get(i), points.get(0), points.get(points.size() - 1));
            if (d > maxDistance) {
                maxIndex = i;
                maxDistance = d;
            }
        }
        List<LocationResponseEntity> filteredPoints = new ArrayList<LocationResponseEntity>();
        if (maxDistance >= epsilon) {
        	List<LocationResponseEntity> left = douglasPeucker(
        			new ArrayList<LocationResponseEntity>(points.subList(0, maxIndex+1)), epsilon);
            List<LocationResponseEntity> right = douglasPeucker(
            		new ArrayList<LocationResponseEntity>(points.subList(maxIndex, points.size())), epsilon);
            filteredPoints.addAll(left.subList(0, left.size() - 1));
            filteredPoints.addAll(right);
        } else {
        	filteredPoints.add(points.get(0));
        	filteredPoints.add(points.get(points.size() - 1));
        }
        return filteredPoints;
    }
}
