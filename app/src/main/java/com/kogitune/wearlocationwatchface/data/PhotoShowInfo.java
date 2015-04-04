package com.kogitune.wearlocationwatchface.data;

import android.os.Bundle;

/**
* Created by takam on 2015/04/04.
*/
public class PhotoShowInfo {
    public static final String ID = "PhotoShowInfo.id";
    public static final String TITLE = "PhotoShowInfo.title";
    public static final String DESCRIPTION = "PhotoShowInfo.description";
    public static final String URL = "PhotoShowInfo.url";
    public static final String USERNAME = "PhotoShowInfo.username";
    public String id;
    public String username;
    public String title;
    public String description;
    public String url;

    public PhotoShowInfo(String id, String title, String description, String username, String url) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = url;
        this.username = username;
    }

    public static PhotoShowInfo parseBundle(Bundle bundle) {
        return new PhotoShowInfo(bundle.getString(ID), bundle.getString(TITLE), bundle.getString(DESCRIPTION), bundle.getString(USERNAME), bundle.getString(URL));
    }

    public Bundle getBundle(){
        final Bundle bundle = new Bundle();
        bundle.putString(ID, id);
        bundle.putString(TITLE, title);
        bundle.putString(DESCRIPTION, description);
        bundle.putString(URL, url);
        bundle.putString(USERNAME, username);
        return bundle;
    }
}
