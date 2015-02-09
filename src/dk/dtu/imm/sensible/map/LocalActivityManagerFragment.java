/*
 * Adapted from the work of Ievgenii Nazaruk
 */
package dk.dtu.imm.sensible.map;

import dk.dtu.imm.sensible.components.CustomFragment;
import android.app.LocalActivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

public abstract class LocalActivityManagerFragment extends CustomFragment {

	private static final String TAG = LocalActivityManagerFragment.class.getSimpleName();
	private static final String KEY_STATE_BUNDLE = "localActivityManagerState";

	private LocalActivityManager mLocalActivityManager;

	protected LocalActivityManager getLocalActivityManager() {
		return mLocalActivityManager;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate(): " + getClass().getSimpleName());

		Bundle state = null;
		if (savedInstanceState != null) {
			state = savedInstanceState.getBundle(KEY_STATE_BUNDLE);
		}

		mLocalActivityManager = new LocalActivityManager(getActivity(), true);
		mLocalActivityManager.dispatchCreate(state);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (getActivity() != null) {
			if (mLocalActivityManager == null) {
				mLocalActivityManager = new LocalActivityManager(getActivity(), true);
			}
			outState.putBundle(KEY_STATE_BUNDLE, mLocalActivityManager.saveInstanceState());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (getActivity() != null) {
			if (mLocalActivityManager == null) {
				mLocalActivityManager = new LocalActivityManager(getActivity(), true);
			}
			mLocalActivityManager.dispatchResume();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (getActivity() != null) {
			if (mLocalActivityManager == null) {
				mLocalActivityManager = new LocalActivityManager(getActivity(), true);
			}
			mLocalActivityManager.dispatchPause(getActivity().isFinishing());
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (getActivity() != null) {
			if (mLocalActivityManager == null) {
				mLocalActivityManager = new LocalActivityManager(getActivity(), true);
			}
			mLocalActivityManager.dispatchStop();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (getActivity() != null) {
			if (mLocalActivityManager == null) {
				mLocalActivityManager = new LocalActivityManager(getActivity(), true);
			}
			mLocalActivityManager.dispatchDestroy(getActivity().isFinishing());
		}
	}
}
