package org.globus.globustransfer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String sGlobusOnlineSignUpURI = "http://www.globusonline.org/SignUp";
	private String mNetworkNotAvailable;
	private static final String sGlobusOnlineAuthenticate = "https://nexus.api.globusonline.org/authenticate";
	private TextView mUsernameTextView = null;
	private TextView mPasswordTextView = null;
	private Context mContext;
	private String mSamlCookie = null;
	private String mUsername = null;
	private ProgressBar mAttemptLogInProgressBar;
	private Button mLogInButton;
	private boolean mSwitch = true;
	private SharedPreferences mSharedPreferences;
	private CheckBox mRememberMeCheckBox;
	private TextView mRememberMeTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mNetworkNotAvailable = getString(R.string.offline_warning);
		mAttemptLogInProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		mUsernameTextView = (TextView) findViewById(R.id.username);
		mPasswordTextView = (TextView) findViewById(R.id.password);
		mSharedPreferences = getSharedPreferences(
				getString(R.string.preferences_name), MODE_PRIVATE);
		mRememberMeCheckBox = (CheckBox) findViewById(R.id.remember_me_checkbox);
		mRememberMeTextView = (TextView) findViewById(R.id.remember_me_text_view);
		mContext = this;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Do nothing when the menu button is pressed
		return true;
	}

	/**
	 * Checks if there is an Internet connection available. If there is, it
	 * redirects the user to Globus Online's website to sign up for a new user
	 * account.
	 * 
	 * @param view
	 *            The current view
	 */
	public void goToGlobusOnlineSignUp(View view) {
		if (isInternetConnectionAvailable()) {
			Uri mUri = Uri.parse(sGlobusOnlineSignUpURI);
			Intent mIntent = new Intent(Intent.ACTION_VIEW, mUri);
			startActivity(mIntent);
		} else {
			makeToast(mNetworkNotAvailable);
		}
	}

	/**
	 * Tries to Sign the user In to Globus Online Transfer Service. If there is
	 * no Internet Connection, the username field is empty or the password field
	 * is empty, the user is informed with Toast warning messages. If everything
	 * is ok it calls the execute function of the signInAttempt class.
	 * 
	 * @param view
	 */
	public void signIn(View view) {

		String username = mUsernameTextView.getText().toString();
		String password = mPasswordTextView.getText().toString();
		if (!isInternetConnectionAvailable()) {
			makeToast(mNetworkNotAvailable);
			return;
		}
		if (username.length() == 0 && password.length() == 0) {
			makeToast(getString(R.string.no_username_and_password_warning));
			return;
		}
		if (username.length() == 0) {
			makeToast(getString(R.string.no_username_warning));
			return;
		}
		if (password.length() == 0) {
			makeToast(getString(R.string.no_password_warning));
			return;
		}
		new signInAttempt().execute(username, password);

	}

	/**
	 * It creates a short-lived message on the screen.
	 * 
	 * @param text
	 *            The contents of the message
	 */
	public void makeToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Checks if there is Internet Connection available.
	 * 
	 * @return The Internet Connection availability
	 */
	private boolean isInternetConnectionAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	

	/**
	 * Sets in the Edit Text fields the saved username and password (if any) of
	 * the user.
	 * 
	 * @param view
	 *            The current view
	 */
	public void setSavedCredentials(View view) {
		if (mSwitch) {
			mUsernameTextView.setText(mSharedPreferences.getString("username",
					""));
			mPasswordTextView.setText(mSharedPreferences.getString("password",
					""));
			mSwitch = false;
		} else {
			mUsernameTextView.setText("");
			mPasswordTextView.setText("");
			mSwitch = true;
		}
	}

	/**
	 * This class attempts to sign in to Globus Online by POSTing to the Globus
	 * Online Authenticate URL the username and password provided by the user
	 * through the Edit Text fields. If the user's credentials are valid,
	 * signing in succeeds and a cookie is returned from Globus Online
	 * authentication service, which along with the username will allow us to
	 * contact the API and, if the "Remember me" checkbox is selected, the username
	 * and password are stored on the devices memory. If the signing in process fails,
	 * the user is informed with a Toast message.
	 * (This class is partially based on Peter Hadlaw's corresponding GOUserLogInAttempt class)
	 * 
	 * @author christos
	 */
	private class signInAttempt extends AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			//The login-related fields and buttons temporarily disappear from the screen
			mUsernameTextView.setVisibility(View.INVISIBLE);
			mPasswordTextView.setVisibility(View.INVISIBLE);
			mLogInButton = (Button) findViewById(R.id.log_in_button);
			mLogInButton.setVisibility(View.INVISIBLE);
			mAttemptLogInProgressBar.setVisibility(View.VISIBLE);
			mRememberMeCheckBox.setVisibility(View.INVISIBLE);
			mRememberMeTextView.setVisibility(View.INVISIBLE);

		}

		@Override
		protected String doInBackground(String... credentials) {

			HttpsURLConnection mConnection = null;
			JSONObject mSendJson = new JSONObject();
			String mReceiveString = "";
			
			//Username and Password are retrieved from the arguments String Array
			String mUsernameTemp = credentials[0];
			String mPasswordTemp = credentials[1];
			String mResult = getString(R.string.error);

			try {

				URL mUrl = new URL(sGlobusOnlineAuthenticate);
				mConnection = (HttpsURLConnection) mUrl.openConnection();

				mSendJson.put("username", mUsernameTemp);
				mSendJson.put("password", mPasswordTemp);
				
				//The communication with the API is configured
				mConnection.setRequestMethod("POST");
				mConnection.setRequestProperty("Content-Type",
						"application/json");
				mConnection.setRequestProperty("Content-Length", ""
						+ mSendJson.toString().getBytes().length);
				mConnection.setRequestProperty("Content-Language", "en-US");
				mConnection.setDoOutput(true);
				mConnection.setDoInput(true);

				DataOutputStream mOutputStreamReader = new DataOutputStream(
						mConnection.getOutputStream());

				mOutputStreamReader.writeBytes(mSendJson.toString());
				mOutputStreamReader.flush();
				mOutputStreamReader.close();

				InputStreamReader mInputStreamReader = null;
				BufferedReader mBufferedReader = null;
				String mLine = "";

				try {
					mInputStreamReader = new InputStreamReader(
							mConnection.getInputStream());
					mBufferedReader = new BufferedReader(mInputStreamReader);

					while ((mLine = mBufferedReader.readLine()) != null) {
						mReceiveString = mReceiveString.concat(mLine);
					}
					mBufferedReader.close();

				} catch (Exception e) {
					return mContext.getResources().getString(
							R.string.login_fail_warning);
				}

				//The header files returned are retrieved
				Map<String, List<String>> mAllHeaderFields = mConnection
						.getHeaderFields();
				List<String> mAllCookieFields = mAllHeaderFields
						.get("Set-Cookie");

				//The saml cookie that is necessary for the interaction with the API is retrieved
				for (int i = 0; i < mAllCookieFields.size(); i++) {
					List<HttpCookie> mCookies = HttpCookie
							.parse(mAllCookieFields.get(i));

					for (int j = 0; j < mCookies.size(); j++) {
						HttpCookie cookie = mCookies.get(j);
						String cookieKey = cookie.getName();
						String cookieValue = cookie.getValue();
						if (cookieKey.equals("saml")) {
							mSamlCookie = cookieValue;
						}
					}
				}
				
				//If the responce code is 200 (success) and the saml cookie has been found a login successfull string is returned
				if (mConnection.getResponseCode() == 200 && mSamlCookie != null) {
					mUsername = mUsernameTemp;
					return getString(R.string.login_successfull);
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (ProtocolException e) {
				e.printStackTrace();
				mResult = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				mResult = e.getMessage();
			} catch (JSONException e) {
				e.printStackTrace();
				mResult = e.getMessage();
			}

			return mResult;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			if (result.equals(getString(R.string.login_successfull))) {
				
				//A new intent is created  in order to call the Menu Activity 
				Intent intent = new Intent(mContext, MenuActivity.class);
				
				//A bundle is created in order to send information to the Menu Activity
				Bundle bundle = new Bundle();
				bundle.putString("result", result);
				bundle.putString("username", mUsername);
				bundle.putString("samlCookie", mSamlCookie);
				intent.putExtras(bundle);
				if (mRememberMeCheckBox.isChecked()) {
					SharedPreferences.Editor editor = mSharedPreferences.edit();
					editor.putString("username", mUsernameTextView.getText()
							.toString());
					editor.putString("password", mPasswordTextView.getText()
							.toString());
					editor.commit();
				}
				startActivity(intent);

			} else {

				makeToast(result);
			}
			
			//The login-related fields and button become visible again
			mUsernameTextView.setVisibility(View.VISIBLE);
			mPasswordTextView.setVisibility(View.VISIBLE);
			mLogInButton.setVisibility(View.VISIBLE);
			mRememberMeCheckBox.setVisibility(View.VISIBLE);
			mRememberMeTextView.setVisibility(View.VISIBLE);
			mAttemptLogInProgressBar.setVisibility(View.GONE);

		}

	}

}
