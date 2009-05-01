package com.sadko.androblogger.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.sadko.androblogger.BlogConfig;
import com.sadko.androblogger.editor.SpannableBufferHelper;


public class DBClient {
        
        private SQLiteDatabase db = null;
        private static final String TAG = "DBUtil";
        private static final String CREATE_ENTRY_TABLE = 
                                "CREATE TABLE IF NOT EXISTS ENTRY ("+
                                "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                                "published_in INTEGER," +
                                "title TEXT,"+
                                "blogentry TEXT,"+
                                "created TEXT"+
                                ");";
        private static final String CREATE_BLOGCONFIG_TABLE =
                                "CREATE TABLE IF NOT EXISTS BLOGCONFIG ("+
                                "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                                "blogname TEXT,"+
                                "rssfeedurl TEXT,"+
                                "lastwritten TEXT,"+
                                "lastentry INT," +
                                "postconfig TEXT," +
                                "username TEXT," +
                                "password TEXT," +
                                "postmethod INT"+
                                ");";   
        private static final String CREATE_SETTINGS_TABLE = 
                                "CREATE TABLE IF NOT EXISTS SETTINGS ("+
                                "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                                "settingname TEXT,"+
                                "settingtitle TEXT,"+
                                "settingvalue TEXT"+
                                ");";

        public boolean testDatabaseReady(Context app) {
                DBHelper helper = new DBHelper(app,DBConstants.DB_NAME,null, DBConstants.DB_VER);
                Log.d(TAG,"testDataBaseReady Called... helper is "+helper);
                db = helper.getWritableDatabase();
                Log.d(TAG,"getWritableDatabase called, db is "+db);
                if(db == null) {
                        return false;
                } else {
                        db.close();
                        return true;
                }
        }
        
        private boolean isDatabaseReady(Context app) {
                DBHelper helper = new DBHelper(app,DBConstants.DB_NAME,null, DBConstants.DB_VER);
                Log.d(TAG,"isDataBaseReady Called... helper is "+helper);
                db = helper.getWritableDatabase();
                Log.d(TAG,"getWritableDatabase called, db is "+db);
                if(db == null) {
                        return false;
                } else {
                        return true;
                }
        }
                        
        
        public boolean insert(Context app, BlogConfig b) {
                String sql = "INSERT INTO BLOGCONFIG (blogname, rssfeedurl, "+
                "lastwritten, lastentry, postconfig, postmethod, username, password) VALUES "+
                "(?,?,?,?,?,?,?,?)";
                
                if(isDatabaseReady(app)) {
                        try {
                                SQLiteStatement insertStmt = db.compileStatement(sql);
                                insertStmt.bindString(1,b.getBlogname());
                                //seems that binding null crashes the VM on the emulator m5-rc15_linux-x86
                                insertStmt.bindString(3,(b.getLastwritten() == null)? "" : b.getLastwritten());
                                insertStmt.bindLong(4,b.getLastentry());
                                insertStmt.bindString(5,b.getPostConfig().toString());
                                insertStmt.bindLong(6,b.getPostmethod());
                                insertStmt.bindString(7,b.getUsername());
                                insertStmt.bindString(8,b.getPassword());
                                insertStmt.execute();
                        } catch (SQLException e) {
                                Log.e(TAG, "SQLException while executing: "+sql+"\n");
                                return false;
                        }
                        db.close();
                        return true;
                } else {
                        Log.d(TAG,"DB was not open while inserting BlogConfig!");
                        return false;
                }
        }
        
        public boolean insert(Context app,BlogEntry b) {
                SpannableBufferHelper helper = new SpannableBufferHelper();
                String sql = "INSERT INTO ENTRY (published_in, title, blogentry, created) VALUES "+
                "(?,?,?,?)";
                if(isDatabaseReady(app)) {
                        try {
                                SQLiteStatement insertStmt = db.compileStatement(sql);
                                insertStmt.bindLong(1,b.getPublishedIn());
                                insertStmt.bindString(2, b.getTitle());
                                insertStmt.bindString(3,helper.getSerializedSequence(b.getBlogEntry()));
                                insertStmt.bindString(4,dateToString(b.getCreated()));
                                insertStmt.execute();
                        } catch (SQLException e) {
                                Log.e(TAG, "SQLException while executing: "+sql+"\n");
                                return false;
                        }
                        db.close();
                        return true;
                } else {
                        Log.d(TAG,"DB was not open while inserting BlogEntryBean!");
                        return false;
                }
        }
        
        public long insertAndReturnRow(Context app,BlogEntry b) {
                SpannableBufferHelper helper = new SpannableBufferHelper();
                ContentValues newRow = new ContentValues();
                newRow.put("published_in", b.getPublishedIn());
                newRow.put("title", b.getTitle());
                newRow.put("blogentry", helper.getSerializedSequence(b.getBlogEntry()));
                newRow.put("created", dateToString(b.getCreated()));
                if(isDatabaseReady(app)) {
                        long res = -1;
                        try {
                                res = this.db.insert("ENTRY", "NULL", newRow);
                        } catch (SQLException e) {
                                Log.e(TAG, "SQLException while executing insert query: "+e.getMessage()+"\n");
                                return -1;
                        }
                        db.close();
                        return res;
                } else {
                        Log.d(TAG,"DB was not open while inserting SettingsBean!");
                        return -1;
                }
        }
        
        public boolean insert(Context app,Setting s) {
                String sql = "INSERT INTO SETTINGS (settingname, settingtitle, settingvalue) VALUES "+
                "(?,?,?)";
                if(isDatabaseReady(app)) {
                        try {
                                SQLiteStatement insertStmt = db.compileStatement(sql);
                                insertStmt.bindString(1,s.getSettingname());
                                insertStmt.bindString(2,s.getSettingtitle());
                                insertStmt.bindString(3,s.getSettingvalue());
                                insertStmt.execute();
                        } catch (SQLException e) {
                                Log.e(TAG, "SQLException while executing: "+sql+"\n");
                                return false;
                        }
                        return true;
                } else {
                        Log.d(TAG,"DB was not open while inserting SettingsBean!");
                        return false;
                }       
        }
        
        public BlogConfig[] getConfiguredBlogs(Context app) {
                final String SQL = "SELECT id, blogname from BLOGCONFIG";
                Cursor cur = null;
                BlogConfig[] bcbs = null;
                if(isDatabaseReady(app)) {
                        cur = db.rawQuery(SQL, null);
                } else {
                        Log.e(TAG,"Database is not open when getting blog names and IDs!");
                        return null;
                }
                if((cur != null) && (cur.getCount() > 0)) {
                        bcbs = new BlogConfig[cur.getCount()];
                        cur.moveToFirst();
                        for(int i = 0; i < cur.getCount(); i++) {
                                BlogConfig b = new BlogConfig();
                                b.setId(cur.getInt(0));
                                b.setBlogname(cur.getString(1));
                                bcbs[i] = b;
                                cur.moveToNext();
                        }
                        cur.close();
                }
                db.close();
                return bcbs;
        }
        
        public LinkedList<BlogConfig> getBlogNames(Context ctx) {
                LinkedList<BlogConfig> res = null;
                BlogConfig [] bcbs = null;
                bcbs = getConfiguredBlogs(ctx);
                if(bcbs != null) {
                        res = new LinkedList<BlogConfig>();
                        for(int i = 0; i < bcbs.length; i++) {
                                res.add(bcbs[i]);
                        }
                        return res;
                } else {
                        Log.e(TAG,"Failed to get the blog configs from DB!");
                        return null;
                }
        }
        
        public LinkedList<BlogEntry> getBlogEntriesByConfig(Context ctx, int id) {
                LinkedList<BlogEntry> res = null;
                BlogEntry [] bes = null;
                bes = getEntriesByBlogId(""+id,ctx);
                if(bes != null) {
                        res = new LinkedList<BlogEntry>();
                        for(int i = 0; i < bes.length; i++) {
                                res.add(bes[i]);
                        }
                        return res;
                } else {
                        Log.e(TAG,"Failed to get the blog entries from DB!");
                        return null;
                }
        }


        
        public BlogEntry[] getEntriesByBlogId(String blogid,Context c) {
                final String SQL = "SELECT id, title, blogentry, created from ENTRY where published_in = ?"; 
                Cursor cur = null;
                BlogEntry[] bcbs = null;
                SpannableBufferHelper helper = new SpannableBufferHelper();
                if(isDatabaseReady(c)) {
                        cur = db.rawQuery(SQL, new String[]{blogid});
                } else {
                        Log.e(TAG,"Database is not open when getting blog entries!");
                        return null;
                }
                if((cur != null) && (cur.getCount() > 0)) {
                        bcbs = new BlogEntry[cur.getCount()];
                        cur.moveToFirst();
                        for(int i = 0; i < cur.getCount(); i++) {
                                BlogEntry b = new BlogEntry();
                                b.setId(cur.getInt(0));
                                b.setTitle(cur.getString(1));
                                b.setBlogEntry(helper.deSerializeStringToSequence(cur.getString(2),c));
                                b.setCreated(stringToDate(cur.getString(4)));
                                bcbs[i] = b;
                                cur.moveToNext();
                        }
                        cur.close();
                }
                db.close();
                return bcbs;    
        }
        
        public BlogEntry[] getBlogEntries(Context c) {
                final String SQL = "SELECT id, title, blogentry, created from ENTRY"; 
                SpannableBufferHelper helper = new SpannableBufferHelper();
                Cursor cur = null;
                BlogEntry[] bcbs = null;
                if(isDatabaseReady(c)) {
                        cur = db.rawQuery(SQL,null);
                } else {
                        Log.e(TAG,"Database is not open when getting blog entries!");
                        return null;
                }
                if((cur != null) && (cur.getCount() > 0)) {
                        bcbs = new BlogEntry[cur.getCount()];
                        cur.moveToFirst();
                        for(int i = 0; i < cur.getCount(); i++) {
                        	BlogEntry b = new BlogEntry();
                                b.setId(cur.getInt(0));
                                b.setTitle(cur.getString(1));
                                b.setBlogEntry(helper.deSerializeStringToSequence(cur.getString(2),c));
                                b.setCreated(stringToDate(cur.getString(3)));
                                bcbs[i] = b;
                                cur.moveToNext();
                        }
                        cur.close();
                }
                db.close();
                return bcbs;    
        }
        
       
        public BlogEntry getBlogEntryById(Context c, int id) {
                final String SQL = "SELECT id, published_in, title, blogentry, created from ENTRY where id = ?"; 
                Cursor cur = null;
                SpannableBufferHelper helper = new SpannableBufferHelper();
                BlogEntry eb = null;
                if(isDatabaseReady(c)) {
                        cur = db.rawQuery(SQL, new String[]{""+id});
                } else {
                        Log.e(TAG,"Database is not open when getting blog entries!");
                        return null;
                }
                if((cur != null) && (cur.getCount() == 1)) {
                        eb = new BlogEntry();
                        cur.moveToFirst();
                        eb.setId(cur.getInt(0));
                        eb.setPublishedIn(cur.getInt(1));
                        eb.setTitle(cur.getString(2));
                        eb.setBlogEntry(helper.deSerializeStringToSequence(cur.getString(3),c));
                        eb.setCreated(stringToDate(cur.getString(4)));
                        cur.close();
                }
                db.close();
                return eb;
        }
        
        public int getBlogIdByTitleAndType(Context app,String title, int type) {
                final String SQL = "SELECT id from ENTRY where title = ? and published_in = ?";
                Cursor cur = null;
                int res = -1;
                if(isDatabaseReady(app)) {
                        Log.d(TAG,"Querying: "+SQL+",title="+title+",type="+type);
                        cur = db.rawQuery(SQL, new String[]{""+title,""+type});
                } else {
                        Log.e(TAG,"Database is not open when getting blog id (by title and type)!");
                        return -1;
                }
                if((cur != null) && (cur.getCount() == 1)) {
                        cur.moveToFirst();
                        res = cur.getInt(0);
                        Log.d(TAG,"Got result: "+res);
                        cur.close();
                }
                db.close();
                return res;
        }
        
        public BlogConfig getBlogConfigById(Context app,int id) {
                final String SQL = 
                        "SELECT blogname, rssfeedurl, lastwritten, lastentry, postconfig, username, password, postmethod FROM BLOGCONFIG where id = ?"; 
                Cursor cur = null;
                BlogConfig cb = null;
                if(isDatabaseReady(app)) {
                        cur = db.rawQuery(SQL, new String[]{""+id});
                } else {
                        Log.e(TAG,"Database is not open when getting blog entries!");
                        return null;
                }
                if((cur != null) && (cur.getCount() == 1)) {
                        cb = new BlogConfig();
                        cur.moveToFirst();
                        cb.setId(id);
                        cb.setBlogname(cur.getString(0));
                        cb.setLastwritten(cur.getString(2));
                        cb.setLastentry(cur.getInt(3));
                        cb.setPostConfig(cur.getString(4));
                        cb.setUsername(cur.getString(5));
                        cb.setPassword(cur.getString(6));
                        cb.setPostmethod(cur.getInt(7));
                        cur.close();
                }
                db.close();
                return cb;
        }
       
      
        public void update(Context app,final BlogConfig mb) {
                int id = -1;
                String SQL = null;
                if(mb != null) {
                        id = mb.getId();
                }
                if(id != -1) {
                        SQL = "UPDATE BLOGCONFIG SET "+
                        "blogname = ?,"+
                        "rssfeedurl = ?,"+
                        "lastwritten = ?,"+
                        "lastentry = ?,"+
                        "postconfig = ?,"+
                        "username = ?,"+
                        "password = ?,"+
                        "postmethod = ? "+
                        "WHERE id = ?";
                } else {
                        Log.e(TAG,"Failed to update BLOGCONFIG since the id is not good!");
                }
                if(isDatabaseReady(app)) {
                        SQLiteStatement updateStmt = db.compileStatement(SQL);
                        updateStmt.bindString(1,mb.getBlogname());
                        updateStmt.bindString(3,mb.getLastwritten());
                        updateStmt.bindLong(4,mb.getLastentry());
                        updateStmt.bindString(5,mb.getPostConfig().toString());
                        updateStmt.bindString(6,mb.getUsername());
                        updateStmt.bindString(7,mb.getPassword());
                        updateStmt.bindLong(8,mb.getPostmethod());
                        updateStmt.execute();
                        db.close();
                        Log.d(TAG,"Executing: "+SQL);
                } else {
                        Log.e(TAG,"Database is not open updating blog config entries!");
                }
                Log.d(TAG,"Blog config updated with, id = "+id);
        }
        /**
         * Update SettingBean in the DB, have ID there in the bean, otherwise it won't
         * work!
         * @param mb
         */
        
        public void update(Context app,final Setting mb) {
                int id = -1;
                String SQL = null;
                if(mb != null) {
                        id = mb.getId();
                }
                if(id != -1) {
                        SQL = "UPDATE SETTINGS SET "+
                        "settingname = ?,"+
                        "settingtitle = ?,"+
                        "settingvalue = ? "+
                        "WHERE id = ?";
                } else {
                        Log.e(TAG,"Failed to update SETTINGS since the id is not good!");
                }
                if(isDatabaseReady(app)) {
                        Log.d(TAG,"Executing: "+SQL);
                        SQLiteStatement updateStmt = db.compileStatement(SQL);
                        updateStmt.bindString(1,mb.getSettingname());
                        updateStmt.bindString(2,mb.getSettingtitle());
                        updateStmt.bindString(3,mb.getSettingvalue());
                        updateStmt.bindLong(4, mb.getId());
                        updateStmt.execute();
                        db.close();
                } else {
                        Log.e(TAG,"Database is not open when updating settings!");
                }
                Log.d(TAG,"Settings updated with, id = "+id);
        }

        
        public void update(Context app,final BlogEntry mb) {
                SpannableBufferHelper helper = new SpannableBufferHelper();
                int id = -1;
                String SQL = null;
                if(mb != null) {
                        id = mb.getId();
                }
                if(id != -1) {
                        SQL = "UPDATE ENTRY SET "+
                        "published_in = ?,"+
                        "title = ?,"+
                        "blogentry = ?,"+
                        "created = ? "+
                        "WHERE id = ?";
                } else {
                        Log.e(TAG,"Failed to update BLOGENTRY since the id is not good!");
                }
                if(isDatabaseReady(app)) {
                        SQLiteStatement updateStmt = db.compileStatement(SQL);
                        updateStmt.bindLong(1,mb.getPublishedIn());
                        updateStmt.bindString(2, mb.getTitle());
                        updateStmt.bindString(3,helper.getSerializedSequence(mb.getBlogEntry()));
                        updateStmt.bindString(4,dateToString(mb.getCreated()));
                        updateStmt.bindLong(5, mb.getId());
                        updateStmt.execute();
                        db.close();
                        Log.d(TAG,"Executing: "+SQL);
                } else {
                        Log.e(TAG,"Database is not open when updating blog entry!");
                }
                Log.d(TAG,"Blog entry updated with, id = "+id);
        }
        
  
        
        public BlogConfig getBlogConfigByUserPassAndType(Context app,int postmethod, String username, String password) {
                final String SQL = 
                        "SELECT blogname, rssfeedurl, lastwritten, lastentry, postconfig, "+
                                        "username, password, postmethod FROM BLOGCONFIG where postmethod = ? and "+
                                        "username = ? and password = ?";
                Cursor cur = null;
                BlogConfig cb = null;
                if(isDatabaseReady(app)) {
                        cur = db.rawQuery(SQL, new String[]{""+postmethod,username,password});
                } else {
                        Log.e(TAG,"Database is not open when getting blog entries!");
                        return null;
                }
                if((cur != null) && (cur.getCount() == 1)) {
                        cb = new BlogConfig();
                        cur.moveToFirst();
                        cb.setBlogname(cur.getString(0));
                        cb.setLastwritten(cur.getString(2));
                        cb.setLastentry(cur.getInt(3));
                        cb.setPostConfig(cur.getString(4));
                        cb.setUsername(cur.getString(5));
                        cb.setPassword(cur.getString(6));
                        cb.setPostmethod(cur.getInt(7));
                        cur.close();
                }
                db.close();
                return cb;
        }
        
        public void deleteBlogConfigItem(Context app,int id) {
                //final String SQL = "DELETE from BLOGCONFIG WHERE id = ?";
                if(isDatabaseReady(app)) {
                        db.delete("BLOGCONFIG","id=?",new String[]{""+id});
                        db.close();
                }
        }
        public void deleteBlogEntryItem(Context app,int id) {
                //final String SQL = "DELETE from ENTRY WHERE id = ?";
                if(isDatabaseReady(app)) {
                        db.delete("ENTRY", "id=?", new String[]{""+id});
                        db.close();
                }
        }
        public void deleteMemoItem(Context app,int id) {
                //final String SQL = "DELETE from MEMO WHERE id = ?";
                if(isDatabaseReady(app)) {
                        db.delete("MEMO", "id=?", new String[]{""+id});
                        db.close();
                }
        }
        public void deleteSettingsItem(Context app,int id) {
                //final String SQL = "DELETE from SETTINGS WHERE id = ?";
                if(isDatabaseReady(app)) {
                        db.delete("SETTINGS","id=?", new String[]{""+id});
                        db.close();
                }
        }
        
        public void deleteImageRepositoryItem(Context app,int id) {
                //final String SQL = "DELETE from IMAGEREPO WHERE id = ?";
                if(isDatabaseReady(app)) {
                        db.delete("IMAGEREPO","id=?", new String[]{""+id});
                        db.close();
                }
        }
        
        private Date stringToDate(String dateStr) {
                SimpleDateFormat df = new SimpleDateFormat(DBConstants.DB_DATE_FORMAT);
                Date d = null;
                try {
                        d = df.parse(dateStr);
                } catch (ParseException e) {
                        Log.e(TAG,"Failed to parse date to SQL date... Revert to now.");
                        return new Date(System.currentTimeMillis());
                }
                return d;
        }
        
        private String dateToString(Date date) {
                SimpleDateFormat df = new SimpleDateFormat(DBConstants.DB_DATE_FORMAT); 
                return df.format(date);
        }
        
        
        private void createDefaultSettings(Context app) {
                if(isDatabaseReady(app)) {
                        Log.d(TAG,"Deleting the settings table contents.");
                        db.delete("SETTINGS",null,null);
                        db.close();
                }
                for(int i = 0; i < DBConstants.DEFAULT_SETTINGS.length; i++) {
                        Log.d(TAG,"Creating default setting: "+DBConstants.DEFAULT_SETTINGS[i]);
                        this.insert(app,DBConstants.DEFAULT_SETTINGS[i]);
                }
        }
        
        public Setting[] getSavedSettings(Context app) {
                final String SQL = "SELECT id, settingname, settingtitle, settingvalue from SETTINGS"; 
                Cursor cur = null;
                Setting[] settings = null;
                if(isDatabaseReady(app)) {
                        cur = db.rawQuery(SQL,null);
                } else {
                        Log.e(TAG,"Database is not open when getting settings!");
                        return null;
                }
                if((cur != null) && (cur.getCount() > 0)) {
                        settings = new Setting[cur.getCount()];
                        cur.moveToFirst();
                        for(int i = 0; i < cur.getCount(); i++) {
                                Setting b = new Setting();
                                b.setId(cur.getInt(0));
                                b.setSettingname(cur.getString(1));
                                b.setSettingtitle(cur.getString(2));
                                b.setSettingvalue(cur.getString(3));
                                settings[i] = b;
                                cur.moveToNext();
                        }       
                        cur.close();
                }
                db.close();
                return settings;        
        }
        
        public Setting getSettingByName(Context app, String name) {
                final String SQL = 
                        "SELECT id, settingname, settingtitle, settingvalue FROM SETTINGS where settingname = ?";
                Cursor cur = null;
                Setting cb = null;
                if(isDatabaseReady(app)) {
                        cur = db.rawQuery(SQL, new String[]{""+name});
                } else {
                        Log.e(TAG,"Database is not open when getting blog entries!");
                        return null;
                }
                if((cur != null) && (cur.getCount() == 1)) {
                        cb = new Setting();
                        cur.moveToFirst();
                        cb.setId(cur.getInt(1));
                        cb.setSettingname(cur.getString(1));
                        cb.setSettingtitle(cur.getString(2));
                        cb.setSettingvalue(cur.getString(3));
                        cur.close();
                }
                db.close();
                return cb;
        }
}