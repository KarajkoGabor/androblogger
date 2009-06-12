package com.sadko.androblogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.data.TextContent;
import com.google.gdata.util.ServiceException;
import com.sadko.androblogger.db.DBAdapter;
import com.sadko.androblogger.util.Alert;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message; //import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class ViewComments extends ListActivity {
	// private final String TAG = "ViewComments";
	private final String MSG_KEY = "value";
	public static Entry currentEntry = null;
	public static Feed resultCommentFeed = null;
	private int attempt = 0;
	private DBAdapter mDbHelper;
	private static Cursor setting = null;
	private static boolean viewOk = false;
	private ProgressDialog viewProgress = null;
	int viewStatus = 0;

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
		setContentView(R.layout.viewcomments);

		mDbHelper = new DBAdapter(this);
		try {
			mDbHelper.open();
		} catch (SQLException e) {
			// Log.e(TAG, "Database has not opened");
		}
		setting = mDbHelper.fetchSettindById(1);
		startManagingCursor(setting);

		currentEntry = ViewPost.currentEntry;
		// Log.i(TAG, "CurrentEntry obtained from ViewPost");

		resultCommentFeed = ViewPost.resultCommentFeed;
		// Log.i(TAG, "ResultCommentFeed obtained from ViewPost");

		int w = this.getWindow().getWindowManager().getDefaultDisplay()
				.getWidth() - 12;
		((Button) this.findViewById(R.id.BackToViewPost)).setWidth(w / 2);
		((Button) this.findViewById(R.id.RefreshCommentsList)).setWidth(w / 2);

		int maxCharTitle = 30;
		if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			maxCharTitle = 40;
		} else if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			maxCharTitle = 30;
		}
		String nontruncatedTitle = null;
		String truncatedTitle = null;
		nontruncatedTitle = currentEntry.getTitle().getPlainText();
		if (nontruncatedTitle.length() == 0) {
			truncatedTitle = "Title: <Empty title>";
		} else if (nontruncatedTitle.length() > maxCharTitle) {
			truncatedTitle = "Title: "
					+ nontruncatedTitle.substring(0, maxCharTitle) + "...";
		} else {
			truncatedTitle = "Title: " + nontruncatedTitle;
		}
		TextView postTitle = (TextView) (this.findViewById(R.id.PostTitle));
		postTitle.setText(truncatedTitle);

		if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			((LinearLayout) this.findViewById(R.id.LayoutForHeadline))
					.setPadding(0, 0, 0, 0);
			((LinearLayout) this.findViewById(R.id.LayoutForHeadline))
					.setBackgroundDrawable(null);
			((LinearLayout) this.findViewById(R.id.LayoutForHeadline))
					.removeAllViews();
			((LinearLayout) this.findViewById(R.id.LayoutForComments))
					.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
							210));
		}
		List<Map<String, Object>> resourceNames = new ArrayList<Map<String, Object>>();
		Map<String, Object> data;
		for (int j = resultCommentFeed.getEntries().size(); j > 0; j--) {
			data = new HashMap<String, Object>();
			Entry commentEntry = resultCommentFeed.getEntries().get(j - 1);
			try {
				data.put("line1", commentEntry.getAuthors().get(0).getName());
				data.put("line3", commentEntry.getUpdated().toStringRfc822()
						.substring(
								0,
								commentEntry.getUpdated().toStringRfc822()
										.length() - 5));
				data.put("line2", ((TextContent) commentEntry.getContent())
						.getContent().getPlainText());
				resourceNames.add(data);
			} catch (Resources.NotFoundException nfe) {
			}
		}

		SimpleAdapter notes = new SimpleAdapter(this, resourceNames,
				R.layout.commentrow, new String[]{"line1", "line3", "line2"},
				new int[]{R.id.text1, R.id.text3, R.id.text2});
		setListAdapter(notes);

		this.findViewById(R.id.RefreshCommentsList).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						viewPostComments();
					}
				});

		this.findViewById(R.id.BackToViewPost).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(ViewComments.this, ViewPost.class);
						startActivity(i);
						finish();
					}
				});
	}

	protected void viewPostComments() {
		viewProgress = ProgressDialog.show(ViewComments.this,
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
				mDbHelper.close();
				setting.close();
				String postID = currentEntry.getId().split("post-")[1];
				BlogInterface blogapi = null;
				BlogConfigBLOGGER.BlogInterfaceType typeEnum = BlogConfigBLOGGER
						.getInterfaceTypeByNumber(1);
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
								.showAlert(ViewComments.this,
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
					attempt++;
					authFlag = false;
					while ((attempt <= MainActivity.AMOUNTOFATTEMPTS)
							&& (!authFlag)) {
						try {
							ViewPost.resultCommentFeed = blogapi
									.getAllPostComments(username, password,
											postID);
							// Log.i(TAG,
							// "Post comments successfully received");
							viewOk = true;
							authFlag = true;
							attempt = 0;
						} catch (ServiceException e) {
							e.printStackTrace();
							attempt++;
							// Log.e(TAG, "ServiceException " + e.getMessage());
						} catch (IOException e) {
							e.printStackTrace();
							attempt++;
							// Log.e(TAG, "IOException " + e.getMessage());
						} catch (Exception e) {
							// Log.e(TAG, "Exception: " + e.getMessage());
							Alert
									.showAlert(ViewComments.this,
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
					// Log.d(TAG, "Viewing of comments failed!");
					viewStatus = 4;
				}
				mHandler.post(mViewResults);

				if ((resultCommentFeed != null) && (viewOk)) {
					Intent i = new Intent(ViewComments.this, ViewComments.class);
					startActivity(i);
					finish();
				}
			}
		};
		viewThread.start();
		viewProgress.setMessage("Viewing in progress...");
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent(ViewComments.this, ViewPost.class);
			startActivity(i);
			finish();
			return true;
		}
		return false;
	}

	private void showViewStatus() {
		viewProgress.dismiss();
		if (attempt > MainActivity.AMOUNTOFATTEMPTS) {
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
