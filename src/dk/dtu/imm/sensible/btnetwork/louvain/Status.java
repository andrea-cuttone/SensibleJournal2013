package dk.dtu.imm.sensible.btnetwork.louvain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.util.SparseIntArray;

public class Status {

	public int total_weight;
	public SparseIntArray node2com;
	public SparseIntArray degrees;
	public SparseIntArray gdegrees;
	public SparseIntArray internals;
	public SparseIntArray loops;

	public Status() {
		this.node2com = new SparseIntArray();
		this.degrees = new SparseIntArray();
		this.gdegrees = new SparseIntArray();
		this.internals = new SparseIntArray();
		this.loops = new SparseIntArray();
		this.total_weight = 0;
	}
	
	public void init(Graph graph) {
		this.node2com.clear();
		this.degrees.clear();
		this.gdegrees.clear();
		this.internals.clear();
		this.loops.clear();
		
		int count = 0;
		this.total_weight = graph.size();
		for (Integer node : graph.getNodes()) {
			this.node2com.put(node, count);
			int deg = graph.degree(node);
			this.degrees.put(count, deg);
			this.gdegrees.put(node, deg);
			Graph.Edge edge = graph.getEdge(node, node);
			this.loops.put(node, edge != null ? edge.weight : 0);
			this.internals.put(count, this.loops.get(node));
			count = count + 1;
		}
	}

	public String toString() {
		return ("node2com : " + this.node2com + " degrees : " + this.degrees + " internals : " + this.internals + " total_weight : " + this.total_weight);
	}

}
