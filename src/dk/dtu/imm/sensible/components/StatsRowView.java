package dk.dtu.imm.sensible.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import dk.dtu.imm.sensiblejournal.R;

public class StatsRowView extends LinearLayout {

	private TextView tvTitle;
	private TextView tvData;
	private View viewColor;

	public StatsRowView(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(R.layout.stats_row_layout, this);
		
		viewColor = findViewById(R.id.rect_stats_row_color);
		tvTitle = (TextView) findViewById(R.id.textview_stats_row_title);
		tvData = (TextView) findViewById(R.id.textview_stats_row_data);
	}
	
	public void setColor(int color) {
		viewColor.setBackgroundColor(color);
	}
	
	public void setTitle(String title) {
		tvTitle.setText(title);
	}
	
	public void setData(String data) {
		tvData.setText(data);
	}

}
