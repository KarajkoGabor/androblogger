package com.sadko.androblogger.db;


import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;


public class DBHelper extends SQLiteOpenHelper {


        private static final String TAG = "AndroBloggerDBHelper";
        private static final String CREATE_MEMO_TABLE = 
                                "CREATE TABLE IF NOT EXISTS MEMO ("+
                                "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                                "title TEXT,"+
                                "contents TEXT,"+
                                "created TEXT"+
                                ");";
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
        private static final String CREATE_IMAGEREPO_TABLE =
                                "CREATE TABLE IF NOT EXISTS IMAGEREPO ("+
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                                "title TEXT, " +
                                "username TEXT, " +
                                "password TEXT, " +
                                "interfacetype INT," +
                                "postconfig INT"+
                                ");";
        private static final String MEMO_TABLE_INITIAL = "INSERT INTO MEMO (title,contents,created) "+
                                                                                                        "VALUES ('demomemo','Remember to take a picture of the site visited at 12th!','01-12-2007');";
        
        
                
        
        boolean dbIsOpen;
        
        public DBHelper(Context context, String name,
                        CursorFactory factory, int version) {
                super(context, name, factory, version);
                Log.d(TAG,"DBHelper constructor called, called super()");
                // TODO Auto-generated constructor stub
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
                Log.d(TAG, "onCreate called!");
                String ctable = null;
                try {   
                        Log.d(TAG, "Create memo table...");
                        ctable = "MEMO";
                        db.execSQL(CREATE_MEMO_TABLE);
                        ctable = "example memo in MEMO";
                        //db.execSQL(MEMO_TABLE_INITIAL);
                        ctable = "BLOGCONFIG";
                        db.execSQL(CREATE_BLOGCONFIG_TABLE);
                        ctable = "ENTRY";
                        db.execSQL(CREATE_ENTRY_TABLE);
                        ctable = "SETTINGS";
                        db.execSQL(CREATE_SETTINGS_TABLE);
                        ctable = "IMAGEREPO";
                        db.execSQL(CREATE_IMAGEREPO_TABLE);
                        // reset to default settings
                        // a bit ugly, but should work. Open db so that the settings
                        // insert method works.
                        this.dbIsOpen = true;
                        this.createDefaultSettings(db);
                        // let the isDatabaseOpen decide whether this shop is open.
                        this.dbIsOpen = false;
                } catch (SQLException e) {
                        Log.e(TAG, "Failed to create tables.");
                        Log.d(TAG, "Creation stopped after "+ctable+" table.");
                }
        }
        
        private void createDefaultSettings(SQLiteDatabase db) {
                Log.d(TAG, "onCreate called!");
                if(dbIsOpen) {
                        Log.d(TAG,"Deleting the settings table contents.");
                        db.delete("SETTINGS",null,null);
                }
                for(int i = 0; i < DBConstants.DEFAULT_SETTINGS.length; i++) {
                        Log.d(TAG,"Creating default setting: "+DBConstants.DEFAULT_SETTINGS[i]);
                        this.insert(DBConstants.DEFAULT_SETTINGS[i], db);
                }
        }
        
        public boolean insert(Setting s, SQLiteDatabase db) {
                String sql = "INSERT INTO SETTINGS (settingname, settingtitle, settingvalue) VALUES "+
                "(?,?,?)";
                if(dbIsOpen) {
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


        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                // TODO Auto-generated method stub
                Log.d(TAG, "onUpgrade called!");
        }


}