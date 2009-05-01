package com.sadko.androblogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.data.TextContent;
import com.google.gdata.data.blogger.PostCommentFeed;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class ViewComments extends ListActivity{
	private static int CONFIG_ORDER=0;
	private final String TAG = "ViewComments";
	public static Entry currentEntry = null;
	public static Feed resultCommentFeed = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewcomments);
		
		Intent i = this.getIntent();
		CONFIG_ORDER=i.getIntExtra("ConfigOrder", 0);
		
		currentEntry=ViewPost.currentEntry;
		Log.i(TAG, "CurrentEntry obtained from ViewPost");
		
		resultCommentFeed=ViewPost.resultCommentFeed;
		Log.i(TAG, "ResultCommentFeed obtained from ViewPost");
		
		TextView postTitle=(TextView)(this.findViewById(R.id.PostTitle));
		postTitle.setText(currentEntry.getTitle().getPlainText());
		
		TextView postContent=(TextView)(this.findViewById(R.id.PostContent));
		postContent.setText(((TextContent)currentEntry.getContent()).getContent().getPlainText());
		
	    List<Map<String, Object>> resourceNames = new ArrayList<Map<String, Object>>();
        Map<String, Object> data;
        for (int j = resultCommentFeed.getEntries().size(); j > 0;  j--){
            data = new HashMap<String, Object>();
            Entry commentEntry =  resultCommentFeed.getEntries().get(j-1);
            try{
                data.put("line1", commentEntry.getAuthors().get(0).getName());
                data.put("line3", commentEntry.getUpdated().toStringRfc822().substring(0, commentEntry.getUpdated().toStringRfc822().length()-5));
                data.put("line2", ((TextContent) commentEntry.getContent()).getContent().getPlainText());
                resourceNames.add(data);
            }
            catch (Resources.NotFoundException nfe )
            {}
        }

        SimpleAdapter notes = new SimpleAdapter(
        		this /*Context context*/, 
        		resourceNames /*List<? extends Map<String, ?>> data*/, 
        		R.layout.commentrow /*int resource*/, 
        		new String[]{"line1", "line3", "line2"} /*String[] from*/, 
        		new int[]{R.id.text1, R.id.text3, R.id.text2/*, R.id.img*/} /*int[] to*/);
        setListAdapter(notes);
	    
		this.findViewById(R.id.TopPanel).setBackgroundResource(android.R.drawable.alert_dark_frame);
        
		this.findViewById(R.id.BackToViewPost).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ViewComments.this,ViewPost.class);
				i.putExtra("ConfigOrder", CONFIG_ORDER);
				startActivity(i);
                finish();
			}
		});
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) { 
    	if(keyCode==KeyEvent.KEYCODE_BACK){
    		Intent i = new Intent(ViewComments.this,ViewPost.class);
    		i.putExtra("ConfigOrder", CONFIG_ORDER);
            startActivity(i);
            finish();
            return true;
    	}
		return false; 
	}

}
