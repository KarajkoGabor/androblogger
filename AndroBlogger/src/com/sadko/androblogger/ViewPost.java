package com.sadko.androblogger;

import java.io.IOException;
import java.util.LinkedList;

import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.data.TextContent;
import com.google.gdata.util.ServiceException;
import com.sadko.androblogger.db.DBClient;
import com.sadko.androblogger.util.Alert;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class ViewPost extends Activity{
	private static int CONFIG_ORDER=0;
	private final String TAG = "ViewPost";
	private final String MSG_KEY="value";
	private ProgressDialog viewProgress = null;
	int viewStatus = 0;
	private BlogConfig config = null;
	public static Entry currentEntry = null;
	public static Feed resultCommentFeed = null;
	private int atempt = 0;
	
	final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	Bundle content = msg.getData();
            String progressId = content.getString(MSG_KEY);
            if(progressId != null) {
            	if(progressId.equals("1")) {
            		viewProgress.setMessage("Preparing blog config...");
            		} else if(progressId.equals("2")) {
                    viewProgress.setMessage("Authenticating...");
                    } else if(progressId.equals("3")) {
                    viewProgress.setMessage("Receiving post comments...");
                    } else if(progressId.equals("4")) {
                    viewProgress.setMessage("Done...");
                    }
            } 
        }
	};
	
    final Runnable mViewResults = new Runnable() {
        public void run () {
        	showViewStatus();
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpost);
		
		Intent i = this.getIntent();
		CONFIG_ORDER=i.getIntExtra("ConfigOrder", 0);
		
		currentEntry=ViewBlog.currentEntry;
		Log.i(TAG, "CurrentEntry obtained from ViewBlog");
		
		TextView postTitle=(TextView)(this.findViewById(R.id.PostTitle));
		postTitle.setText(currentEntry.getTitle().getPlainText());
		
		TextView postAuthor=(TextView)(this.findViewById(R.id.PostAuthor));
		postAuthor.setText(currentEntry.getAuthors().get(0).getName());
		
		TextView postID=(TextView)(this.findViewById(R.id.PostID));
		postID.setText(currentEntry.getId().split("post-")[1]);
		
		TextView postPublishDate=(TextView)(this.findViewById(R.id.PostPublishDate));
		postPublishDate.setText(currentEntry.getPublished().toStringRfc822().substring(0, currentEntry.getPublished().toStringRfc822().length()-5));
		
		TextView postUpdateDate=(TextView)(this.findViewById(R.id.PostUpdateDate));
		postUpdateDate.setText(currentEntry.getUpdated().toStringRfc822().substring(0, currentEntry.getUpdated().toStringRfc822().length()-5));
		
		TextView postContent=(TextView)(this.findViewById(R.id.PostContent));
		postContent.setText(((TextContent)currentEntry.getContent()).getContent().getPlainText());
		
		
		this.findViewById(R.id.BackToViewBlog).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ViewPost.this,ViewBlog.class);
				i.putExtra("ConfigOrder", CONFIG_ORDER);
				startActivity(i);
                finish();
			}
	    });
		
		this.findViewById(R.id.Comments).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				atempt++;
                viewPostComments();
			}
	    });
	}
	
	protected void viewPostComments() {
		viewProgress = ProgressDialog.show(ViewPost.this, "Viewing post comments", "Starting to view post comments...");

		Thread viewThread = new Thread () {
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
        		DBClient db = new DBClient();
        		LinkedList<BlogConfig> configList = db.getBlogNames(ViewPost.this);
        		config = db.getBlogConfigById(ViewPost.this, configList.get(CONFIG_ORDER).getId());
        		String username = config.getUsername();
        		String password = config.getPassword();
        		String postID = currentEntry.getId().split("post-")[1];
        		BlogInterface blogapi = null;
        	    BlogConfigBLOGGER.BlogInterfaceType typeEnum = BlogConfigBLOGGER.getInterfaceTypeByNumber(config.getPostmethod());
        	    blogapi = BlogInterfaceFactory.getInstance(typeEnum);
        	    Log.d(TAG,"Using interface type: "+typeEnum);
                Log.d(TAG,"Preparing the API with saved editor data: "+config.getPostConfig());
                blogapi.setInstanceConfig(config.getPostConfig());
                status.putString(MSG_KEY, "2");
                statusMsg = mHandler.obtainMessage();
                statusMsg.setData(status);
                mHandler.sendMessage(statusMsg);
                String auth_id = blogapi.getAuthId(username, password);
                viewStatus = 1;
                Log.d(TAG,"Got auth token:"+auth_id);
                viewStatus = 2;
                if(auth_id != null) {
                	status.putString(MSG_KEY, "3");
                	statusMsg = mHandler.obtainMessage();
                	statusMsg.setData(status);
                	mHandler.sendMessage(statusMsg);
            	    try {
            	    	resultCommentFeed=blogapi.getAllPostComments(username, password, postID);
            	    	Log.i(TAG,"Post comments successfully received");
            	    	viewOk=true;
            		} catch (ServiceException e) {
            			e.printStackTrace();
            		} catch (IOException e) {
            			e.printStackTrace();
            		}
                }else {
                    viewStatus = 3;
                }
                status.putString(MSG_KEY, "4");
                statusMsg = mHandler.obtainMessage();
                statusMsg.setData(status);
                mHandler.sendMessage(statusMsg);
                if(viewOk) {
                	Log.d(TAG,"Success!");
                	viewStatus = 5;
                } else {
                	Log.d(TAG,"Viewing of comments failed!");
                	viewStatus = 4;
                }
                mHandler.post(mViewResults);
                
                if((resultCommentFeed!=null)&&(viewOk)){
                	Intent i = new Intent(ViewPost.this,ViewComments.class);
    				i.putExtra("ConfigOrder", CONFIG_ORDER);
    				startActivity(i);
    		        finish();
                }
        	}
        }; 
        viewThread.start();
        viewProgress.setMessage("Viewing in progress...");
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) { 
    	if(keyCode==KeyEvent.KEYCODE_BACK){
    		Intent i = new Intent(ViewPost.this,ViewBlog.class);
    		i.putExtra("ConfigOrder", CONFIG_ORDER);
            startActivity(i);
            finish();
            return true;
    	}
		return false; 
	}
	
	private void showViewStatus() {
        viewProgress.dismiss();
        if((viewStatus==4)&&(atempt<2)) {
        	atempt++;
    		this.viewPostComments();
        } else {
        	atempt=0;
        	Alert.showAlert(this,"View status","View failed! (Code "+viewStatus+")\nTry again.");
        }
    }
}
