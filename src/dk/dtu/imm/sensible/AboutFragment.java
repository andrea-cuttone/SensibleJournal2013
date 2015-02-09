package dk.dtu.imm.sensible;

import com.googlecode.androidannotations.annotations.EFragment;

import dk.dtu.imm.sensible.components.CustomFragment;
import dk.dtu.imm.sensible.components.UILogger;
import dk.dtu.imm.sensiblejournal.R;

import android.os.Bundle;

@EFragment(R.layout.about)
public class AboutFragment extends CustomFragment {

	// workaround, see http://code.google.com/p/android/issues/detail?id=19917#c15
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outState.isEmpty()) {
			outState.putBoolean("bug:fix", true);
		}
	}

	@Override
	public void onTabSelected() {
		if(getActivity() != null) {
			UILogger.instance(getActivity().getApplicationContext()).logEvent("About");
		}
	}

	@Override
	public void onTabUnselected() {
		
	}
	
	@Override
	public String getTabTitle() {
		return "About";
	}

}
