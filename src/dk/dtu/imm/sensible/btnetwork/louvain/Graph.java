package dk.dtu.imm.sensible.btnetwork.louvain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Graph {

	private HashMap<Integer,HashMap<Integer,Edge>> edges;

	public Graph() {
		edges = new HashMap<Integer, HashMap<Integer, Edge>>();
	}

	public int size() {
		int s = 0;
		for(Integer v : edges.keySet()) {
			for(Edge e : edges.get(v).values()) {
				s += e.weight;
			}
		}
		return s / 2;
	}
	
	public Set<Integer> getNodes() {
		return edges.keySet();
	}

	public int degree(Integer node) {
		int d = 0;
		for (Edge e : edges.get(node).values()) {
			d += e.weight;
		}
		return d / 2;
	}
	
	public Edge getEdge(Integer a, Integer b) {
		HashMap<Integer, Edge> edgesFromA = edges.get(a);
		if(edgesFromA != null) {
			Edge e = edgesFromA.get(b);
			if(e != null) {
				return e;
			}
		}
		return null;
	}

	public Collection<Edge> getNeighborsEdges(Integer node) {
		return edges.get(node).values();
	}

	public void addNode(Integer node) {
		if(edges.containsKey(node) == false) {
			edges.put(node, new HashMap<Integer, Edge>());
		}
	}
	
	public void addEdge(Integer a, Integer b, int w) {
		addNode(a);
		addNode(b);
		Edge edge = getEdge(a, b);
		if(edge != null) {
			edge.weight = w;
		} else {
			edge = new Edge(a, b, w);
			edges.get(a).put(b, edge);
			edges.get(b).put(a, edge);
		}
	}
	
	public Graph clone() {
		Graph copy = new Graph();
		for(Integer n : this.getNodes()) {
			copy.addNode(n);
			for(Edge e : edges.get(n).values()) {
				copy.addEdge(e.a, e.b, e.weight);
			}
		}
		return copy;
	}
	
	public Collection<HashMap<Integer,Edge>> getAllEdges() {
		return edges.values();
	}
	
	public static class Edge {
		public Integer a;
		public Integer b;
		public int weight;
		
		public Edge(Integer a, Integer b, int weight) {
			this.a = a;
			this.b = b;
			this.weight = weight;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((b == null) ? 0 : b.hashCode());
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
			Edge other = (Edge) obj;
			if ((a == other.a && b == other.b) ||
				(a == other.b && b == other.a)   )	
					return true;
			return false;
		}
	}

}

