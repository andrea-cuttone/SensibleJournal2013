package dk.dtu.imm.sensible.components;

import hirondelle.date4j.DateTime;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

public class CustomerDatePickerDlg extends DialogFragment implements DatePickerDialog.OnDateSetListener {

	private DateTime datetime;
	private OnDateSetListener listener;

	public CustomerDatePickerDlg(DateTime datetime, DatePickerDialog.OnDateSetListener listener) {
		this.datetime = datetime;
		this.listener = listener;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new DatePickerDialog(getActivity(), this, datetime.getYear(), datetime.getMonth() - 1, datetime.getDay());
	}

	@Override
	public void onDateSet(DatePicker picker, int year, int month, int day) {
		listener.onDateSet(picker, year, month + 1, day);
	}
	
	public DateTime getDatetime() {
		return datetime;
	}
}