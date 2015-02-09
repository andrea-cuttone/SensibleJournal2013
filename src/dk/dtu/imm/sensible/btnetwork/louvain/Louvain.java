package dk.dtu.imm.sensible.btnetwork.louvain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

import dk.dtu.imm.sensible.btnetwork.louvain.Graph.Edge;

import android.util.SparseIntArray;

public class Louvain {

	private static final double __PASS_MAX = -1;
	private static final double __MIN = 0.0000001;
	
	private ObjectPool<SparseIntArray> pool;
	private HashSet<Integer> communities;
	private HashMap<Integer, Float> inc;
	private HashMap<Integer, Float> deg;
	
	public Louvain() {
		pool = new StackObjectPool<SparseIntArray>(new SparseIntArrayFactory());
		communities = new HashSet<Integer>();
		inc = new HashMap<Integer, Float>();
		deg = new HashMap<Integer, Float>();
	}

	public ObjectPool<SparseIntArray> getPool() {
		return pool;
	}
	
	public float modularity(HashMap<Integer, Integer> partition, Graph graph) {
		inc.clear();
		deg.clear();
	    float links = graph.size();
	    if(links == 0) {
	    	throw new IllegalArgumentException("A graph without link has an undefined modularity");
	    }
	    for(Integer node : graph.getNodes()) {
	        int com = partition.get(node);
	        deg.put(com, deg.get(com) + graph.degree(node));
	        for(Graph.Edge e : graph.getNeighborsEdges(node)) {
	        	Integer neighbor = e.a.equals(node) ? e.b : e.a;
	            if(partition.get(neighbor) == com) {
	            	if(neighbor == node) {
	            		inc.put(com, inc.containsKey(com) ? inc.get(com) : 0 + e.weight);
	            	} else {
	            		inc.put(com, inc.containsKey(com) ? inc.get(com) : 0 + e.weight / 2); 
	            	}
	            }
	        }
	    }
	    float res = 0;
	    for(Integer com : new HashSet<Integer>(partition.values())) {
	    	res += (inc.containsKey(com) ? inc.get(com) : 0 / links) - Math.pow((deg.containsKey(com) ? deg.get(com) : 0 / (2*links)), 2);
	    }
	    return res;
	}
	    		
	public SparseIntArray best_partition(Graph graph) throws Exception {
		List<SparseIntArray> dendo = generate_dendogram(graph);
	    SparseIntArray partition_at_level = partition_at_level(dendo, dendo.size() - 1);
	    for(SparseIntArray s : dendo) {
	    	pool.returnObject(s);
	    }
		return partition_at_level;
	}
	
	public SparseIntArray partition_at_level(List<SparseIntArray> dendogram, int level) throws Exception {
		SparseIntArray partition = pool.borrowObject();
		for (int i = 0; i < dendogram.get(0).size(); i++) {
			partition.append(dendogram.get(0).keyAt(i), dendogram.get(0).get(dendogram.get(0).keyAt(i)));
		}
	    for(int index = 1; index < level + 1; index++) {
	    	for (int i = 0; i < partition.size(); i++) {
	        	int node = partition.keyAt(i);
	        	int community = partition.get(node);
				partition.put(node, dendogram.get(index).get(community));
	        }
	    }
	    return partition;
	}
	    		
	public List<SparseIntArray> generate_dendogram(Graph graph) throws Exception {
		if (graph.size() == 0) {
			SparseIntArray part = pool.borrowObject();
			for (Integer n : graph.getNodes()) {
				part.put(n, n);
				List<SparseIntArray> l = new ArrayList<SparseIntArray>();
				l.add(part);
				return l;
			}
		}
		Graph current_graph = graph.clone();
		Status status = new Status();
		status.init(current_graph);
		float mod = __modularity(status);
		List<SparseIntArray> status_list = new ArrayList<SparseIntArray>();
		__one_level(current_graph, status);
		float new_mod = __modularity(status);
		SparseIntArray partition = __renumber(status.node2com);
		status_list.add(partition);
		mod = new_mod;
		current_graph = induced_graph(partition, current_graph);
		status.init(current_graph);
		while (true) {
			__one_level(current_graph, status);
			new_mod = __modularity(status);
			if (new_mod - mod < __MIN) {
				break;
			}
			partition = __renumber(status.node2com);
			status_list.add(partition);
			mod = new_mod;
			current_graph = induced_graph(partition, current_graph);
			status.init(current_graph);
		}
		return new ArrayList(status_list);
	}
	    		
	public Graph induced_graph(SparseIntArray partition, Graph graph) {
		Graph ret = new Graph();
		for (int i = 0; i < partition.size(); i++) {
			ret.addNode(partition.valueAt(i));
		}
		for (HashMap<Integer, Edge> list : graph.getAllEdges()) {
			for (Graph.Edge e : list.values()) {
				int weight = e.weight > 1 ? e.weight : 1;
				Integer com1 = partition.get(e.a);
				Integer com2 = partition.get(e.b);
				Graph.Edge edge = ret.getEdge(com1, com2);
				int w_prec = edge != null ? edge.weight : 1;
				ret.addEdge(com1, com2, w_prec + weight);
			}
		}
		return ret;
	}
	
	private SparseIntArray __renumber(SparseIntArray dictionary) throws Exception {
	    int count = 0;
	    SparseIntArray ret = pool.borrowObject();
	    for (int i = 0; i < dictionary.size(); i++) {
			ret.append(dictionary.keyAt(i), dictionary.get(dictionary.keyAt(i)));
		}
	    SparseIntArray new_values = pool.borrowObject();
	    for(int i = 0; i < dictionary.size(); i++) {
	    	int key = dictionary.keyAt(i);
	        int value = dictionary.get(key);
	        int new_value = new_values.get(value, -1);
	        if(new_value == -1) {
	            new_values.put(value, count);
	            new_value = count;
	            count++;
	        }
			ret.put(key, new_value);
	    }
	    pool.returnObject(new_values);
	    return ret;
	}
	
	private void __one_level(Graph graph, Status status) throws Exception {
		boolean modif = true;
		int nb_pass_done = 0;
		float cur_mod = __modularity(status);
		float new_mod = cur_mod;
		while (modif && nb_pass_done != __PASS_MAX) {
			cur_mod = new_mod;
			modif = false;
			nb_pass_done++;
			for (Integer node : graph.getNodes()) {
				int com_node = status.node2com.get(node);
				float degc_totw = status.gdegrees.get(node) / (status.total_weight * 2f);
				SparseIntArray neigh_communities = __neighcom(node, graph, status);
				__remove(node, com_node, neigh_communities.get(com_node), status);
				int best_com = com_node;
				float best_increase = 0f;
				for(int i = 0; i < neigh_communities.size(); i++) {
					int key = neigh_communities.keyAt(i);
					float incr = neigh_communities.get(key) - status.degrees.get(key) * degc_totw;
					if (incr > best_increase) {
						best_increase = incr;
						best_com = key;
					}
					__insert(node, best_com, neigh_communities.get(best_com), status);
					if (best_com != com_node) {
						modif = true;
					}
					new_mod = __modularity(status);
					if (new_mod - cur_mod < __MIN) {
						break;
					}
				}
				pool.returnObject(neigh_communities);
			}
		}
	}
	
	private SparseIntArray __neighcom(Integer node, Graph graph, Status status) throws Exception {
		SparseIntArray weights = pool.borrowObject();
		for(Graph.Edge e : graph.getNeighborsEdges(node)) {
			Integer neighbor = e.a.equals(node) ? e.b : e.a;
			Integer neighborcom = status.node2com.get(neighbor);
			int f = weights.get(neighborcom) + e.weight;
			weights.put(neighborcom, f);
		}
		return weights;
	}
	
	private void __remove(Integer node, int com, int weight, Status status) {
		status.degrees.put(com, status.degrees.get(com) - status.gdegrees.get(node));
		status.internals.put(com, status.internals.get(com) - weight - status.loops.get(node));
		status.node2com.put(node, -1);
	} 

	
	private void __insert(Integer node, int com, int weight, Status status) {
		status.node2com.put(node, com);
		status.degrees.put(com, status.degrees.get(com) + status.gdegrees.get(node));
		status.internals.put(com, status.internals.get(com) + weight + status.loops.get(node));
	}

	private float __modularity(Status status) {
		float links = status.total_weight;
		double result = 0;
		communities.clear();
		for (int i = 0; i < status.node2com.size(); i++) {
			communities.add(status.node2com.valueAt(i));
		}
		for(Integer community : communities) {
			double in_degree = status.internals.get(community);
			double degree = status.degrees.get(community);
			if (links > 0) {
				result = result + in_degree / links - Math.pow((degree / (2.0f * links)), 2);
			}
		}
		return (float) result;
	}
	
	static class SparseIntArrayFactory extends BasePoolableObjectFactory<SparseIntArray> { 
	    public SparseIntArray makeObject() { 
	        return new SparseIntArray(); 
	    } 
	     
	    public void passivateObject(SparseIntArray s) { 
	        s.clear(); 
	    }
	}
}


