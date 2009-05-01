package com.sadko.androblogger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.sadko.androblogger.util.Alert;

public class CreateBlogEntry extends Activity {
	private static int CONFIG_ORDER = 0;

	//private static final String TAG = "CreateBlogEntry";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.createblogentry);
		Intent i = this.getIntent();
		if ((i.getStringArrayExtra("PostTitleAndContent")[0].length()!=0)&&(i.getStringArrayExtra("PostTitleAndContent")[1].length()!=0)){
			EditText postTitle = (EditText)findViewById(com.sadko.androblogger.R.id.TextPostTitle);
	        EditText postContent = (EditText)findViewById(com.sadko.androblogger.R.id.TextPostContent);
	        postTitle.setText(i.getStringArrayExtra("PostTitleAndContent")[0]);
	        postContent.setText(i.getStringArrayExtra("PostTitleAndContent")[1]);
		}
		CONFIG_ORDER=i.getIntExtra("ConfigOrder", 0);
		this.findViewById(R.id.BackToMainActivities).setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				Intent i = new Intent(CreateBlogEntry.this,MainActivity.class);
				i.putExtra("ConfigOrder", CONFIG_ORDER);
				startActivity(i);
                finish();
       		}
		});
		
    	int w = this.getWindow().getWindowManager().getDefaultDisplay().getWidth()-12;
        ((Button)this.findViewById(R.id.BackToMainActivities)).setWidth(w/2);
        ((Button)this.findViewById(R.id.Preview)).setWidth(w/2);
		
        this.findViewById(R.id.Preview).setOnClickListener(new OnClickListener(){
        	public void onClick(View v){
				if ((((EditText)findViewById(com.sadko.androblogger.R.id.TextPostTitle)).getText().length()==0)||
						(((EditText)findViewById(com.sadko.androblogger.R.id.TextPostTitle)).getText()==null)||
						(((EditText)findViewById(com.sadko.androblogger.R.id.TextPostContent)).getText().length()==0)||
						(((EditText)findViewById(com.sadko.androblogger.R.id.TextPostContent)).getText()==null)){
					Alert.showAlert(CreateBlogEntry.this,"Empty title or content","Please fill all fields");
				} else {
					Intent i = new Intent(CreateBlogEntry.this,PreviewAndPublish.class);
					String URIStr = "content://com.sadko.androblogger/blogentry/";
	                i.setData(Uri.parse(URIStr));
					EditText postTitle = (EditText)findViewById(com.sadko.androblogger.R.id.TextPostTitle);
			        EditText postContent = (EditText)findViewById(com.sadko.androblogger.R.id.TextPostContent);
					String[] titleAndContent = new String[2];
					titleAndContent[0] = postTitle.getText().toString();
					titleAndContent[1] = postContent.getText().toString();
					i.putExtra("PostTitleAndContent", titleAndContent);
					i.putExtra("ConfigOrder", CONFIG_ORDER);
					startActivity(i);
	                finish();
				}
       		}
        });  
    }
	
	public boolean onKeyDown(int keyCode, KeyEvent event) { 
    	if(keyCode==KeyEvent.KEYCODE_BACK){
    		Intent i = new Intent(CreateBlogEntry.this,MainActivity.class);
    		i.putExtra("ConfigOrder", CONFIG_ORDER);
            startActivity(i);
            finish();
            return true;
    	}
		return false; 
	}
	
}
