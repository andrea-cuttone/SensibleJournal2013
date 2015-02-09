package dk.dtu.imm.sensible.stats;

import hirondelle.date4j.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.Toast;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.components.CustomFragment;
import dk.dtu.imm.sensible.components.UILogger;
import dk.dtu.imm.sensible.rest.LocationResponseEntity;
import dk.dtu.imm.sensible.rest.RestClientV2;
import dk.dtu.imm.sensiblejournal.R;

@EFragment(R.layout.stats_layout)
public class StatsFragment extends CustomFragment implements OnScrollListener {

	@ViewById(R.id.listView_stats) ListView listView;

	private StatsAdapter adapter;
	private DateTime currentDate;
	private AsyncTask<DateTime, StatsHolder, StatsHolder> asyncTask;
	private Geocoder geocoder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		geocoder = new Geocoder(getActivity().getBaseContext());
	}
	
	@AfterViews
	protected void afterViews() {
		onTabSelected();
	}
	
	public void initListView() {
		listView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
		});
		if(asyncTask != null) {
			asyncTask.cancel(true);
			asyncTask = null;
		}
		currentDate =  DateTime.now(TimeZone.getDefault()); //DateTime.forDateOnly(2012, 10, 26); 
		adapter = new StatsAdapter(getActivity(), new ArrayList<StatsHolder>());
		listView.setAdapter(adapter);
		listView.setOnScrollListener(this);
	}

	public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
		boolean loadMore = firstVisible + visibleCount >= totalCount;
		if (loadMore) {
			if (asyncTask == null && currentDate.gt(Constants.EARLIEST_DATE)) {
				asyncTask = new AsyncTask<DateTime, StatsHolder, StatsHolder>() {

					@Override
					protected void onProgressUpdate(StatsHolder... values) {
						super.onProgressUpdate(values);
						adapter.add(values[0]);
					}

					@Override
					protected StatsHolder doInBackground(DateTime... params) {
						StatsHolder statsHolder = new StatsHolder();
						statsHolder.datetime = params[0];
						statsHolder.isLoading = true;
						publishProgress(statsHolder);
						try {
							List<LocationResponseEntity> locs = RestClientV2.instance(getActivity().getApplicationContext()).getLocations(currentDate);
							statsHolder.speedStats = StatsCalculator.calculateSpeedStats(locs);
							statsHolder.timeAtLocs = StatsCalculator.findSummaryStaticLocs(
									StatsCalculator.findStaticLocs(locs));
							for(TimeAtLocation t : statsHolder.timeAtLocs) {
								t.geocode(geocoder);
							}
							statsHolder.isLoading = false;
							return statsHolder;
						} catch (Exception e) {
							Log.e(Constants.APP_NAME, e.toString());
							return null;
						}
					}

					@Override
					protected void onPostExecute(StatsHolder result) {
						super.onPostExecute(result);
						if(result != null) {
							adapter.replaceLast(result);
						} else {
							adapter.removeLast();
							FragmentActivity activity = getActivity();
							if(activity != null) {
								Toast.makeText(activity, "Error while contacting server", Toast.LENGTH_SHORT).show();
							}
						}
						currentDate = currentDate.minusDays(1);
						asyncTask = null;
					}

				};
				asyncTask.execute(currentDate);
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) { }

	@Override
	public void onTabSelected() {
		if(getActivity() != null) {
			UILogger.instance(getActivity().getApplicationContext()).logEvent("Stats");
			initListView();
		}
	}

	@Override
	public void onTabUnselected() {
		if(asyncTask != null) {
			asyncTask.cancel(true);
			asyncTask = null;
		}
	}

	@Override
	public String getTabTitle() {
		return "Stats";
	}

}
