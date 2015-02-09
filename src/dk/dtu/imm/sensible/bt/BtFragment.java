package dk.dtu.imm.sensible.bt;

import hirondelle.date4j.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.components.CustomFragment;
import dk.dtu.imm.sensible.components.UILogger;
import dk.dtu.imm.sensible.rest.BluetoothResponseEntity;
import dk.dtu.imm.sensible.rest.RestClientV2;
import dk.dtu.imm.sensible.utils.DateTimeUtils;
import dk.dtu.imm.sensiblejournal.R;

@EFragment(R.layout.bt_layout)
public class BtFragment extends CustomFragment {

	@ViewById(R.id.listView_bt) ListView listView;
	@ViewById(R.id.progress_bt) View progress;
	
	private AsyncTask<Void, List<BtBean>, Void> task;
	
	@AfterViews
	public void afterViews() {
		final BtAdapter btAdapter = new BtAdapter(getActivity());
		listView.setAdapter(btAdapter);
		if(task != null) {
			task.cancel(true);
		}
		task = new AsyncTask<Void, List<BtBean>, Void>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progress.setVisibility(View.VISIBLE);
			}
			
			@Override
			protected Void doInBackground(Void... params) {
				long t = DateTimeUtils.toTimestamp(DateTime.now(TimeZone.getDefault()));
				ArrayList<BtBean> beans = new ArrayList<BtBean>();
				while (isCancelled() == false && t > DateTimeUtils.toTimestamp(Constants.EARLIEST_DATE)) {
					try {
						List<BluetoothResponseEntity> entities = RestClientV2.instance(getActivity().getBaseContext()).
								getBluetooth(t, t + Constants.SECS_ONE_DAY);
						BtProcessor.getFreqs(t, entities, beans);
						if (beans.size() > 0) {
							publishProgress(beans);
						}
						t -= Constants.SECS_ONE_DAY;
					} catch (Exception e) {
						Log.e(Constants.APP_NAME, e.toString());
					}
				}
				return null;
			}
			
			protected void onProgressUpdate(List<BtBean>... values) {
				progress.setVisibility(View.GONE);
				btAdapter.clear();
				btAdapter.addAll(values[0]);
			}
			
			@Override
			protected void onCancelled(Void result) {
				super.onCancelled(result);
				progress.setVisibility(View.GONE);
			}
			
		};
		task.execute();
	}
	
	@Override
	public void onTabSelected() {
		if(getActivity() != null) {
			UILogger.instance(getActivity().getBaseContext()).logEvent("BtStats");
			afterViews();			
		}
	}

	@Override
	public void onTabUnselected() {
		if(task != null) {
			task.cancel(true);
		}
	}

	@Override
	public String getTabTitle() {
		return "BTContacts";
	}

}
