package com.sadko.androblogger;

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
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
import com.sadko.androblogger.db.DBClient;
import com.sadko.androblogger.editor.SpannableBufferHelper;
import com.sadko.androblogger.util.Alert;

public class PreviewAndPublish extends Activity implements /*AdapterView.OnItemSelectedListener,*/
View.OnClickListener{
	
	private static final String TAG = "PreviewAndPublish";
	private String title;
	private String content;
	private BlogEntry myEntry = null;
	private final String MSG_KEY="value";
	int publishStatus = 0;
	private ProgressDialog publishProgress = null;
	private DBClient db = null;
	HashMap<Integer, Integer> configItemOrder = null;
    int publishToConfigID = -1;
    private static int CONFIG_ORDER=0;
    private int atempt = 0;
    
	final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	Bundle content = msg.getData();
            String progressId = content.getString(MSG_KEY);
            if(progressId != null) {
            	if(progressId.equals("1")) {
            		publishProgress.setMessage("Preparing blog config...");
            		} else if(progressId.equals("2")) {
                    publishProgress.setMessage("Authenticating...");
                    } else if(progressId.equals("3")) {
                    publishProgress.setMessage("Contacting server...");
                    } else if(progressId.equals("4")) {
                    publishProgress.setMessage("Creating new entry...");
                    } else if(progressId.equals("5")) {
                    publishProgress.setMessage("Done...");
                    }
            } 
        }
	};
	
    final Runnable mPublishResults = new Runnable() {
        public void run () {
        	showPublishedStatus();
        }
    };
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.previewandpublish);
    	Intent i = this.getIntent();
    	if(i == null) {
            Alert.showAlert(this,"Intent data missing!",
                            "The intent used to launch this Activity was null!");
            this.finish();
    	}
    	CONFIG_ORDER=i.getIntExtra("ConfigOrder", 0);
    	String[] titleAndContent = i.getStringArrayExtra("PostTitleAndContent");
    	title = titleAndContent[0];
    	content = titleAndContent[1];
    	Log.i(TAG, "Title of post: "+title+". Content of post: "+content);
    	EditText textTitle = (EditText)this.findViewById(R.id.PreviewTitle);
    	textTitle.setText(title);
    	textTitle.setTextColor(Color.BLACK);
    	textTitle.setEnabled(false);
    	EditText textContent = (EditText)this.findViewById(R.id.PreviewContent);
    	textContent.setText(content);
    	textContent.setTextColor(Color.BLACK);
    	textContent.setEnabled(false);
    	
    	Button publishButton = (Button) findViewById(R.id.Publish);
    	
    	/*
    	Spinner profileSpinner = (Spinner)findViewById(R.id.ChoiceProfile);
        
    	if(db == null) {
            db = new DBClient();
        }
    	LinkedList<BlogConfig> configs = db.getBlogNames(this);
    	if(configItemOrder == null) {
            configItemOrder = new HashMap<Integer,Integer>(configs.size());
        }
    	
    	for(int j=0; j<configs.size();j++) {
            try {
            	configItemOrder.put(new Integer(j), new Integer(configs.get(j).getId()));
            } catch (NullPointerException ne) {
            	Log.d(TAG,"Config items contains a null entry at "+j+"! Default to first config.");
            	configItemOrder.put(new Integer(j), new Integer(0));
            }
        }
    	Object res = new ArrayAdapter(this,android.R.layout.simple_spinner_item,configs);
    	ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>)res;
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(adapter);
        profileSpinner.setOnItemSelectedListener(this);
        */
        
        publishButton.setOnClickListener(this);
        this.findViewById(R.id.BackToCreateBlogEntry).setOnClickListener(new OnClickListener(){
        	public void onClick(View v){
        		Intent i = new Intent(PreviewAndPublish.this,CreateBlogEntry.class);
        		String[] titleAndContent = new String[2];
				titleAndContent[0] = title;
				titleAndContent[1] = content;
				i.putExtra("PostTitleAndContent", titleAndContent);
				i.putExtra("ConfigOrder", CONFIG_ORDER);
        		startActivity(i);
                finish();
        	}
        });
        myEntry = new BlogEntry();
        myEntry.setBlogEntry(content);
        myEntry.setTitle(title);
        myEntry.setCreated(new Date(System.currentTimeMillis()));
        
        //this.myEntryBean = db.getBlogEntryById(this.getApplication(),entryid);
        //int selectedConfig = findListIndexOfConfig(configs,this.publishToConfigID);
        
	}

	/*@Override
	public void onItemSelected(AdapterView parent, View v, int position, long id) {
		this.myEntry.setPublishedIn(position);
        publishToConfigID = configItemOrder.get(position);	
	}

	@Override
	public void onNothingSelected(AdapterView parent) {
		// TODO Auto-generated method stub
	}
	*/
	
	@Override
	public void onClick(View v) {
		atempt++;
		this.publishBlogEntry();	
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) { 
    	if(keyCode==KeyEvent.KEYCODE_BACK){
    		Intent i = new Intent(PreviewAndPublish.this,CreateBlogEntry.class);
    		String[] titleAndContent = new String[2];
			titleAndContent[0] = title;
			titleAndContent[1] = content;
			i.putExtra("PostTitleAndContent", titleAndContent);
			i.putExtra("ConfigOrder", CONFIG_ORDER);
    		startActivity(i);
            finish();
            return true;
    	}
		return false; 
	}

    private void publishBlogEntry() {
        final Activity thread_parent = this;
        publishProgress = ProgressDialog.show(this, "Publishing blog entry", "Starting to publish blog entry...");
        Thread publish = new Thread () {
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
                        int confignum = publishToConfigID;
                        if(db == null) {
                        	db = new DBClient();
                        }
                        LinkedList<BlogConfig> configList = db.getBlogNames(PreviewAndPublish.this);
                		BlogConfig bc = null;
                		bc = db.getBlogConfigById(PreviewAndPublish.this, configList.get(CONFIG_ORDER).getId());
                        BlogConfigBLOGGER.BlogInterfaceType typeEnum = BlogConfigBLOGGER.getInterfaceTypeByNumber(bc.getPostmethod());
                        BlogInterface blogapi = null;
                        blogapi = BlogInterfaceFactory.getInstance(typeEnum);
                        Log.d(TAG,"Using interface type: "+typeEnum);
                        Log.d(TAG,"Preparing the API with saved editor data: "+bc.getPostConfig());
                        blogapi.setInstanceConfig(bc.getPostConfig());
                        Log.d(TAG,"Trying to get auth token for config id: "+confignum+" with username "+bc.getUsername());
                        status.putString(MSG_KEY, "2");
                        statusMsg = mHandler.obtainMessage();
                        statusMsg.setData(status);
                        mHandler.sendMessage(statusMsg);
                        String auth_id = blogapi.getAuthId(bc.getUsername(), bc.getPassword());
                        publishStatus = 1;
                        Log.d(TAG,"Got auth token:"+auth_id);
                        publishStatus = 2;
                        if(auth_id != null) {
                        	status.putString(MSG_KEY, "3");
                        	statusMsg = mHandler.obtainMessage();
                        	statusMsg.setData(status);
                        	mHandler.sendMessage(statusMsg);
                        	String postUri = null;
							try {
								postUri = blogapi.getPostUrl(auth_id);
							} catch (ServiceException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
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
                        	publishOk = blogapi.createPost(thread_parent, auth_id, postUri, null, 
                        			myEntry.getTitle(), null, 
                                        /*myEntryBean.getBlogEntry().toString(),*/entry, 
                                        bc.getUsername(), bc.getUsername(), myEntry.isDraft());
                        	} else {
                            publishStatus = 3;      
                        }
                        status.putString(MSG_KEY, "5");
                        statusMsg = mHandler.obtainMessage();
                        statusMsg.setData(status);
                        mHandler.sendMessage(statusMsg);
                        if(publishOk) {
                        	Log.d(TAG,"Post published successfully!");
                        	publishStatus = 5;
                        } else {
                        	Log.d(TAG,"Publishing of the post failed!");
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
        if(publishStatus == 5) {
                Alert.showAlert(this,"Publish status","Progress OK!");
        } else 
        if((publishStatus==4)&&(atempt<2)){
        		atempt++;
        		this.publishBlogEntry();
        		//Alert.showAlert(this,"Publish status","Publish failed! (Code "+publishStatus+")");
        } else{
        		atempt=0;
        		Alert.showAlert(this,"Publish status","Publish failed! (Code "+publishStatus+")");
        }
    }
}
