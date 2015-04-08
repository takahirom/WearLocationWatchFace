package com.kogitune.wearlocationwatchface.observable;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.kogitune.wearlocationwatchface.BuildConfig;
import com.kogitune.wearlocationwatchface.data.PhotoShowInfo;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Created by takam on 2015/03/23.
 */
public class FlickrObservable {
    private static final String WEB_SERVICE_BASE_URL = "https://api.flickr.com/services/rest";
    private static final String TAG = "FlickrObservable";
    private final FlickrService mWebService;

    public FlickrObservable(Context context) {
        OkHttpClient httpClient = new OkHttpClient();
        try {
            Cache responseCache = new Cache(context.getCacheDir(), 1024 * 1024);
            httpClient.setCache(responseCache);
        } catch (Exception e) {
            Log.d(TAG, "Unable to set http cache", e);
        }
        httpClient.setReadTimeout(30, TimeUnit.SECONDS);
        httpClient.setConnectTimeout(30, TimeUnit.SECONDS);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(WEB_SERVICE_BASE_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();
        mWebService = restAdapter.create(FlickrService.class);
    }

    public Observable<Location> fetchPhotoLocation(String photoId){
        return mWebService.fetchPhotoLocation(photoId).map(new Func1<PhotoDatas.PhotoLocationInfo, Location>() {
            @Override
            public Location call(PhotoDatas.PhotoLocationInfo photoLocationInfo) {
                final Location location = new Location("");
                location.setLatitude(Double.parseDouble(photoLocationInfo.photo.location.latitude));
                location.setLongitude(Double.parseDouble(photoLocationInfo.photo.location.longitude));
                location.setAccuracy(Float.parseFloat(photoLocationInfo.photo.location.accuracy));
                Log.d(TAG, "latitude" + photoLocationInfo.photo.location.latitude);
                Log.d(TAG, "latitude" + photoLocationInfo.photo.location.longitude);
                Log.d(TAG, "accuracy" + photoLocationInfo.photo.location.accuracy);
                return location;
            }
        });
    }


    public Observable<PhotoShowInfo> fetchPhotoInfo(final String photoId) {
        return Observable.zip(mWebService.fetchPhotoInfo(photoId),
                mWebService.fetchPhotoSizes(photoId), new Func2<PhotoDatas.PhotoInfo, PhotoDatas.PhotoSizes, PhotoShowInfo>() {
                    @Override
                    public PhotoShowInfo call(PhotoDatas.PhotoInfo photoInfo, PhotoDatas.PhotoSizes photoSizes) {
                        String source = photoSizes.sizes.size.get(0).source;
                        for (PhotoDatas.PhotoSizes.Sizes.Size size : photoSizes.sizes.size) {
                            if ("Large".equals(size.label)) {
                                source = size.source;
                            }
                        }
                        final PhotoDatas.PhotoInfo.Photo photo = photoInfo.photo;
                        return new PhotoShowInfo(photoId, photo.title.toString(), photo.description.toString(), photo.owner.username, source);
                    }
                });
    }

    private interface FlickrService {
        @GET("/?format=json&api_key=" + BuildConfig.FLICKR_API_KEY + "&method=flickr.photos.getInfo&nojsoncallback=1")
        Observable<PhotoDatas.PhotoInfo> fetchPhotoInfo(@Query("photo_id") String photoId);

        @GET("/?format=json&api_key=" + BuildConfig.FLICKR_API_KEY + "&method=flickr.photos.getSizes&nojsoncallback=1")
        Observable<PhotoDatas.PhotoSizes> fetchPhotoSizes(@Query("photo_id") String photoId);

        @GET("/?format=json&api_key=" + BuildConfig.FLICKR_API_KEY + "&method=flickr.photos.geo.getLocation&nojsoncallback=1")
        Observable<PhotoDatas.PhotoLocationInfo> fetchPhotoLocation(@Query("photo_id") String photoId);
    }

    public static class PhotoDatas {
        public static class Content {
            String _content;

            @Override
            public String toString() {
                return _content;
            }
        }

        public static class PhotoSizes {
            public Sizes sizes;
            public class Sizes {
                public List<Size> size;
                public class Size{
                    public String source;
                    public String label;
                }
            }
        }

        public static class PhotoInfo {
            public Photo photo;

            public static class Photo {
                String id;
                Content description;
                Content title;
                Owner owner;
           }
        }

        public static class Owner {
            String username;
        }

        public static class PhotoLocationInfo {
            public Photo photo;
            public static class Photo{
                Location location;
                public static class Location{
                    String latitude;
                    String longitude;
                    String accuracy;
                }
            }
        }
    }
}
