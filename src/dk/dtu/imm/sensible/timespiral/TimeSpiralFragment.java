package dk.dtu.imm.sensible.timespiral;

import hirondelle.date4j.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import android.content.Intent;
import android.location.Geocoder;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.components.CustomFragment;
import dk.dtu.imm.sensible.components.HorizontalListView;
import dk.dtu.imm.sensible.components.TutorialActivity;
import dk.dtu.imm.sensible.components.UILogger;
import dk.dtu.imm.sensible.movement.LocationProcessor;
import dk.dtu.imm.sensible.rest.LocationResponseEntity;
import dk.dtu.imm.sensible.rest.RestClientV2;
import dk.dtu.imm.sensible.stats.StatsCalculator;
import dk.dtu.imm.sensible.stats.TimeAtLocation;
import dk.dtu.imm.sensible.utils.DBScan;
import dk.dtu.imm.sensible.utils.DateTimeUtils;
import dk.dtu.imm.sensiblejournal.R;

@EFragment(R.layout.timespiral_layout)
public class TimeSpiralFragment extends CustomFragment {

	@ViewById(R.id.surface_spiral) SpiralSurfaceView surfaceSpiral;
	@ViewById(R.id.timeline) HorizontalListView timeline;
	@ViewById(R.id.textview_locs) TextView textviewLocs;
	@ViewById(R.id.progress_timespiral) View progress;

	private static final int REQUEST_SIZE = 7;

	private Geocoder geocoder;
	private volatile boolean tabSelected;
	private volatile DateTime startDate;
	private List<TimeAtLocation> allStaticLocs;
	private ColorGenerator colorGenerator;
	private TimelineAdapter timelineAdapter;
	private int selected;
	private ArrayList<TimeAtLocation> clusters;
	
	@AfterViews
	public void afterViews() {
		allStaticLocs = new ArrayList<TimeAtLocation>();
		geocoder = new Geocoder(getActivity().getBaseContext());
		startDate = DateTime.now(TimeZone.getDefault()).getStartOfDay();
		colorGenerator = new ColorGenerator(); 
		surfaceSpiral.setColorGenerator(colorGenerator);
		timelineAdapter = new TimelineAdapter();
		timeline.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				selected = arg2;
				surfaceSpiral.render(allStaticLocs,  selected);
			}
		});
		timeline.setAdapter(timelineAdapter);
		loadData();
	}
	
	private class TimelineAdapter extends BaseAdapter {
		
		public int getCount() {
            return allStaticLocs.size();
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {  
        	View itemView = convertView;
    		if (itemView == null) {
    			LayoutInflater inflater = getActivity().getLayoutInflater();
    			itemView = inflater.inflate(R.layout.timeline_item_layout, null, true);
    		}
        	TimeAtLocation t = allStaticLocs.get(position);
    		TextView textviewDateStart = (TextView) itemView.findViewById(R.id.tw_date);
    		DateTime from = DateTimeUtils.timestampToDateTime(t.start);
			DateTime to = DateTimeUtils.timestampToDateTime(t.start + t.duration);
			String dateTxt = "";
			if(from.isSameDayAs(to)) {
				dateTxt = from.format("WWW D MMM", Locale.US) + ", from " + from.format("hh:mm") + " to " + to.format("hh:mm");
			} else {
				dateTxt = "from " + from.format("WWW D MMM hh:mm", Locale.US) + " to " + to.format("WWW D MMM hh:mm", Locale.US);
			}
			textviewDateStart.setText(dateTxt);
			textviewDateStart.setTextColor(colorGenerator.get(t.clusterId));
    		TextView textviewEvent = (TextView) itemView.findViewById(R.id.tw_event);
    		textviewEvent.setText(t.description);
    		textviewEvent.setTextColor(colorGenerator.get(t.clusterId));
    		String colStr = Integer.toHexString(colorGenerator.get(t.clusterId)).substring(2);
			String letter = Character.toString((char) ('A' + t.clusterId - 1));
			String url = "http://maps.googleapis.com/maps/api/staticmap?key=AIzaSyCA9d5S16ROB3mAMclBgxGuMxRvzGV-z9c&" +
    				"scale=4&sensor=true&zoom=12&size=250x90&maptype=roadmap&markers=color:0x" + colStr + "%7Clabel:" + letter + "%7C";
    		String latlon = String.format("%f,%f", t.lat, t.lon);
    		ImageView img = (ImageView) itemView.findViewById(R.id.img_event);
    		ImageLoader.getInstance().displayImage(url + latlon, img);    		
            return itemView;
        }

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}
        
}
	
	@Background
	public void loadData() {
		TimeAtLocation lastStatic = null;
		while (startDate.gt(Constants.EARLIEST_DATE) && tabSelected) {
			showProgress();
			try {
				// fetch locs, find static locs, add to UI all but last, repeat merging last static loc
				List<LocationResponseEntity> locs = RestClientV2.instance(getActivity().getApplicationContext()).getLocations(
						startDate.minusDays(REQUEST_SIZE - 1), startDate);
				List<TimeAtLocation> newStaticLocs = StatsCalculator.findStaticLocs(locs);
				if (lastStatic != null && newStaticLocs.size() > 0) {
					TimeAtLocation tal = newStaticLocs.get(newStaticLocs.size() - 1);
					if (LocationProcessor.haversiveDist(
							new LocationResponseEntity(0, (float) lastStatic.lat, (float) lastStatic.lon),
							new LocationResponseEntity(0, (float) tal.lat, (float) tal.lon)) < 300) {
						TimeAtLocation newTal = new TimeAtLocation(tal.start, tal.duration + lastStatic.duration, 
																(lastStatic.lat + tal.lat) / 2, (lastStatic.lon + tal.lon) / 2);
						newStaticLocs.set(newStaticLocs.size() - 1, newTal);
					}
				}
				List<TimeAtLocation> cloned = new ArrayList<TimeAtLocation>();
				for(TimeAtLocation t : allStaticLocs) {
					cloned.add(new TimeAtLocation(t));
				}
				for (int i = newStaticLocs.size() - 1; i > 0; i--) {
					newStaticLocs.get(i).geocode(geocoder);
					cloned.add(newStaticLocs.get(i));
				}
				DBScan.assignCluster(cloned, 300, 1);
				allStaticLocs = cloned;
				clusters = getClusteredTimeAtLocs();
				colorGenerator.buildColorMap(clusters);
				surfaceSpiral.render(allStaticLocs,  selected);
				updateUI();
				lastStatic = newStaticLocs.size() > 0 ? newStaticLocs.get(0) : null;
			} catch (Exception e) {
				Log.e(Constants.APP_NAME, e.toString());
			}
			startDate = startDate.minusDays(REQUEST_SIZE);
		}
		Log.d(Constants.APP_NAME, "done");
		removeProgress();
	}
	
	@UiThread
	protected void showProgress() {
		progress.setVisibility(View.VISIBLE);		
	}
	
	@UiThread
	protected void removeProgress() {
		progress.setVisibility(View.GONE);		
	}

	@UiThread
	public void updateUI() {
		timelineAdapter.notifyDataSetChanged();
		SpannableStringBuilder builder = new SpannableStringBuilder();
		for(TimeAtLocation t : clusters) {
			SpannableString str = new SpannableString(" " + t.description + " ");
			str.setSpan(new ForegroundColorSpan(colorGenerator.get(t.clusterId)), 0, str.length(), 0);
			float scale = (float) (Math.log(t.duration) / Math.log(clusters.get(0).duration));
			str.setSpan(new RelativeSizeSpan(scale), 0, str.length(), 0);
			builder.append(str);
		}
		textviewLocs.setText(builder);
	}
	
	private ArrayList<TimeAtLocation> getClusteredTimeAtLocs() {
		Map<Integer, TimeAtLocation> clusters = new HashMap<Integer, TimeAtLocation>();
		for(TimeAtLocation t : allStaticLocs) {
			if(clusters.containsKey(t.clusterId) == false) {
				TimeAtLocation t2 = new TimeAtLocation(0, t.duration, 0, 0);
				t2.clusterId = t.clusterId;
				t2.description = t.description;
				clusters.put(t.clusterId, t2);
			} else {
				TimeAtLocation t2 = clusters.get(t.clusterId);
				t2.duration += t.duration;
			}
		}
		ArrayList<TimeAtLocation> tal = new ArrayList<TimeAtLocation>(clusters.values());
		Collections.sort(tal, new Comparator<TimeAtLocation>() {

			@Override
			public int compare(TimeAtLocation lhs, TimeAtLocation rhs) {
				return (int) (rhs.duration - lhs.duration);
			}
		});
		return tal;
	}

	@Override
	public void onTabSelected() {
		tabSelected = true;
		if(getActivity() != null) {
			UILogger.instance(getActivity().getApplicationContext()).logEvent("TimeSpiral");
			if(TutorialActivity.wasShowFor(getActivity(), getTabTitle()) == false) {
				Intent intent = TutorialActivity.getIntent(getActivity().getBaseContext(), 
						new int[] { R.drawable.tut_spiral_1, R.drawable.tut_spiral_2 }); 
				startActivity(intent);
			} else {
				afterViews();
			}
		}
	}

	@Override
	public void onTabUnselected() {
		tabSelected = false;
	}
	
	@Override
	public String getTabTitle() {
		return "Timeline";
	}
	
}
