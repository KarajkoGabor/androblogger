package com.sadko.androblogger;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message; //import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.gdata.data.Feed;
import com.google.gdata.util.ServiceException;
import com.sadko.about.AboutActivity;
import com.sadko.androblogger.db.DBAdapter;
import com.sadko.androblogger.util.Alert;

public class MainActivity extends Activity {
	// final static String TAG = "MainActivity";
	private ProgressDialog viewProgress = null;
	private final String MSG_KEY = "value";
	public static Feed resultFeed = null;
	private DBAdapter mDbHelper;
	private static Cursor setting = null;
	int viewStatus = 0;
	public static final int AMOUNTOFATTEMPTS = 7;
	private int attempt = 0;
	GoogleAnalyticsTracker tracker;

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
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("UA-11702470-1", this);

		if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			((LinearLayout) this.findViewById(R.id.LayoutForLogo)).setPadding(
					0, 0, 0, 0);
			((LinearLayout) this.findViewById(R.id.LayoutForLogo))
					.setBackgroundDrawable(null);
			((LinearLayout) this.findViewById(R.id.LayoutForLogo))
					.removeAllViews();
		} else if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
		}

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
						mDbHelper = new DBAdapter(MainActivity.this);
						try {
							mDbHelper.open();
						} catch (SQLException e) {
							// Log.e(TAG, "Database has not opened");
						}
						setting = mDbHelper.fetchSettindById(1);
						startManagingCursor(setting);
						if (setting.getCount() != 0) {
							if ((setting
									.getString(
											setting
													.getColumnIndexOrThrow(DBAdapter.KEY_LOGIN))
									.length() == 0)
									&& (setting
											.getString(
													setting
															.getColumnIndexOrThrow(DBAdapter.KEY_PASSWORD))
											.length() == 0)) {
								mDbHelper.close();
								setting.close();
								Alert
										.showAlert(MainActivity.this,
												"Profile is not created",
												"Please, input 'login/password' in settings");
							} else {
								Intent i = new Intent(MainActivity.this,
										CreateBlogEntry.class);
								mDbHelper.close();
								setting.close();
								startActivity(i);
								finish();
							}
						} else {
							mDbHelper.close();
							setting.close();
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
						mDbHelper = new DBAdapter(MainActivity.this);
						try {
							mDbHelper.open();
						} catch (SQLException e) {
							// Log.e(TAG, "Database has not opened");
						}
						setting = mDbHelper.fetchSettindById(1);
						startManagingCursor(setting);
						if (setting.getCount() != 0) {
							if ((setting
									.getString(
											setting
													.getColumnIndexOrThrow(DBAdapter.KEY_LOGIN))
									.length() == 0)
									&& (setting
											.getString(
													setting
															.getColumnIndexOrThrow(DBAdapter.KEY_PASSWORD))
											.length() == 0)) {
								mDbHelper.close();
								setting.close();
								Alert
										.showAlert(MainActivity.this,
												"Profile is not created",
												"Please, input 'login/password' in settings");
							} else {
								mDbHelper.close();
								setting.close();
								viewBlogPosts();
							}
						} else {
							mDbHelper.close();
							setting.close();
							Alert
									.showAlert(MainActivity.this,
											"Profile is not created",
											"Please, input 'login/password' in settings");
						}

					}
				});

		this.findViewById(R.id.About).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final Intent intent = new Intent();
				intent.setClass(MainActivity.this, AboutActivity.class);
				startActivity(intent);
				finish();
			}
		});

		this.findViewById(R.id.Exit).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		tracker.stop();
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
				mDbHelper = new DBAdapter(MainActivity.this);
				try {
					mDbHelper.open();
				} catch (SQLException e) {
					// Log.e(TAG, "Database has not opened");
				}
				setting = mDbHelper.fetchSettindById(1);
				startManagingCursor(setting);
				String username = setting.getString(setting
						.getColumnIndexOrThrow(DBAdapter.KEY_LOGIN));
				String password = setting.getString(setting
						.getColumnIndexOrThrow(DBAdapter.KEY_PASSWORD));
				mDbHelper.close();
				setting.close();
				BlogInterface blogapi = null;
				BlogConfigBLOGGER.BlogInterfaceType typeEnum = BlogConfigBLOGGER
						.getInterfaceTypeByNumber(1);
				blogapi = BlogInterfaceFactory.getInstance(typeEnum);
				// Log.d(TAG, "Using interface type: " + typeEnum);
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
						// Log.e(TAG, "AuthenticationException " +
						// e.getMessage());
						attempt++;
					} catch (Exception e) {
						// Log.e(TAG, "Exception: " + e.getMessage());
						Alert
								.showAlert(MainActivity.this,
										"Network connection failed",
										"Please, check network settings of your device");
						finish();
					}

				}
				viewStatus = 1;
				// Log.d(TAG, "Got auth token:" + auth_id);
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
							// Log.i(TAG, "Blog entries successfully received");
							viewOk = true;
							authFlag = true;
							attempt = 0;
						} catch (ServiceException e) {
							attempt++;
							// Log.e(TAG,"ServiceException (getAllPosts(username, password))");
						} catch (IOException e) {
							attempt++;
							// Log.e(TAG,"Exception (getAllPosts(username, password))");
						} catch (Exception e) {
							// Log.e(TAG, "Exception: " + e.getMessage());
							Alert
									.showAlert(MainActivity.this,
											"Network connection failed",
											"Please, check network settings of your device");
							finish();
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
					// Log.d(TAG, "Success!");
					viewStatus = 5;
				} else {
					// Log.d(TAG, "Viewing of the blog failed!");
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
		viewThread.start();
		viewProgress.setMessage("Viewing in progress...");
	}

	private void showViewStatus() {
		viewProgress.dismiss();
		if (viewStatus != 5) {
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