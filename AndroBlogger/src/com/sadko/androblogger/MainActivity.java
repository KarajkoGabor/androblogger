package com.sadko.androblogger;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.gdata.data.Feed;
import com.google.gdata.util.ServiceException;
import com.sadko.androblogger.db.DBAdapter;
import com.sadko.androblogger.util.Alert;

public class MainActivity extends Activity {
	final static String TAG = "MainActivity";
	private ProgressDialog viewProgress = null;
	private final String MSG_KEY = "value";
	public static Feed resultFeed = null;
	private DBAdapter mDbHelper;
	private static Cursor setting = null;
	int viewStatus = 0;
	public static final int AMOUNTOFATTEMPTS = 5;
	private int attempt = 0;

	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle content = msg.getData();
			String progressId = content.getString(MSG_KEY);
			if (progressId != null) {
				if (progressId.equals("1")) {
					viewProgress.setMessage("Preparing blog config...");
				} else if (progressId.equals("2")) {
					viewProgress.setMessage("Authenticating...");
				} else if (progressId.equals("3")) {
					viewProgress.setMessage("Receiving blog entries...");
				} else if (progressId.equals("4")) {
					viewProgress.setMessage("Done...");
				}
			}
		}
	};

	final Runnable mViewResults = new Runnable() {
		public void run() {
			showViewStatus();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mDbHelper = new DBAdapter(this);
		try {
			mDbHelper.open();
		} catch (SQLException e) {
			Log.e(TAG, "Database has not opened");
		}
		setting = mDbHelper.fetchSettindById(1);
		startManagingCursor(setting);

		this.findViewById(R.id.Settings).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						Intent i = new Intent(MainActivity.this, Settings.class);
						startActivity(i);
						finish();
					}
				});

		this.findViewById(R.id.CreateNewPost).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						if (setting.getCount() != 0) {
							Intent i = new Intent(MainActivity.this,
									CreateBlogEntry.class);
							String[] s = {"", ""};
							i.putExtra("PostTitleAndContent", s);
							startActivity(i);
							finish();
						} else {
							Alert
									.showAlert(MainActivity.this,
											"Profile is not created",
											"Please, input 'login/password' in settings");
						}
					}
				});

		this.findViewById(R.id.ViewBlog).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						if (setting.getCount() != 0) {
							viewBlogPosts();
						} else {
							Alert
									.showAlert(MainActivity.this,
											"Profile is not created",
											"Please, input 'login/password' in settings");
						}

					}
				});

		this.findViewById(R.id.Exit).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}

	protected void viewBlogPosts() {
		viewProgress = ProgressDialog.show(MainActivity.this,
				"Viewing blog entries", "Starting to view blog entries...");

		Thread viewThread = new Thread() {
			public void run() {
				Bundle status = new Bundle();
				mHandler.getLooper();
				Looper.prepare();
				Message statusMsg = mHandler.obtainMessage();
				viewStatus = 0;
				status.putString(MSG_KEY, "1");
				statusMsg.setData(status);
				mHandler.sendMessage(statusMsg);
				boolean viewOk = false;
				String username = setting.getString(setting
						.getColumnIndexOrThrow(DBAdapter.KEY_LOGIN));
				String password = setting.getString(setting
						.getColumnIndexOrThrow(DBAdapter.KEY_PASSWORD));
				BlogInterface blogapi = null;
				BlogConfigBLOGGER.BlogInterfaceType typeEnum = BlogConfigBLOGGER
						.getInterfaceTypeByNumber(1);
				blogapi = BlogInterfaceFactory.getInstance(typeEnum);
				Log.d(TAG, "Using interface type: " + typeEnum);
				CharSequence postconfig = "";
				blogapi.setInstanceConfig(postconfig);
				status.putString(MSG_KEY, "2");
				statusMsg = mHandler.obtainMessage();
				statusMsg.setData(status);
				mHandler.sendMessage(statusMsg);
				String auth_id = null;
				boolean authFlag = false;
				attempt = 0;
				while ((attempt <= MainActivity.AMOUNTOFATTEMPTS)
						&& (!authFlag)) {
					try {
						auth_id = blogapi.getAuthId(username, password);
						authFlag = true;
						attempt = 0;
					} catch (com.google.gdata.util.AuthenticationException e) {
						Log.e(TAG, "AuthenticationException " + e.getMessage());
						attempt++;
					}
				}
				viewStatus = 1;
				Log.d(TAG, "Got auth token:" + auth_id);
				viewStatus = 2;
				if (auth_id != null) {
					status.putString(MSG_KEY, "3");
					statusMsg = mHandler.obtainMessage();
					statusMsg.setData(status);
					mHandler.sendMessage(statusMsg);
					authFlag = false;
					attempt = 0;
					while ((attempt <= MainActivity.AMOUNTOFATTEMPTS)
							&& (!authFlag)) {
						try {
							resultFeed = blogapi
									.getAllPosts(username, password);
							Log.i(TAG, "Blog entries successfully received");
							viewOk = true;
							authFlag = true;
							attempt = 0;
						} catch (ServiceException e) {
							attempt++;
							Log
									.e(TAG,
											"ServiceException (getAllPosts(username, password))");
						} catch (IOException e) {
							attempt++;
							Log
									.e(TAG,
											"Exception (getAllPosts(username, password))");
						}
					}
				} else {
					viewStatus = 3;
				}
				status.putString(MSG_KEY, "4");
				statusMsg = mHandler.obtainMessage();
				statusMsg.setData(status);
				mHandler.sendMessage(statusMsg);
				if (viewOk) {
					Log.d(TAG, "Success!");
					viewStatus = 5;
				} else {
					Log.d(TAG, "Viewing of the blog failed!");
					viewStatus = 4;
				}
				mHandler.post(mViewResults);

				if ((resultFeed != null) && (viewOk)) {
					Intent i = new Intent(MainActivity.this, ViewBlog.class);
					startActivity(i);
					finish();
				}
			}
		};
		ConnectivityManager cm = (ConnectivityManager) MainActivity.this
				.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = cm.getActiveNetworkInfo();
		if (netinfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
			viewThread.start();
		} else {
			Alert.showAlert(MainActivity.this, "Network connection needed",
					"Please, connect your device to the Internet");
		}
		viewProgress.setMessage("Viewing in progress...");
	}

	private void showViewStatus() {
		viewProgress.dismiss();
		if (viewStatus != 5) {
			/*
			 * Alert.showAlert(this, "Viewing failed", "Error code " +
			 * viewStatus + "\nTry again.");
			 */
			Alert.showAlert(this, "Viewing failed", "Error code " + viewStatus,
					"Try again", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							viewBlogPosts();
						}
					}, "Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
		}
	}
}