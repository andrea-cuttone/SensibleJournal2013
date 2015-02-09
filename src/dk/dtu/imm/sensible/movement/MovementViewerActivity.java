package dk.dtu.imm.sensible.movement;
 
import hirondelle.date4j.DateTime;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Rect;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.OverlayItem;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.components.CustomDatePicker;
import dk.dtu.imm.sensible.components.CustomDatePicker.DateChangeListener;
import dk.dtu.imm.sensible.components.ISelectableTab;
import dk.dtu.imm.sensible.components.TutorialActivity;
import dk.dtu.imm.sensible.components.UILogger;
import dk.dtu.imm.sensible.login.LoginActivity;
import dk.dtu.imm.sensible.map.CustomMapView;
import dk.dtu.imm.sensible.map.LocationHelper;
import dk.dtu.imm.sensible.map.MapEventsListener;
import dk.dtu.imm.sensible.map.SimpleItemizedOverlay;
import dk.dtu.imm.sensible.movement.renderers.AnimMovementRenderer;
import dk.dtu.imm.sensible.movement.renderers.CanvasRenderer;
import dk.dtu.imm.sensible.rest.LocationResponseEntity;
import dk.dtu.imm.sensible.rest.RestClientV2;
import dk.dtu.imm.sensible.stats.SpeedStats;
import dk.dtu.imm.sensible.stats.StatsCalculator;
import dk.dtu.imm.sensible.stats.TimeAtLocation;
import dk.dtu.imm.sensible.utils.DateTimeUtils;
import dk.dtu.imm.sensiblejournal.R;
 
@EActivity(R.layout.movement_map_layout)
public class MovementViewerActivity extends MapActivity implements ISelectableTab {

	@ViewById(R.id.map_movement)			CustomMapView mapView;
	@ViewById(R.id.surface_movement) 		CustomSurfaceView surfaceView;
	@ViewById(R.id.progress_movement) 		View progressView;
	@ViewById(R.id.datepicker_movement) 	CustomDatePicker datePicker;
	@ViewById(R.id.textview_no_loc_data) 	TextView tvNoLocData;
	@ViewById(R.id.seekbar_movement) 		SeekBar seekbarAnim;
	@ViewById(R.id.btn_movmap_playstop)		ImageView imgviewPlayStop;
	
	private AsyncTask<DateTime, Void, Object[]> loadingTask;
	private CanvasRenderer renderer;
	private boolean isPlaying;
	private SimpleItemizedOverlay itemizedoverlay;
	private Geocoder geocoder;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		isPlaying = false;
		geocoder = new Geocoder(getBaseContext());
	}
	
	@AfterViews
	protected void afterViews() {
        datePicker.setDateChangeListener(new DateChangeListener() {
			public void onDateChanged() {
				refreshData();
			}
		});
        mapView.setMapEventsListener(new MapEventsListener() {
			private void renderAgain() {
				renderer.render(seekbarAnim.getProgress());
			}

			@Override
			public void onZoom() {
				renderAgain();
			}
			
			@Override
			public void onPan() {
				renderAgain();
			}
		});
        itemizedoverlay = new SimpleItemizedOverlay(getResources().getDrawable(R.drawable.map_marker), mapView);
		mapView.getOverlays().add(itemizedoverlay);
        seekbarAnim.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				renderer.render(progress);
				if(fromUser) {
					setPlaying(false); 
				}
			}
		});
        imgviewPlayStop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setPlaying(!isPlaying);
			}

		});
        renderer = new AnimMovementRenderer(surfaceView, mapView);
	}
	
	private void setPlaying(boolean status) {
		isPlaying = status;
		imgviewPlayStop.setImageResource(isPlaying ? R.drawable.pause : R.drawable.play);
		if(isPlaying) {
			if(seekbarAnim.getProgress() == seekbarAnim.getMax()) {
				seekbarAnim.setProgress(0);
			}
			seekbarAnim.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					if(isPlaying && seekbarAnim.getProgress() < seekbarAnim.getMax()) {
						seekbarAnim.setProgress(seekbarAnim.getProgress() + 1);
						seekbarAnim.postDelayed(this, renderer.getFrameDuration());
					} else {
						setPlaying(false);
					}
				}
			}, 10);
		}
	}

	private void refreshData() {
		if(loadingTask != null) {
			loadingTask.cancel(true);
		}
		seekbarAnim.setMax(0);
		seekbarAnim.setEnabled(false);
		setPlaying(false);
		itemizedoverlay.clear();
		mapView.invalidate();
		surfaceView.clearCanvas();
		loadingTask = new AsyncTask<DateTime, Void, Object[]>() {
		
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progressView.setVisibility(View.VISIBLE);
				tvNoLocData.setVisibility(View.GONE);
			}
		
			@Override
			protected Object[] doInBackground(DateTime... params) {
				if(RestClientV2.instance(getApplicationContext()).hasValidToken() == false) {
					showSessionExpiredDlg();
					cancel(true);
				}
				try {
					List<LocationResponseEntity> locs = loadFromServer(params[0]);    
					if(locs == null) {
						return null;
					}
					if(isCancelled() == false && locs.size() > 0) {
						int minLat = Integer.MAX_VALUE;
						int maxLat = Integer.MIN_VALUE;
						int minLon = Integer.MAX_VALUE;
						int maxLon = Integer.MIN_VALUE;
						for (LocationResponseEntity e : locs) {
							int lat = e.getGeoPoint().getLatitudeE6();
							int lon = e.getGeoPoint().getLongitudeE6();
							maxLat = Math.max(lat, maxLat);
							minLat = Math.min(lat, minLat);
							maxLon = Math.max(lon, maxLon);
							minLon = Math.min(lon, minLon);
						}
						// TODO: threading problem?
						renderer.initLocations(locs);
						List<TimeAtLocation> staticLocs = StatsCalculator.findStaticLocs(locs);
						List<TimeAtLocation> summaryStaticLocs = StatsCalculator.findSummaryStaticLocs(staticLocs);
						for(TimeAtLocation t : summaryStaticLocs) {
							t.geocode(geocoder);
						}
						return new Object [] { locs, new Rect(minLat, minLon, maxLat, maxLon), summaryStaticLocs };
					}
				} catch (Exception exc) {
					Log.e(Constants.APP_NAME, getClass().getSimpleName()  + ": " + exc.toString());
				}
				return new Object [] { new ArrayList<LocationResponseEntity>() , null };
			}
		
			@Override
			protected void onPostExecute(Object[] result) {
				super.onPostExecute(result);
				if (result != null) {
					List<LocationResponseEntity> locations = (List<LocationResponseEntity>) result[0];
					if (locations.size() > 0) {
						Rect bounds = (Rect) result[1];
						//TODO: change to animateTo, update canvas correctly
						mapView.getController().setCenter(new GeoPoint(bounds.centerX(), bounds.centerY()));
						mapView.getController().zoomToSpan(bounds.width(), bounds.height());
						List<TimeAtLocation> summaryStaticLocs = (List<TimeAtLocation>) result[2];
						for(TimeAtLocation l : summaryStaticLocs) {
							itemizedoverlay.addOverlay(
									new OverlayItem(LocationHelper.createGeoPoint(l.lat, l.lon), 
											l.description, 
											"Total time at location: " + DateTimeUtils.smartFormatTime(l.duration)));
						}
						mapView.invalidate();
						seekbarAnim.setProgress(0);
						seekbarAnim.setMax(renderer.getTotalFrames());
						seekbarAnim.setEnabled(true);
						setPlaying(true);
					} else {
						tvNoLocData.setVisibility(View.VISIBLE);
						seekbarAnim.setEnabled(false);
						seekbarAnim.setMax(0);
					}
				} else {
					Toast.makeText(getBaseContext(), "Error while contacting server", Toast.LENGTH_SHORT).show();
					seekbarAnim.setEnabled(false);
					seekbarAnim.setMax(0);
				}
				progressView.setVisibility(View.INVISIBLE);
			}
			
			@Override
			protected void onCancelled() {
				super.onCancelled();
			}
		};
		loadingTask.execute(datePicker.getDateTime());
	}
	
	private List<LocationResponseEntity> loadFromServer(DateTime datetime) {
		try {
			List<LocationResponseEntity> locs = RestClientV2.instance(getApplicationContext()).getLocations(datetime);
			return locs;
		} catch (Exception ignore) { }
		return null;
	}
	
	@UiThread
	protected void showSessionExpiredDlg() {
		LoginActivity.showSessionExpiredDlg(this);
	}
	
	@Override
	public void onTabSelected() {
		UILogger.instance(getApplicationContext()).logEvent("Movement");
		if(TutorialActivity.wasShowFor(this, getTabTitle()) == false) {
			Intent intent = TutorialActivity.getIntent(getBaseContext(), 
					new int[] { R.drawable.tut_mov_1, R.drawable.tut_mov_2 }); 
			startActivity(intent); 
		} else {
			refreshData();
		}
	}

	@Override
	public void onTabUnselected() {
		if(loadingTask != null) {
			loadingTask.cancel(true);
		}
		setPlaying(false);
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
    @Override
    public void onBackPressed() {
    	// do nothing
    }
    
	@Override
	public String getTabTitle() {
		return "Movements";
	}
    
}