package com.sadko.androblogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.gdata.data.Feed;
import com.google.gdata.util.ServiceException;
import com.sadko.androblogger.db.DBClient;
import com.sadko.androblogger.util.Alert;

public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener{
	private static final int BLOGCONFIG_REQUEST = 4;
	final static String TAG = "MainActivity";
	private static int CONFIG_ORDER = 0;
	private ProgressDialog viewProgress = null;
	private final String MSG_KEY="value";
	public static Feed resultFeed = null;
	private LinkedList<BlogConfig> configs = null;
	int viewStatus = 0;
	private BlogConfig config = null;
	
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
                    viewProgress.setMessage("Receiving blog entries...");
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Intent i = this.getIntent();
        if(i.hasExtra("ConfigOrder")){
        	CONFIG_ORDER=i.getIntExtra("ConfigOrder", 0);
        }
        else{
        	CONFIG_ORDER=0;
        }
        
        this.findViewById(R.id.CreateProfile).setOnClickListener(new OnClickListener(){
			public void onClick(View v){
        		
				Intent i = new Intent(MainActivity.this,CreateProfile.class);
                i.setAction(Intent.ACTION_INSERT);
                i.putExtra("ConfigOrder", CONFIG_ORDER);
                Uri cURI = null;
                cURI = Uri.parse("content://com/sadko/androblogger/blogconfig");
                i.setData(cURI);
                startActivityForResult(i,BLOGCONFIG_REQUEST);
                finish();
       		}
		}); 
        
        this.findViewById(R.id.CreateNewPost).setOnClickListener(new OnClickListener(){
			public void onClick(View v){        		
				if(configs!=null){
					Intent i = new Intent(MainActivity.this,CreateBlogEntry.class);
					String[] s = {"",""};
					i.putExtra("ConfigOrder", CONFIG_ORDER);
					i.putExtra("PostTitleAndContent", s);
					startActivity(i);
	                finish();
				}else{
					Alert.showAlert(MainActivity.this, "Profile is not created", "Please, create profile");
				}		
       		}
		});
        
        Spinner profileSpinner = (Spinner)this.findViewById(R.id.ChoiceProfileAndViewBlog);
        DBClient db = new DBClient();
    	configs = db.getBlogNames(MainActivity.this);
    	if(configs!=null){
    		HashMap<Integer, Integer> configItemOrder = new HashMap<Integer,Integer>(configs.size());
        	for(int j=0; j<configs.size();j++) {
                try {
                	configItemOrder.put(new Integer(j), new Integer(configs.get(j).getId()));
                } catch (NullPointerException ne) {
                	Log.d(TAG,"Config items contains a null entry at "+j+"! Default to first config.");
                	configItemOrder.put(new Integer(j), new Integer(0));
                }
            }
        	Object res = new ArrayAdapter(MainActivity.this,android.R.layout.simple_spinner_item,configs);
        	ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>)res;
        	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            profileSpinner.setAdapter(adapter);
            profileSpinner.setOnItemSelectedListener(MainActivity.this);
            profileSpinner.setSelection(CONFIG_ORDER);
    	}
        this.findViewById(R.id.ViewBlog).setOnClickListener(new OnClickListener(){
			public void onClick(View v){   
				if(configs!=null){
					viewBlogPosts();
				}else{
					Alert.showAlert(MainActivity.this, "Profile is not created", "Please, create profile");
				}
			
       		}
		});
        
        this.findViewById(R.id.Exit).setOnClickListener(new OnClickListener(){
			public void onClick(View v){   
                finish();
       		}
		});
    }

	protected void viewBlogPosts() {
		viewProgress = ProgressDialog.show(MainActivity.this, "Viewing blog entries", "Starting to view blog entries...");

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
        		LinkedList<BlogConfig> configList = db.getBlogNames(MainActivity.this);
        		config = db.getBlogConfigById(MainActivity.this, configList.get(CONFIG_ORDER).getId());
        		String username = config.getUsername();
        		String password = config.getPassword();
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
            	    	resultFeed=blogapi.getAllPosts(username, password);
            	    	Log.i(TAG,"Blog entries successfully received");
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
                	Log.d(TAG,"Viewing of the blog failed!");
                	viewStatus = 4;
                }
                mHandler.post(mViewResults);
                
                if((resultFeed!=null)&&(viewOk)){
                	Intent i = new Intent(MainActivity.this,ViewBlog.class);
    				i.putExtra("ConfigOrder", CONFIG_ORDER);
    				startActivity(i);
    		        finish();
                }
        	}
        }; 
        viewThread.start();
        viewProgress.setMessage("Viewing in progress...");
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
		// TODO Auto-generated method stub
		CONFIG_ORDER=position;
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub	
	}
	
	private void showViewStatus() {
        viewProgress.dismiss();
        if(viewStatus != 5) {
        	Alert.showAlert(this,"View status","View failed! (Code "+viewStatus+")\nTry again.");
        }
    }
}