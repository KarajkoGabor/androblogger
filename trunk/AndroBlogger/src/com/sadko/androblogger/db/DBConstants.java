package com.sadko.androblogger.db;

import android.content.Context;
import com.sadko.androblogger.db.Setting;


public class DBConstants {
        public static final String DB_NAME = "androblogger.db";
        public static final String DB_FULL_PATH = "/data/data/com.sadko.androblogger/databases/"+DB_NAME;
        public static final int DB_VER = 1;
        public static final int DB_MODE = Context.MODE_PRIVATE;
        public static final String SETTING_KEY_LANGUAGE = "Language";
        public static final String SETTING_KEY_AUTHOR = "Author";
        public static final String SETTING_KEY_IMAGELIB = "ImageLib";
        public static final String SETTING_KEY_PICASAWEBUSER = "PicasaWebUserID";
        public static Setting[] DEFAULT_SETTINGS = new Setting[4];              
        public static final String DB_DATE_FORMAT = "MM/dd/yyyy HH:mm";
        
        static {
                DEFAULT_SETTINGS[0] = new Setting();
                DEFAULT_SETTINGS[0].setSettingname(SETTING_KEY_LANGUAGE);
                DEFAULT_SETTINGS[0].setSettingtitle("Application Language");
                DEFAULT_SETTINGS[0].setSettingvalue("en");
                DEFAULT_SETTINGS[1] = new Setting();
                DEFAULT_SETTINGS[1].setSettingname(SETTING_KEY_AUTHOR);
                DEFAULT_SETTINGS[1].setSettingtitle("Author name for postings");
                DEFAULT_SETTINGS[1].setSettingvalue("Mr.Anderson");
                DEFAULT_SETTINGS[2] = new Setting();
                DEFAULT_SETTINGS[2].setSettingname(SETTING_KEY_IMAGELIB);
                DEFAULT_SETTINGS[2].setSettingtitle("Default image library");
                DEFAULT_SETTINGS[2].setSettingvalue("PicasaWeb");
                DEFAULT_SETTINGS[3] = new Setting();
                DEFAULT_SETTINGS[3].setSettingname(SETTING_KEY_PICASAWEBUSER);
                DEFAULT_SETTINGS[3].setSettingtitle("PicasaWebUserID");
                DEFAULT_SETTINGS[3].setSettingvalue("PicasaWeb");
                
        }
        
}