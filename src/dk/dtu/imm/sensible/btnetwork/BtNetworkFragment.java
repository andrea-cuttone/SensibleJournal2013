package dk.dtu.imm.sensible.btnetwork;

import hirondelle.date4j.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensible.bt.BtProcessor;
import dk.dtu.imm.sensible.btnetwork.louvain.Graph;
import dk.dtu.imm.sensible.btnetwork.louvain.Graph.Edge;
import dk.dtu.imm.sensible.btnetwork.louvain.Louvain;
import dk.dtu.imm.sensible.components.CustomFragment;
import dk.dtu.imm.sensible.components.TutorialActivity;
import dk.dtu.imm.sensible.components.UILogger;
import dk.dtu.imm.sensible.rest.BluetoothResponseEntity;
import dk.dtu.imm.sensible.rest.PublicProfileResponseEntity;
import dk.dtu.imm.sensible.rest.RestClientV2;
import dk.dtu.imm.sensible.utils.DateTimeUtils;
import dk.dtu.imm.sensiblejournal.R;

@EFragment(R.layout.btnetwork_layout)
public class BtNetworkFragment extends CustomFragment {

	@ViewById(R.id.surface_btnetwork) BtNetworkView surface;
	@ViewById(R.id.seekbar_btnetwork) SeekBar seekBar;
	@ViewById(R.id.btn_btnetwork_playstop) ImageView btnPlayStop;
	@ViewById(R.id.progress_btnetwork) View viewProgress;

	private static final long DELTAT = Constants.SECS_ONE_DAY;

	private List<List<BtNetworkBean>> beans;
	private List<Long> timestamps;
	private boolean isPlaying;
	private Map<Integer, String> hashToUids = new HashMap<Integer, String>();
	private long startTimestamp;
	private long endTimestamp;
	private AsyncTask<Void, Object, Void> loadBtTask;

	private void setPlaying(boolean status) {
		isPlaying = status;
		btnPlayStop.setImageResource(isPlaying ? R.drawable.pause : R.drawable.play);
		if (isPlaying) {
			if (seekBar.getProgress() == seekBar.getMax()) {
				seekBar.setProgress(0);
				surface.clearBubbles();
			}
			seekBar.postDelayed(new Runnable() {

				@Override
				public void run() {
					if (isPlaying && seekBar.getProgress() < seekBar.getMax()) {
						seekBar.setProgress(seekBar.getProgress() + 1);
						seekBar.postDelayed(this, 500);
					} else {
						setPlaying(false);
					}
				}
			}, 10);
		}
	}

	@AfterViews
	public void afterViews() {
		if(TutorialActivity.wasShowFor(getActivity(), getTabTitle()) == false) {
			Intent intent = TutorialActivity.getIntent(getActivity().getBaseContext(), 
					new int[] { R.drawable.tut_bt1, R.drawable.tut_bt2 }); 
			startActivity(intent);
		}
		startTimestamp = DateTimeUtils.toTimestamp(Constants.EARLIEST_DATE);
		endTimestamp = DateTimeUtils.toTimestamp(DateTime.now(TimeZone.getDefault()));
		beans = new ArrayList<List<BtNetworkBean>>();
		timestamps = new ArrayList<Long>();
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					setPlaying(false);
				}
				if (progress < beans.size()) {
					surface.setBtBeans(timestamps.get(progress), beans.get(progress));
				} else {
					seekBar.setProgress(beans.size() - 1);
				}
			}
		});
		btnPlayStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setPlaying(!isPlaying);
			}

		});
		if(loadBtTask != null) {
			loadBtTask.cancel(true);
		}
		loadBtTask = new AsyncTask<Void, Object, Void>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				seekBar.setEnabled(false);
				seekBar.setMax((int) ((endTimestamp - startTimestamp) / Constants.SECS_ONE_DAY));
				viewProgress.setVisibility(View.VISIBLE);
			}

			@Override
			protected Void doInBackground(Void... params) {
				long t = startTimestamp;
				Graph graph = new Graph();
				Louvain louvain = new Louvain();
				SparseIntArray userFreq = new SparseIntArray();
				while (isCancelled() == false && t < endTimestamp) {
					try {
						List<BluetoothResponseEntity> entities = RestClientV2.instance(getActivity().getBaseContext()).getBluetooth(t, t + DELTAT);
						Map<Long, Set<String>> buckets = BtProcessor.getBuckets(entities);
						for (Set<String> b : buckets.values()) {
							ArrayList<String> list = new ArrayList<String>(b);
							for (int i = 0; i < list.size(); i++) {
								int uid1 = list.get(i).hashCode();
								hashToUids.put(uid1, list.get(i));
								graph.addNode(uid1);
								int w = 1000 / b.size();
								for (int j = 0; j < list.size(); j++)
									if (i != j) {
										int uid2 = list.get(j).hashCode();
										Edge edge = graph.getEdge(uid1, uid2);
										if (edge != null) {
											edge.weight += w;
										} else {
											graph.addEdge(uid1, uid2, w);
										}
									}
								if (userFreq.indexOfKey(uid1) < 0) {
									userFreq.put(uid1, w);
								} else {
									userFreq.put(uid1, userFreq.get(uid1) + w);
								}
							}
						}
						Log.d(Constants.APP_NAME, "graph");
						ArrayList<BtNetworkBean> beans = new ArrayList<BtNetworkBean>();
						SparseIntArray partition = louvain.best_partition(graph);
						Log.d(Constants.APP_NAME, "partition");
						for (int i = 0; i < partition.size(); i++) {
							int key = partition.keyAt(i);
							BtNetworkBean btBean = new BtNetworkBean();
							btBean.uid = hashToUids.get(key);
							btBean.freq = userFreq.get(key);
							btBean.community = partition.get(key);
							beans.add(btBean);
						} 
						louvain.getPool().returnObject(partition);
						publishProgress(t, beans);
						t += DELTAT;
					} catch (Exception e) {
						Log.e(Constants.APP_NAME, e.toString());
					}
				}
				return null;
			}

			protected void onProgressUpdate(Object... values) {
				beans.add((List<BtNetworkBean>) values[1]);
				timestamps.add((Long) values[0]);
				seekBar.setSecondaryProgress(beans.size());
				if(beans.size() == 1) {
					seekBar.setEnabled(true);
					surface.setBtBeans(timestamps.get(0), beans.get(0));
				}
			}
			
			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				AsyncTask<Void, Void, Void> loadProfilesTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						for(List<BtNetworkBean> list : beans) {
							for(BtNetworkBean b : list) {
								try {
									PublicProfileResponseEntity publicProfile = RestClientV2.instance(getActivity().getApplicationContext())
											.fetchPublicProfile(b.uid);
								} catch (Exception e) {
									Log.e(Constants.APP_NAME, e.toString());
								}
							}
						}
						return null;
					}
					
					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						surface.update();
						viewProgress.setVisibility(View.INVISIBLE);
					}
					
				};
				loadProfilesTask.execute();
			}

		};
		loadBtTask.execute();
	}
	
	@Override
	public void onTabSelected() {
		if(getActivity() != null) {
			UILogger.instance(getActivity().getApplicationContext()).logEvent("BtNetwork");
			afterViews();
		}
	}

	@Override
	public void onTabUnselected() {
		setPlaying(false);
		if(loadBtTask != null) {
			loadBtTask.cancel(true);
		}
	}

	@Override
	public String getTabTitle() {
		return "Social Network";
	}

}
