/* 
 * Copyright (C) 2008 OpenIntents.org
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

package com.sadko.androblogger;

import org.openintents.intents.FlashlightIntents;
import org.openintents.widget.ColorCircle;
import org.openintents.widget.ColorSlider;
import org.openintents.widget.OnColorChangedListener;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ColorPickerActivity extends Activity implements
		OnColorChangedListener {
	final static String TAG = "ColorPickerActivity";
	boolean isTextColor = true;
	int selectionStart = 0;
	int selectionEnd = 0;
	GoogleAnalyticsTracker tracker;
	ColorCircle mColorCircle;
	ColorSlider mSaturation;
	ColorSlider mValue;
	Intent mIntent;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.colorpicker);
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("UA-11702470-1", this);

		mIntent = getIntent();
		if (mIntent != null) {
			Bundle b = mIntent.getExtras();
			isTextColor = b.getBoolean("isTextColor");
			selectionStart = b.getInt("selStart");
			selectionEnd = b.getInt("selEnd");
		} else
			mIntent = new Intent();
		int color = mIntent.getIntExtra(FlashlightIntents.EXTRA_COLOR, 0);

		mColorCircle = (ColorCircle) findViewById(R.id.colorcircle);
		mColorCircle.setOnColorChangedListener(this);
		mColorCircle.setColor(color);

		mSaturation = (ColorSlider) findViewById(R.id.saturation);
		mSaturation.setOnColorChangedListener(this);
		mSaturation.setColors(color, 0xff000000);

		mValue = (ColorSlider) findViewById(R.id.value);
		mValue.setOnColorChangedListener(this);
		mValue.setColors(0xFFFFFFFF, color);
		int w = this.getWindow().getWindowManager().getDefaultDisplay()
				.getWidth() - 12;
		((Button) this.findViewById(R.id.BackToCreateBlogEntry))
				.setWidth(w / 2);
		this.findViewById(R.id.BackToCreateBlogEntry).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						Intent i = new Intent(ColorPickerActivity.this,
								CreateBlogEntry.class);
						startActivity(i);
						finish();
					}
				});
	}

	public int toGray(int color) {
		int a = Color.alpha(color);
		int r = Color.red(color);
		int g = Color.green(color);
		int b = Color.blue(color);
		int gray = (r + g + b) / 3;
		return Color.argb(a, gray, gray, gray);
	}

	public void onColorChanged(View view, int newColor) {
		if (view == mColorCircle) {
			mValue.setColors(0xFFFFFFFF, newColor);
			mSaturation.setColors(newColor, 0xff000000);
		} else if (view == mSaturation) {
			mColorCircle.setColor(newColor);
			mValue.setColors(0xFFFFFFFF, newColor);
		} else if (view == mValue) {
			mColorCircle.setColor(newColor);
		}
	}

	public void onColorPicked(View view, int newColor) {
		Intent i = new Intent(ColorPickerActivity.this, CreateBlogEntry.class);
		i.putExtra("isTextColor", isTextColor);
		i.putExtra("selStart", selectionStart);
		i.putExtra("selEnd", selectionEnd);
		i.putExtra("color", newColor);
		startActivity(i);
		finish();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent(ColorPickerActivity.this,
					CreateBlogEntry.class);
			startActivity(i);
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

}