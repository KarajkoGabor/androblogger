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
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable; //import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.gdata.data.HtmlTextConstruct;
import com.google.gdata.util.ServiceException;
import com.sadko.androblogger.db.BlogEntry;
import com.sadko.androblogger.db.DBAdapter;
import com.sadko.androblogger.db.DBTextAdapter;
import com.sadko.androblogger.editor.SpannableBufferHelper;
import com.sadko.androblogger.util.Alert;

public class PreviewAndPublish extends Activity implements View.OnClickListener {

	// private static final String TAG = "PreviewAndPublish";
	private String title;
	private String content;
	private BlogEntry myEntry = null;
	private final String MSG_KEY = "value";
	int publishStatus = 0;
	private ProgressDialog publishProgress = null;
	private int attempt = 0;
	private DBAdapter mDbHelper;
	private static Cursor setting = null;
	private DBTextAdapter mDbTextHelper;
	private static Cursor post = null;
	WebView webview;
	GoogleAnalyticsTracker tracker;

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
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("UA-11702470-1", this);
		
		mDbTextHelper = new DBTextAdapter(this);
		try {
			mDbTextHelper.open();
		} catch (SQLException e) {
			// Log.e(TAG, "Database has not opened");
		}
		post = mDbTextHelper.fetchPostdById(1);
		startManagingCursor(post);
		if (post.getCount() != 0) {
			try {
				title = post.getString(post
						.getColumnIndexOrThrow(DBTextAdapter.KEY_TITLE));
				content = post.getString(post
						.getColumnIndexOrThrow(DBTextAdapter.KEY_CONTENT));
				// Log.i(TAG, "Title of post: " + title + ". Content of post: "+
				// content);
			} catch (IllegalArgumentException e) {
				// Log.e(TAG, "IllegalArgumentException (DataBase failed)");
			} catch (Exception e) {
				// Log.e(TAG, "Exception (DataBase failed)");
			}
		}
		mDbTextHelper.close();
		post.close();
		EditText textTitle = (EditText) this.findViewById(R.id.PreviewTitle);
		textTitle.setText(title);
		textTitle.setTextColor(Color.BLACK);
		textTitle.setEnabled(false);
		
		webview = (WebView) findViewById(R.id.PreviewContent);
		webview.loadDataWithBaseURL(null, content, "text/html", "UTF-8", "about:blank");
		WebSettings websettings = webview.getSettings();
		websettings.setJavaScriptEnabled(true);
		websettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webview.setClickable(true);
		websettings.setLightTouchEnabled(true);
		
		if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			((LinearLayout) this.findViewById(R.id.LayoutForWebWiew)).setLayoutParams(
					new LayoutParams(LayoutParams.FILL_PARENT,105));
		} else if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			((LinearLayout) this.findViewById(R.id.LayoutForWebWiew)).setLayoutParams(
					new LayoutParams(LayoutParams.FILL_PARENT,262));;
		}

		int w = this.getWindow().getWindowManager().getDefaultDisplay()
				.getWidth() - 12;
		((Button) this.findViewById(R.id.BackToCreateBlogEntry))
				.setWidth(w / 2);
		((Button) this.findViewById(R.id.Publish)).setWidth(w / 2);

		Button publishButton = (Button) findViewById(R.id.Publish);
		publishButton.setOnClickListener(this);
		this.findViewById(R.id.BackToCreateBlogEntry).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						Intent i = new Intent(PreviewAndPublish.this,
								CreateBlogEntry.class);
						startActivity(i);
						finish();
					}
				});
				
		myEntry = new BlogEntry();
		myEntry.setBlogEntry(content);
		myEntry.setTitle(title);
		myEntry.setCreated(new Date(System.currentTimeMillis()));
		publishButton.requestFocus();
	}

	@Override
	public void onClick(View v) {
		this.publishBlogEntry();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		tracker.stop();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent(PreviewAndPublish.this, CreateBlogEntry.class);
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
				// Log.d(TAG, "Using interface type: " + typeEnum);
				blogapi.setInstanceConfig("");
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
						mDbHelper = new DBAdapter(PreviewAndPublish.this);
						try {
							mDbHelper.open();
						} catch (SQLException e) {
							// Log.e(TAG, "Database has not opened");
						}
						setting = mDbHelper.fetchSettindById(1);
						startManagingCursor(setting);
						auth_id = blogapi
								.getAuthId(
										setting
												.getString(setting
														.getColumnIndexOrThrow(DBAdapter.KEY_LOGIN)),
										setting
												.getString(setting
														.getColumnIndexOrThrow(DBAdapter.KEY_PASSWORD)));
						mDbHelper.close();
						setting.close();
						authFlag = true;
						attempt = 0;
					} catch (com.google.gdata.util.AuthenticationException e) {
						attempt++;
						// Log.e(TAG, "AuthenticationException " +
						// e.getMessage());
					} catch (SQLException e) {
						// Log.e(TAG, "SQLException: " + e.getMessage());
					} catch (Exception e) {
						// Log.e(TAG, "Exception: " + e.getMessage());
						Alert
								.showAlert(PreviewAndPublish.this,
										"Network connection failed",
										"Please, check network settings of your device");
						finish();
					}

				}
				publishStatus = 1;
				// Log.d(TAG, "Got auth token:" + auth_id);
				publishStatus = 2;
				if (auth_id != null) {
					status.putString(MSG_KEY, "3");
					statusMsg = mHandler.obtainMessage();
					statusMsg.setData(status);
					mHandler.sendMessage(statusMsg);
					String postUri = null;
					authFlag = false;
					attempt = 0;
					while ((attempt <= MainActivity.AMOUNTOFATTEMPTS)
							&& (!authFlag)) {
						try {
							postUri = blogapi.getPostUrl();
							authFlag = true;
							attempt = 0;
						} catch (ServiceException e) {
							// Log.e(TAG, "ServiceException " + e.getMessage());
							attempt++;
						} catch (IOException e) {
							// Log.e(TAG, "IOException " + e.getMessage());
							attempt++;
						} catch (Exception e) {
							// Log.e(TAG, "Exception: " + e.getMessage());
							Alert
									.showAlert(PreviewAndPublish.this,
											"Network connection failed",
											"Please, check network settings of your device");
							finish();
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
					while ((attempt <= MainActivity.AMOUNTOFATTEMPTS)
							&& (!authFlag)) {
						try {
							mDbHelper = new DBAdapter(PreviewAndPublish.this);
							try {
								mDbHelper.open();
							} catch (SQLException e) {
								// Log.e(TAG, "Database has not opened");
							}
							setting = mDbHelper.fetchSettindById(1);
							startManagingCursor(setting);
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
							mDbHelper.close();
							setting.close();
							authFlag = true;
							attempt = 0;
						} catch (ServiceException e) {
							// Log.e(TAG, "ServiceException: " +
							// e.getMessage());
							attempt++;
						} catch (SQLException e) {
							// Log.e(TAG, "SQLException: " + e.getMessage());
						} catch (Exception e) {
							// Log.e(TAG, "Exception: " + e.getMessage());
							Alert
									.showAlert(PreviewAndPublish.this,
											"Network connection failed",
											"Please, check network settings of your device");
							mDbHelper.close();
							setting.close();
							finish();
						}
					}
				} else {
					publishStatus = 3;
					mDbHelper.close();
					setting.close();
				}
				status.putString(MSG_KEY, "5");
				statusMsg = mHandler.obtainMessage();
				statusMsg.setData(status);
				mHandler.sendMessage(statusMsg);
				if (publishOk) {
					// Log.d(TAG, "Post published successfully!");
					publishStatus = 5;
				} else {
					// Log.d(TAG, "Publishing of the post failed!");
					publishStatus = 4;
				}
				mHandler.post(mPublishResults);
			}
		};
		publish.start();
		publishProgress.setMessage("Publishing in progress...");
	}
	private void showPublishedStatus() {
		publishProgress.dismiss();
		if (publishStatus == 5) {
			try {
				mDbTextHelper = new DBTextAdapter(this);
				try {
					mDbTextHelper.open();
				} catch (SQLException e) {
					// Log.e(TAG, "Database has not opened");
				}
				post = mDbTextHelper.fetchPostdById(1);
				startManagingCursor(post);
				mDbTextHelper.updatePostById((long) 1, "", "");
				mDbTextHelper.close();
				post.close();
			} catch (SQLException e) {
				// Log.e(TAG, "SQLException: " + e.getMessage());
			} catch (Exception e) {
				// Log.e(TAG, "Exception: " + e.getMessage());
			}
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
			mDbTextHelper.close();
			post.close();
			attempt = 0;
			Alert.showAlert(this, "Publishing failed", "Error code "
					+ publishStatus, "Try again",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							publishBlogEntry();
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
