package com.sadko.androblogger;

import java.io.IOException;

import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.data.TextContent;
import com.google.gdata.util.ServiceException;
import com.sadko.androblogger.db.DBAdapter;
import com.sadko.androblogger.util.Alert;

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
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ViewPost extends Activity {
	private final String TAG = "ViewPost";
	private final String MSG_KEY = "value";
	private ProgressDialog viewProgress = null;
	int viewStatus = 0;
	public static Entry currentEntry = null;
	public static Feed resultCommentFeed = null;
	private int attempt = 0;
	private DBAdapter mDbHelper;
	private static Cursor setting = null;
	private static boolean viewOk = false;

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
					viewProgress.setMessage("Receiving post comments...");
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpost);

		mDbHelper = new DBAdapter(this);
		try {
			mDbHelper.open();
		} catch (SQLException e) {
			Log.e(TAG, "Database has not opened");
		}
		setting = mDbHelper.fetchSettindById(1);
		startManagingCursor(setting);

		currentEntry = ViewBlog.currentEntry;
		Log.i(TAG, "CurrentEntry obtained from ViewBlog");

		TextView postTitle = (TextView) (this.findViewById(R.id.PostTitle));
		postTitle.setText(currentEntry.getTitle().getPlainText());

		TextView postAuthor = (TextView) (this.findViewById(R.id.PostAuthor));
		postAuthor.setText(currentEntry.getAuthors().get(0).getName());

		TextView postPublishDate = (TextView) (this
				.findViewById(R.id.PostPublishDate));
		postPublishDate
				.setText(currentEntry.getPublished().toStringRfc822()
						.substring(
								0,
								currentEntry.getPublished().toStringRfc822()
										.length() - 5));

		TextView postUpdateDate = (TextView) (this
				.findViewById(R.id.PostUpdateDate));
		postUpdateDate
				.setText(currentEntry.getUpdated().toStringRfc822()
						.substring(
								0,
								currentEntry.getUpdated().toStringRfc822()
										.length() - 5));

		TextView postContent = (TextView) (this.findViewById(R.id.PostContent));
		postContent.setText(((TextContent) currentEntry.getContent())
				.getContent().getPlainText());

		int w = this.getWindow().getWindowManager().getDefaultDisplay()
				.getWidth() - 12;
		((Button) this.findViewById(R.id.BackToViewBlog)).setWidth(w / 2);
		((Button) this.findViewById(R.id.Comments)).setWidth(w / 2);

		this.findViewById(R.id.BackToViewBlog).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(ViewPost.this, ViewBlog.class);
						startActivity(i);
						finish();
					}
				});

		this.findViewById(R.id.Comments).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						// attempt++;
						viewPostComments();
					}
				});
	}

	protected void viewPostComments() {
		viewProgress = ProgressDialog.show(ViewPost.this,
				"Viewing post comments", "Starting to view post comments...");

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
				String username = setting.getString(setting
						.getColumnIndexOrThrow(DBAdapter.KEY_LOGIN));
				String password = setting.getString(setting
						.getColumnIndexOrThrow(DBAdapter.KEY_PASSWORD));
				String postID = currentEntry.getId().split("post-")[1];
				BlogInterface blogapi = null;
				BlogConfigBLOGGER.BlogInterfaceType typeEnum = BlogConfigBLOGGER
						.getInterfaceTypeByNumber(1);
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
					attempt++;
					authFlag = false;
					while ((attempt <= MainActivity.AMOUNTOFATTEMPTS)
							&& (!authFlag)) {
						try {
							resultCommentFeed = blogapi.getAllPostComments(
									username, password, postID);
							Log.i(TAG, "Post comments successfully received");
							viewOk = true;
							authFlag = true;
							attempt = 0;
						} catch (ServiceException e) {
							e.printStackTrace();
							attempt++;
							Log.e(TAG, "ServiceException " + e.getMessage());
						} catch (IOException e) {
							e.printStackTrace();
							attempt++;
							Log.e(TAG, "IOException " + e.getMessage());
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
					Log.d(TAG, "Viewing of comments failed!");
					viewStatus = 4;
				}
				mHandler.post(mViewResults);

				if ((resultCommentFeed != null) && (viewOk)) {
					Intent i = new Intent(ViewPost.this, ViewComments.class);
					startActivity(i);
					finish();
				}
			}
		};
		ConnectivityManager cm = (ConnectivityManager) ViewPost.this
				.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = cm.getActiveNetworkInfo();
		if (netinfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
			viewThread.start();
		} else {
			Alert.showAlert(ViewPost.this, "Network connection needed",
					"Please, connect your device to the Internet");
		}
		viewProgress.setMessage("Viewing in progress...");
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent(ViewPost.this, ViewBlog.class);
			startActivity(i);
			finish();
			return true;
		}
		return false;
	}

	private void showViewStatus() {
		viewProgress.dismiss();
		if (attempt > MainActivity.AMOUNTOFATTEMPTS) {
			/*
			 * Alert.showAlert(this, "Viewing failed", "Error code " +
			 * viewStatus); }
			 */
			Alert.showAlert(this, "Viewing failed", "Error code " + viewStatus,
					"Try again", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							viewPostComments();
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
