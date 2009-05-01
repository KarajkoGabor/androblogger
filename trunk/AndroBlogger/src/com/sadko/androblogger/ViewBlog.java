package com.sadko.androblogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.data.TextContent;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class ViewBlog extends ListActivity{
	private static int CONFIG_ORDER=0;
	//private BlogConfig config = null;
	private final String TAG = "ViewBlog";
	public static Feed resultFeed = null;
	public static Entry currentEntry = null;
	int viewStatus = 0;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewblog);
		
		Intent i = this.getIntent();
		CONFIG_ORDER=i.getIntExtra("ConfigOrder", 0);
		
		resultFeed = MainActivity.resultFeed;
		Log.i(TAG, "ResultFeed obtained from MainActivity");
		
    	int w = this.getWindow().getWindowManager().getDefaultDisplay().getWidth()-12;
        ((Button)this.findViewById(R.id.BackToMainActivity)).setWidth(w/2);
        ((Button)this.findViewById(R.id.Details)).setWidth(w/2);
		
		TextView blogTitle=(TextView)ViewBlog.this.findViewById(R.id.BlogTitle);
    	blogTitle.setText(resultFeed.getTitle().getPlainText());
    	
	    List<Map<String, Object>> resourceNames = new ArrayList<Map<String, Object>>();
        Map<String, Object> data;
        for (int j = 0; j < resultFeed.getEntries().size(); j++){
            data = new HashMap<String, Object>();
            Entry entry =  resultFeed.getEntries().get(j);
            try{
                data.put("line1", entry.getTitle().getPlainText());
                data.put("line2", ((TextContent) entry.getContent()).getContent().getPlainText());
                resourceNames.add(data);
            }
            catch (Resources.NotFoundException nfe )
            {}
        }

        SimpleAdapter notes = new SimpleAdapter(
        		this /*Context context*/, 
        		resourceNames /*List<? extends Map<String, ?>> data*/, 
        		R.layout.row /*int resource*/, 
        		new String[]{"line1", "line2"/*, "img"*/} /*String[] from*/, 
        		new int[]{R.id.text1, R.id.text2/*, R.id.img*/} /*int[] to*/);
        setListAdapter(notes);
	    
	    /*listViewArrayAdapter.add(new String[] {"1"});
	    listView.setAdapter(listViewArrayAdapter);
	    listView.setFocusableInTouchMode(true);
	    listView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
	    	@Override
	    	public void onFocusChange(View arg0, boolean arg1) {
	    		Log.i(TAG, "onFocusChanged() - view=" + arg0);
	    	}
	    });
	    
	    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView adapterView, View view, int arg2, long arg3) {
				int selectedPosition = adapterView.getSelectedItemPosition();
				Log.i(TAG, "Click on position"+selectedPosition);
			}
		});
	    */
		this.findViewById(R.id.BackToMainActivity).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ViewBlog.this,MainActivity.class);
				i.putExtra("ConfigOrder", CONFIG_ORDER);
				startActivity(i);
                finish();
			}
	    });
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) { 
    	if(keyCode==KeyEvent.KEYCODE_BACK){
    		Intent i = new Intent(ViewBlog.this,MainActivity.class);
    		i.putExtra("ConfigOrder", CONFIG_ORDER);
            startActivity(i);
            finish();
            return true;
    	}
		return false; 
	}

	/*@Override
	public void onItemClick(AdapterView parent, View v, int position, long id) {
		System.out.println("IT'S WORK!!!");
	}*/
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		/*System.out.println("\n\tListView: "+l.toString()+", " +
				"\n\tposition: "+position+", " +
				"\n\tid: "+id);*/
		currentEntry = resultFeed.getEntries().get((int)id);
		/*String postTitle = entry.getTitle().getPlainText();
		String postContent = ((TextContent) entry.getContent()).getContent().getPlainText();
		String postTimeMinSek = entry.getPublished().toString().substring(11, 16);
		String postTimeYearDate = entry.getPublished().toString().substring(0, 10);
		Alert.showAlert(ViewBlog.this, "Blog post: "+id,"TITLE: "+postTitle+"\nCONTENT: "+postContent+"\n" +
				"TIME: "+postTimeYearDate+" "+postTimeMinSek);*/
		Intent i = new Intent(ViewBlog.this,ViewPost.class);
		i.putExtra("ConfigOrder", CONFIG_ORDER);
        startActivity(i);
        finish();
		
		}
}