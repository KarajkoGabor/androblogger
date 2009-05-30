package com.sadko.androblogger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

import com.google.gdata.client.GoogleService;
import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.data.Link;
import com.google.gdata.util.ServiceException;
import com.sadko.androblogger.db.DBAdapter;
import com.sadko.androblogger.db.DBClient;
import com.sadko.androblogger.db.DBHelper;
import com.sadko.androblogger.util.Alert;

public class Settings extends Activity {
	private EditText textUsername, textPassword;
	private final String TAG = "CreateProfile";
	private final String MSG_KEY = "value";
	private final String STATUS_KEY = "status";
	private final String STATUS_RESPONSE_NULL = "1";
	private final String STATUS_ZERO_BLOGS = "2";
	private final String STATUS_OK = "3";
	private final String STATUS_BAD_AUTH = "4";
	private final String RESPONSE_NAMES_KEY = "response_names";
	private final String RESPONSE_IDS_KEY = "response_ids";
	private final String NO_POST_URL_FOR_ENTRY = "NO_POST_URL_FOR_ENTRY";
	private URL feedUrl = null;
	private DBAdapter mDbHelper;
	private Long mRowId;
	private ProgressDialog verifyProgress = null;
	private int verifyStatus = 0;

	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle content = msg.getData();
			String progressId = content.getString(MSG_KEY);
			if (progressId != null) {
				if (progressId.equals("1")) {
					verifyProgress.setMessage("Preparing...");
					verifyStatus = 1;
				} else if (progressId.equals("2")) {
					verifyProgress.setMessage("Authenticating...");
					verifyStatus = 2;
				} else if (progressId.equals("3")) {
					verifyProgress.setMessage("Contacting server...");
					verifyStatus = 3;
				} else if (progressId.equals("4")) {
					verifyProgress.setMessage("Extracting response...");
					verifyStatus = 4;
				} else if (progressId.equals("5")) {
					verifyProgress.setMessage("Done...");
					verifyStatus = 5;
				}
				String status = content.getString(STATUS_KEY);
				if (status != null && verifyStatus == 5) {
					if (status.equals(STATUS_RESPONSE_NULL)) {
						verifyStatus = 6;
					} else if (status.equals(STATUS_ZERO_BLOGS)) {
						verifyStatus = 7;
					} else if (status.equals(STATUS_OK)) {
						verifyProgress.setMessage("Displaying blog names...");
						String names = content.getString(RESPONSE_NAMES_KEY);
						Log.d(TAG, "names here: " + names);
						String ids = content.getString(RESPONSE_IDS_KEY);
						Log.d(TAG, "ids here: " + ids);
						String[] namearr = names.split("\\|");
						String[] idsarr = ids.split("\\|");
						for (int i = 0; i < namearr.length; i++) {
							Log.d(TAG, "namearr[" + i + "]=" + namearr[i]);
						}
						if (namearr == null || namearr.length < 1) {
							verifyStatus = 8;
						} else if (idsarr == null || idsarr.length < 1) {
							verifyStatus = 9;
						}
					} else if (status.equals(STATUS_BAD_AUTH)) {
						verifyStatus = 10;
					}
				}
			}
		}
	};

	final Runnable mFetchResults = new Runnable() {
		public void run() {
			showVerifyStatus();
		}

		private void showVerifyStatus() {
			verifyProgress.dismiss();
			if (verifyStatus == 5) {
				Alert.showAlert(Settings.this, "Status", "Blogs verified OK!");
				Button SaveProfile = (Button) (Settings.this
						.findViewById(R.id.Save));
				SaveProfile.setEnabled(true);
				Log.d(TAG, "Button 'Save' is enable");
				EditText username = (EditText) Settings.this
						.findViewById(R.id.Username);
				EditText password = (EditText) Settings.this
						.findViewById(R.id.Password);
				Button verifyButton = (Button) Settings.this
						.findViewById(R.id.Verify);
				if (username != null) {
					username.setEnabled(false);
				}
				if (password != null) {
					password.setEnabled(false);
				}
				if (verifyButton != null) {
					verifyButton.setEnabled(false);
				}
			} else if (verifyStatus == 6) {
				Alert.showAlert(Settings.this, "Status",
						"Can't find any blogs for this user (null answer).");
			} else if (verifyStatus == 7) {
				Alert.showAlert(Settings.this, "Status",
						"Can't find any blogs for this user.");
			} else if (verifyStatus == 8) {
				Alert.showAlert(Settings.this, "Status",
						"Can't extract blog names from respose.");
			} else if (verifyStatus == 9) {
				Alert.showAlert(Settings.this, "Status",
						"Can't extract blog ids from response.");
			} else if (verifyStatus == 10) {
				Alert
						.showAlert(Settings.this, "Status",
								"Authentication failed. Did you enter the username and password correctly?");
			} else {
				Alert.showAlert(Settings.this, "Status",
						"Fetch of blogs failed! (Code " + verifyStatus + ")");
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		((EditText) this.findViewById(R.id.Username))
				.setOnKeyListener(new OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						((Button) Settings.this.findViewById(R.id.Verify))
								.setEnabled(true);
						((Button) Settings.this.findViewById(R.id.Save))
								.setText("Update");
						return false;
					}
				});
		((EditText) this.findViewById(R.id.Password))
				.setOnKeyListener(new OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						((Button) Settings.this.findViewById(R.id.Verify))
								.setEnabled(true);
						/*************************/
						((Button) Settings.this.findViewById(R.id.Save))
								.setEnabled(true);
						/*************************/
						return false;
					}
				});
		mDbHelper = new DBAdapter(this);
		mDbHelper.open();
		mRowId = savedInstanceState != null ? savedInstanceState
				.getLong(DBAdapter.KEY_ROWID) : null;
		if (mRowId != null) {
			if (mRowId != 0) {
				Cursor setting = mDbHelper.fetchSettindById(1);
				startManagingCursor(setting);
				((EditText) this.findViewById(R.id.Username)).setText(setting
						.getString(setting
								.getColumnIndexOrThrow(DBAdapter.KEY_LOGIN)));
				((EditText) this.findViewById(R.id.Password))
						.setText(setting.getString(setting
								.getColumnIndexOrThrow(DBAdapter.KEY_PASSWORD)));
				((Button) this.findViewById(R.id.Verify)).setEnabled(false);
				((Button) this.findViewById(R.id.Save)).setEnabled(false);
			}
		}

		int w = this.getWindow().getWindowManager().getDefaultDisplay()
				.getWidth() - 12;
		((Button) this.findViewById(R.id.BackToMainActivity)).setWidth(w / 3);
		((Button) this.findViewById(R.id.Save)).setWidth(w / 3);
		((Button) this.findViewById(R.id.Verify)).setWidth(w / 3);

		this.findViewById(R.id.BackToMainActivity).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						Intent i = new Intent(Settings.this, MainActivity.class);
						startActivity(i);
						finish();
					}
				});

		this.findViewById(R.id.Verify).setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						String username = null;
						String password = null;
						EditText usernameView = (EditText) Settings.this
								.findViewById(R.id.Username);
						if (usernameView.getText() == null
								|| usernameView.getText().length() < 1) {
							Alert
									.showAlert(Settings.this,
											"Username needed",
											"You need to give your Google username in order to continue!");
							return;
						} else {
							username = usernameView.getText().toString();
						}
						usernameView = null;
						EditText passwordView = (EditText) Settings.this
								.findViewById(R.id.Password);
						if (passwordView.getText() == null
								|| passwordView.getText().length() < 1) {
							Alert
									.showAlert(Settings.this,
											"Password needed",
											"You need to give your Google password in order to continue!");
							return;
						} else {
							password = passwordView.getText().toString();
						}
						final String verifyUsername = username;
						final String verifyPassword = password;
						verifyProgress = ProgressDialog.show(Settings.this,
								"Verifying the data",
								"Starting to verify blogs...");
						Thread fetchGoogleBlogs = new Thread() {
							public void run() {
								Feed resultFeed = null;
								Bundle status = new Bundle();
								Message statusMsg = mHandler.obtainMessage();
								status.putString(MSG_KEY, "1");
								statusMsg.setData(status);
								mHandler.sendMessage(statusMsg);
								try {
									feedUrl = new URL(
											"http://www.blogger.com/feeds/default/blogs");
								} catch (MalformedURLException e) {
									Log
											.e(TAG,
													"The default blog feed url is malformed!");
								}
								Log.d(TAG, "Querying Blogger blog, URL:"
										+ feedUrl + ", user: " + verifyUsername
										+ ", pass:" + verifyPassword);
								GoogleService myVerifyService = new GoogleService(
										"blogger", BlogConfigBLOGGER.APPNAME);
								statusMsg = mHandler.obtainMessage();
								status.putString(MSG_KEY, "2");
								statusMsg.setData(status);
								mHandler.sendMessage(statusMsg);
								try {
									myVerifyService.setUserCredentials(
											verifyUsername, verifyPassword);
								} catch (com.google.gdata.util.AuthenticationException e) {
									Log.e(TAG, "Authentication exception! "
											+ e.getMessage());
									statusMsg = mHandler.obtainMessage();
									status.putString(STATUS_KEY,
											STATUS_BAD_AUTH);
									status.putString(MSG_KEY, "5");
									statusMsg.setData(status);
									mHandler.sendMessage(statusMsg);
									mHandler.post(mFetchResults);
									return;
								}
								statusMsg = mHandler.obtainMessage();
								status.putString(MSG_KEY, "3");
								statusMsg.setData(status);
								mHandler.sendMessage(statusMsg);
								try {
									resultFeed = myVerifyService.getFeed(
											feedUrl, Feed.class);
								} catch (IOException e) {
									e.printStackTrace();
								} catch (ServiceException e) {
									e.printStackTrace();
								}
								statusMsg = mHandler.obtainMessage();

								if (resultFeed == null) {
									status.putString(STATUS_KEY,
											STATUS_RESPONSE_NULL);
									status.putString(MSG_KEY, "5");
									statusMsg.setData(status);
									mHandler.sendMessage(statusMsg);
									mHandler.post(mFetchResults);
									return;
								} else if (resultFeed.getEntries() == null
										|| resultFeed.getEntries().size() == 0) {
									status.putString(STATUS_KEY,
											STATUS_ZERO_BLOGS);
									status.putString(MSG_KEY, "5");
									statusMsg.setData(status);
									mHandler.sendMessage(statusMsg);
									mHandler.post(mFetchResults);
									return;
								} else {
									status.putString(STATUS_KEY, STATUS_OK);
									status.putString(MSG_KEY, "4");
								}
								statusMsg.setData(status);
								mHandler.sendMessage(statusMsg);
								StringBuffer titles = new StringBuffer();
								StringBuffer ids = new StringBuffer();
								Log.d(TAG, (resultFeed.getTitle()
										.getPlainText()));
								for (int i = 0; i < resultFeed.getEntries()
										.size(); i++) {
									Entry entry = resultFeed.getEntries()
											.get(i);
									titles.append(entry.getTitle()
											.getPlainText()
											+ "|");
									List<Link> links = entry.getLinks();
									Iterator<Link> iter = links.iterator();
									Log.d(TAG, "This entry has links:");
									boolean postFound = false;
									while (iter.hasNext()) {
										Link link = iter.next();
										String rel = link.getRel();
										String href = link.getHref();
										String type = link.getType();
										Log.d(TAG, "<link rel ='" + rel
												+ "' type = '" + type
												+ "' href = '" + href + "'/>");
										if (rel.endsWith("#post")) {
											postFound = true;
											ids.append("" + href + "|");
										}
									}
									if (!postFound) {
										ids.append(NO_POST_URL_FOR_ENTRY + "|");
									}

								}
								statusMsg = mHandler.obtainMessage();
								status.putString(RESPONSE_NAMES_KEY, titles
										.toString());
								status.putString(RESPONSE_IDS_KEY, ids
										.toString());
								status.putString(MSG_KEY, "5");
								statusMsg.setData(status);
								mHandler.sendMessage(statusMsg);
								myVerifyService = null;
								mHandler.post(mFetchResults);
							}
						};
						fetchGoogleBlogs.start();
						verifyProgress
								.setMessage("Started to verify your blogs...");
					}
				});

		this.findViewById(R.id.Save).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				textUsername = (EditText) findViewById(R.id.Username);
				textPassword = (EditText) findViewById(R.id.Password);
				if (textPassword == null || textPassword.getText() == null) {
					Log
							.d(TAG,
									"password editor view is null when trying to read!");
					return;
				}
				if (textUsername == null || textUsername.getText() == null) {
					Log
							.d(TAG,
									"username editor view is null when trying to read!");
					return;
				}
				String usernameStr = textUsername.getText().toString();
				String passwordStr = textPassword.getText().toString();
				if (usernameStr.length() < 1) {
					Alert.showAlert(Settings.this, "Empty username",
							"You need to have a username for this blog.");
					return;
				}
				if (passwordStr.length() < 1) {
					Alert.showAlert(Settings.this, "Empty password",
							"You need to have a password for this blog.");
					return;
				}
				Cursor setting = mDbHelper.fetchSettindById(1);
				startManagingCursor(setting);
				if (mRowId == null) {
					mDbHelper.createSetting(usernameStr, passwordStr);
				} else {
					mDbHelper.updateSettingById((long) 1, usernameStr,
							passwordStr);
				}
				Log.d(TAG, "Blog Config saved to database.");
				final Dialog dlg;
				if (mRowId == null) {
					dlg = new AlertDialog.Builder(Settings.this)
							.setIcon(
									com.sadko.androblogger.R.drawable.ic_dialog_alert)
							.setTitle("Success")
							.setPositiveButton("OK", null)
							.setMessage(
									"Your profile has been successfully saved.")
							.create();
				} else {
					dlg = new AlertDialog.Builder(Settings.this)
							.setIcon(
									com.sadko.androblogger.R.drawable.ic_dialog_alert)
							.setTitle("Success")
							.setPositiveButton("OK", null)
							.setMessage(
									"Your profile has been successfully updated.")
							.create();
				}
				dlg.setOnDismissListener(new OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						Intent i = new Intent(Settings.this, MainActivity.class);
						startActivity(i);
						finish();
					}
				});
				dlg.show();
			}
		});
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent(Settings.this, MainActivity.class);
			startActivity(i);
			finish();
			return true;
		}
		return false;
	}
}
