/*
 * Adapted from the work of Ievgenii Nazaruk
 */
package dk.dtu.imm.sensible.movement;

import android.app.Activity;
import dk.dtu.imm.sensible.map.ActivityHostFragment;

public class MovementViewerFragment extends ActivityHostFragment {
    
    @Override
    protected Class<? extends Activity> getActivityClass() {
        return MovementViewerActivity_.class;
    }

	@Override
	public void onTabSelected() {
		if(getHostedActivity() != null) {
			getHostedActivity().onTabSelected();
		}
	}
	
	@Override
	public void onTabUnselected() {
		if(getHostedActivity() != null) {
			getHostedActivity().onTabUnselected();
		}
	}

	@Override
	public MovementViewerActivity getHostedActivity() {
		return (MovementViewerActivity) super.getHostedActivity();
	}

	@Override
	public String getTabTitle() {
		return "Movements";
	}
}
