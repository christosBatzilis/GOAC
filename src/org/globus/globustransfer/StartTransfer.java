package org.globus.globustransfer;





import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.globusonline.transfer.APIError;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient.Result;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class StartTransfer extends Activity {

	protected String mUsername;
	protected String mSamlCookie;
	private static JSONTransferAPIClient sClient;
	private Context mContext;
	private String mEndpointACurrentPath = "/";
	private String mEndpointBCurrentPath = "/";
	private ProgressBar mEndpointLoadProgressBar;
	private ProgressBar mEndpointAProgressBar, mEndpointBProgressBar;
	private Dialog mSettingsDialog, mCreateFolderDialog, mDeleteFilesDialog,
			mMakeTransferDialog;
	private ToggleButton mEndpointsVisibleToggleButton, mHiddenToggleButton;
	private Button mCreateFolderOkButton, mDeleteFilesOkButton,
			mMakeTransferOkButton, mSettingOkButton;
	private Button mSelectEndpointAButton, mSelectEndpointBButton;
	private Button mDeleteAButton, mDeleteBButton;
	private Button mUpAButton, mUpBButton;
	private Button mTransferAButton, mTransferBButton;
	private Button mCreateAButton, mCreateBButton;
	private Button mRefreshAButton, mRefreshBButton;
	private ArrayList<String> mEndpoints;
	private ArrayList<String> mFileListA, mFileListB;
	private SharedPreferences mSharedPreferences;
	private ListView mAListView, mBListView;
	private TextView mATextView, mBTextView;
	private TextView mPathATextView, mPathBTextView;
	private List<String> mSelectedFilesA, mSelectedFilesB;
	private EditText mCreateFolderEditText, mDeleteFilesEditText,
			mMakeTransferEditText;
	private String mFolderName, mTransferName, mDeletionName, mNode;
	private Map<String, Long> mLastTimePressedA, mLastTimePressedB;
	private Map<String, Float> mFilesSizeA, mFilesSizeB;
	private static long mInterval = 900;
	private Boolean mEndpointsTemp, mHiddenTemp;
	private JSONObject mActivationRequirements;
	private Dialog mActivateDialog;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_transfer);

		// The username and cookie are retrieved from the Intent
		Intent mIntent = getIntent();
		Bundle mInfo = mIntent.getExtras();

		mActivationRequirements = null;

		mLastTimePressedA = new HashMap<String, Long>();
		mLastTimePressedB = new HashMap<String, Long>();

		mFilesSizeA = new HashMap<String, Float>();
		mFilesSizeB = new HashMap<String, Float>();

		mSelectedFilesA = new ArrayList<String>();
		mSelectedFilesB = new ArrayList<String>();

		mFileListA = new ArrayList<String>();
		mFileListB = new ArrayList<String>();

		mUsername = mInfo.getString("username");
		mSamlCookie = mInfo.getString("samlCookie");

		mContext = this;
		mEndpointLoadProgressBar = (ProgressBar) findViewById(R.id.endpoints_progress_bar);
		mEndpointLoadProgressBar.setMax(4);

		mAListView = (ListView) findViewById(R.id.endpoint_A_list_view);
		mBListView = (ListView) findViewById(R.id.endpoint_B_list_view);

		mRefreshAButton = (Button) findViewById(R.id.refresh_A_button);
		mRefreshBButton = (Button) findViewById(R.id.refresh_B_button);

		mTransferAButton = (Button) findViewById(R.id.transfer_A_to_B_button);
		mTransferBButton = (Button) findViewById(R.id.transfer_B_to_A_button);

		mSelectEndpointAButton = (Button) findViewById(R.id.select_endpoint_A_button);
		mSelectEndpointBButton = (Button) findViewById(R.id.select_endpoint_B_button);

		mUpAButton = (Button) findViewById(R.id.back_A_button);
		mUpBButton = (Button) findViewById(R.id.back_B_button);

		mATextView = (TextView) findViewById(R.id.textView1);
		mBTextView = (TextView) findViewById(R.id.textView2);

		mPathATextView = (TextView) findViewById(R.id.path_A);
		mPathBTextView = (TextView) findViewById(R.id.path_B);

		mEndpointAProgressBar = (ProgressBar) findViewById(R.id.endpoints_A_directory_progress);
		mEndpointBProgressBar = (ProgressBar) findViewById(R.id.endpoints_B_directory_progress);

		mDeleteAButton = (Button) findViewById(R.id.delete_A_button);
		mDeleteBButton = (Button) findViewById(R.id.delete_B_button);

		// The Settings Dialog is initialized
		mSettingsDialog = new Dialog(this);
		mSettingsDialog.setCanceledOnTouchOutside(false);
		mSettingsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mSettingsDialog.setContentView(R.layout.settings_dialog);

		mAListView.setOnItemClickListener(mMessageClickedHandlerA);
		mBListView.setOnItemClickListener(mMessageClickedHandlerB);

		mSettingOkButton = (Button) mSettingsDialog
				.findViewById(R.id.ok_button);
		mCreateAButton = (Button) findViewById(R.id.create_folder_A_button);
		mCreateBButton = (Button) findViewById(R.id.create_folder_B_button);
		mSharedPreferences = getSharedPreferences(
				getString(R.string.preferences_name), MODE_PRIVATE);
		mEndpointsVisibleToggleButton = (ToggleButton) mSettingsDialog
				.findViewById(R.id.toggleButton1);

		mHiddenToggleButton = (ToggleButton) mSettingsDialog
				.findViewById(R.id.toggleButton2);

		mEndpointsVisibleToggleButton.setChecked(mSharedPreferences.getBoolean(
				"EndpointsActiveOnly", true));
		mHiddenToggleButton.setChecked(mSharedPreferences.getBoolean(
				"ShowHiddenFiles", false));

		mEndpointsVisibleToggleButton
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean isChecked) {

						SharedPreferences.Editor mEditor = mSharedPreferences
								.edit();
						if (isChecked) {

							mEditor.putBoolean("EndpointsActiveOnly", true);

						} else {

							mEditor.putBoolean("EndpointsActiveOnly", false);

						}
						mEditor.commit();
					}

				});

		mHiddenToggleButton
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean isChecked) {

						SharedPreferences.Editor mEditor = mSharedPreferences
								.edit();
						if (isChecked) {

							mEditor.putBoolean("ShowHiddenFiles", true);

						} else {

							mEditor.putBoolean("ShowHiddenFiles", false);

						}
						mEditor.commit();
					}

				});

		mSettingOkButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mSettingsDialog.dismiss();
				if (mSharedPreferences.getBoolean("EndpointsActiveOnly", true) != mEndpointsTemp) {
					new GetEndpointsList().execute();

				} else if (mSharedPreferences.getBoolean("ShowHiddenFiles",
						false) != mHiddenTemp) {
					if (mSelectEndpointAButton.getText().toString() != getString(R.string.select_endpoint_prompt)) {
						new GetEndpointsDirectory("a", mSelectEndpointAButton
								.getText().toString()).execute();
					}
					if (mSelectEndpointBButton.getText().toString() != getString(R.string.select_endpoint_prompt)) {
						new GetEndpointsDirectory("b", mSelectEndpointBButton
								.getText().toString()).execute();
					}

				}

			}

		});

		try {
			
			// Creates a client to communicate with the GO API
			sClient = new JSONTransferAPIClient(mUsername, null, mSamlCookie);
		} catch (Exception e) {
			e.printStackTrace();

		}

		// When the activity is restarted (e.g. the screen's orientation
		// changes) all the relative information is reloaded
		if (savedInstanceState != null) {

			mEndpoints = savedInstanceState
					.getStringArrayList("endpoints_list");
			mPathATextView.setText(savedInstanceState
					.getString("path_a_text_view"));
			mPathBTextView.setText(savedInstanceState
					.getString("path_b_text_view"));
			mEndpointACurrentPath = savedInstanceState.getString("path_a");
			mEndpointBCurrentPath = savedInstanceState.getString("path_b");
			mSelectEndpointAButton.setText(savedInstanceState
					.getString("endpoint_a"));
			mSelectEndpointBButton.setText(savedInstanceState
					.getString("endpoint_b"));
			mSelectedFilesA = savedInstanceState
					.getStringArrayList("selected_files_a");
			mSelectedFilesB = savedInstanceState
					.getStringArrayList("selected_files_b");

			mFileListA = savedInstanceState.getStringArrayList("dir_a");

			mFilesSizeA = (Map<String, Float>) savedInstanceState
					.getSerializable("filesizeA");

			if (!mFileListA.isEmpty()) {
				ArrayAdapter mAdapterA = new CustomAdapter(mContext,
						android.R.layout.simple_list_item_1, mFileListA);

				mAListView.setAdapter(mAdapterA);
			}

			mFileListB = savedInstanceState.getStringArrayList("dir_b");

			mFilesSizeB = (Map<String, Float>) savedInstanceState
					.getSerializable("filesizeB");

			if (!mFileListB.isEmpty()) {
				ArrayAdapter mAdapterB = new CustomAdapter(mContext,
						android.R.layout.simple_list_item_1, mFileListB);

				mBListView.setAdapter(mAdapterB);
			}
			if (!mSelectedFilesB.isEmpty()
					&& mSelectEndpointAButton.getText().toString() != getString(R.string.select_endpoint_prompt)) {
				mTransferBButton
						.setBackgroundResource(R.drawable.transfer_up_active);
			}
			if (!mSelectedFilesA.isEmpty()
					&& mSelectEndpointBButton.getText().toString() != getString(R.string.select_endpoint_prompt)) {
				mTransferAButton
						.setBackgroundResource(R.drawable.transfer_down_active);
			}
		} else {
			new GetEndpointsList().execute();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start_transfer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh_menu_item:

			// If the Refresh Button is pressed the endpoint's List is reloaded
			new GetEndpointsList().execute();
			return true;
		case R.id.transfer_settings_menu_item:

			// If the Settings Button is pressed the Settings Menu is loaded
			mEndpointsTemp = mSharedPreferences.getBoolean(
					"EndpointsActiveOnly", true);
			mHiddenTemp = mSharedPreferences.getBoolean("ShowHiddenFiles",
					false);
			mSettingsDialog.show();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * It creates a short-lived message on the screen.
	 * 
	 * @param text
	 *            The contents of the message
	 * @param duration
	 *            The duration of the appearance of the message on the screen
	 */
	public void makeToast(String text, int duration) {
		Toast.makeText(this, text, duration).show();
	}

	/***
	 * Function which is called when an endpoint is selected from the list. The
	 * path is set to "/" , the endpoint's name is set on the corresponding text
	 * field and if some files on the other endpoint are selected, the
	 * corresponding transfer button is activated.
	 * */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (data.getExtras().containsKey("endpoint")
				&& data.getExtras().containsKey("endId")) {
			if (data.getStringExtra("endId").contentEquals("a")) {
				if (!mSelectedFilesB.isEmpty()) {
					mTransferBButton
							.setBackgroundResource(R.drawable.transfer_up_active);
				}
				mSelectEndpointAButton.setText(data.getStringExtra("endpoint"));
				mEndpointACurrentPath = "/";
				mPathATextView.setText(getString(R.string.path_placeholder));
				new GetEndpointsDirectory("a", data.getStringExtra("endpoint"))
						.execute();

			} else if (data.getStringExtra("endId").contentEquals("b")) {
				if (!mSelectedFilesA.isEmpty()) {
					mTransferAButton
							.setBackgroundResource(R.drawable.transfer_down_active);
				}
				mSelectEndpointBButton.setText(data.getStringExtra("endpoint"));
				mEndpointBCurrentPath = "/";
				mPathBTextView.setText(getString(R.string.path_placeholder));
				new GetEndpointsDirectory("b", data.getStringExtra("endpoint"))
						.execute();
			}

		}
	}

	/**
	 * On click listener for List View A. If one file or folder is pressed its
	 * background color changes to blue if it was white before and it is added
	 * to the Selected Files list. If it was already on the list its background
	 * color turns white and it is deleted from the list. If a folder is pressed
	 * twice in less than 0.9 seconds the path is set to that folder.
	 */
	private OnItemClickListener mMessageClickedHandlerA = new OnItemClickListener() {
		public void onItemClick(AdapterView parent, View v, int position,
				long id) {

			long mPressTime = System.currentTimeMillis();
			long mTimeDifference;
			TextView mFileNameTextView = (TextView) v
					.findViewById(R.id.file_name_in_list_text_view);

			String mFilename = mFileNameTextView.getText().toString();

			if (mLastTimePressedA.containsKey(mFilename)) {
				mTimeDifference = mPressTime - mLastTimePressedA.get(mFilename);
			} else {
				mTimeDifference = mInterval + 1;
			}
			if (mTimeDifference <= mInterval && mFilename.endsWith("/")) {

				mEndpointACurrentPath = mEndpointACurrentPath.concat(mFilename);
				new GetEndpointsDirectory("a", mSelectEndpointAButton.getText()
						.toString()).execute();

			} else {

				if (!mSelectedFilesA.contains(mFilename)) {
					v.setBackgroundColor(getResources().getColor(
							R.color.android_light_blue));
					mSelectedFilesA.add(mFilename);
					if (mSelectEndpointBButton.getText().toString() != getString(R.string.select_endpoint_prompt))
						mTransferAButton
								.setBackgroundResource((R.drawable.transfer_down_active));
				} else {
					v.setBackgroundColor(Color.WHITE);
					mSelectedFilesA.remove(mFilename);
					if (mSelectedFilesA.isEmpty()) {
						mTransferAButton.setBackgroundResource(R.drawable.down);
					}

				}
			}
			mLastTimePressedA.put(mFilename, mPressTime);
		}

	};

	/**
	 * On click listener for List View Β. If one file or folder is pressed its
	 * background color changes to blue if it was white before and it is added
	 * to the Selected Files list. If it was already on the list its background
	 * color turns white and it is deleted from the list. If a folder is pressed
	 * twice in less than 0.9 seconds the path is set to that folder.
	 */
	private OnItemClickListener mMessageClickedHandlerB = new OnItemClickListener() {
		public void onItemClick(AdapterView parent, View v, int position,
				long id) {
			long mPressTime = System.currentTimeMillis();
			long mTimeDifference;
			TextView mFilenameTextView = (TextView) v
					.findViewById(R.id.file_name_in_list_text_view);

			String mFilename = mFilenameTextView.getText().toString();

			if (mLastTimePressedB.containsKey(mFilename)) {
				mTimeDifference = mPressTime - mLastTimePressedB.get(mFilename);
			} else {
				mTimeDifference = mInterval + 1;
			}
			if (mTimeDifference <= mInterval && mFilename.endsWith("/")) {

				mEndpointBCurrentPath = mEndpointBCurrentPath.concat(mFilename);
				new GetEndpointsDirectory("b", mSelectEndpointBButton.getText()
						.toString()).execute();

			} else {
				if (!mSelectedFilesB.contains(mFilename)) {
					v.setBackgroundColor(getResources().getColor(
							R.color.android_light_blue));

					mSelectedFilesB.add(mFilename);
					if (mSelectEndpointAButton.getText().toString() != getString(R.string.select_endpoint_prompt))
						mTransferBButton
								.setBackgroundResource((R.drawable.transfer_up_active));
				} else {
					v.setBackgroundColor(Color.WHITE);
					mSelectedFilesB.remove(mFilename);
					if (mSelectedFilesB.isEmpty()) {
						mTransferBButton.setBackgroundResource(R.drawable.up);
					}
				}
			}
			mLastTimePressedB.put(mFilename, mPressTime);

		}
	};

	/**
	 * Goes up one folder level. If the current path is the root path it notifies
	 * the user with a Toast that they can not go further up.
	 * 
	 * @param view
	 *            The ListView (representing an endpoint's directory) we want to
	 *            act on
	 */
	public void goBack(View view)

	{
		int mIdButton = ((Button) view).getId();
		String mNoEndpoint = getString(R.string.select_endpoint_prompt);

		if (mIdButton == R.id.back_A_button) {

			// If no endpoint is selected the user is notified with a warning Toast
			// message
			if (mSelectEndpointAButton.getText() == mNoEndpoint) {
				makeToast(getString(R.string.no_endpoint_selected_warning),
						Toast.LENGTH_SHORT);
			} else {

				if (!mEndpointACurrentPath.contentEquals("/")) {

					mEndpointACurrentPath = backPath(mEndpointACurrentPath);
					new GetEndpointsDirectory("a", mSelectEndpointAButton
							.getText().toString()).execute();
				} else {
					// If the current path is the root path the user is notified
					// with a warning Toast message
					String text = getString(R.string.root_folder_warning)
							.toString();
					makeToast(text, Toast.LENGTH_SHORT);
				}
			}

		} else if (mIdButton == R.id.back_B_button) {

			// If no endpoint is selected the user is notified with a warning Toast
			// message
			if (mSelectEndpointBButton.getText() == mNoEndpoint) {
				makeToast(
						getString(R.string.no_endpoint_selected_warning,
								Toast.LENGTH_SHORT), Toast.LENGTH_SHORT);
			} else {
				if (!mEndpointBCurrentPath.contentEquals("/")) {
					mEndpointBCurrentPath = backPath(mEndpointBCurrentPath);
					new GetEndpointsDirectory("b", mSelectEndpointBButton
							.getText().toString()).execute();
				} else {
					// If the current path is the root path the user is notified
					// with a warning Toast message
					String text = getString(R.string.root_folder_warning)
							.toString();
					makeToast(text, Toast.LENGTH_SHORT);
				}
			}

		}

	}

	/**
	 * Creates a Deletion Dialog asking from the user for a label for deleting
	 * the files selected in the corresponding View (endpoint). It gives
	 * appropriate error messages with Toasts when there is no endpoint selected
	 * in the View or if no files are selected.If the user clicks on the "OK"
	 * Button of the Dialog -having typed a valid label or no label at all- it
	 * calls the execute method of the FileDeletion class.
	 * 
	 * @param view
	 *            The ListView (representing an endpoint's directory) we want to
	 *            act on
	 */
	public void deleteFiles(final View view) {

		if (view.getId() == mDeleteAButton.getId()) {
			{
				mNode = "a";

				// If no endpoint is selected the user is notified with a warning Toast
				// message
				if (mSelectEndpointAButton
						.getText()
						.toString()
						.contentEquals(
								getString(R.string.select_endpoint_prompt))) {
					makeToast(getString(R.string.no_endpoint_selected_warning),
							Toast.LENGTH_SHORT);
					return;
				}

				// If no files are selected the user in notified with a wanring Toast
				// message
				if (mSelectedFilesA.isEmpty()) {
					makeToast(getString(R.string.no_files_selected_warning),
							Toast.LENGTH_SHORT);
					return;
				}

			}

		} else if (view.getId() == mDeleteBButton.getId()) {
			mNode = "b";

			// If no endpoint is selected the user is notified with a  warning Toast
			// message
			if (mSelectEndpointBButton.getText().toString()
					.contentEquals(getString(R.string.select_endpoint_prompt))) {
				makeToast(getString(R.string.no_endpoint_selected_warning),
						Toast.LENGTH_SHORT);
				return;
			}

			// If no files are selected the user in notified with a warning Toast
			// message
			if (mSelectedFilesB.isEmpty()) {
				makeToast(getString(R.string.no_files_selected_warning),
						Toast.LENGTH_SHORT);
				return;
			}
		}

		// The Delete Files Dialog is initialized
		mDeleteFilesDialog = new Dialog(this);
		mDeleteFilesDialog.setCanceledOnTouchOutside(false);
		mDeleteFilesDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mDeleteFilesDialog.setContentView(R.layout.select_deletion_name);
		mDeleteFilesEditText = (EditText) mDeleteFilesDialog
				.findViewById(R.id.deletion_name_edit_text);
		mDeleteFilesOkButton = (Button) mDeleteFilesDialog
				.findViewById(R.id.ok_button);

		mDeleteFilesDialog.show();

		// This onClickListener defines what happens when the user presses the
		// OK button on the Deletion Dialog
		mDeleteFilesOkButton.setOnClickListener(new OnClickListener()

		{

			@Override
			public void onClick(View arg0) {

				mDeletionName = mDeleteFilesEditText.getText().toString();
				mDeleteFilesEditText.setText("");

				// If the deletion task's label contains escape characters
				// (which result to an error) the user in informed with a warning Toast
				// message
				if (mDeletionName.contains("\n")) {
					makeToast(
							getString(R.string.no_escape_characters_allowed_warning),
							Toast.LENGTH_SHORT);
				} else {
					mDeleteFilesDialog.dismiss();

					new FileDeletion(mNode, mDeletionName).execute();
				}
			}

		});

	}

	/**
	 * Creates a folder creation Dialog asking from the user for the name of the
	 * folder to be created in the corresponding view (endpoint). It gives
	 * appropriate error messages with Toasts when there in no endpoint selected
	 * in the view. If the user clicks on the "OK" button of the dialog -having
	 * typed a valid folder name- it calls the execute method of the
	 * directoryCreation class.
	 * 
	 * @param view
	 *            The ListView (representing an endpoint's directory) we want to
	 *            act on
	 */
	public void createNewFolder(View view) {

		int mIdButton = ((Button) view).getId();
		String mNoEndpoint = getString(R.string.select_endpoint_prompt);
		if (mIdButton == R.id.create_folder_A_button) {
			mNode = "a";

			// If no endpoint is selected the user is notified with a warning Toast
			// message
			if (mSelectEndpointAButton.getText() == mNoEndpoint) {
				makeToast(getString(R.string.no_endpoint_selected_warning),
						Toast.LENGTH_SHORT);
				return;
			}
		} else if (mIdButton == R.id.create_folder_B_button) {
			mNode = "b";
			// If no endpoint is selected the user is notified with a warning Toast
			// message
			if (mSelectEndpointBButton.getText() == mNoEndpoint) {
				makeToast(getString(R.string.no_endpoint_selected_warning),
						Toast.LENGTH_SHORT);
				return;
			}
		}

		// The Create Folder Dialog is initialized
		mCreateFolderDialog = new Dialog(this);
		mCreateFolderDialog.setCanceledOnTouchOutside(false);
		mCreateFolderDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mCreateFolderDialog.setContentView(R.layout.select_folder_name);
		mCreateFolderEditText = (EditText) mCreateFolderDialog
				.findViewById(R.id.folder_name_edit_text);
		mCreateFolderOkButton = (Button) mCreateFolderDialog
				.findViewById(R.id.ok_button);
		mCreateFolderDialog.show();

		// This onClickListener defines what happens when the user presses the
		// OK Button on the Create Folder Dialog
		mCreateFolderOkButton.setOnClickListener(new OnClickListener()

		{

			@Override
			public void onClick(View arg0) {

				mFolderName = mCreateFolderEditText.getText().toString();

				// If no folder name is typed the user is informed with a warning Toast
				// message
				if (mFolderName.isEmpty()) {
					makeToast(getString(R.string.no_folder_title_warning),
							Toast.LENGTH_SHORT);
				}

				// If the folder's name contains escape characters
				// (which result to an error) the user in informed with a warning Toast
				// message
				else if (mFolderName.contains("\n")) {
					makeToast(
							getString(R.string.no_escape_characters_allowed_warning),
							Toast.LENGTH_SHORT);
				} else {

					mCreateFolderDialog.dismiss();
					new DirectoryCreation(mNode, mFolderName).execute();
				}
			}

		});

	}

	/**
	 * It initiates with an Intent the Endpoint Activity, waiting to get back a
	 * result with the name of the Enpoint selected by the user.
	 * 
	 * @param view
	 *            The Endpoint Selection Button that was clicked
	 */
	public void startEndpointList(View view) {
		String mEndpoint = null;

		// The endpoint (A or B) that called the startEndpointList function is
		// defined
		if (view.getId() == mSelectEndpointAButton.getId())
			mEndpoint = "a";
		else if (view.getId() == mSelectEndpointBButton.getId())
			mEndpoint = "b";

		// An intent is created in order to call the Endpoint Activity
		Intent mIntent = new Intent(mContext, EndpointActivity.class);
		mIntent.putStringArrayListExtra("endpointslist", mEndpoints);
		mIntent.putExtra("endId", mEndpoint);

		try {

			// The Endpoint Activity is called and a String Array List
			// containing the endpoint's directory contents is expected
			startActivityForResult(mIntent, 1);
		} catch (Exception e) {

		}
	}

	/**
	 * Returns the path of the folder being one level up from the current
	 * folder.
	 * 
	 * @param path
	 *            The current path
	 * @return The path of the folder one level up
	 */
	public String backPath(String path) {
		String mDelims = "/";
		String[] mTokens = path.split(mDelims);
		String mResult = "/";
		int mTemp;
		if (path.endsWith("/"))
			mTemp = 1;
		else
			mTemp = 2;
		for (int i = 0; i < mTokens.length; i++) {

			if (i > 0 && i < (mTokens.length - mTemp)) {
				mResult = mResult.concat(mTokens[i] + "/");
			}
		}

		return mResult;

	}

	/**
	 * It refreshes the directory contents of the ListView selected.
	 * 
	 * @param view
	 *            The ListView (representing an endpoint's directory) we want to
	 *            act on
	 */
	public void refresh(View view) {
		if (view.getId() == R.id.refresh_A_button) {
			if (!mSelectEndpointAButton.getText().toString()
					.contentEquals(getString(R.string.select_endpoint_prompt))) {
				mAListView.setAdapter(null);
				mAListView.setVisibility(View.INVISIBLE);
				mEndpointAProgressBar.setVisibility(View.VISIBLE);
				new GetEndpointsDirectory("a", mSelectEndpointAButton.getText()
						.toString()).execute();
			} else {
				makeToast(getString(R.string.no_endpoint_selected_warning),
						Toast.LENGTH_SHORT);
			}

		} else if (view.getId() == R.id.refresh_B_button) {
			if (!mSelectEndpointBButton.getText().toString()
					.contentEquals(getString(R.string.select_endpoint_prompt))) {
				mBListView.setAdapter(null);
				mBListView.setVisibility(View.INVISIBLE);
				mEndpointBProgressBar.setVisibility(View.VISIBLE);
				new GetEndpointsDirectory("b", mSelectEndpointBButton.getText()
						.toString()).execute();
			} else {
				makeToast(getString(R.string.no_endpoint_selected_warning),
						Toast.LENGTH_SHORT);
			}
		}

	}

	/**
	 * Returns a string containing the size of a file converted to KBs, MBs, GBs
	 * or just bytes according to its size.
	 * 
	 * @param size
	 *            The given file size in bytes
	 * @return The file size along with the appropriate unit of size
	 */
	public String sizeFromBytes(Float size) {

		String mSizeResult = "0b";

		final float KB = 1024;
		final float MB = 1024 * KB;
		final float GB = 1024 * MB;

		if (size < KB) {
			mSizeResult = String.format("%.0f", size).concat(" b");
		} else if (KB <= size && size < MB) {
			size = size / KB;

			mSizeResult = String.format("%.0f", size).concat(" KB");
		} else if (MB <= size && size < GB) {
			size = size / MB;

			mSizeResult = String.format("%.0f", size).concat(" MB");
		} else if (size >= GB) {
			size = size / GB;
			mSizeResult = String.format("%.1f", size).concat(" GB");
		}

		return mSizeResult.concat(" ");
	}

	/**
	 * Creates a dialog asking from the user to type a label for the transfer to
	 * be initiated. If the source or the destination Endpoints have not been
	 * selected, or if the are no files selected in the source Endpoint
	 * directory List View, it warns the user with Toasts containing the
	 * corresponding warning messages. If the the user types a valid label or no
	 * label at all and clicks the "OK" button in the dialog, the execute method
	 * of the fileTransfer class is called.
	 * 
	 * @param view
	 *            The Transfer Button clicked
	 */
	public void makeTransfer(View view) {
		int mIdButton = ((Button) view).getId();
		String mNoEndpoint = getString(R.string.select_endpoint_prompt);

		if (mSelectEndpointBButton.getText() == mNoEndpoint
				|| mSelectEndpointAButton.getText() == mNoEndpoint) {

			return;
		}
		if (mIdButton == R.id.transfer_A_to_B_button) {
			mNode = "a";
			if (mSelectedFilesA.isEmpty()) {

				return;
			}
		} else if (mIdButton == R.id.transfer_B_to_A_button) {
			mNode = "b";
			if (mSelectedFilesB.isEmpty()) {

				return;
			}
		}

		mMakeTransferDialog = new Dialog(this);
		mMakeTransferDialog.setCanceledOnTouchOutside(false);
		mMakeTransferDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mMakeTransferDialog.setContentView(R.layout.make_transfer_dialog);
		mMakeTransferEditText = (EditText) mMakeTransferDialog
				.findViewById(R.id.transfer_name_edit_text);
		mMakeTransferOkButton = (Button) mMakeTransferDialog
				.findViewById(R.id.ok_button);
		mMakeTransferDialog.show();
		mMakeTransferOkButton.setOnClickListener(new OnClickListener()

		{

			@Override
			public void onClick(View arg0) {

				mTransferName = mMakeTransferEditText.getText().toString();
				mMakeTransferEditText.setText("");
				if (mTransferName.contains("\n")) {
					makeToast(
							getString(R.string.no_escape_characters_allowed_warning),
							Toast.LENGTH_SHORT);
				} else {
					mMakeTransferDialog.dismiss();
					new FilesTransfer(mNode, mTransferName).execute();
				}
			}

		});

	}

	/**
	 * 
	 * This adapter is a custom adapter used for populating the List View
	 * representing the endpoints' directories. In every row of the List it puts
	 * the name of the file/folder, along with its appropriate icon based on
	 * whether it is a file or a folder, its size if it is a file or the word
	 * "Folder" if it is a folder, as well as paints the background light blue
	 * or white, depending on whether the specific file or folder is selected by
	 * the user or not.
	 * 
	 * @author christos
	 * 
	 */
	public class CustomAdapter extends ArrayAdapter<String> {

		private Context mLocalContext;

		public CustomAdapter(Context context, int textViewResourceId,
				List<String> objects) {
			super(context, textViewResourceId, objects);
			this.mLocalContext = context;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			String mFile = (String) this.getItem(position);
			View mRow = convertView;

			if (mRow == null) {
				LayoutInflater vi = (LayoutInflater) mLocalContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				mRow = vi.inflate(R.layout.directory_list_view, null);
			}

			TextView mFilenameTextView = (TextView) mRow
					.findViewById(R.id.file_name_in_list_text_view);
			ImageView mFileTypeImageView = (ImageView) mRow
					.findViewById(R.id.file_type_in_list);
			TextView mFileSize = (TextView) mRow
					.findViewById(R.id.file_size_in_text_view);

			// If the file's name ends with "/" it is assigned the folder icon;
			// otherwise it is assigned the file icon
			if (mFile.endsWith("/")) {
				mFileTypeImageView.setImageResource(R.drawable.folder);
				mFileSize.setText(getString(R.string.folder));
			} else {
				mFileTypeImageView.setImageResource(R.drawable.file);
				float mTemp = 0;

				// The file's size is retrieved
				if (parent.getId() == R.id.endpoint_A_list_view) {
					if (mFilesSizeA.containsKey(mFile))
						mTemp = mFilesSizeA.get(mFile);
				} else if (parent.getId() == R.id.endpoint_B_list_view) {
					if (mFilesSizeB.containsKey(mFile))
						mTemp = mFilesSizeB.get(mFile);
				}

				// The file's size is converted to the appropriate unit of size
				String mSizeString = sizeFromBytes(mTemp);

				// The file's size is set
				mFileSize.setText(mSizeString);

			}

			// The file/folder's background color is set to either light blue or
			// white, depending on whether it is selected or not
			if (parent.getId() == R.id.endpoint_A_list_view) {
				if (mSelectedFilesA.contains(mFile)) {
					mRow.setBackgroundColor(getResources().getColor(
							R.color.android_light_blue));
				} else {
					mRow.setBackgroundColor(Color.WHITE);
				}
			} else if (parent.getId() == R.id.endpoint_B_list_view) {
				if (mSelectedFilesB.contains(mFile)) {
					mRow.setBackgroundColor(getResources().getColor(
							R.color.android_light_blue));
				} else {
					mRow.setBackgroundColor(Color.WHITE);

				}

			}

			// The file's name text view is set
			mFilenameTextView.setText(mFile);
			return mRow;
		}

	}

	/**
	 * This class attempts to create a new directory by POSTing a JSON object
	 * containing the new folder's name to endpoint/<canonical_name>/mkdir. It
	 * informs the user with a Toast for the request's result, and if it's
	 * successful it refreshes the corresponding endpoint's directory.
	 * 
	 * @author christos
	 * 
	 */
	public class DirectoryCreation extends AsyncTask<String, Void, String> {

		String mLocalNode, mLocalfolderName;

		/**
		 * Constructor. It sets the node where the directory should be created
		 * and the name of the new folder.
		 * 
		 * @param node
		 *            The node (Endpoint A or B) where the directory should be
		 *            created
		 * @param folderName
		 *            The name of the new folder
		 */
		public DirectoryCreation(String node, String folderName) {
			this.mLocalNode = node;
			this.mLocalfolderName = folderName;

		}

		@Override
		protected String doInBackground(String... args) {

			String mResult = getString(R.string.error);
			String mTargetPath = "";
			String mEndpoint = "";

			// The path and the endpoint's name are retrieved
			if (mLocalNode.contentEquals("a")) {
				mTargetPath = mEndpointACurrentPath;
				mEndpoint = mSelectEndpointAButton.getText().toString();
			} else if (mLocalNode.contentEquals("b")) {
				mTargetPath = mEndpointBCurrentPath;
				mEndpoint = mSelectEndpointBButton.getText().toString();

			}

			// The endpoint's canonical name is split into user's name and
			// endpoint's name in order to replace
			// "#" with "%23" for encoding reasons
			String mDelims = "#";
			String[] tokens = mEndpoint.split(mDelims);
			Result mQueryResult;
			try {

				JSONObject mSubmit = new JSONObject();
				mSubmit.put("path", mTargetPath + mLocalfolderName);
				mSubmit.put("DATA_TYPE", "mkdir");

				// The directory creation request is POSTed to the API along
				// with a JSON Object containing
				// information about the path and the new folder's name
				mQueryResult = sClient.postResult("/endpoint/" + tokens[0]
						+ "%23" + tokens[1] + "/mkdir", mSubmit);
				JSONObject mJsonObject = mQueryResult.document;

				if (!mJsonObject.getString("DATA_TYPE").equals("mkdir_result")) {
					return mResult;
				}

				// The message String is retrieved and returned
				mResult = mJsonObject.getString("message");
				return mResult;
			} catch (MalformedURLException e) {

				e.printStackTrace();
				mResult = e.getMessage();
			} catch (IOException e) {

				e.printStackTrace();
				mResult = e.getMessage();
			} catch (GeneralSecurityException e) {

				e.printStackTrace();
				mResult = e.getMessage();
			} catch (JSONException e) {

				e.printStackTrace();
				mResult = e.getMessage();
			} catch (APIError e) {

				e.printStackTrace();
				mResult = e.message;

			}

			return mResult;
		}

		@Override
		protected void onPostExecute(String result) {

			super.onPostExecute(result);

			// The user is informed of the outcome of their request with a Toast message
			makeToast(result, Toast.LENGTH_LONG);

			// If the directory is successfully created, the endpoint's contents
			// are reloaded
			if (result.contentEquals("The directory was created successfully")) {
				if (mLocalNode.contentEquals("a")) {
					mAListView.setAdapter(null);
					mAListView.setVisibility(View.INVISIBLE);
					mEndpointAProgressBar.setVisibility(View.VISIBLE);
					new GetEndpointsDirectory(mLocalNode,
							mSelectEndpointAButton.getText().toString())
							.execute();
				} else if (mLocalNode.contentEquals("b")) {
					mBListView.setAdapter(null);
					mBListView.setVisibility(View.INVISIBLE);
					mEndpointBProgressBar.setVisibility(View.VISIBLE);
					new GetEndpointsDirectory(mLocalNode,
							mSelectEndpointBButton.getText().toString())
							.execute();

				}

			}

		}

	}

	/**
	 * This class tries to delete the files selected by the user. It first gets
	 * a submission id by sending GET /submission_id. Once the submission id is
	 * acquired, it POSTs a JSON Object to /delete, containing the submission
	 * id, the deletion task's name, the endpoint's name, an JSONArray
	 * containing the paths of the files/folders that are to be deleted and a
	 * flag indicating whether there should be recursive file deletion or not.
	 * It informs the user for the request's result with a Toast.
	 * 
	 * @author christos
	 */
	public class FileDeletion extends AsyncTask<String, Void, List<String>> {

		String mLocalNode;
		String mLocalDeletionName;

		/**
		 * Constructor. Sets the node (Endpoint A or B) we want to delete
		 * files/folders from and the name of the deletion task.
		 * 
		 * @param node
		 *            The node (Endpoint A or B) we want do delete files/folders
		 *            from
		 * @param deletionName
		 *            The name of the deletion task
		 */
		public FileDeletion(String node, String deletionName) {
			this.mLocalNode = node;
			this.mLocalDeletionName = deletionName;
		}

		@Override
		protected void onPreExecute() {

			super.onPreExecute();

			// The user is informed with a Toast message that the deletion
			// request has been submitted
			makeToast(getString(R.string.delete_request_submitted),
					Toast.LENGTH_SHORT);
		}

		@Override
		protected List<String> doInBackground(String... args) {

			String mId;
			List<String> mMessage = new ArrayList<String>();
			boolean mContainsFolders = false;
			try {
				List<String> mTargetPath = new ArrayList<String>();
				String mEndpoint = null;
				Result mQueryResult;

				// A GET request is send to the API in order to acquire a
				// submission id, necessary for submitting any transfer
				// or deletion task.
				mQueryResult = sClient.getResult("/submission_id");

				JSONObject mJsonObject = mQueryResult.document;

				if (!mJsonObject.getString("DATA_TYPE").equals("submission_id")) {
					mMessage.add(getString(R.string.error));
					return mMessage;
				}
				mId = mJsonObject.getString("value");

				// The selected files for deletion are added to the Target Path
				// List.
				// A flag indicates whether there are any folders selected or not
				if (mLocalNode.contentEquals("a")) {
					for (int i = 0; i < mSelectedFilesA.size(); i++) {
						mTargetPath.add(mEndpointACurrentPath
								.concat(mSelectedFilesA.get(i)));
						if (mSelectedFilesA.get(i).endsWith("/"))
							mContainsFolders = true;
					}
					mEndpoint = mSelectEndpointAButton.getText().toString();
				} else if (mLocalNode.contentEquals("b")) {
					for (int i = 0; i < mSelectedFilesB.size(); i++) {
						mTargetPath.add(mEndpointBCurrentPath
								.concat(mSelectedFilesB.get(i)));
						if (mSelectedFilesB.get(i).endsWith("/"))
							mContainsFolders = true;
					}
					mEndpoint = mSelectEndpointBButton.getText().toString();

				}

				Result mResult;

				// All the paths of the files to be deleted are put in a
				// JSON Array
				JSONArray mJArray = new JSONArray();
				for (int i = 0; i < mTargetPath.size(); i++) {
					JSONObject mTemp = new JSONObject();
					mTemp.put("path", mTargetPath.get(i));
					mTemp.put("DATA_TYPE", "delete_item");
					mJArray.put(mTemp);
				}

				// A JSON Object is created containing the submission id, the
				// endpoint to delete files from,
				// a flag indicating the existence of folders, the deletion
				// task's label and the JSON Array containing
				// the files' and folders' paths
				JSONObject mSubmit = new JSONObject();
				mSubmit.put("submission_id", mId);
				mSubmit.put("endpoint", mEndpoint);
				mSubmit.put("recursive", mContainsFolders);

				if (!mLocalDeletionName.isEmpty()) {
					mSubmit.put("label", mLocalDeletionName);
				}

				mSubmit.put("DATA_TYPE", "delete");
				mSubmit.put("ingore_missing", false);
				mSubmit.put("DATA", mJArray);

				// The deletion request is POSTed to the API along with the JSON
				// Object containing all the relevant information
				mResult = sClient.postResult("delete", mSubmit);
				JSONObject mJsonObject2 = mResult.document;

				// The message String returned by the API is retrieved and returned
				mMessage.add(mJsonObject2.getString("message"));
				mMessage.add(mJsonObject2.getString("code"));

				return mMessage;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				mMessage.add(e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				mMessage.add(e.getMessage());
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				mMessage.add(e.getMessage());
			} catch (JSONException e) {
				e.printStackTrace();
				mMessage.add(e.getMessage());
			} catch (APIError e) {
				e.printStackTrace();
				mMessage.add(e.message);
			}

			return mMessage;
		}

		@Override
		protected void onPostExecute(List<String> result) {

			super.onPostExecute(result);

			// The user is informed of their request's outcome with a Toast message
			makeToast(result.get(0), Toast.LENGTH_LONG);

		}

	}

	/**
	 * This class attempts to get the contents of a specific endpoint's
	 * directory by sending a GET request to endpoint/<canonical_name>/ls along
	 * with request parameters containing the path we want to access. It makes
	 * the corresponding ListView invisible and puts a progress bar on its place
	 * while communicating with the Server. If it gets the directory's contents
	 * successfully it presents them through the corresponding List View.
	 * Otherwise it informs the user for any error that may have occurred, and
	 * if the case is that the Endpoint is not activated, it calls the execute
	 * function of the AutoActivate class.
	 * 
	 * 
	 * @author christos
	 * 
	 */
	public class GetEndpointsDirectory extends
			AsyncTask<String, Void, ArrayList<String>> {

		String mLocalNode;
		String mLocalEndpoint;

		/**
		 * Constructor. It sets the node (Endpoint A or B) we want to the
		 * directory information about and the name of the endpoint.
		 * 
		 * @param node
		 * @param endpoint
		 */
		public GetEndpointsDirectory(String node, String endpoint) {
			this.mLocalNode = node;
			this.mLocalEndpoint = endpoint;
		}

		@Override
		protected void onPreExecute() {

			super.onPreExecute();

			if (mLocalNode.contentEquals("a")) {

				mAListView.setAdapter(null);
				mTransferAButton.setBackgroundResource(R.drawable.down);
				mPathATextView.setText(mEndpointACurrentPath);
				mAListView.setVisibility(View.INVISIBLE);
				mEndpointAProgressBar.setVisibility(View.VISIBLE);
				mLastTimePressedA.clear();
				mFilesSizeA.clear();

			} else if (mLocalNode.contentEquals("b")) {

				mBListView.setAdapter(null);
				mTransferBButton.setBackgroundResource(R.drawable.up);
				mPathBTextView.setText(mEndpointBCurrentPath);
				mBListView.setVisibility(View.INVISIBLE);
				mEndpointBProgressBar.setVisibility(View.VISIBLE);
				mLastTimePressedB.clear();
				mFilesSizeB.clear();

			}
		}

		@Override
		protected ArrayList<String> doInBackground(String... args) {

			ArrayList<String> mDirectoryList = new ArrayList<String>();
			Map<String, String> mRequestParams = new HashMap<String, String>();

			// The current path of the endpoint is retrieved
			if (mLocalNode.contentEquals("a")) {
				mRequestParams.put("path", mEndpointACurrentPath);
			} else if (mLocalNode.contentEquals("b")) {
				mRequestParams.put("path", mEndpointBCurrentPath);
			}

			// The "show hidden files" filter is set according the user's
			// settings
			if (!mSharedPreferences.getBoolean("ShowHiddenFiles", false)) {
				mRequestParams.put("show_hidden", "False");
			}

			// The endpoint's canonical name is split into user's name and
			// endpoint's name in order to replace
			// "#" with "%23" for encoding reasons
			String mDelims = "#";
			String[] mTokens = mLocalEndpoint.split(mDelims);
			Result mQueryResult;
			try {

				// The GET request is sent to the API along with a JSON Object
				// containing all the relative parameters
				mQueryResult = sClient.getResult("/endpoint/" + mTokens[0]
						+ "%23" + mTokens[1] + "/ls", mRequestParams);
				JSONObject jO = mQueryResult.document;

				if (!jO.getString("DATA_TYPE").equals("file_list")) {

					mDirectoryList.add("");
					return mDirectoryList;
				}

				// The JSON Array DATA is retrieved
				JSONArray mJsonArray = jO.getJSONArray("DATA");
				JSONObject mJsonObject;

				for (int i = 0; i < mJsonArray.length(); i++) {
					mJsonObject = mJsonArray.getJSONObject(i);
					String mLocalEndpoint;
					Float mSize;

					// If the file is a directory (folder) an "/" is postfixed
					// to its name
					if (mJsonObject.getString("type").contentEquals("dir")) {

						mLocalEndpoint = mJsonObject.getString("name") + "/";
					} else {
						mLocalEndpoint = mJsonObject.getString("name");
						mSize = Float.parseFloat(mJsonObject.getString("size"));
						if (mLocalNode.contentEquals("a")) {
							mFilesSizeA.put(mLocalEndpoint, mSize);
						} else if (mLocalNode.contentEquals("b")) {
							mFilesSizeB.put(mLocalEndpoint, mSize);
						}
					}
					// The file/folder is added to the directory list
					mDirectoryList.add(mLocalEndpoint);
				}

				// The list containing all the contents of the current path is
				// returned
				return mDirectoryList;
			} catch (MalformedURLException e) {

				e.printStackTrace();
				mDirectoryList.add("Error");
				mDirectoryList.add(e.getMessage());
			} catch (IOException e) {

				e.printStackTrace();
				mDirectoryList.add("Error");
				mDirectoryList.add(e.getMessage());
			} catch (GeneralSecurityException e) {

				e.printStackTrace();
				mDirectoryList.add("Error");
				mDirectoryList.add(e.getMessage());
			} catch (JSONException e) {

				e.printStackTrace();
				mDirectoryList.add("Error");
				mDirectoryList.add(e.getMessage());
			} catch (APIError e) {

				e.printStackTrace();
				mDirectoryList.add("Error");
				mDirectoryList.add(e.message);

			}

			return mDirectoryList;

		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {

			super.onPostExecute(result);

			// The Custom Adapter we have created populates the list with the
			// files and folders
			// as they were retrieved from the communication with the API
			ArrayAdapter adapter = new CustomAdapter(mContext,
					android.R.layout.simple_list_item_1, result);
			String mCode = "";
			if (result.size() > 0) {
				mCode = result.get(0);
			}

			if (mLocalNode.contentEquals("a")) {

				if (!mCode.contentEquals("Error")) {
					mAListView.setAdapter(adapter);
					mFileListA = result;

				} else {

					// If the endpoint is not activated, the user is informed
					// with a Toast message and the execute method
					// of the AutoActivate class is called
					makeToast(result.get(1), Toast.LENGTH_LONG);
					if (result.get(1).endsWith("not activated"))
						new AutoActivate("a", mLocalEndpoint)
								.execute(mActivationRequirements);
				}

				mAListView.setVisibility(View.VISIBLE);
				mEndpointAProgressBar.setVisibility(View.GONE);
				mSelectedFilesA.clear();

			} else if (mLocalNode.contentEquals("b")) {

				if (!mCode.contentEquals("Error")) {
					mBListView.setAdapter(adapter);
					mFileListB = result;
				} else {
					
					// If the endpoint is not activated, the user is informed
					// with a Toast message and the execute method
					// of the AutoActivate class is called
					makeToast(result.get(1), Toast.LENGTH_LONG);
					if (result.get(1).endsWith("not activated"))
						new AutoActivate("b", mLocalEndpoint)
								.execute(mActivationRequirements);
				}
				mBListView.setVisibility(View.VISIBLE);
				mEndpointBProgressBar.setVisibility(View.GONE);
				mSelectedFilesB.clear();

			}

		}

	}

	/**
	 * 
	 * 
	 * This class attempts to get the list of all the endpoints visible to the
	 * user by sending a GET request to endpoint_list. It hides all the view
	 * from the screen while communicating with the server and instead shows a
	 * progress bar.
	 * 
	 * @author christos
	 */
	public class GetEndpointsList extends
			AsyncTask<String, Void, ArrayList<String>> {

		@Override
		protected void onPreExecute() {
			mEndpointLoadProgressBar.setProgress(0);
			super.onPreExecute();

			// Everything on the screen temporarily disappears and a Progress
			// Bar along with a Text message take their place
			mSelectEndpointAButton.setVisibility(View.INVISIBLE);
			mSelectEndpointBButton.setVisibility(View.INVISIBLE);
			mAListView.setVisibility(View.INVISIBLE);
			mBListView.setVisibility(View.INVISIBLE);
			mATextView.setVisibility(View.INVISIBLE);
			mBTextView.setVisibility(View.INVISIBLE);
			mUpAButton.setVisibility(View.INVISIBLE);
			mUpBButton.setVisibility(View.INVISIBLE);
			mCreateAButton.setVisibility(View.INVISIBLE);
			mCreateBButton.setVisibility(View.INVISIBLE);
			mDeleteAButton.setVisibility(View.INVISIBLE);
			mDeleteBButton.setVisibility(View.INVISIBLE);
			mRefreshAButton.setVisibility(View.INVISIBLE);
			mRefreshBButton.setVisibility(View.INVISIBLE);
			mEndpointLoadProgressBar.setVisibility(View.VISIBLE);
			mPathATextView.setVisibility(View.INVISIBLE);
			mPathBTextView.setVisibility(View.INVISIBLE);
			mTransferAButton.setVisibility(View.INVISIBLE);
			mTransferBButton.setVisibility(View.INVISIBLE);

			TextView mLoadMessage = (TextView) findViewById(R.id.loading_endpoints_textview);
			mLoadMessage.setVisibility(View.VISIBLE);

		}

		@Override
		protected ArrayList<String> doInBackground(String... arg0) {

			List<String> mEndpoints = new ArrayList<String>();

			Map<String, String> mRequestParams = new HashMap<String, String>();
			mRequestParams.put("limit", "0");
			mRequestParams.put("fields", "canonical_name");

			// It is decided whether all of the endpoints will be returned or
			// only the active ones, based on the user's settings
			if (mSharedPreferences.getBoolean("EndpointsActiveOnly", true)) {
				mRequestParams.put("filter", "activated:true");
			}

			// The Bar's Progress is updated
			mEndpointLoadProgressBar.setProgress(1);
			Result mQueryResult;
			try {

				// The GET request is sent to the API along with all the
				// relevant parameters
				mQueryResult = sClient.getResult("/endpoint_list",
						mRequestParams);

				mEndpointLoadProgressBar.setProgress(2);
				JSONObject mJsonObject = mQueryResult.document;

				if (!mJsonObject.getString("DATA_TYPE").equals("endpoint_list")) {
					mEndpoints.add(getString(R.string.error));
					return (ArrayList<String>) mEndpoints;
				}

				// The DATA JSON Array is retrieved
				JSONArray mJsonArray = mJsonObject.getJSONArray("DATA");

				mEndpointLoadProgressBar.setProgress(3);
				
				// The endpoints are added to the mEndpoints Array List
				for (int i = 0; i < mJsonArray.length(); i++) {

					String endpoint = mJsonArray.getJSONObject(i).getString(
							"canonical_name");
					mEndpoints.add(endpoint);

				}

				mEndpointLoadProgressBar.setProgress(4);

			} catch (MalformedURLException e) {
				e.printStackTrace();
				mEndpoints.add(getString(R.string.error));
				mEndpoints.add(e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				mEndpoints.add(getString(R.string.error));
				mEndpoints.add(e.getMessage());
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				mEndpoints.add(getString(R.string.error));
				mEndpoints.add(e.getMessage());
			} catch (JSONException e) {
				e.printStackTrace();
				mEndpoints.add(getString(R.string.error));
				mEndpoints.add(e.getMessage());
			} catch (APIError e) {
				e.printStackTrace();
				mEndpoints.add(getString(R.string.error));
				mEndpoints.add(e.message);
			}

			return (ArrayList<String>) mEndpoints;
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {

			super.onPostExecute(result);

			// The Progress Bar and Text disappear and everything else appears
			// back on the screen
			mSelectEndpointAButton.setVisibility(View.VISIBLE);
			mSelectEndpointBButton.setVisibility(View.VISIBLE);
			mAListView.setVisibility(View.VISIBLE);
			mBListView.setVisibility(View.VISIBLE);
			mATextView.setVisibility(View.VISIBLE);
			mBTextView.setVisibility(View.VISIBLE);
			mUpAButton.setVisibility(View.VISIBLE);
			mUpBButton.setVisibility(View.VISIBLE);
			mCreateAButton.setVisibility(View.VISIBLE);
			mCreateBButton.setVisibility(View.VISIBLE);
			mDeleteAButton.setVisibility(View.VISIBLE);
			mDeleteBButton.setVisibility(View.VISIBLE);
			mRefreshAButton.setVisibility(View.VISIBLE);
			mRefreshBButton.setVisibility(View.VISIBLE);
			mPathATextView.setVisibility(View.VISIBLE);
			mPathBTextView.setVisibility(View.VISIBLE);
			mTransferAButton.setVisibility(View.VISIBLE);
			mTransferBButton.setVisibility(View.VISIBLE);

			mEndpoints = (ArrayList<String>) result.clone();
			TextView loadMessage = (TextView) findViewById(R.id.loading_endpoints_textview);
			loadMessage.setVisibility(View.GONE);
			mEndpointLoadProgressBar.setVisibility(View.GONE);

		}

	}

	/**
	 * This class attempts to transfer the files selected by the user from the
	 * one Endpoint to the other. It first gets a submission id by sending GET
	 * /submission_id. Once the submission id is acquired, it POSTs a JSON
	 * Object to /transfer, containing the submission id, the transfer task's
	 * name(if any), the source endpoint's name, the destination endpoint's name
	 * and a JSONArray containing the paths of the files/folders that are to be
	 * transfered along with the paths that they are bound to be transferred to.
	 * It informs the user for the request's result with a Toast.
	 * 
	 * 
	 * 
	 * @author christos
	 * 
	 */
	public class FilesTransfer extends AsyncTask<String, Void, List<String>> {

		String mLocalNode;
		String mLocaltransferName;

		/**
		 * Constructor. It sets the node (Endpoint A or B) that we want to
		 * transfer files from as well as the name of the transfer task. *
		 * 
		 * @param node
		 * @param transferName
		 */
		public FilesTransfer(String node, String transferName) {
			this.mLocalNode = node;
			this.mLocaltransferName = transferName;
		}

		@Override
		protected void onPreExecute() {

			super.onPreExecute();

			// The user is informed that their transfer request has been
			// submitted
			makeToast(getString(R.string.transfer_request_submitted),
					Toast.LENGTH_SHORT);
		}

		@Override
		protected List<String> doInBackground(String... arg0) {

			String mId;
			List<String> mMessage = new ArrayList<String>();
			try {

				Map<String, String> mPaths = new HashMap<String, String>();
				String mSourceEndpoint = null;
				String mDestinationEndpoint = null;
				Result mQueryResult;

				// A GET request is send to the API in order to acquire a
				// submission id, necessary for submitting any transfer
				// or deletion task
				mQueryResult = sClient.getResult("/submission_id");

				JSONObject mJsonObject = mQueryResult.document;

				if (!mJsonObject.getString("DATA_TYPE").equals("submission_id")) {
					mMessage.add(getString(R.string.error));
					return mMessage;
				}
				mId = mJsonObject.getString("value");

				// The mPaths HashMap is populated with the paths of the files
				// folders to be transferred
				if (mLocalNode.contentEquals("a")) {
					for (int i = 0; i < mSelectedFilesA.size(); i++) {

						mPaths.put(mEndpointACurrentPath.concat(mSelectedFilesA
								.get(i)), mEndpointBCurrentPath
								.concat(mSelectedFilesA.get(i)));

					}

					mSourceEndpoint = mSelectEndpointAButton.getText()
							.toString();
					mDestinationEndpoint = mSelectEndpointBButton.getText()
							.toString();

				}
				
				// The mPaths HashMap is populated with the paths of the files
				// folders to be transferred
				else if (mLocalNode.contentEquals("b")) {
					for (int i = 0; i < mSelectedFilesB.size(); i++) {

						mPaths.put(mEndpointBCurrentPath.concat(mSelectedFilesB
								.get(i)), mEndpointACurrentPath
								.concat(mSelectedFilesB.get(i)));
					}
					mSourceEndpoint = mSelectEndpointBButton.getText()
							.toString();
					mDestinationEndpoint = mSelectEndpointAButton.getText()
							.toString();

				}

				Result mResult;
				JSONArray mJsonArray = new JSONArray();

				// A JSON Array is created containing for every transfer the
				// source path, the destination path
				// and a flag defining whether the item sent is a file or a
				// folder
				for (Map.Entry<String, String> entry : mPaths.entrySet()) {
					JSONObject temp = new JSONObject();
					String mSourcePath = entry.getKey();
					String mDestinationPath = entry.getValue();
					temp.put("source_path", mSourcePath);
					temp.put("DATA_TYPE", "transfer_item");
					temp.put("destination_path", mDestinationPath);
					if (mSourcePath.endsWith("/"))
						temp.put("recursive", true);
					else
						temp.put("recursice", false);
					mJsonArray.put(temp);
				}

				// A JSON Object is created containing the submission id the
				// source and destination endpoints, the transfer task's
				// label (if any) and the JSON Array DATA created above
				JSONObject mSubmit = new JSONObject();
				mSubmit.put("submission_id", mId);
				mSubmit.put("source_endpoint", mSourceEndpoint);
				mSubmit.put("destination_endpoint", mDestinationEndpoint);
				mSubmit.put("DATA_TYPE", "transfer");

				if (!mLocaltransferName.isEmpty()) {
					mSubmit.put("label", mLocaltransferName);
				}

				mSubmit.put("DATA", mJsonArray);

				// The transfer request is POSTed to the API along with a JSON
				// Object containing all the relevant information
				mResult = sClient.postResult("transfer", mSubmit);
				JSONObject jsonObject = mResult.document;

				// The message String is retrieved and returned
				mMessage.add(jsonObject.getString("message"));
				mMessage.add(jsonObject.getString("code"));

				return mMessage;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				mMessage.add(e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				mMessage.add(e.getMessage());
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				mMessage.add(e.getMessage());
			} catch (JSONException e) {
				e.printStackTrace();
				mMessage.add(e.getMessage());
			} catch (APIError e) {
				e.printStackTrace();
				mMessage.add(e.message);
			}

			return mMessage;
		}

		@Override
		protected void onPostExecute(List<String> result) {

			super.onPostExecute(result);

			// The user is informed of their request's outcome with a Toast
			// message
			makeToast(result.get(0), Toast.LENGTH_LONG);

		}

	}

	/**
	 * This class attempts to autoactivate an Endpoint, in case it is not
	 * activated, by POSTing and empty JSON Object to
	 * endpoint/<canonical_name>/autoactivate. In case the Endpoint can not be
	 * auto activated, it creates a dialog prompting the user to enter the
	 * credentials needed for the Endpoint to be activated. If the user types
	 * the necessary credentials and clicks the "Activate" button, the execution
	 * function of the Activate class is called.
	 * 
	 * @author christos
	 * 
	 */
	public class AutoActivate extends
			AsyncTask<JSONObject, Void, ArrayList<String>> {

		String mLocalNode;
		String mEndpoint;

		/**
		 * Constructor. Sets the node (Endpoint A or B) to be autoactivated and
		 * the Endpoint's name.
		 * 
		 * @param node
		 *            The node to be autoactivated
		 * @param endpoint
		 *            The Endpoint's name to be autoactivated
		 */
		public AutoActivate(String node, String endpoint) {
			this.mLocalNode = node;
			this.mEndpoint = endpoint;
			mActivationRequirements = null;
		}

		@Override
		protected ArrayList<String> doInBackground(JSONObject... args) {

			ArrayList<String> mResult = new ArrayList<String>();

			// The endpoint's canonical name is split into user's name and
			// endpoint's name in order to replace
			// "#" with "%23" for encoding reasons
			String mDelims = "#";
			String[] mTokens = mEndpoint.split(mDelims);

			Result mQueryResult;
			try {

				JSONObject mEmptyJsonObject = args[0];

				// The user's request is POSTed to the API along with an empty
				// JSON Object
				mQueryResult = sClient.postResult("/endpoint/" + mTokens[0]
						+ "%23" + mTokens[1] + "/autoactivate",
						mEmptyJsonObject);
				JSONObject mJsonObject = mQueryResult.document;

				if (!mJsonObject.getString("DATA_TYPE").equals(
						"activation_result")) {
					mResult.add(getString(R.string.error));
					mResult.add(getString(R.string.error));
					return mResult;
				}

				// The message returned from the API is retrieved
				mResult.add(mJsonObject.getString("code"));
				mResult.add(mJsonObject.getString("message"));

				// If the message does not state that the endpoint has been auto
				// activated, the mJsonObject is retrieved
				// because it containts information about the -manual-
				// activation requirements
				if (!mResult.get(0).startsWith("AutoActivated")) {
					mActivationRequirements = mJsonObject;

				}

			} catch (MalformedURLException e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.getMessage());
			} catch (IOException e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.getMessage());
			} catch (GeneralSecurityException e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.getMessage());
			} catch (JSONException e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.getMessage());
			} catch (APIError e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.message);

			}

			return mResult;
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {

			super.onPostExecute(result);

			final JSONArray mJSONArray;

			// If the endpoint has been autoactivated the user is informed with
			// a Toast message
			// and its contents are loaded
			if (result.get(0).startsWith("AutoActivated")) {
				new GetEndpointsDirectory(mLocalNode, mEndpoint).execute();
				makeToast(result.get(1), Toast.LENGTH_LONG);
			}
			// If the endpoint has not been auto activated a Dialog Window is
			// created prompting the user to
			// type the necessary information in order to activate the endpoint
			else {
				String title = "";

				mActivateDialog = new Dialog(mContext);
				mActivateDialog.setCanceledOnTouchOutside(false);
				mActivateDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				mActivateDialog.setContentView(R.layout.activate_dialog);
				TextView mTitleTextView = (TextView) mActivateDialog
						.findViewById(R.id.activate_title_id);

				LinearLayout layout = (LinearLayout) mActivateDialog
						.findViewById(R.id.linear);

				final Map<Integer, EditText> mEditTextMap = new HashMap<Integer, EditText>();
				final Map<Integer, TextView> mTextViewMap = new HashMap<Integer, TextView>();
				try {
					mJSONArray = mActivationRequirements.getJSONArray("DATA");
					int size = mJSONArray.length();
					title = mActivationRequirements.getString("endpoint");
					for (int i = 0; i < size; i++) {
						JSONObject mTempObject = mJSONArray.getJSONObject(i);
						if (!mTempObject.getString("type").contentEquals(
								"delegate_proxy")) {

							TextView mTextView = new TextView(mContext);
							EditText mEditText = new EditText(mContext);

							// If the field is marked as "required" a * is put
							// next to the field's name
							if (!mTempObject.getBoolean("required")) {
								mTextView.setText(mTempObject
										.getString("ui_name"));
							} else {
								mTextView.setText("*"
										+ mTempObject.getString("ui_name"));
							}
							String mValue = mTempObject.getString("value");

							if (!mValue.contentEquals("null")) {
								mEditText.setText(mValue);

							}
							mTextView.setLayoutParams(new LayoutParams(
									ViewGroup.LayoutParams.WRAP_CONTENT,
									ViewGroup.LayoutParams.WRAP_CONTENT));
							mEditText.setLayoutParams(new LayoutParams(
									ViewGroup.LayoutParams.MATCH_PARENT,
									ViewGroup.LayoutParams.WRAP_CONTENT));
							if (mTempObject.getBoolean("private")) {
								mEditText
										.setInputType(InputType.TYPE_CLASS_TEXT
												| InputType.TYPE_TEXT_VARIATION_PASSWORD);
							}

							mEditTextMap.put(i, mEditText);

							mTextViewMap.put(i, mTextView);
							layout.addView(mTextView);
							layout.addView(mEditText);

						}
					}

					// If the endpoint can not be activated through the
					// application the user is informed
					// with an appropriate message on the Activation Window
					mTitleTextView.setText("Activate Endpoint:" + title);
					if (mEditTextMap.isEmpty()) {
						TextView mTextView = new TextView(mContext);
						mTextView.setText(getString(R.string.oauth));
						mTextView.setLayoutParams(new LayoutParams(
								ViewGroup.LayoutParams.WRAP_CONTENT,
								ViewGroup.LayoutParams.WRAP_CONTENT));
						layout.addView(mTextView);

					}

					Button mOkButton = (Button) mActivateDialog
							.findViewById(R.id.ok_button);

					mOkButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {

							// If the Window contains nothing but a message
							// informing the user that the endpoint
							// can not be activated, when the OK Button is
							// pressed the Dialog disappears nothing happens
							if (mEditTextMap.isEmpty()) {
								mActivateDialog.dismiss();
							} else {
								boolean mReady = true;

								for (Entry<Integer, EditText> entry : mEditTextMap
										.entrySet()) {

									String mEditTextValue = entry.getValue()
											.getText().toString();
									int key = entry.getKey();
									String mTextViewValue = mTextViewMap
											.get(key).getText().toString();

									if (mTextViewValue.startsWith("*")
											&& mEditTextValue.isEmpty()) {
										mReady = false;

									}

								}
								// If the user has filled all the necessary
								// fields, the Activate Dialog
								// is dismissed and the execute method of the
								// Activate class is called
								if (mReady) {

									try {
										for (Entry<Integer, EditText> entry : mEditTextMap
												.entrySet()) {
											int key = entry.getKey();
											JSONObject mJSONObject = mJSONArray
													.getJSONObject(key);
											mJSONObject.put("value", entry
													.getValue().getText()
													.toString());
											mJSONArray.put(key, mJSONObject);
											Log.d("VALUE", mJSONObject
													.getString("value"));
										}

										mActivationRequirements.remove("DATA");
										mActivationRequirements.put("DATA",
												mJSONArray);

									} catch (JSONException e) {

										e.printStackTrace();
									}

									mActivateDialog.dismiss();
									new Activate(mLocalNode, mEndpoint)
											.execute(mActivationRequirements);
								}
								// If some of the required fields have not been
								// filled, the user is informed
								// with a Toast message
								else {

									makeToast(
											getString(R.string.fields_not_filled_warning),
											Toast.LENGTH_SHORT);
								}

							}

						}

					});

					mActivateDialog.show();

				} catch (JSONException e) {

					e.printStackTrace();
				}

			}

		}

	}

	/**
	 * This class attempts to activate an endpoint by POSTing a JSON Object
	 * containing the Credentials Required as they were filled by the user to
	 * endpoint/<canonical_name>/activate. It informs the user of the result
	 * with a Toast message.
	 * 
	 * @author christos
	 * 
	 */
	public class Activate extends
			AsyncTask<JSONObject, Void, ArrayList<String>> {

		String mLocalNode;
		String mEndpoint;

		public Activate(String node, String endpoint) {
			this.mLocalNode = node;
			this.mEndpoint = endpoint;

		}

		@Override
		protected ArrayList<String> doInBackground(JSONObject... args) {

			ArrayList<String> mResult = new ArrayList<String>();

			// The endpoint's canonical name is split into user's name and
			// endpoint's name in order to replace
			// "#" with "%23" for encoding reasons
			String mDelims = "#";
			String[] mTokens = mEndpoint.split(mDelims);

			Result mQueryResult;
			try {
				JSONObject mRequirementsJsonObject = args[0];
				
				//The user's activate request is POSTed to the API along with the requirements JSON Object
				mQueryResult = sClient.postResult("/endpoint/" + mTokens[0]
						+ "%23" + mTokens[1] + "/activate",
						mRequirementsJsonObject);
				JSONObject mJsonObject = mQueryResult.document;

				if (!mJsonObject.getString("DATA_TYPE").equals(
						"activation_result")) {
					mResult.add(getString(R.string.error));
					mResult.add(getString(R.string.error));
					return mResult;
				}

				//The returned from the API message and code are retrieved and returned
				mResult.add(mJsonObject.getString("code"));
				mResult.add(mJsonObject.getString("message"));

			} catch (MalformedURLException e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.getMessage());
			} catch (IOException e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.getMessage());
			} catch (GeneralSecurityException e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.getMessage());
			} catch (JSONException e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.getMessage());
			} catch (APIError e) {

				e.printStackTrace();
				mResult.add(getString(R.string.error));
				mResult.add(e.message);

			}

			return mResult;
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {

			super.onPostExecute(result);

			//The user is informed of their request's outcome with a Toast message 
			makeToast(result.get(1), Toast.LENGTH_LONG);

		}

	}

	/**
	 * Android build-in function that we override in order to save data when the
	 * activity is temporarily destroyed
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putStringArrayList("endpoints_list", mEndpoints);
		outState.putString("path_a", mEndpointACurrentPath);
		outState.putString("path_b", mEndpointBCurrentPath);
		outState.putString("path_a_text_view", mPathATextView.getText()
				.toString());
		outState.putString("path_b_text_view", mPathBTextView.getText()
				.toString());
		outState.putStringArrayList("selected_files_a",
				(ArrayList<String>) mSelectedFilesA);
		outState.putStringArrayList("selected_files_b",
				(ArrayList<String>) mSelectedFilesB);
		outState.putString("endpoint_a", mSelectEndpointAButton.getText()
				.toString());
		outState.putString("endpoint_b", mSelectEndpointBButton.getText()
				.toString());
		outState.putStringArrayList("dir_a", mFileListA);
		outState.putStringArrayList("dir_b", mFileListB);
		outState.putSerializable("filesizeA", (Serializable) mFilesSizeA);
		outState.putSerializable("filesizeB", (Serializable) mFilesSizeB);
	}
}
