package org.globus.globustransfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MenuActivity extends Activity {

	protected String mUsername;
	protected String mSamlCookie;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		
		// The username and cookie are retrieved from the Intent
		Intent mIntent = getIntent();
		Bundle mInfo = mIntent.getExtras();
		mUsername = mInfo.getString("username");
		mSamlCookie = mInfo.getString("samlCookie");

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		// If the user selects logout the logout function is called
		case R.id.logout_menu_item:
			logout();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Creates a Dialog asking the user if they really want to log out. If user
	 * selects "YES", the activity is terminated, if they select "NO" the dialog
	 * disappears.
	 */
	private void logout() {
		new AlertDialog.Builder(this)
				.setCancelable(false)
				.setMessage(R.string.log_out_question)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								MenuActivity.this.finish();
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

							}
						}).show();

	}

	

	@Override
	public void onBackPressed() {
		
		//When the back button is pressed the user in notified that they can logout using the menu logout button
		Toast.makeText(this, getString(R.string.logout_instructions),
				Toast.LENGTH_SHORT).show();

	}

	/**
	 * It initiates an instance of the Monitor Activity, putting as extra
	 * information within a Bundle the users' credentials.
	 * 
	 * 
	 * @param view
	 *            The current view
	 */
	public void startMonitorActivity(View view) {

		//An Intent is created in order to call the Monitor Activity
		Intent mIntent = new Intent(this, MonitorActivity.class);
		
		//A Bundle is created in order to send information to the Monitor Activity
		Bundle bundle = new Bundle();
		bundle.putString("username", mUsername);
		bundle.putString("samlCookie", mSamlCookie);
		mIntent.putExtras(bundle);
		startActivity(mIntent);

	}

	
	/**
	 * It initiates an instance of the StartTransfer Activity, putting as extra
	 * information within a Bundle the users' credentials.
	 * 
	 * 
	 * @param view
	 *            The current view
	 */
	public void startTransferActivity(View view) {

		//An intent is created in order to call the StartTransfer Activity
		Intent mIntent = new Intent(this, StartTransfer.class);
		
		//A bundle is created in order to send information to the StartTransfer Activity
		Bundle mBundle = new Bundle();
		mBundle.putString("username", mUsername);
		mBundle.putString("samlCookie", mSamlCookie);
		mIntent.putExtras(mBundle);
		startActivity(mIntent);

	}
}
