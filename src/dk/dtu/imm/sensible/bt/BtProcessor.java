package dk.dtu.imm.sensible.bt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.dtu.imm.sensible.bt.BtBean.ContactFreq;
import dk.dtu.imm.sensible.rest.BluetoothResponseEntity;
import dk.dtu.imm.sensible.utils.MathUtils;

public class BtProcessor {

	public static void getFreqs(long timestamp, List<BluetoothResponseEntity> entities, List<BtBean> beans) {
		Map<Long, Set<String>> buckets = getBuckets(entities);
		Map<String, Float> contacts = new HashMap<String, Float>(); 
		for(Set<String> b : buckets.values()) {
			float w = 1f / b.size();
			for(String str : b) {
				if(contacts.containsKey(str)) {
					contacts.put(str, contacts.get(str) + w);
				} else {
					contacts.put(str, w);
				}
			}
		}
		for(String uid : contacts.keySet()) {
			BtBean b = new BtBean();
			b.uid = uid;
			int pos = beans.indexOf(b);
			float f = contacts.get(uid);
			if(pos >= 0) {
				beans.get(pos).freqs.add(new ContactFreq(timestamp, f));
			} else {
				b.freqs = new ArrayList<BtBean.ContactFreq>();
				b.freqs.add(new ContactFreq(timestamp, f));
				beans.add(b);
			}
		}
		Collections.sort(beans);
	}
	
	public static Map<Long, Set<String>> getBuckets(List<BluetoothResponseEntity> entities) {
		Map<Long, Set<String>> buckets = new HashMap<Long, Set<String>>();
		for(BluetoothResponseEntity e : entities) {
			long index = (long) MathUtils.roundToMultiple(e.timestamp, 300);
			if(buckets.containsKey(index) == false) {
				buckets.put(index, new HashSet<String>());
			}
			buckets.get(index).add(e.devices[0].sensible_user_id);
		}
		return buckets;
	}
	
}