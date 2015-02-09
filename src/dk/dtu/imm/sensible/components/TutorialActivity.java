package dk.dtu.imm.sensible.components;

import dk.dtu.imm.sensible.Constants;
import dk.dtu.imm.sensiblejournal.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class TutorialActivity extends Activity {

	private static final String DRAWABLE_IDS = "DRAWABLE_ID";
	private int[] ids;
	private int currentImg;
	private ImageView img;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tutorial);
		img = (ImageView) findViewById(R.id.img_tutorial);
		ids = getIntent().getExtras().getIntArray(DRAWABLE_IDS);
		if(ids == null) {
			finish();
		}
		img.setImageResource(ids[0]);
		currentImg = 0;
		img.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				currentImg++;
				if(currentImg < ids.length) {
					img.setImageResource(ids[currentImg]);
				} else {
					finish();
				}
			}
		});
	}
	
	public static Intent getIntent(Context context, int[] drawableIds) {
		Intent intent = new Intent(context, TutorialActivity.class);
		Bundle b = new Bundle();
		b.putIntArray(TutorialActivity.DRAWABLE_IDS, drawableIds);
		intent.putExtras(b);
		return intent;
	}
	
	public static boolean wasShowFor(Activity activity, String name) {
		String key = "TUTORIAL_" + name.toUpperCase();
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
		boolean shown = sharedPrefs.contains(key);
		if (shown == false) {
			Editor editor = sharedPrefs.edit();
			editor.putBoolean(key, true);
			editor.commit();
		}
		return shown;
	}
}
