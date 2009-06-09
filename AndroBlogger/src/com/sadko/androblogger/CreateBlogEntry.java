package com.sadko.androblogger;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.sadko.androblogger.db.DBTextAdapter;
import com.sadko.androblogger.util.Alert;

public class CreateBlogEntry extends Activity {
	private static final String TAG = "CreateBlogEntry";
	private DBTextAdapter mDbTextHelper;
	private static Cursor post = null;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.createblogentry);
		mDbTextHelper = new DBTextAdapter(this);
		try {
			mDbTextHelper.open();
		} catch (SQLException e) {
			Log.e(TAG, "Database has not opened");
		}
		post = mDbTextHelper.fetchPostdById(1);
		startManagingCursor(post);
		if (post.getCount() != 0) {
			try {
				((EditText) this.findViewById(R.id.TextPostTitle))
						.setText(post
								.getString(post
										.getColumnIndexOrThrow(DBTextAdapter.KEY_TITLE)));
				((EditText) this.findViewById(R.id.TextPostContent))
						.setText(post
								.getString(post
										.getColumnIndexOrThrow(DBTextAdapter.KEY_CONTENT)));
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "IllegalArgumentException (DataBase failed)");
			} catch (Exception e) {
				Log.e(TAG, "Exception (DataBase failed)");
			}
		}
		if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			((EditText) this.findViewById(R.id.TextPostContent)).setHeight(105);
		} else if (this.getWindow().getWindowManager().getDefaultDisplay()
				.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			((EditText) this.findViewById(R.id.TextPostContent)).setHeight(265);
		}
		this.findViewById(R.id.BackToMainActivities).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						EditText postTitle = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostTitle);
						EditText postContent = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostContent);
						String strTitle = "";
						String strContent = "";
						if (post.getCount() == 0) {
							try {
								/*********************************************************************/
								strTitle = postTitle.getText().toString();
								strContent = postContent.getText().toString();
								mDbTextHelper.createPost(strTitle, strContent);
								/*********************************************************************/
								Log.d(TAG, "Post saved to database.");
							} catch (SQLException e) {
								Log
										.e(TAG,
												"SQLException (createPost(title, content))");
							} catch (Exception e) {
								Log.e(TAG, "Exception: " + e.getMessage());
							}
						} else {
							try {
								/*********************************************************************/
								strTitle = postTitle.getText().toString();
								strContent = postContent.getText().toString();
								mDbTextHelper.updatePostById((long) 1,
										strTitle, strContent);
								/*********************************************************************/
								Log.d(TAG, "Post updated in database.");
							} catch (SQLException e) {
								Log
										.e(TAG,
												"SQLException (updatePostById(rowId, title, content))");
							}
						}
						Intent i = new Intent(CreateBlogEntry.this,
								MainActivity.class);
						startActivity(i);
						finish();
					}
				});

		int w = this.getWindow().getWindowManager().getDefaultDisplay()
				.getWidth() - 8;
		((Button) this.findViewById(R.id.BackToMainActivities)).setWidth(w / 3);
		((Button) this.findViewById(R.id.ClearAll)).setWidth(w / 3);
		((Button) this.findViewById(R.id.Preview)).setWidth(w / 3);

		this.findViewById(R.id.Preview).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						if ((((EditText) findViewById(com.sadko.androblogger.R.id.TextPostTitle))
								.getText().length() == 0)
								|| (((EditText) findViewById(com.sadko.androblogger.R.id.TextPostTitle))
										.getText() == null)
								|| (((EditText) findViewById(com.sadko.androblogger.R.id.TextPostContent))
										.getText().length() == 0)
								|| (((EditText) findViewById(com.sadko.androblogger.R.id.TextPostContent))
										.getText() == null)) {
							Alert.showAlert(CreateBlogEntry.this,
									"Empty title or content",
									"Please fill all fields");
						} else {
							Intent i = new Intent(CreateBlogEntry.this,
									PreviewAndPublish.class);
							EditText postTitle = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostTitle);
							EditText postContent = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostContent);
							String[] titleAndContent = new String[2];
							titleAndContent[0] = postTitle.getText().toString();
							titleAndContent[1] = postContent.getText()
									.toString();
							String strTitle = "";
							String strContent = "";
							if (post.getCount() == 0) {
								try {
									/*********************************************************************/
									strTitle = postTitle.getText().toString();
									strContent = postContent.getText()
											.toString();
									mDbTextHelper.createPost(strTitle,
											strContent);
									/*********************************************************************/
									Log.d(TAG, "Post saved to database.");
								} catch (SQLException e) {
									Log
											.e(TAG,
													"SQLException (createPost(title, content))");
								} catch (Exception e) {
									Log.e(TAG, "Exception: " + e.getMessage());
								}
							} else {
								try {
									/*********************************************************************/
									strTitle = postTitle.getText().toString();
									strContent = postContent.getText()
											.toString();
									mDbTextHelper.updatePostById((long) 1,
											strTitle, strContent);
									/*********************************************************************/
									Log.d(TAG, "Post updated in database.");
								} catch (SQLException e) {
									Log
											.e(TAG,
													"SQLException (updatePostById(rowId, title, content))");
								}
							}
							i.putExtra("PostTitleAndContent", titleAndContent);
							startActivity(i);
							finish();
						}
					}
				});
		this.findViewById(R.id.ClearAll).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						EditText postTitle = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostTitle);
						EditText postContent = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostContent);
						postTitle.setText("");
						postContent.setText("");
					}
				});
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "Method 'onPause()' launched");
		mDbTextHelper = new DBTextAdapter(this);
		try {
			mDbTextHelper.open();
		} catch (SQLException e) {
			Log.e(TAG, "Database has not opened");
		}
		post = mDbTextHelper.fetchPostdById(1);
		startManagingCursor(post);
		EditText postTitle = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostTitle);
		EditText postContent = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostContent);
		String strTitle = "";
		String strContent = "";
		if (post.getCount() == 0) {
			try {
				/*********************************************************************/
				strTitle = postTitle.getText().toString();
				strContent = postContent.getText().toString();
				mDbTextHelper.createPost(strTitle, strContent);
				/*********************************************************************/
				Log.d(TAG, "Post saved to database.");
			} catch (SQLException e) {
				Log.e(TAG, "SQLException (createPost(title, content))");
			} catch (Exception e) {
				Log.e(TAG, "Exception: " + e.getMessage());
			}
		} else {
			try {
				/*********************************************************************/
				strTitle = postTitle.getText().toString();
				strContent = postContent.getText().toString();
				mDbTextHelper.updatePostById((long) 1, strTitle, strContent);
				/*********************************************************************/
				Log.d(TAG, "Post updated in database.");
			} catch (SQLException e) {
				Log.e(TAG,
						"SQLException (updatePostById(rowId, title, content))");
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Method 'onDestroy()' launched");
		mDbTextHelper = new DBTextAdapter(this);
		try {
			mDbTextHelper.open();
		} catch (SQLException e) {
			Log.e(TAG, "Database has not opened");
		}
		post = mDbTextHelper.fetchPostdById(1);
		startManagingCursor(post);
		EditText postTitle = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostTitle);
		EditText postContent = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostContent);
		String strTitle = "";
		String strContent = "";
		if (post.getCount() == 0) {
			try {
				/*********************************************************************/
				strTitle = postTitle.getText().toString();
				strContent = postContent.getText().toString();
				mDbTextHelper.createPost(strTitle, strContent);
				/*********************************************************************/
				Log.d(TAG, "Post saved to database.");
			} catch (SQLException e) {
				Log.e(TAG, "SQLException (createPost(title, content))");
			} catch (Exception e) {
				Log.e(TAG, "Exception: " + e.getMessage());
			}
		} else {
			try {
				/*********************************************************************/
				strTitle = postTitle.getText().toString();
				strContent = postContent.getText().toString();
				mDbTextHelper.updatePostById((long) 1, strTitle, strContent);
				/*********************************************************************/
				Log.d(TAG, "Post updated in database.");
			} catch (SQLException e) {
				Log.e(TAG,
						"SQLException (updatePostById(rowId, title, content))");
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "Method 'onStop()' launched");
		mDbTextHelper = new DBTextAdapter(this);
		try {
			mDbTextHelper.open();
		} catch (SQLException e) {
			Log.e(TAG, "Database has not opened");
		}
		post = mDbTextHelper.fetchPostdById(1);
		startManagingCursor(post);
		EditText postTitle = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostTitle);
		EditText postContent = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostContent);
		String strTitle = "";
		String strContent = "";
		if (post.getCount() == 0) {
			try {
				/*********************************************************************/
				strTitle = postTitle.getText().toString();
				strContent = postContent.getText().toString();
				mDbTextHelper.createPost(strTitle, strContent);
				/*********************************************************************/
				Log.d(TAG, "Post saved to database.");
			} catch (SQLException e) {
				Log.e(TAG, "SQLException (createPost(title, content))");
			} catch (Exception e) {
				Log.e(TAG, "Exception: " + e.getMessage());
			}
		} else {
			try {
				/*********************************************************************/
				strTitle = postTitle.getText().toString();
				strContent = postContent.getText().toString();
				mDbTextHelper.updatePostById((long) 1, strTitle, strContent);
				/*********************************************************************/
				Log.d(TAG, "Post updated in database.");
			} catch (SQLException e) {
				Log.e(TAG,
						"SQLException (updatePostById(rowId, title, content))");
			}
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			EditText postTitle = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostTitle);
			EditText postContent = (EditText) findViewById(com.sadko.androblogger.R.id.TextPostContent);
			String strTitle = "";
			String strContent = "";
			if (post.getCount() == 0) {
				try {
					/*********************************************************************/
					strTitle = postTitle.getText().toString();
					strContent = postContent.getText().toString();
					mDbTextHelper.createPost(strTitle, strContent);
					/*********************************************************************/
					Log.d(TAG, "Post saved to database.");
				} catch (SQLException e) {
					Log.e(TAG, "SQLException (createPost(title, content))");
				} catch (Exception e) {
					Log.e(TAG, "Exception: " + e.getMessage());
				}
			} else {
				try {
					/*********************************************************************/
					strTitle = postTitle.getText().toString();
					strContent = postContent.getText().toString();
					mDbTextHelper
							.updatePostById((long) 1, strTitle, strContent);
					/*********************************************************************/
					Log.d(TAG, "Post updated in database.");
				} catch (SQLException e) {
					Log
							.e(TAG,
									"SQLException (updatePostById(rowId, title, content))");
				}
			}
			Intent i = new Intent(CreateBlogEntry.this, MainActivity.class);
			startActivity(i);
			finish();
			return true;
		}
		return false;
	}

}
