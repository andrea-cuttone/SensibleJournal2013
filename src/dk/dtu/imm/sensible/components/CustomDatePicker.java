package dk.dtu.imm.sensible.components;

import java.util.Locale;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import dk.dtu.imm.sensiblejournal.R;

public class CustomDatePicker extends LinearLayout {
	private DateTime datetime;
	private DateChangeListener dateChangeListener;
	private TextView textviewDate;

	public CustomDatePicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(R.layout.datepicker_layout, this);

		datetime = DateTime.now(TimeZone.getDefault());
		dateChangeListener = new DateChangeListener() {
			public void onDateChanged() {
			}
		};
		final GestureDetector gestureDetector = new GestureDetector(context,
				new GestureDetector.SimpleOnGestureListener() {

					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
						float deltax = e1.getX() - e2.getX();
						if (deltax < -50) {
							gotoPrevDate();
						} else if (deltax > 50) {
							gotoNextDate();
						}
						return super.onFling(e1, e2, velocityX, velocityY);
					}
					
					@Override
					public boolean onDoubleTap(MotionEvent e) {
						CustomerDatePickerDlg dlg = new CustomerDatePickerDlg(datetime, new OnDateSetListener() {
							
							@Override
							public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
								DateTime newdate = DateTime.forDateOnly(year, monthOfYear, dayOfMonth);
								if(newdate.gt(DateTime.now(TimeZone.getDefault()))) {
									Toast.makeText(getContext(), "Date cannot be in the future", Toast.LENGTH_SHORT).show();
								} else {
									datetime = newdate;
									updateText();
									dateChangeListener.onDateChanged();
								}
							}
						});
					    dlg.show(((Activity)getContext()).getFragmentManager(), "datePicker");
						return super.onDoubleTap(e);
					}

				});

		textviewDate = (TextView) findViewById(R.id.text_datepicker_date);
		textviewDate.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				gestureDetector.onTouchEvent(event);
				return true;
			}
		});
		View btnPrev = findViewById(R.id.btn_datepicker_prev);
		btnPrev.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gotoPrevDate();
			}
		});
		View btnNext = findViewById(R.id.btn_datepicker_next);
		btnNext.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gotoNextDate();
			}
		});
		updateText();
	}

	private void gotoNextDate() {
		if(datetime.isSameDayAs(DateTime.now(TimeZone.getDefault())) == false) {
			datetime = datetime.plusDays(1);
			updateText();
			dateChangeListener.onDateChanged();
		}
	}

	private void gotoPrevDate() {
		datetime = datetime.minusDays(1);
		updateText();
		dateChangeListener.onDateChanged();
	}

	public DateTime getDateTime() {
		return datetime;
	}

	public void setDateChangeListener(DateChangeListener listener) {
		this.dateChangeListener = listener;
	}

	private void updateText() {
		textviewDate.setText("on " + datetime.format("WWW DD MMM", Locale.getDefault()));
	}

	public interface DateChangeListener {
		public void onDateChanged();
	}

}
