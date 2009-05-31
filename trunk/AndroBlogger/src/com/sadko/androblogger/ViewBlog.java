package com.sadko.androblogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.data.TextContent;
import com.sadko.androblogger.util.Alert;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class ViewBlog extends ListActivity {
	private final String TAG = "ViewBlog";
	public static Feed resultFeed = null;
	public static Entry currentEntry = null;
	private int selectedItemId = -1;
	int viewStatus = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewblog);

		resultFeed = MainActivity.resultFeed;
		Log.i(TAG, "ResultFeed obtained from MainActivity");

		int w = this.getWindow().getWindowManager().getDefaultDisplay()
				.getWidth() - 12;
		((Button) this.findViewById(R.id.BackToMainActivity)).setWidth(w / 2);
		((Button) this.findViewById(R.id.Details)).setWidth(w / 2);

		TextView blogTitle = (TextView) ViewBlog.this
				.findViewById(R.id.BlogTitle);
		blogTitle.setText(resultFeed.getTitle().getPlainText());

		List<Map<String, Object>> resourceNames = new ArrayList<Map<String, Object>>();
		Map<String, Object> data;
		int maxCharTitle = 22;
		int maxCharContent = 30;
		if(this.getWindow().getWindowManager().getDefaultDisplay().getOrientation()==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
			maxCharTitle = 40;
			maxCharContent = 50;
		}else if(this.getWindow().getWindowManager().getDefaultDisplay().getOrientation()==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
			maxCharTitle = 22;
			maxCharContent = 30;
		}
		for (int j = 0; j < resultFeed.getEntries().size(); j++) {
			data = new HashMap<String, Object>();
			Entry entry = resultFeed.getEntries().get(j);
			try {
				String truncatedTitle = null;
				if(entry.getTitle().getPlainText().length()>maxCharTitle){
					truncatedTitle = entry.getTitle().getPlainText().substring(0, maxCharTitle)+"...";
				} else{
					truncatedTitle = entry.getTitle().getPlainText();
				}
				data.put("line1", truncatedTitle);
				String truncatedContent = null;
				if(((TextContent) entry.getContent()).getContent().getPlainText().length()>maxCharContent){
					truncatedContent = ((TextContent) entry.getContent()).getContent().getPlainText().substring(0, maxCharContent)+"...";
				} else{
					truncatedContent = ((TextContent) entry.getContent()).getContent().getPlainText();
				}
				
				data.put("line2", truncatedContent);
				
				data.put("line3", entry.getUpdated().toStringRfc822()
						.substring(
								0,
								entry.getUpdated().toStringRfc822()
										.length() - 5));
				resourceNames.add(data);
			} catch (Resources.NotFoundException nfe) {
				Log.e(TAG, "NotFoundException "+nfe.getMessage());
			}
		}

		SimpleAdapter notes = new SimpleAdapter(this, resourceNames,
				R.layout.row, new String[]{"line1", "line2", "line3"}, new int[]{
						R.id.text1, R.id.text2, R.id.text3});
		setListAdapter(notes);

		this.findViewById(R.id.BackToMainActivity).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(ViewBlog.this, MainActivity.class);
						startActivity(i);
						finish();
					}
				});
		ListView currentListView = getListView();
		currentListView.setOnItemSelectedListener(new OnItemSelectedListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onItemSelected(AdapterView parent, View v,
					int position, long id) {
				Log.d(TAG, "Selected: " + id + " element");
				selectedItemId = (int) id;
			}

			@SuppressWarnings("unchecked")
			@Override
			public void onNothingSelected(AdapterView parent) {
				Log.d(TAG, "Selected: -1 element");
			}
		});
		this.findViewById(R.id.Details).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (selectedItemId >= 0) {
							currentEntry = resultFeed.getEntries().get(
									selectedItemId);
							Intent i = new Intent(ViewBlog.this, ViewPost.class);
							startActivity(i);
							finish();
						} else {
							Alert.showAlert(ViewBlog.this, "Nothing selected",
									"Please select some post");
						}
					}
				});
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent(ViewBlog.this, MainActivity.class);
			startActivity(i);
			finish();
			return true;
		}
		return false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		currentEntry = resultFeed.getEntries().get((int) id);
		Intent i = new Intent(ViewBlog.this, ViewPost.class);
		startActivity(i);
		finish();
	}
}