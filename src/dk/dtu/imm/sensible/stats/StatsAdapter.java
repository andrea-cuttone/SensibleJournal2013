package dk.dtu.imm.sensible.stats;

import hirondelle.date4j.DateTime;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import dk.dtu.imm.sensible.components.StatsRowView;
import dk.dtu.imm.sensible.utils.DateTimeUtils;
import dk.dtu.imm.sensiblejournal.R;

public class StatsAdapter extends ArrayAdapter<StatsHolder> {
	private final Activity context;
	private final List<StatsHolder> statsHolders;
	private int [] colors;

	public StatsAdapter(Activity context, List<StatsHolder> data) {
		super(context, R.layout.stats_listviewitem_layout, data);
		this.context = context;
		this.statsHolders = data;
		colors = new int [] {
				context.getResources().getColor(R.color.stats1),
				context.getResources().getColor(R.color.stats2),
				context.getResources().getColor(R.color.stats3),
		};
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View itemView = convertView;
		if (itemView == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			itemView = inflater.inflate(R.layout.stats_listviewitem_layout, null, true);
		}
		StatsHolder selected = statsHolders.get(position);
		TextView tvDate = (TextView) itemView.findViewById(R.id.textview_listviewitemstats_date);
		String datestring = "";
		if (selected.datetime.isSameDayAs(DateTime.today(TimeZone.getDefault()))) {
			datestring = "today, ";
		} else if (selected.datetime.isSameDayAs(DateTime.today(TimeZone.getDefault()).minusDays(1))) {
			datestring = "yesterday, ";
		}
		datestring += selected.datetime.format("WWW D MMM YYYY", Locale.US);
		tvDate.setText(datestring);

		StatsRowView rowStationary = (StatsRowView) itemView.findViewById(R.id.rowview_stationary);
		StatsRowView rowWalking = (StatsRowView) itemView.findViewById(R.id.rowview_walking);
		StatsRowView rowVehicle = (StatsRowView) itemView.findViewById(R.id.rowview_vehicle);
		StatsRowView rowLoc1 = (StatsRowView) itemView.findViewById(R.id.rowview_loc1);
		StatsRowView rowLoc2 = (StatsRowView) itemView.findViewById(R.id.rowview_loc2);
		StatsRowView rowLoc3 = (StatsRowView) itemView.findViewById(R.id.rowview_loc3);
		LinearLayout chartLayout = (LinearLayout) itemView.findViewById(R.id.chart_stats);
		View progressView = itemView.findViewById(R.id.progress_stats);
		View noData = itemView.findViewById(R.id.textview_no_stats_data);
		View v_separator = itemView.findViewById(R.id.v_separator);
		
		if (selected.isLoading) {
			noData.setVisibility(View.GONE);
			progressView.setVisibility(View.VISIBLE);
			rowStationary.setVisibility(View.GONE);
			rowWalking.setVisibility(View.GONE);
			rowVehicle.setVisibility(View.GONE);
			chartLayout.setVisibility(View.GONE);
			rowLoc1.setVisibility(View.GONE);
			rowLoc2.setVisibility(View.GONE);
			rowLoc3.setVisibility(View.GONE);
			v_separator.setVisibility(View.GONE);
		} else if(selected.speedStats.timeStationary + selected.speedStats.timeWalking + selected.speedStats.timeVehicle < 1) {
			noData.setVisibility(View.VISIBLE);
			progressView.setVisibility(View.GONE);
			rowStationary.setVisibility(View.GONE);
			rowWalking.setVisibility(View.GONE);
			rowVehicle.setVisibility(View.GONE);
			chartLayout.setVisibility(View.GONE);
			rowLoc1.setVisibility(View.GONE);
			rowLoc2.setVisibility(View.GONE);
			rowLoc3.setVisibility(View.GONE);
			v_separator.setVisibility(View.GONE);
		} else {
			noData.setVisibility(View.GONE);
			progressView.setVisibility(View.GONE);
			rowStationary.setVisibility(View.VISIBLE);
			rowWalking.setVisibility(View.VISIBLE);
			rowVehicle.setVisibility(View.VISIBLE);
			chartLayout.setVisibility(View.VISIBLE);
			rowLoc1.setVisibility(View.VISIBLE);
			rowLoc2.setVisibility(View.VISIBLE);
			rowLoc3.setVisibility(View.VISIBLE);
			v_separator.setVisibility(View.VISIBLE);

			rowStationary.setColor(colors[0]);
			rowStationary.setTitle("Stationary");
			rowStationary.setData(DateTimeUtils.smartFormatTime(selected.speedStats.timeStationary));
			rowWalking.setColor(colors[1]);
			rowWalking.setTitle("Walking");
			rowWalking.setData(String.format("%s, %.1f km", DateTimeUtils.smartFormatTime(selected.speedStats.timeWalking), selected.speedStats.distanceWalking / 1000.0));
			rowVehicle.setColor(colors[2]);
			rowVehicle.setTitle("On a vehicle");
			rowVehicle.setData(String.format("%s, %.1f km", DateTimeUtils.smartFormatTime(selected.speedStats.timeVehicle), selected.speedStats.distanceVehicle / 1000.0));
			double timeTotal = (selected.speedStats.timeStationary + selected.speedStats.timeWalking + selected.speedStats.timeVehicle) / 100.0;
			View pieChart = getPieChartView(new Double[] { selected.speedStats.timeStationary / timeTotal, selected.speedStats.timeWalking / timeTotal,
					selected.speedStats.timeVehicle / timeTotal });
			chartLayout.removeAllViewsInLayout();
			chartLayout.addView(pieChart, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			StatsRowView[] rowLocs = new StatsRowView[] { rowLoc1, rowLoc2, rowLoc3};
			for (int i = 0; i < selected.timeAtLocs.size() && i < 3; i++) {
				rowLocs[i].setTitle(selected.timeAtLocs.get(i).description);
				rowLocs[i].setData(DateTimeUtils.smartFormatTime(selected.timeAtLocs.get(i).duration));
			}
			for (int i = selected.timeAtLocs.size(); i < rowLocs.length; i++) {
				rowLocs[i].setVisibility(View.GONE);
			}
		}
		return itemView;
	}

	private View getPieChartView(Double[] values) {
		DefaultRenderer renderer = new DefaultRenderer();
		renderer.setMargins(new int[] { 0, 0, 0, 0 });
		renderer.setShowLegend(false);
		for (int color : colors) {
			SimpleSeriesRenderer r = new SimpleSeriesRenderer();
			r.setColor(color);
			renderer.addSeriesRenderer(r);
		}
		renderer.setZoomButtonsVisible(false);
		renderer.setZoomEnabled(false);
		renderer.setShowLabels(false);
		renderer.setPanEnabled(false);
		renderer.setChartTitleTextSize(25);
		CategorySeries series = new CategorySeries("");
		for (int i = 0; i < values.length; i++) {
			series.add("", values[i]);
		}
		CategorySeries buildCategoryDataset = series;
		return ChartFactory.getPieChartView(getContext(), buildCategoryDataset, renderer);
	}

	public void add(StatsHolder holder) {
		statsHolders.add(holder);
		notifyDataSetChanged();
	}

	public void replaceLast(StatsHolder holder) {
		statsHolders.set(statsHolders.size() - 1, holder);
		notifyDataSetChanged();
	}

	public void removeLast() {
		statsHolders.remove(statsHolders.size() - 1);
		notifyDataSetChanged();
	}

}
