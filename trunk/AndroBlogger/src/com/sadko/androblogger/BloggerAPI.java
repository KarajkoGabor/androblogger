package com.sadko.androblogger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.apache.http.auth.AuthenticationException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.gdata.client.GoogleService;
import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.util.ServiceException;
import com.sadko.androblogger.util.Alert;


public class BloggerAPI implements BlogInterface, OnClickListener,
        AdapterView.OnItemSelectedListener {
        
        private String userid = null;
        private String password = null;
        private static GoogleService myService;
        private final String APPNAME="AndroBlogger";
        private final String SERVICENAME = "blogger";
        private final String TAG = "GoogleBloggerAPI";
        private final String MSG_KEY="value";
        private final String STATUS_KEY="status";
        private final String STATUS_RESPONSE_NULL = "1";
        private final String STATUS_ZERO_BLOGS = "2";
        private final String STATUS_OK = "3";
        private final String STATUS_BAD_AUTH = "4";
        private final String RESPONSE_NAMES_KEY = "response_names";
        private final String RESPONSE_IDS_KEY = "response_ids";
        private final String NO_POST_URL_FOR_ENTRY = "NO_POST_URL_FOR_ENTRY";
        private final int FETCH_BUTTON_ID = 7531;
        private URL feedUrl = null;
        private static String feedUri;
        private static final String FEED_URI_BASE = "https://www.blogger.com/feeds";
        private static final String POSTS_FEED_URI_SUFFIX = "/posts/default";
        private static final String COMMENTS_FEED_URI_SUFFIX = "/comments/default";
        private boolean connected = false; 
        private Activity parentRef = null;
        private ProgressDialog fetchProgress = null;
        private String[] blogNames = null;
        private String[] blogIds = null;
        private String[] listData = null;
        private Spinner userblogs = null;
        private int fetchStatus = 0;
        private int currentySelectedBlog = -1;
        private String postPublishURL = null;
        
        //the handler for the UI callback from the publish thread
        final Handler mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                        Bundle content = msg.getData();
                        String progressId = content.getString(MSG_KEY);
                        if(progressId != null) {
                                if(progressId.equals("1")) {
                                        fetchProgress.setMessage("Preparing...");
                                        fetchStatus = 1;
                                } else if(progressId.equals("2")) {
                                        fetchProgress.setMessage("Authenticating...");
                                        fetchStatus = 2;
                                } else if(progressId.equals("3")) {
                                        fetchProgress.setMessage("Contacting server...");
                                        fetchStatus = 3;
                                } else if(progressId.equals("4")) {
                                        fetchProgress.setMessage("Extracting response...");
                                        fetchStatus = 4;
                                } else if(progressId.equals("5")) {
                                        fetchProgress.setMessage("Done...");
                                        fetchStatus = 5;
                                }
                                String status = content.getString(STATUS_KEY);
                                if(status != null && fetchStatus == 5) {
                                        if(status.equals(STATUS_RESPONSE_NULL)) {
                                                fetchStatus = 6;
                                        } else if(status.equals(STATUS_ZERO_BLOGS)) {
                                                fetchStatus = 7;
                                        } else if(status.equals(STATUS_OK)) {
                                                // keep fetch status as 5 to indicate success to showFetchStatus method.
                                                fetchProgress.setMessage("Displaying blog names...");
                                                String names = content.getString(RESPONSE_NAMES_KEY);
                                                Log.d(TAG,"names here: "+names);
                                                String ids = content.getString(RESPONSE_IDS_KEY);
                                                Log.d(TAG,"ids here: "+ids);
                                                String [] namearr = names.split("\\|");
                                                String [] idsarr = ids.split("\\|");
                                                for(int i = 0; i < namearr.length; i++) {
                                                        Log.d(TAG,"namearr["+i+"]="+namearr[i]);
                                                }
                                                if(namearr == null || namearr.length < 1) {
                                                        fetchStatus = 8;
                                                } else if (idsarr == null || idsarr.length < 1) {
                                                        fetchStatus = 9;
                                                } else {
                                                        // extract the response from thread and feed to Spinner.
                                                        blogNames = namearr;
                                                        blogIds = idsarr;
                                                }
                                                //updateSpinner(userblogs);
                                        } else if(status.equals(STATUS_BAD_AUTH)) {
                                                fetchStatus = 10;
                                        }
                                }
                                // Continue from here by handling the responses from the metafeed fetch thread.
                        } else {
                                // panic!
                        }
                }
        };
        
        final Runnable mFetchResults = new Runnable() {
                public void run () {
                                showFetchStatus();
                }
        };
        
        /**
         * The only data we need from this config editor is the post link URL
         * of the blog that user has selected in the spinner.
         */
        
        public CharSequence getConfigEditorData() {
                if(currentySelectedBlog == -1) {
                        Alert.showAlert(parentRef,"Select a blog","You need to select one blog prior saving the configuration.");
                        return "";
                } else {
                        if(currentySelectedBlog > blogIds.length-1) {
                                Log.e(TAG, "System allows spinner index to be selected outside source data!");
                                return "";
                        }
                        return blogIds[currentySelectedBlog];
                }
        }
        
        protected BloggerAPI() {
                try {
                feedUrl = new URL("http://www.blogger.com/feeds/default/blogs");
                } catch (MalformedURLException e) {
                        Log.e(TAG,"The default blog feed url is malformed!");
                }
        }
        
        public void createOnClickListener(Activity parentRef, Button b){
        	this.parentRef = parentRef; 
            b.setOnClickListener(this);       
        }

        public boolean createPost(Activity parent, 
                        String authToken, String postUrl,
                        String titleType, String title, String contentType, String content,
                        String authorName, String authorEmail, boolean isDraft) {
            Entry entry = new Entry();
            entry.setTitle(new PlainTextConstruct(title));
            entry.setContent(new PlainTextConstruct(content));
            //Person author = new Person(authorName, null, this.userid);
            //entry.getAuthors().add(author);
            //entry.setDraft(isDraft);
            if(myService == null) {
                getAuthId(this.userid, this.password);
                Log.d(TAG,"GoogleService is null while creating post. Are you calling this from outside the PreviewDialog? ");
            }
            // if it's still null, scream
            if(myService == null) {
                Log.e(TAG,"The GoogleService is null while creating post. Are you calling this from outside the PreviewDialog?");
                return false;
            }
            try {
                URL postingUrl = new URL(postUrl);
                if(myService.insert(postingUrl, entry) != null) {
                        return true;
                } else {
                        return false;
                }
            } catch (MalformedURLException mfue) {
                Log.e(TAG,"Malformed post URL used: "+postUrl);
                return false;
            } catch (IOException ioe) {
                Log.e(TAG, "IOException when inserting post. Message:"+ioe.getMessage());
                return false;
            } catch (ServiceException se) {
                Log.e(TAG, "ServiceException when inserting post. Message:"+se.getMessage());
                return false;
            }
        }

        public Feed getAllPosts(String username, String password)
        	throws ServiceException, IOException {
        	if(myService == null) {
                getAuthId(username, password);
                Log.d(TAG,"GoogleService is null while creating post. Are you calling this from outside the PreviewDialog? ");
            }
        	//String blogId = "7036447552300041367";
        	String blogId = getBlogId(myService);
        	feedUri = FEED_URI_BASE + "/" + blogId;
        	URL feedUrl = new URL(feedUri + POSTS_FEED_URI_SUFFIX);
        	//URL feedUrl = new URL("http://www.blogger.com/feeds/default/blogs");
        	Feed resultFeed =  myService.getFeed(feedUrl, Feed.class);
        	System.out.println(resultFeed.getTitle().getPlainText());
        	/*for (int i = 0; i < resultFeed.getEntries().size(); i++) {
        		Entry entry =  resultFeed.getEntries().get(i);
        		System.out.println("\t"+(i+1)+") "+ entry.getTitle().getPlainText());
        		System.out.println("\t\tid: "+entry.getId());
        	}*/
        	return resultFeed;
        }
        
        public Feed getAllPostComments(String username, String password, String postID)
        	throws ServiceException, IOException {
        	if(myService == null) {
        		getAuthId(username, password);
        		Log.d(TAG,"GoogleService is null while creating post. Are you calling this from outside the PreviewDialog? ");
            }
        	String commentsFeedUri = feedUri + "/" + postID + COMMENTS_FEED_URI_SUFFIX;
            URL feedUrl = new URL(commentsFeedUri);
            Feed resultFeed =  myService.getFeed(feedUrl, Feed.class);
            return resultFeed;
        }

        public String getAuthId(String username, String password) {
                this.userid = username;
                this.password = password;
                myService = new GoogleService("blogger",BlogConfigBLOGGER.APPNAME);
                try {
                        myService.setUserCredentials(username, password);
                } catch (com.google.gdata.util.AuthenticationException e) {
                        Log.e(TAG,"Authentication exception! "+e.getMessage());
                }
                return "this is not really needed";
        }

        
        public String getPostUrl(String authToken) 
        	throws ServiceException, IOException{
        	String blogId = getBlogId(myService);
            //return this.postPublishURL;
        	return "https://www.blogger.com/feeds/"+blogId+"/posts/default";
            //return "http://www.blogger.com/feeds/default/blogs";
        }
        
        private static String getBlogId(GoogleService myService)
        	throws ServiceException, IOException {
        	final URL feedUrl = new URL("http://www.blogger.com/feeds/default/blogs");
        	Feed resultFeed = myService.getFeed(feedUrl, Feed.class);
        	if (resultFeed.getEntries().size() > 0) {
        		Entry entry =  resultFeed.getEntries().get(0);
        		return entry.getId().split("blog-")[1];
        	}
        throw new IOException("User has no blogs!");
        }
        
        public BloggerAPI(String userid, String password) {
                this.userid = userid;
                this.password = password;
                myService = new GoogleService(SERVICENAME, APPNAME);
                if(myService != null) {
                        try {
                                myService.setUserCredentials(this.userid, this.password);
                                connected = true;
                        } catch (com.google.gdata.util.AuthenticationException e) {
                                Log.e(TAG,"Authentication failed for user "+this.userid+".");
                        }
                } else {
                        Log.e(TAG,"Unable to create GoogleService!");
                }
        }
        
        public boolean statusOk() {
                return connected;
        }
        
        public void onClick(View arg0) {
                getUserBlogs();
        }
        
        private void getUserBlogs() {
                if(parentRef == null) {
                        Log.e(TAG, "Error: Can't get the parent blogs without a valid reference to invoking activity!");
                        throw new IllegalStateException("Cannot continue with null parent!");                   
                }
                String username = null;
                String password = null;
                EditText usernameView = (EditText)parentRef.findViewById(R.id.Username);
                if(usernameView.getText() == null || usernameView.getText().length() < 1 ) {
                    Alert.showAlert(parentRef,"Username needed","You need to give your Google username in order to continue!");
                    return;
                } else {
                    username = usernameView.getText().toString();
                }
                usernameView = null;
                EditText passwordView = (EditText)parentRef.findViewById(R.id.Password);
                if(passwordView.getText() == null || passwordView.getText().length() < 1) {
                    Alert.showAlert(parentRef,"Password needed", "You need to give your Google password in order to continue!");
                    return;
                } else {
                    password = passwordView.getText().toString();
                }
                final String fetchUsername = username;
                final String fetchPassword = password;
                fetchProgress = ProgressDialog.show(parentRef, "Verifying the data", "Starting to verify blogs...");
                Thread fetchGoogleBlogs = new Thread() {
                        public void run() {
                                Feed resultFeed = null;
                                Bundle status = new Bundle();
                                Message statusMsg = mHandler.obtainMessage();
                                status.putString(MSG_KEY, "1");
                                statusMsg.setData(status);
                                mHandler.sendMessage(statusMsg);
                                Log.d(TAG,"Querying Blogger blog, URL:"+feedUrl+", user: "+fetchUsername+", pass:"+fetchPassword);
                                GoogleService myFetchService = new GoogleService("blogger",BlogConfigBLOGGER.APPNAME);
                                statusMsg = mHandler.obtainMessage();
                                status.putString(MSG_KEY, "2");
                                statusMsg.setData(status);
                                mHandler.sendMessage(statusMsg);
                                try {
                                        myFetchService.setUserCredentials(fetchUsername, fetchPassword);
                                } catch (com.google.gdata.util.AuthenticationException e) {
                                        Log.e(TAG,"Authentication exception! "+e.getMessage());
                                        statusMsg = mHandler.obtainMessage();
                                        status.putString(STATUS_KEY,STATUS_BAD_AUTH);
                                        status.putString(MSG_KEY,"5");
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
                                        resultFeed = myFetchService.getFeed(feedUrl, Feed.class);
                                } catch (IOException e) {
                                        e.printStackTrace();
                                } catch (ServiceException e) {
                                        e.printStackTrace();
                                }
                                statusMsg = mHandler.obtainMessage();
                                
                                if(resultFeed == null) {
                                        status.putString(STATUS_KEY, STATUS_RESPONSE_NULL);
                                        status.putString(MSG_KEY, "5");
                                        statusMsg.setData(status);
                                        mHandler.sendMessage(statusMsg);
                                        mHandler.post(mFetchResults);
                                        return;
                                } else if( resultFeed.getEntries() == null || resultFeed.getEntries().size() == 0) {
                                        status.putString(STATUS_KEY, STATUS_ZERO_BLOGS);
                                        status.putString(MSG_KEY, "5");
                                        statusMsg.setData(status);
                                        mHandler.sendMessage(statusMsg);
                                        mHandler.post(mFetchResults);
                                        return;
                                } else {
                                        //Log.d(TAG,"Result feed: "+resultFeed.toString());
                                        status.putString(STATUS_KEY, STATUS_OK);
                                        status.putString(MSG_KEY, "4");
                                }
                                statusMsg.setData(status);
                                mHandler.sendMessage(statusMsg);
                                StringBuffer titles = new StringBuffer();
                                StringBuffer ids = new StringBuffer();
                                Log.d(TAG,(resultFeed.getTitle().getPlainText()));
                                for (int i = 0; i < resultFeed.getEntries().size(); i++) {
                                        Entry entry = resultFeed.getEntries().get(i);
                                        titles.append(entry.getTitle().getPlainText()+"|");
                                        List<Link> links = entry.getLinks();
                                        Iterator<Link> iter = links.iterator();
                                        Log.d(TAG,"This entry has links:");
                                        boolean postFound = false;
                                        while(iter.hasNext()) {
                                                Link link = iter.next();
                                                String rel = link.getRel();
                                                String href = link.getHref();
                                                String type = link.getType();
                                                Log.d(TAG,"<link rel ='"+rel+"' type = '"+type+"' href = '"+href+"'/>");
                                                if(rel.endsWith("#post")) {
                                                        postFound = true;
                                                        ids.append(""+href+"|");
                                                } 
                                        }
                                        if(!postFound) {
                                                ids.append(NO_POST_URL_FOR_ENTRY+"|");
                                        }
                                        
                                }
                                statusMsg = mHandler.obtainMessage();
                                status.putString(RESPONSE_NAMES_KEY,titles.toString());
                                status.putString(RESPONSE_IDS_KEY, ids.toString());
                                status.putString(MSG_KEY, "5");
                                statusMsg.setData(status);
                                mHandler.sendMessage(statusMsg);
                                myFetchService = null;
                                mHandler.post(mFetchResults);
                        }
                }; // end Thread
                fetchGoogleBlogs.start();
                fetchProgress.setMessage("Started to fetch your blogs...");
        } // end Method
        
        private void showFetchStatus() {
                fetchProgress.dismiss();
                if(fetchStatus == 5) {
                        Alert.showAlert(parentRef,"Status","Blogs verified OK!");
                        // activity should finish after ok.
                        // disable the username and password so that user can't change them anymore
                        // since they are only valid for fetched blogs...
                        EditText username = (EditText)parentRef.findViewById(R.id.Username);
                        EditText password = (EditText)parentRef.findViewById(R.id.Password);
                        Button fetchButton = (Button)parentRef.findViewById(FETCH_BUTTON_ID);
                        if(username != null) {
                                username.setEnabled(false);
                        }
                        if(password != null) {
                                password.setEnabled(false);
                        }
                        if(fetchButton != null) {
                                fetchButton.setEnabled(false);
                        }
                } else if (fetchStatus == 6) {
                        Alert.showAlert(parentRef,"Status","Can't find any blogs for this user (null answer).");
                } else if (fetchStatus == 7) {
                        Alert.showAlert(parentRef,"Status","Can't find any blogs for this user.");
                } else if (fetchStatus == 8) {
                        Alert.showAlert(parentRef,"Status","Can't extract blog names from respose.");
                }  else if (fetchStatus == 9) {
                        Alert.showAlert(parentRef,"Status","Can't extract blog ids from response.");
                } else if  (fetchStatus == 10) {
                        Alert.showAlert(parentRef,"Status","Authentication failed. Did you enter the username and password correctly?");
                } else {
                        Alert.showAlert(parentRef,"Status","Fetch of blogs failed! (Code "+fetchStatus+")");
                }
        }
        
        private void updateSpinner(Spinner list) { 
        if(blogNames != null) {
                list.setVisibility(View.VISIBLE);
                listData = new String[blogNames.length];
                for(int i = 0; i < blogNames.length; i++) {
                	listData[i] = blogNames[i];
                }
                Object res = new ArrayAdapter(parentRef,android.R.layout.simple_spinner_item,listData);
                ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>)res;
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                list.setAdapter(adapter);
                list.setOnItemSelectedListener(this);
        } else {
                this.currentySelectedBlog = -1;
                list.setVisibility(View.INVISIBLE);
        }
        }   
               
        public void setInstanceConfig(CharSequence config) {
                if(config != null) {
                        this.postPublishURL = config.toString();
                } else {
                        Log.e(TAG,"Trying to set a null installce config when configuring Google Blogger API!");
                }
                
        }

        public void onItemSelected(AdapterView parent, View v, int position, long id) {
                this.currentySelectedBlog = position;        
        }
            
        public void onNothingSelected(AdapterView arg0) {
                this.currentySelectedBlog = -1;
        }
        
        
        
}