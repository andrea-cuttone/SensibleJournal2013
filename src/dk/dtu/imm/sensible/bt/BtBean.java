package dk.dtu.imm.sensible.bt;

import java.util.List;
import java.util.Map;

public class BtBean implements Comparable<BtBean> {
	public String uid;
	public List<ContactFreq> freqs;
	public Integer community;

	public float getTotalFreq() {
		int sum = 0;
		for(ContactFreq x : this.freqs) {
			sum += x.c;
		}
		return sum;
	}
	
	@Override
	public int compareTo(BtBean another) {
		return Float.compare(another.getTotalFreq(), this.getTotalFreq());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uid == null) ? 0 : uid.hashCode());
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
		BtBean other = (BtBean) obj;
		if (uid == null) {
			if (other.uid != null)
				return false;
		} else if (!uid.equals(other.uid))
			return false;
		return true;
	}
	
	public static class ContactFreq implements Comparable<ContactFreq> {
		public long timestamp;
		public float c;
		
		public ContactFreq(long timestamp, float c) {
			this.timestamp = timestamp;
			this.c = c;
		}

		@Override
		public int compareTo(ContactFreq another) {
			return Float.compare(another.c, this.c);
		}
		
	}
	
}
