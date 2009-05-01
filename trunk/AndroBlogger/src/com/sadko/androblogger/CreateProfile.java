package com.sadko.androblogger;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.sadko.androblogger.db.DBClient;
import com.sadko.androblogger.util.Alert;


public class CreateProfile extends Activity {
	private EditText textProfile, textUsername, textPassword;
	BlogInterface bi;
	BlogConfig myBlogConfig = null;
	private final String TAG = "CreateProfile";
	private int mState = 0;
	private static final int STATE_INSERT = 0;
    private static int CONFIG_ORDER=0;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// TODO Auto-generated method stub
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.createprofile);
        final Intent intent = getIntent();
        CONFIG_ORDER=intent.getIntExtra("ConfigOrder", 0);
        if(intent.getAction().equals(Intent.ACTION_INSERT)) {
            mState = STATE_INSERT;
            myBlogConfig = new BlogConfig();
            myBlogConfig.setPostmethod(BlogConfigBLOGGER.getInterfaceNumberByType(BlogConfigBLOGGER.BlogInterfaceType.BLOGGER));
            myBlogConfig.setPostConfig("");
            if(mState == STATE_INSERT) {
                createConfigDependentFields(myBlogConfig);
            }
        }
        int w = this.getWindow().getWindowManager().getDefaultDisplay().getWidth()-12;
        ((Button)this.findViewById(R.id.BackToMainActivity)).setWidth(w/3);
        ((Button)this.findViewById(R.id.Save)).setWidth(w/3);
        ((Button)this.findViewById(R.id.FETCH_BUTTON_ID)).setWidth(w/3);
        
        
        this.findViewById(R.id.BackToMainActivity).setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				Intent i = new Intent(CreateProfile.this,MainActivity.class);
				i.putExtra("ConfigOrder", CONFIG_ORDER);
                startActivity(i);
                finish();
       		}
		}); 
        this.findViewById(R.id.Save).setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				textProfile = (EditText)findViewById(R.id.ProfileName);
		        textUsername = (EditText)findViewById(R.id.Username);
		        textPassword = (EditText)findViewById(R.id.Password);
                if(textPassword == null || textPassword.getText() == null) {
                        Log.d(TAG,"password editor view is null when trying to read!");
                        return;
                }
                if (textUsername == null || textUsername.getText() == null) {
                        Log.d(TAG,"username editor view is null when trying to read!");
                        return;
                }
                if (textProfile == null || textProfile.getText() == null) {
                        Log.d(TAG,"blogname editor view is null when trying to read!");
                        return;
                }
                String blognameStr = textProfile.getText().toString();
                String usernameStr = textUsername.getText().toString();
                String passwordStr = textPassword.getText().toString();
                
                if(blognameStr.length() < 1) {
                        Alert.showAlert(CreateProfile.this,"Empty name of profile","You need to have a name for this profile.");
                        return;
                }       
                if(usernameStr.length() < 1) {
                        Alert.showAlert(CreateProfile.this,"Empty username","You need to have a username for this blog.");
                        return;
                } 
                if(passwordStr.length() < 1) {
                        Alert.showAlert(CreateProfile.this,"Empty password","You need to have a password for this blog.");
                        return;
                } 
                myBlogConfig.setBlogname(textProfile.getText().toString());
                myBlogConfig.setUsername(textUsername.getText().toString());
                myBlogConfig.setPassword(textPassword.getText().toString());
                /*if(bi != null) {
                        CharSequence cs = bi.getConfigEditorData();
                        myBlogConfig.setPostConfig(cs);
                } else {
                        Alert.showAlert(CreateProfile.this,"nag", "nag");
                }*/
                //remember that the post method (i.e. which type of blog this is)
                //is already set by the OnItemSelected callback.
                DBClient conn = new DBClient();
                if(conn != null) {
                        if(mState == STATE_INSERT) {
                                conn.insert(CreateProfile.this,myBlogConfig);
                                Log.d(TAG,"Blog Config saved to database.");
                                Alert.showAlert(CreateProfile.this,"Success","Your profile has been successfully saved.");
                        }/*else if(mState == STATE_EDIT) {
                                conn.update(this,myBlogConfig);
                                Log.d(TAG,"Blog Config with id="+myBlogConfig.getId()+"updated in database.");
                        }*/
                } else {
                        Alert.showAlert(CreateProfile.this,"ERROR","Unable to save configuration. Check your device memory.");
                        Log.e(TAG, "ERROR saving blog config to DB due to DBUtil error.");
                }
        		}
			});
    }
    
    private void createConfigDependentFields(BlogConfig bcb) {
    	BlogConfigBLOGGER.BlogInterfaceType blogtype = BlogConfigBLOGGER.getInterfaceTypeByNumber(bcb.getPostmethod());
        bi = BlogInterfaceFactory.getInstance(blogtype);
        bi.setInstanceConfig(myBlogConfig.getPostConfig());
        Button verify = (Button)findViewById(R.id.FETCH_BUTTON_ID);
        if(bi != null) {
                bi.createOnClickListener(this, verify);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) { 
    	if(keyCode==KeyEvent.KEYCODE_BACK){
    		Intent i = new Intent(CreateProfile.this,MainActivity.class);
    		i.putExtra("ConfigOrder", CONFIG_ORDER);
    		startActivity(i);
            finish();
            return true;
    	}
		return false; 
	}
    
    
    
/*    
    private void shortMessage(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        Float horizontalMargin = toast.getHorizontalMargin();
        toast.setMargin(horizontalMargin, getRequestedOrientation());
    }
    
    private void longMessage(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        Float horizontalMargin = toast.getHorizontalMargin();
        toast.setMargin(horizontalMargin, getRequestedOrientation());
        toast.show();  
    }
*/  
}
