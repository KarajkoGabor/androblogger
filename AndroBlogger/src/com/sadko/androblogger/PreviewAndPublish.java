package com.sadko.androblogger;

import java.io.IOException;
import java.sql.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.google.gdata.util.ServiceException;
import com.sadko.androblogger.db.BlogEntry;
import com.sadko.androblogger.db.DBAdapter;
import com.sadko.androblogger.editor.SpannableBufferHelper;
import com.sadko.androblogger.util.Alert;

public class PreviewAndPublish extends Activity implements View.OnClickListener {

	private static final String TAG = "PreviewAndPublish";
	private String title;
	private String content;
	private BlogEntry myEntry = null;
	private final String MSG_KEY = "value";
	int publishStatus = 0;
	private ProgressDialog publishProgress = null;
	private int attempt = 0;
	private DBAdapter mDbHelper;
	private static Cursor setting = null;

	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle content = msg.getData();
			String progressId = content.getString(MSG_KEY);
			if (progressId != null) {
				if (progressId.equals("1")) {
					publishProgress.setMessage("Preparing blog config...");
				} else if (progressId.equals("2")) {
					publishProgress.setMessage("Authenticating...");
				} else if (progressId.equals("3")) {
					publishProgress.setMessage("Contacting server...");
				} else if (progressId.equals("4")) {
					publishProgress.setMessage("Creating new entry...");
				} else if (progressId.equals("5")) {
					publishProgress.setMessage("Done...");
				}
			}
		}
	};

	final Runnable mPublishResults = new Runnable() {
		public void run() {
			showPublishedStatus();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.previewandpublish);
		mDbHelper = new DBAdapter(this);
		try {
			mDbHelper.open();
		} catch (SQLException e) {
			Log.e(TAG, "Database has not opened");
		}
		setting = mDbHelper.fetchSettindById(1);
		startManagingCursor(setting);
		Intent i = this.getIntent();
		if (i == null) {
			Alert.showAlert(this, "Intent data missing!",
					"The intent used to launch this Activity was null!");
			this.finish();
		}
		String[] titleAndContent = i.getStringArrayExtra("PostTitleAndContent");
		title = titleAndContent[0];
		content = titleAndContent[1];
		Log.i(TAG, "Title of post: " + title + ". Content of post: " + content);
		EditText textTitle = (EditText) this.findViewById(R.id.PreviewTitle);
		textTitle.setText(title);
		textTitle.setTextColor(Color.BLACK);
		textTitle.setEnabled(false);
		EditText textContent = (EditText) this
				.findViewById(R.id.PreviewContent);
		textContent.setText(content);
		textContent.setTextColor(Color.BLACK);
		textContent.setEnabled(false);

		Button publishButton = (Button) findViewById(R.id.Publish);

		int w = this.getWindow().getWindowManager().getDefaultDisplay()
				.getWidth() - 12;
		((Button) this.findViewById(R.id.BackToCreateBlogEntry))
				.setWidth(w / 2);
		((Button) this.findViewById(R.id.Publish)).setWidth(w / 2);

		publishButton.setOnClickListener(this);
		this.findViewById(R.id.BackToCreateBlogEntry).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						Intent i = new Intent(PreviewAndPublish.this,
								CreateBlogEntry.class);
						String[] titleAndContent = new String[2];
						titleAndContent[0] = title;
						titleAndContent[1] = content;
						i.putExtra("PostTitleAndContent", titleAndContent);
						startActivity(i);
						finish();
					}
				});
		myEntry = new BlogEntry();
		myEntry.setBlogEntry(content);
		myEntry.setTitle(title);
		myEntry.setCreated(new Date(System.currentTimeMillis()));
	}

	@Override
	public void onClick(View v) {
		this.publishBlogEntry();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent(PreviewAndPublish.this, CreateBlogEntry.class);
			String[] titleAndContent = new String[2];
			titleAndContent[0] = title;
			titleAndContent[1] = content;
			i.putExtra("PostTitleAndContent", titleAndContent);
			startActivity(i);
			finish();
			return true;
		}
		return false;
	}

	private void publishBlogEntry() {
		final Activity thread_parent = this;
		publishProgress = ProgressDialog.show(this, "Publishing blog entry",
				"Starting to publish blog entry...");
		Thread publish = new Thread() {
			@SuppressWarnings("static-access")
			public void run() {
				Bundle status = new Bundle();
				Looper loop = mHandler.getLooper();
				loop.prepare();
				Message statusMsg = mHandler.obtainMessage();
				publishStatus = 0;
				status.putString(MSG_KEY, "1");
				statusMsg.setData(status);
				mHandler.sendMessage(statusMsg);
				boolean publishOk = false;
				BlogConfigBLOGGER.BlogInterfaceType typeEnum = BlogConfigBLOGGER
						.getInterfaceTypeByNumber(1);
				BlogInterface blogapi = null;
				blogapi = BlogInterfaceFactory.getInstance(typeEnum);
				Log.d(TAG, "Using interface type: " + typeEnum);
				blogapi.setInstanceConfig("");
				status.putString(MSG_KEY, "2");
				statusMsg = mHandler.obtainMessage();
				statusMsg.setData(status);
				mHandler.sendMessage(statusMsg);
				String auth_id = null;
				boolean authFlag = false;
				attempt = 0;
				while ((attempt <= MainActivity.AMOUNTOFATTEMPTS) && (!authFlag)) {
					try {
						auth_id = blogapi
								.getAuthId(
										setting
												.getString(setting
														.getColumnIndexOrThrow(DBAdapter.KEY_LOGIN)),
										setting
												.getString(setting
														.getColumnIndexOrThrow(DBAdapter.KEY_PASSWORD)));
						authFlag = true;
						attempt = 0;
					} catch (com.google.gdata.util.AuthenticationException e) {
						attempt++;
						Log.e(TAG, "AuthenticationException " + e.getMessage());
					}

				}
				publishStatus = 1;
				Log.d(TAG, "Got auth token:" + auth_id);
				publishStatus = 2;
				if (auth_id != null) {
					status.putString(MSG_KEY, "3");
					statusMsg = mHandler.obtainMessage();
					statusMsg.setData(status);
					mHandler.sendMessage(statusMsg);
					String postUri = null;
					authFlag = false;
					attempt = 0;
					while ((attempt <= MainActivity.AMOUNTOFATTEMPTS) && (!authFlag)) {
						try {
							postUri = blogapi.getPostUrl();
							authFlag = true;
							attempt = 0;
						} catch (ServiceException e) {
							Log.e(TAG, "ServiceException " + e.getMessage());
							attempt++;
						} catch (IOException e) {
							Log.e(TAG, "IOException " + e.getMessage());
							attempt++;
						}
					}
					SpannableBufferHelper helper = new SpannableBufferHelper();
					CharSequence cs = myEntry.getBlogEntry();
					EditText et = new EditText(thread_parent);
					et.setText(cs);
					Spannable spa = et.getText();
					spa.setSpan(cs, 0, 1, 1);
					String entry = helper.SpannableToXHTML(spa);
					status.putString(MSG_KEY, "4");
					statusMsg = mHandler.obtainMessage();
					statusMsg.setData(status);
					mHandler.sendMessage(statusMsg);
					authFlag = false;
					attempt = 0;
					while ((attempt <= MainActivity.AMOUNTOFATTEMPTS) && (!authFlag)) {
						try {
							publishOk = blogapi
									.createPost(
											thread_parent,
											auth_id,
											postUri,
											null,
											myEntry.getTitle(),
											null,
											entry,
											setting
													.getString(setting
															.getColumnIndexOrThrow(DBAdapter.KEY_LOGIN)),
											setting
													.getString(setting
															.getColumnIndexOrThrow(DBAdapter.KEY_PASSWORD)),
											myEntry.isDraft());
							authFlag = true;
							attempt = 0;
						} catch (ServiceException e) {
							Log.e(TAG, "ServiceException " + e.getMessage());
							attempt++;
						}
					}
				} else {
					publishStatus = 3;
				}
				status.putString(MSG_KEY, "5");
				statusMsg = mHandler.obtainMessage();
				statusMsg.setData(status);
				mHandler.sendMessage(statusMsg);
				if (publishOk) {
					Log.d(TAG, "Post published successfully!");
					publishStatus = 5;
				} else {
					Log.d(TAG, "Publishing of the post failed!");
					publishStatus = 4;
				}
				mHandler.post(mPublishResults);
			}
		};
		ConnectivityManager cm = (ConnectivityManager) PreviewAndPublish.this
				.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = cm.getActiveNetworkInfo();
		if (netinfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
			publish.start();
		} else {
			Alert.showAlert(PreviewAndPublish.this,
					"Network connection needed",
					"Please, connect your device to the Internet");
		}

		publishProgress.setMessage("Publishing in progress...");
	}
	private void showPublishedStatus() {
		publishProgress.dismiss();
		if (publishStatus == 5) {
			final Dialog dlg = new AlertDialog.Builder(PreviewAndPublish.this)
					.setIcon(com.sadko.androblogger.R.drawable.ic_dialog_alert)
					.setTitle("Publish status").setPositiveButton("OK", null)
					.setMessage("Published").create();
			dlg.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					Intent i = new Intent(PreviewAndPublish.this,
							MainActivity.class);
					startActivity(i);
					finish();
				}
			});
			dlg.show();
		} else {
			attempt = 0;
			Alert.showAlert(this, "Publish status", "Publish failed! (Code "
					+ publishStatus + ")");
		}
	}
}
