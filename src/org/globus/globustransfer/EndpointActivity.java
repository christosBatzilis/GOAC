package org.globus.globustransfer;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class EndpointActivity extends ListActivity {

	private EditText mFilterTextEditText = null;
	private ArrayAdapter<String> mAdapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(LayoutParams.FLAG_NOT_TOUCH_MODAL,
				LayoutParams.FLAG_NOT_TOUCH_MODAL);
		getWindow().setFlags(LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

		setContentView(R.layout.activity_endpoint);
		ArrayList<String> mEndpointsList = new ArrayList<String>();

		// The endpoints' List is retrieved from the intent
		mEndpointsList = getIntent().getExtras().getStringArrayList(
				"endpointslist");
		mFilterTextEditText = (EditText) findViewById(R.building_list.search_box);

		// A text watcher is added
		mFilterTextEditText.addTextChangedListener(filterTextWatcher);

		// The List View is populated with the endpoints' List retrieved from
		// the Intent
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mEndpointsList);
		setListAdapter(mAdapter);

	}

	/**
	 * This Text Watcher is used to filter the endpoints that appear on the List
	 * as user types characters in the Search Box.
	 * 
	 * */
	private TextWatcher filterTextWatcher = new TextWatcher() {

		public void afterTextChanged(Editable s) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			mAdapter.getFilter().filter(s);
		}

	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mFilterTextEditText.removeTextChangedListener(filterTextWatcher);

	}

	/**
	 * When an endpoint is selected from the List, the endpoint's name along with
	 * a string stating whether the endpoint was selected from Button A or B are
	 * returned to the StartTransfer Activity through an Intent.
	 * 
	 **/
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		super.onListItemClick(l, v, position, id);
		Intent mIntent = getIntent();
		String mSelectedEndpoint = (String) getListAdapter().getItem(position);
		String mEndpointAorB = mIntent.getStringExtra("endId");
		mIntent.putExtra("endId", mEndpointAorB);
		mIntent.putExtra("endpoint", mSelectedEndpoint);
		setResult(RESULT_OK, mIntent);
		this.finish();

	}

	/**
	 * If the user presses the Back Button a simple Intent is returned without
	 * including any endpoint id. The StartTransfer Activity recognizes that and
	 * takes no action whatsoever.
	 * */
	@Override
	public void onBackPressed() {
		Log.d("CDA", "onBackPressed Called");
		Intent mIntent = getIntent();
		mIntent.putExtra("endpoint1", "sample");
		setResult(RESULT_OK, mIntent);
		this.finish();
	}

	@Override
	protected void onPause() {

		super.onPause();
		Intent mIntent = getIntent();
		mIntent.putExtra("endpoint1", "sample");
		setResult(RESULT_OK, mIntent);
		this.finish();
	}

	/**
	 * When the user click outside the Activity's area nothing happens (in order
	 * to avoid crushes).
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (MotionEvent.ACTION_OUTSIDE == event.getAction()) {

			return true;
		}
		return super.onTouchEvent(event);
	}

}
