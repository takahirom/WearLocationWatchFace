package com.kogitune.wearlocationwatchface.data;

import android.os.Bundle;

/**
* Created by takam on 2015/04/04.
*/
public class PhotoShowInfo {
    public static final String TITLE = "PhotoShowInfo.title";
    public static final String DESCRIPTION = "PhotoShowInfo.description";
    public static final String URL = "PhotoShowInfo.url";
    public String title;
    public String description;
    public String url;

    public PhotoShowInfo(String title, String description, String url) {
        this.title = title;
        this.description = description;
        this.url = url;
    }

    public static PhotoShowInfo parseBundle(Bundle bundle){
        return new PhotoShowInfo(bundle.getString(TITLE),bundle.getString(DESCRIPTION),bundle.getString(URL));
    }

    public Bundle getBundle(){
        final Bundle bundle = new Bundle();
        bundle.putString(TITLE, title);
        bundle.putString(DESCRIPTION, description);
        bundle.putString(URL, url);
        return bundle;
    }
}
