package dk.dtu.imm.sensible.bt;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.Orientation;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.bt.BtBean.ContactFreq;
import dk.dtu.imm.sensible.rest.PublicProfileResponseEntity;
import dk.dtu.imm.sensible.rest.RestClientV2;
import dk.dtu.imm.sensible.utils.DateTimeUtils;
import dk.dtu.imm.sensiblejournal.R;

public class BtAdapter extends ArrayAdapter<BtBean> {
	private final Activity context;

	public BtAdapter(Activity context) {
		super(context, R.layout.bt_listviewitem_layout);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = context.getLayoutInflater();
		final View itemView = inflater.inflate(R.layout.bt_listviewitem_layout, null, true);  
		final TextView textViewName = (TextView) itemView.findViewById(R.id.textview_bt_name);
		BtBean selected = getItem(position);
		
		final View progress = itemView.findViewById(R.id.progress_bt_item);
		
		final ImageView imgview = (ImageView) itemView.findViewById(R.id.imgview_bt_profile);
		imgview.setImageResource(R.drawable.android_robot);

		LinearLayout chartLayout = (LinearLayout) itemView.findViewById(R.id.chart_bt);
		View pieChart = getBarChartView(selected.freqs);
		chartLayout.removeAllViewsInLayout();
		chartLayout.addView(pieChart, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		new AsyncTask<String, Void, PublicProfileResponseEntity>() {

			protected void onPreExecute() {
				textViewName.setText("Loading user info...");
				progress.setVisibility(View.VISIBLE);
			}
			
			@Override
			protected PublicProfileResponseEntity doInBackground(String... params) {
				PublicProfileResponseEntity profile = null;
				try {
					profile = RestClientV2.instance(context).fetchPublicProfile(params[0]);
				} catch (Exception e) {
					Log.e(Constants.APP_NAME, e.getMessage());
				}
				return profile;
			}
			
			protected void onPostExecute(PublicProfileResponseEntity result) {
				if (result != null) {
					if (result.public_nickname != null && !result.public_nickname.isEmpty()) {
						textViewName.setText(result.public_nickname);
					} else {
						textViewName.setText("Unknown user");
					}
					if (result.public_image_url != null) {
						Log.d(Constants.APP_NAME, result.public_image_url);
						ImageLoader.getInstance().displayImage("https://curie.imm.dtu.dk" + result.public_image_url, imgview, new ImageLoadingListener()  {
							
							@Override
							public void onLoadingStarted() {
								
							}
							
							@Override
							public void onLoadingFailed(FailReason arg0) {
								progress.setVisibility(View.INVISIBLE);
							}
							
							@Override
							public void onLoadingComplete(Bitmap arg0) {
								progress.setVisibility(View.INVISIBLE);
							}
							
							@Override
							public void onLoadingCancelled() {
								progress.setVisibility(View.INVISIBLE);
							}
						});
					} else {
						progress.setVisibility(View.INVISIBLE);
					}
				}
				
			}
		}.execute(selected.uid);
		
		return itemView;
	}
	
	private View getBarChartView(List<ContactFreq> freqs) {
		Collections.sort(freqs);
		XYMultipleSeriesRenderer multipleRenderer = new XYMultipleSeriesRenderer();
		multipleRenderer.setAxisTitleTextSize(25);
		multipleRenderer.setAntialiasing(true);
		multipleRenderer.setLabelsTextSize(24);
		multipleRenderer.setMargins(new int[] { 20, 30, 10, 20 });
		multipleRenderer.setShowLegend(false);
		multipleRenderer.setBackgroundColor(Color.BLACK);
		multipleRenderer.setMarginsColor(Color.BLACK);
		multipleRenderer.setApplyBackgroundColor(true);
		multipleRenderer.setBarSpacing(1f);
		multipleRenderer.setPanEnabled(true, false);
		multipleRenderer.setZoomEnabled(false);
		multipleRenderer.setOrientation(Orientation.HORIZONTAL);
		multipleRenderer.setChartTitle("");
		multipleRenderer.setYTitle("");
		multipleRenderer.setXAxisMin(0.5);
		multipleRenderer.setXAxisMax(5);
		multipleRenderer.setYAxisMin(0.0);
		multipleRenderer.setYAxisMax(50.0);
		multipleRenderer.setAxesColor(Color.GRAY);
		multipleRenderer.setLabelsColor(Color.WHITE);
		multipleRenderer.setXLabels(0);
		multipleRenderer.setYLabels(0);
		for (int i = 0; i < freqs.size(); i++) {
			multipleRenderer.addXTextLabel(i + 1, DateTimeUtils.timestampToDateTime(
					freqs.get(i).timestamp).format("DD MMM", Locale.US));
		}
		SimpleSeriesRenderer simpleRenderer = new SimpleSeriesRenderer();
		simpleRenderer.setColor(context.getResources().getColor(R.color.holo_blue));
		simpleRenderer.setDisplayChartValues(false);
		multipleRenderer.addSeriesRenderer(simpleRenderer);
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		CategorySeries series = new CategorySeries("");
		for (int i = 0; i < freqs.size(); i++) {
			series.add(freqs.get(i).c);
		}
		dataset.addSeries(series.toXYSeries());
		return ChartFactory.getBarChartView(context, dataset, multipleRenderer, Type.DEFAULT);
	}

}
