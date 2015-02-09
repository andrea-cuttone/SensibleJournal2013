package dk.dtu.imm.sensible.timespiral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.stats.TimeAtLocation;

import android.graphics.Color;
import android.util.Log;
import android.widget.ArrayAdapter;

public class ColorGenerator {

	private List<Integer> baseCols;
	private HashMap<Integer, Integer> colorMap;

	public ColorGenerator() {
		colorMap = new HashMap<Integer, Integer>(); 
		baseCols = new ArrayList<Integer>();
		baseCols.add(Color.argb(128, 255, 255, 255));
		for(int col : Constants.BASE_COLORS) {
			baseCols.add(col);
		}
	}
	
	public synchronized void buildColorMap(List<TimeAtLocation> clusters) {
		HashMap<Integer,Integer> map = new HashMap<Integer, Integer>();
		for (int i = 0; i < clusters.size(); i++) {
			int col = i < baseCols.size() ? baseCols.get(i) : Color.LTGRAY;
			map.put(clusters.get(i).clusterId, col);
		}
		colorMap = map;
	}
	
	public synchronized int get(int index) {
		return colorMap.containsKey(index) ? colorMap.get(index) : Color.BLACK;
	}
}
