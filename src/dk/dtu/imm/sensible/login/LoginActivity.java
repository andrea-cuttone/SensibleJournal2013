package dk.dtu.imm.sensible.login;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;

import dk.dtu.imm.sensible.MainTabsActivity;
import dk.dtu.imm.sensiblejournal.R;
import dk.dtu.imm.sensible.rest.RestClientV2;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

@EActivity(R.layout.login)
public class LoginActivity extends Activity {

	private static final String PREF_USERNAME_KEY = "USERNAME";
	
	@ViewById(R.id.etUsername)	protected EditText etUsername;
	@ViewById(R.id.etPassword)	protected EditText etPassword;
	@ViewById(R.id.btnLogin)	protected Button button;
	
	private ProgressDialog progressDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	@AfterViews
	public void afterViews() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		etUsername.setText(sharedPrefs.getString(PREF_USERNAME_KEY, ""));
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				progressDialog = ProgressDialog.show(LoginActivity.this, "", "Logging in...");
				String username = etUsername.getText().toString().toLowerCase().trim();
				String password = etPassword.getText().toString().trim();
				doLogin(username, password);
			}
		});
	}
	
	@Background
	protected void doLogin(String username, String password) {
		boolean success = RestClientV2.instance(getApplicationContext()).doLogin(username, password);
		showLoginResult(success, username);
	}

	@UiThread
	public void showLoginResult(boolean success, String username) {
		if(success) {
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			Editor editor = sharedPrefs.edit();
			editor.putString(PREF_USERNAME_KEY, username);
			editor.commit();
		}
		progressDialog.dismiss();
		if(success) {
			launchMainActivity();
		} else {
			etPassword.setText("");
			AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create();
			alertDialog.setMessage("Login failed");
			alertDialog.setCancelable(false);
			alertDialog.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
				
			});
			alertDialog.show();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		checkToken();
	}

	@Background
	public void checkToken() {
		if(RestClientV2.instance(getApplicationContext()).hasValidToken()) {
			launchMainActivity();
		}
	}
	
	@UiThread
	protected void launchMainActivity() {
		Intent intent = new Intent(getBaseContext(), MainTabsActivity.class);
		startActivity(intent);
		finish();
	}
	
	public static void showSessionExpiredDlg(final Activity activity) {
		AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
		alertDialog.setMessage("Your session has expired. Please log in again");
		alertDialog.setCancelable(false);
		alertDialog.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				Intent intent = new Intent(activity, LoginActivity_.class);
				activity.startActivity(intent);
				activity.finish();
			}
			
		});
		alertDialog.show();
	}
	
}