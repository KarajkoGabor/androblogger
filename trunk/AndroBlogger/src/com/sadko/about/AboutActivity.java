/*
 * Copyright (C) 2009 Sadko Mobile
 * www.sadko.mobi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");

 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0

 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,

 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 */

package com.sadko.about;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.sadko.androblogger.MainActivity;
import com.sadko.androblogger.R;
import com.sadko.androblogger.util.Alert;
import com.sadko.bursaq.BursaqConstants;
import com.sadko.bursaq.GotoBursaqActivity;

/**
 * About dialog with Bursaq payment option integrated
 * 
 * @author benderamp
 * 
 */
public class AboutActivity extends Activity {

	private static final String SEARCH_MARKET_COMPONENT_NAMESPACE = "com.android.vending";
	private static final String SEARCH_MARKET_COMPONENT_CLASS = "com.android.vending.SearchAssetListActivity";
	private static final String SEARCH_QUERY_PUBLISHER = "pub:\"Sadko Mobile\"";

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.about);

		final TextView appNameTxt = (TextView) findViewById(R.id.about_appname_txt);
		appNameTxt.setText(" " + getPackageVersion());

		if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			MarginLayoutParams mparams = (MarginLayoutParams) ((LinearLayout) this
					.findViewById(R.id.LinearLayoutForDevelopers))
					.getLayoutParams();
			mparams.leftMargin = 80;
			((LinearLayout) this.findViewById(R.id.LinearLayoutForDevelopers))
					.setLayoutParams(mparams);
			((LinearLayout) this.findViewById(R.id.LinearLayoutForDevelopers))
					.setOrientation(LinearLayout.HORIZONTAL);
		} else {
			MarginLayoutParams mparams = (MarginLayoutParams) ((LinearLayout) this
					.findViewById(R.id.LinearLayoutForDevelopers))
					.getLayoutParams();
			mparams.leftMargin = 100;
			((LinearLayout) this.findViewById(R.id.LinearLayoutForDevelopers))
					.setLayoutParams(mparams);
			((LinearLayout) this.findViewById(R.id.LinearLayoutForDevelopers))
					.setOrientation(LinearLayout.VERTICAL);
		}

		List<Map<String, Object>> resourceNames = new ArrayList<Map<String, Object>>();
		Map<String, Object> data;
		data = new HashMap<String, Object>();
		try {
			data.put("line", getString(R.string.about_donate));
			data.put("img", R.drawable.donate);
			resourceNames.add(data);
		} catch (NullPointerException e) {
		}
		data = new HashMap<String, Object>();
		try {
			data.put("line", getString(R.string.about_goto_project_page));
			data.put("img", R.drawable.homepage);
			resourceNames.add(data);
		} catch (NullPointerException e) {
		}
		data = new HashMap<String, Object>();
		try {
			data.put("line", getString(R.string.about_more_content));
			data.put("img", R.drawable.more);
			resourceNames.add(data);
		} catch (NullPointerException e) {
		}

		final ListView actionsLst = (ListView) findViewById(R.id.about_actions_lst);
		actionsLst.setAdapter(new SimpleAdapter(this, resourceNames,
				R.layout.about_action_list_item, new String[]{"line", "img"},
				new int[]{R.id.text, R.id.img}));

		actionsLst
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView parent, View v,
							int position, long id) {
						if (position == 0) {
							donate();
						} else if (position == 1) {
							goToProjectPage();
						} else {
							getMoreContent();
						}
					}
				});
	}

	/**
	 * Get application version
	 * 
	 * @return
	 */
	private String getPackageVersion() {
		String version = "";

		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = pi.versionName;
		} catch (NameNotFoundException e) {
		}
		return version;
	}

	/**
	 * Donate to project authors using Bursaq mobile payments manager
	 */
	private void donate() {
		final Intent intent = new Intent();
		intent.setClass(this, GotoBursaqActivity.class);

		// set services to show in Bursaq
		final String bursaqPubName = getString(R.string.bursaq_publisher);
		final String bursaqAppName = getString(R.string.bursaq_appname);
		final String bursaqDonate1 = getString(R.string.bursaq_donate1);
		final String bursaqDonate2 = getString(R.string.bursaq_donate2);

		intent.putExtra(BursaqConstants.PARAM_PUBLISHER, bursaqPubName);
		intent.putExtra(BursaqConstants.PARAM_PRODUCT_NAME, bursaqAppName);
		intent.putExtra(BursaqConstants.PARAM_SERVICE_NAMES, new String[]{
				bursaqDonate1, bursaqDonate2});

		startActivity(intent);
	}

	/**
	 * Open project web page in web browser
	 * 
	 * @param context
	 */
	private void goToProjectPage() {
		final String url = getString(R.string.about_project_page);
		final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));
		startActivity(intent);
	}

	/**
	 * Find more content by Sadko Mobile on the market
	 * 
	 * @param context
	 */
	private void getMoreContent() {
		final Intent intent = new Intent(Intent.ACTION_SEARCH);
		intent.setComponent(new android.content.ComponentName(
				SEARCH_MARKET_COMPONENT_NAMESPACE,
				SEARCH_MARKET_COMPONENT_CLASS));
		intent
				.putExtra(android.app.SearchManager.QUERY,
						SEARCH_QUERY_PUBLISHER);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Alert.showAlert(this, "Search is impossible",
					"Android Market is not available");
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent(AboutActivity.this, MainActivity.class);
			startActivity(i);
			finish();
			return true;
		}
		return false;
	}
}