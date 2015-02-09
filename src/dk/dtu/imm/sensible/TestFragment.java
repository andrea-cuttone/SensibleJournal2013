package dk.dtu.imm.sensible;

import hirondelle.date4j.DateTime;

import java.util.Set;
import java.util.TimeZone;

import dk.dtu.imm.sensible.components.CustomFragment;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;

public class TestFragment extends CustomFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		Log.d(Constants.APP_NAME, "\nstarting @ " + DateTime.now(TimeZone.getDefault()).format("hh:mm"));
	}

	@Override
	public void onTabSelected() {
	}

	@Override
	public void onTabUnselected() {
	}

	@Override
	public String getTabTitle() {
		return "test";
	}
}
