package org.globus.globustransfer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.globus.globustransfer.R.color;
import org.globusonline.transfer.APIError;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient.Result;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MonitorActivity extends Activity {

	private static JSONTransferAPIClient sClient;
	protected String mUsername;
	protected String mSamlCookie;
	private TextView mTitleTextView;
	private ListView mTasksListView;
	private JSONArray mTasksJsonArray;
	private List<String> mTasksList;
	private Context mContext;
	private TextView mLoadingTextView;
	private ProgressBar mLoadingProgressBar;
	private Dialog mSettingsDialog;
	private RadioButton mTenRadioButton, mTwentyRadioButton, mFiftyRadioButton,
			mHundredRadioButton;
	private RadioGroup mNumberOfTasksRadioGroup;
	private Button mSettingsButtonOk;
	private SharedPreferences mSharedPreferences;
	private CheckBox mActiveCheckBox, mInactiveCheckBox, mFailedCheckBox,
			mSucceededCheckBox;
	private Dialog mTransferDetailsDialog;
	private Button mCancelTaskButton;
	private String mTempNumberOfTasks;
	private Boolean mTempSucceeded, mTempFailed, mTempActive, mTempInactive;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_monitor);

		// The username and cookie are retrieved from the Intent
		Intent mIntent = getIntent();
		Bundle mInfo = mIntent.getExtras();
		mUsername = mInfo.getString("username");
		mSamlCookie = mInfo.getString("samlCookie");

		mTitleTextView = (TextView) findViewById(R.id.monitor_title);
		mTitleTextView.setText(String.format("%s's tasks", mUsername));
		mTasksListView = (ListView) findViewById(R.id.tasks_list);
		mTasksList = new ArrayList<String>();
		mLoadingTextView = (TextView) findViewById(R.id.loading_tasks_text_view);
		mLoadingProgressBar = (ProgressBar) findViewById(R.id.loading_tasks_progress_bar);
		mContext = this;

		// The Settings Dialog is initialized
		mSettingsDialog = new Dialog(this);
		mSettingsDialog.setCanceledOnTouchOutside(false);
		mSettingsDialog.setCancelable(false);
		mSettingsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mSettingsDialog.setContentView(R.layout.monitor_settings);
		mTenRadioButton = (RadioButton) mSettingsDialog
				.findViewById(R.id.radio_10);
		mTwentyRadioButton = (RadioButton) mSettingsDialog
				.findViewById(R.id.radio_20);
		mFiftyRadioButton = (RadioButton) mSettingsDialog
				.findViewById(R.id.radio_50);
		mHundredRadioButton = (RadioButton) mSettingsDialog
				.findViewById(R.id.radio_100);
		mNumberOfTasksRadioGroup = (RadioGroup) mSettingsDialog
				.findViewById(R.id.radioGroup1);
		mActiveCheckBox = (CheckBox) mSettingsDialog
				.findViewById(R.id.checkBox_active);
		mInactiveCheckBox = (CheckBox) mSettingsDialog
				.findViewById(R.id.checkBox_inactive);
		mSucceededCheckBox = (CheckBox) mSettingsDialog
				.findViewById(R.id.checkBox_succeeded);
		mFailedCheckBox = (CheckBox) mSettingsDialog
				.findViewById(R.id.checkBox_failed);

		// SharedPreferences are initialized in order to be used for permanent
		// storage of information
		mSharedPreferences = getSharedPreferences(
				getString(R.string.preferences_name), MODE_PRIVATE);

		mActiveCheckBox.setOnCheckedChangeListener(new CheckBoxListener());
		mInactiveCheckBox.setOnCheckedChangeListener(new CheckBoxListener());
		mFailedCheckBox.setOnCheckedChangeListener(new CheckBoxListener());
		mSucceededCheckBox.setOnCheckedChangeListener(new CheckBoxListener());

		// Previously stored preferences are retrieved
		String selection = mSharedPreferences
				.getString("Number_of_tasks", "10");

		if (selection.contentEquals("10")) {
			mTenRadioButton.setChecked(true);
		} else if (selection.contentEquals("20")) {
			mTwentyRadioButton.setChecked(true);
		} else if (selection.contentEquals("50")) {
			mFiftyRadioButton.setChecked(true);
		} else if (selection.contentEquals("100")) {
			mHundredRadioButton.setChecked(true);
		}

		if (mSharedPreferences.getBoolean("active", true)) {
			mActiveCheckBox.setChecked(true);
		} else {
			mActiveCheckBox.setChecked(false);
		}
		if (mSharedPreferences.getBoolean("inactive", true)) {
			mInactiveCheckBox.setChecked(true);
		} else {
			mInactiveCheckBox.setChecked(false);
		}

		if (mSharedPreferences.getBoolean("failed", true)) {
			mFailedCheckBox.setChecked(true);
		} else {
			mFailedCheckBox.setChecked(false);
		}
		if (mSharedPreferences.getBoolean("succeeded", true)) {
			mSucceededCheckBox.setChecked(true);
		} else {
			mSucceededCheckBox.setChecked(false);
		}

		mSettingsButtonOk = (Button) mSettingsDialog
				.findViewById(R.id.ok_button);
		mTasksListView.setOnItemClickListener(new ListListener());

		// A Listener is assigned to the Number of Tasks Radio Group in the
		// Settings menu
		mNumberOfTasksRadioGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {

						// The number of tasks selected by the user is
						// automatically permanently saved using Shared
						// Preferences
						SharedPreferences.Editor editor = mSharedPreferences
								.edit();

						switch (checkedId) {
						case R.id.radio_10:
							editor.putString("Number_of_tasks", "10");
							break;
						case R.id.radio_20:
							editor.putString("Number_of_tasks", "20");
							break;
						case R.id.radio_50:
							editor.putString("Number_of_tasks", "50");
							break;
						case R.id.radio_100:
							editor.putString("Number_of_tasks", "100");
							break;
						default:
							break;

						}
						editor.commit();
					}
				});

		mSettingsButtonOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mSettingsDialog.dismiss();

				String mNewNumberOfTasks = mSharedPreferences.getString(
						"Number_of_tasks", "10");
				Boolean mNewTasksActive = mSharedPreferences.getBoolean(
						"active", true);
				Boolean mNewTaskInactive = mSharedPreferences.getBoolean(
						"inactive", true);
				Boolean mNewTaskFailed = mSharedPreferences.getBoolean(
						"failed", true);
				Boolean mNewTaskSucceeded = mSharedPreferences.getBoolean(
						"succeeded", true);

				// If any of the Settings has changed, the Tasks Lists is
				// reloaded
				if (mNewNumberOfTasks != mTempNumberOfTasks
						|| mNewTasksActive != mTempActive
						|| mNewTaskInactive != mTempInactive
						|| mNewTaskFailed != mTempFailed
						|| mNewTaskSucceeded != mTempSucceeded) {
					new GetTasksList().execute();
				}
			}

		});

		try {

			// Creates a client to communicate with the GO API
			sClient = new JSONTransferAPIClient(mUsername, null, mSamlCookie);
		} catch (Exception e) {
			e.printStackTrace();
		}

		new GetTasksList().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// When the menu Button is pressed a Menu Window appears
		getMenuInflater().inflate(R.menu.monitor, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.monitor_settings:

			// The permanently saved settings are retrieved before the Settings
			// window appears
			mTempActive = mSharedPreferences.getBoolean("active", true);
			mTempInactive = mSharedPreferences.getBoolean("inactive", true);
			mTempSucceeded = mSharedPreferences.getBoolean("succeeded", true);
			mTempFailed = mSharedPreferences.getBoolean("failed", true);
			mTempNumberOfTasks = mSharedPreferences.getString(
					"Number_of_tasks", "10");

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
	 */
	public void makeToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	/**
	 * It refreshes the contents of the Tasks List by calling the execute
	 * function of the getTasksList class
	 * 
	 * @param view
	 *            The current View
	 */
	public void refresh(View view) {

		new GetTasksList().execute();

	}

	/**
	 * It creates a Dialog asking the user if they really want to cancel the
	 * task. If the user clicks "YES" the execute function of the CancelTask
	 * class is called. If they click "NO" the dialog disappears and nothing
	 * happens.
	 * 
	 */
	private void cancelTask() {
		new AlertDialog.Builder(this)
				.setCancelable(false)
				.setMessage(R.string.cancel_task_question)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								mCancelTaskButton
										.setBackgroundResource(color.android_dark_red);
								TextView tv = (TextView) mTransferDetailsDialog
										.findViewById(R.id.id_value_1);
								String id = tv.getText().toString();
								new CancelTask().execute(id);
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								mCancelTaskButton
										.setBackgroundResource(color.android_dark_red);

							}
						}).show();

	}

	/**
	 * This class attempts to get a list with the tasks submitted by the user by
	 * sending a GET query to /task_list along with some request parameters
	 * regarding the number, the type and the status of the tasks we are
	 * interested in. A progress bar appears while communicating with the
	 * server.If there is any type of error in retrieving the tasks the user in
	 * notified with a Toast message.
	 * 
	 * @author christos
	 * 
	 */
	private class GetTasksList extends AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {

			super.onPreExecute();

			// The list is cleared
			mTasksListView.setAdapter(null);
			mTasksList.clear();

			// The loading progress bar and text appear
			mLoadingProgressBar.setVisibility(View.VISIBLE);
			mLoadingTextView.setVisibility(View.VISIBLE);
		}

		@Override
		protected String doInBackground(String... arg0) {

			String mResult = null;

			// This filter is introduced so that not only transfer tasks appear
			// but deletion tasks as well
			String mFilter = "type:TRANSFER,DELETE/status:";
			Map<String, String> mRequestParams = new HashMap<String, String>();
			String mNumberOfTasks;

			// An extra filter is added which defines the number of tasks that
			// will appear on the screen, based on user's settings
			mNumberOfTasks = mSharedPreferences.getString("Number_of_tasks",
					"10");

			// This filter defines the type of tasks that will appear, based on
			// user's settings
			if (mSharedPreferences.getBoolean("active", true)) {
				mFilter = mFilter.concat("ACTIVE,");
			}

			if (mSharedPreferences.getBoolean("inactive", true)) {
				mFilter = mFilter.concat("INACTIVE,");
			}

			if (mSharedPreferences.getBoolean("succeeded", true)) {
				mFilter = mFilter.concat("SUCCEEDED,");
			}
			if (mSharedPreferences.getBoolean("failed", true)) {
				mFilter = mFilter.concat("FAILED,");
			}

			if (mFilter.endsWith(",")) {
				mFilter = mFilter.substring(0, mFilter.length() - 1);
				mRequestParams.put("filter", mFilter);
			} else {
				// If no category of tasks is selected, this trick is used in
				// order to always get zero number of tasks
				mRequestParams.put("filter", "username:NOT" + mUsername);
			}
			mRequestParams.put("limit", mNumberOfTasks);

			Result mQueryResult;
			try {

				// The get request is send along with all the relative filters
				// to the API
				mQueryResult = sClient.getResult("/task_list", mRequestParams);

				JSONObject jO = mQueryResult.document;

				if (!jO.getString("DATA_TYPE").equals("task_list")) {
					return null;
				}

				// The "DATA" JSONArray is retrieved from the returned JSON
				// Object
				mTasksJsonArray = jO.getJSONArray("DATA");

				// A task list is created with the IDs of all the tasks
				for (int i = 0; i < mTasksJsonArray.length(); i++) {

					String taskId = mTasksJsonArray.getJSONObject(i).getString(
							"task_id");
					mTasksList.add(taskId);

				}

				// If no error occurs, an "OK" string is returned
				mResult = "OK";

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

			if (!result.contentEquals("OK")) {

				// If any kind of error occurs, a toast message informs the user
				// of it
				makeToast(result);
			} else {

				// The tasks list is loaded with the tasks retrieved through the
				// API
				ArrayAdapter<String> adapter = new CustomAdapter(mContext,
						android.R.layout.simple_list_item_1, mTasksList);
				mTasksListView.setAdapter(adapter);

			}

			// The loading progress bar and text disappear from the screen
			mLoadingProgressBar.setVisibility(View.GONE);
			mLoadingTextView.setVisibility(View.GONE);

		}

	}

	/**
	 * 
	 * This Adapter is a custom Adapter used for populating the List View
	 * containing the user's tasks. In every row of the List it puts the label (or
	 * id) of the task along with an icon indicating its status and a fraction
	 * indicating how many of its subtasks have been completed.
	 * 
	 * @author christos
	 * 
	 */
	private class CustomAdapter extends ArrayAdapter<String> {

		private Context mLocalContext;

		public CustomAdapter(Context context, int textViewResourceId,
				List<String> objects) {
			super(context, textViewResourceId, objects);
			this.mLocalContext = context;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			String mId = null;
			String mStatus = null;
			JSONObject mTask;
			String mLabel = null;
			String mSubtasks = "0";
			String mSubtasksSucceeded = "0";
			String mProgress = null;

			try {
				// The various Text Fields are filled with text
				mTask = mTasksJsonArray.getJSONObject(position);
				mId = mTask.getString("task_id");
				mStatus = mTask.getString("status");
				mLabel = mTask.getString("label");
				mSubtasks = mTask.getString("subtasks_total");
				mSubtasksSucceeded = mTask.getString("subtasks_succeeded");
			} catch (JSONException e) {

				e.printStackTrace();
				makeToast(e.getMessage());
			}
			View mRow = convertView;

			if (mRow == null) {
				LayoutInflater vi = (LayoutInflater) mLocalContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				mRow = vi.inflate(R.layout.tasks_list_view, null);
			}

			TextView mTaskIdTextView = (TextView) mRow
					.findViewById(R.id.task_id);
			ImageView mStatusImagView = (ImageView) mRow
					.findViewById(R.id.task_status);
			TextView mProgressTextView = (TextView) mRow
					.findViewById(R.id.progress);

			// The appropriate icon is set according to the task's status
			if (mStatus.contentEquals("ACTIVE")) {
				mStatusImagView.setImageResource(R.drawable.inprogress);
			} else if (mStatus.contentEquals("SUCCEEDED")) {
				mStatusImagView.setImageResource(R.drawable.completed);
			} else if (mStatus.contentEquals("FAILED")) {
				mStatusImagView.setImageResource(R.drawable.error);
			} else if (mStatus.contentEquals("INACTIVE")) {
				mStatusImagView.setImageResource(R.drawable.inactive);
			}

			// If the task has a label, the label is used instead of the task's
			// long and bad looking id
			if (!mLabel.contentEquals("null")) {
				mId = mLabel;

			}

			// A string is formatted to show the progress of the task in the
			// form of a fraction
			// Number_of_tasks_succeeded/Number_of_tasks_in_total
			mProgress = String.format(Locale.ENGLISH, "%s/%s ",
					mSubtasksSucceeded, mSubtasks);
			mProgressTextView.setText(mProgress);

			mTaskIdTextView.setText(mId);
			return mRow;
		}

	}

	/**
	 * This class creates a window which informs the user with details of the
	 * task they clicked on, like source endpoint, destination endpoint, status,
	 * submission time, deadline time, status of its subtasks etc. A Cancel
	 * Button also appears if the task's status is either "ACTIVE" or "INACTIVE"
	 * 
	 * @author christos
	 * 
	 */
	private class ListListener implements OnItemClickListener {

		JSONObject mTask;
		String mId = null;
		String mStatus = null;
		String mLabel = null;
		String mNiceStatus = null;
		String mType = null;
		String mRequestTime = null;
		String mDeadline = null;
		String mCompletionTime = null;
		String mSubtasksSucceeded = null;
		String mSubtasksFailed = null;
		String mSubtasksPending = null;
		String mSubtasksRetrying = null;
		String mSubtasksExpired = null;
		String mSubtasksSkipped = null;
		String mSubtasksCancelled = null;
		String mSourceEndpoint = null;
		String mDestinationEndpoint = null;

		Button mOkButton;
		ImageView mStatusImageView;
		TextView mStatusTextView, mIdTextView, mOriginEndpointTextView,
				mDestinationEndpointTextView, mLabelTextView,
				mRequestTimeTextView, mDeadlineTextView,
				mCompletionTimeTextView, mTypeTextView;
		TextView mFailedSubtasksTextView, mPendingSubtasksTextView,
				mRetryingSubtasksTextView, mExpiredSubtasksTextView,
				mSkippedSubtasksTextView, mCancelledSubtasksTextView,
				mSucceededSubtasksTextView;

		@Override
		public void onItemClick(AdapterView parent, View arg1, int position,
				long id_of_task) {

			try {
				// The task with the nth position on the JSONArray is retrieved
				mTask = mTasksJsonArray.getJSONObject(position);

				// All the relevant information about the task is retrieved
				mId = mTask.getString("task_id");
				mStatus = mTask.getString("status");
				mLabel = mTask.getString("label");
				mSourceEndpoint = mTask.getString("source_endpoint");
				mDestinationEndpoint = mTask.getString("destination_endpoint");

				mRequestTime = mTask.getString("request_time");
				mDeadline = mTask.getString("deadline");
				mCompletionTime = mTask.getString("completion_time");

				mSubtasksSucceeded = mTask.getString("subtasks_succeeded");
				mSubtasksFailed = mTask.getString("subtasks_failed");
				mSubtasksPending = mTask.getString("subtasks_pending");
				mSubtasksRetrying = mTask.getString("subtasks_retrying");
				mSubtasksExpired = mTask.getString("subtasks_expired");
				mSubtasksSkipped = mTask.getString("files_skipped");
				mSubtasksCancelled = mTask.getString("subtasks_canceled");
				mNiceStatus = mTask.getString("nice_status");
				mType = mTask.getString("type");

			} catch (JSONException e) {
				e.printStackTrace();
				makeToast(e.getMessage());
			}

			// The dialog window which contains all the details of the task is
			// created
			mTransferDetailsDialog = new Dialog(mContext);
			mTransferDetailsDialog.setCanceledOnTouchOutside(false);
			mTransferDetailsDialog
					.requestWindowFeature(Window.FEATURE_NO_TITLE);
			mTransferDetailsDialog.setContentView(R.layout.task_details);
			mOkButton = (Button) mTransferDetailsDialog
					.findViewById(R.id.ok_button);

			mStatusTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.status_value);

			mIdTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.id_value_1);

			mLabelTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.label_value);

			mOriginEndpointTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.origin_value);

			mDestinationEndpointTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.destination_value);

			mStatusImageView = (ImageView) mTransferDetailsDialog
					.findViewById(R.id.status_image);

			mRequestTimeTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.request_value);

			mDeadlineTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.deadline_value);

			mCompletionTimeTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.completion_value);

			mTypeTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.type_value);

			mCancelTaskButton = (Button) mTransferDetailsDialog
					.findViewById(R.id.cancel_task_button);

			mFailedSubtasksTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.failed_value);
			mPendingSubtasksTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.pending_value);
			mRetryingSubtasksTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.retrying_value);
			mExpiredSubtasksTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.expired_value);
			mSkippedSubtasksTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.skipped_value1);
			mCancelledSubtasksTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.cancelled_value);
			mSucceededSubtasksTextView = (TextView) mTransferDetailsDialog
					.findViewById(R.id.succeeded_value);

			// The values of all the text fields containing information about
			// the task are set
			mFailedSubtasksTextView.setText(mSubtasksFailed);
			mPendingSubtasksTextView.setText(mSubtasksPending);
			mRetryingSubtasksTextView.setText(mSubtasksRetrying);
			mExpiredSubtasksTextView.setText(mSubtasksExpired);
			mSkippedSubtasksTextView.setText(mSubtasksSkipped);
			mCancelledSubtasksTextView.setText(mSubtasksCancelled);
			mSucceededSubtasksTextView.setText(mSubtasksSucceeded);
			mOriginEndpointTextView.setText(mSourceEndpoint);
			mTypeTextView.setText(mType);
			mIdTextView.setText(mId);
			mRequestTimeTextView.setText(mRequestTime.substring(0, 19));
			mDeadlineTextView.setText(mDeadline.substring(0, 19));

			if (!mLabel.contentEquals("null")) {
				mLabelTextView.setText(mLabel);
			}

			if (!mNiceStatus.contentEquals("null")) {
				mStatusTextView.setText(mStatus + " (" + mNiceStatus + ")");
			} else {
				mStatusTextView.setText(mStatus);
			}

			if (!mDestinationEndpoint.contentEquals("null")) {
				mDestinationEndpointTextView.setText(mDestinationEndpoint);
			}

			if (!mCompletionTime.contentEquals("null")) {
				mCompletionTimeTextView.setText(mCompletionTime
						.substring(0, 19));
			}

			// The proper icon according to the task's status is set. If the
			// task is either Active or Inactive (pending) a Cancel Button
			// appears
			if (mStatus.contentEquals("ACTIVE")) {
				mStatusImageView.setImageResource(R.drawable.inprogress);
				mCancelTaskButton.setVisibility(View.VISIBLE);
			} else if (mStatus.contentEquals("SUCCEEDED")) {
				mStatusImageView.setImageResource(R.drawable.completed);
			} else if (mStatus.contentEquals("FAILED")) {
				mStatusImageView.setImageResource(R.drawable.error);
			} else if (mStatus.contentEquals("INACTIVE")) {
				mStatusImageView.setImageResource(R.drawable.inactive);
				mCancelTaskButton.setVisibility(View.VISIBLE);
			}

			mCancelTaskButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					// If the user click on the Cancel Task button, the
					// cancelTask() function is called
					mCancelTaskButton
							.setBackgroundResource(color.android_light_red);
					cancelTask();
				}
			});

			mTransferDetailsDialog.show();
			mOkButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					mTransferDetailsDialog.dismiss();

				}

			});

		}

	}

	/**
	 * This class is used to monitor the selection status of the checkboxes
	 * determining the tasks with statuses that we are interested in. When the
	 * checkboxes are checked or unchecked the boolean selection value is saved
	 * on the device's memory as SharedPreferences.
	 * 
	 * 
	 * @author christos
	 * 
	 */
	private class CheckBoxListener implements OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {

			int mId = buttonView.getId();
			SharedPreferences.Editor mEditor = mSharedPreferences.edit();
			switch (mId) {
			case R.id.checkBox_active:
				if (isChecked) {
					mEditor.putBoolean("active", true);
				} else {
					mEditor.putBoolean("active", false);
				}
				break;
			case R.id.checkBox_failed:
				if (isChecked) {
					mEditor.putBoolean("failed", true);
				} else {
					mEditor.putBoolean("failed", false);
				}
				break;
			case R.id.checkBox_inactive:
				if (isChecked) {
					mEditor.putBoolean("inactive", true);
				} else {
					mEditor.putBoolean("inactive", false);
				}
				break;
			case R.id.checkBox_succeeded:
				if (isChecked) {
					mEditor.putBoolean("succeeded", true);
				} else {
					mEditor.putBoolean("succeeded", false);
				}
				break;
			default:
				break;
			}
			mEditor.commit();
		}
	}

	/**
	 * 
	 * This class attempts to cancel an active task with POSTing an empty JSON
	 * Object to task/<task_id>/cancel. The user is informed for the query's
	 * result with a Toast message. If the task is cancelled successfully the
	 * settings window closes and the tasks list is updated.
	 * 
	 * @author christos
	 * 
	 */
	private class CancelTask extends AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {

			super.onPreExecute();

			// A toast message informs the user that cancel request has been
			// submitted
			makeToast(getString(R.string.cancel_request_submitted));

		}

		@Override
		protected String doInBackground(String... task) {

			String mResult = getString(R.string.error);

			// The task's id is retrieved from the corresponding String argument
			String mTaskName = task[0];

			Result mQueryResult;
			try {
				JSONObject mJsonObject = new JSONObject();

				// The request is POSTed to the API along with an empty JSON
				// Object
				mQueryResult = sClient.postResult("/task/" + mTaskName
						+ "/cancel", mJsonObject);

				mJsonObject = mQueryResult.document;

				if (!mJsonObject.getString("DATA_TYPE").equals("result")) {
					return mResult;
				}

				// The String message is retrieved from the returned JSON Object
				// and returned
				mResult = mJsonObject.getString("message");

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

			// The user is informed with a toast message of the outcome of their
			// request
			makeToast(result);

			// If the task is successfully cancelled, the task details dialog is
			// terminated and the task list is refreshed
			if (result.endsWith("successfully.")) {
				mTransferDetailsDialog.cancel();
				new GetTasksList().execute();
			}
		}

	}

}
