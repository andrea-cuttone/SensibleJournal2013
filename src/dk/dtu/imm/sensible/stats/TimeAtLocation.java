package dk.dtu.imm.sensible.stats;

import hirondelle.date4j.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.utils.DateTimeUtils;

import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

public class TimeAtLocation {
		public long start;
		public long duration;
		public double lat;
		public double lon;
		public String description;
		public Integer clusterId;
		private String datetimeString;

		public TimeAtLocation(long start, long duration, double lat, double lon) {
			this.start = start;
			this.duration = duration;
			this.lat = lat;
			this.lon = lon;
			this.description = "";
			this.clusterId = null;
		}
		
		public TimeAtLocation(TimeAtLocation orig) {
			this.start = orig.start;
			this.duration = orig.duration;
			this.lat = orig.lat;
			this.lon = orig.lon;
			this.description = orig.description;
			this.clusterId = orig.clusterId;
		}
		
		public void geocode(Geocoder geocoder) {
			List<Address> list = null;
			try {
				list = geocoder.getFromLocation(lat, lon, 1);
			} catch (IOException e) {
				Log.e(Constants.APP_NAME, e.toString());
			}
			if (list != null && list.size() > 0) {
				description = list.get(0).getAddressLine(0);
			} else {
				description = String.format("%f, %f", lat, lon);
			}
		}
		
		public String getDateTimeString() {
			if(this.datetimeString == null) {
				this.datetimeString = DateTimeUtils.timestampToDateTime(start + duration / 2).format("DD MMM", Locale.US);
			}
			return datetimeString;
		}
		
	}