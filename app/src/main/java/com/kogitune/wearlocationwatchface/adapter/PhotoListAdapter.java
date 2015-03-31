package com.kogitune.wearlocationwatchface.adapter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.kogitune.activity_transition.ActivityTransitionLauncher;
import com.kogitune.wearlocationwatchface.R;
import com.kogitune.wearlocationwatchface.activity.PhotoDetailActivity;
import com.kogitune.wearlocationwatchface.observable.FlickrObservable;
import com.kogitune.wearlocationwatchface.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class PhotoListAdapter extends RecyclerView.Adapter<PhotoListAdapter.ViewHolder> {

    private final List<FlickrObservable.PhotoShowInfo> photoShowInfoList = new ArrayList<>();
    private int lastAnimatedPosition = -1;

    public PhotoListAdapter() {
    }

    public void addPhotoShowInfo(FlickrObservable.PhotoShowInfo photoShowInfo){
        photoShowInfoList.add(photoShowInfo);
        notifyDataSetChanged();
    }

    @Override
    public PhotoListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_list_item_row, parent, false);

        ViewHolder vhItem = new ViewHolder(v, viewType);

        return vhItem;

    }

    @Override
    public void onBindViewHolder(PhotoListAdapter.ViewHolder holder, int position) {
        runEnterAnimation(holder.itemView, position);
        final FlickrObservable.PhotoShowInfo photoShowInfo = photoShowInfoList.get(position);
        holder.textView.setText(photoShowInfo.title);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(v.getContext(), PhotoDetailActivity.class);
                intent.putExtras(photoShowInfoList.get(position).getBundle());
                final Bitmap bitmap = ((BitmapDrawable) holder.imageView.getDrawable()).getBitmap();
                ActivityTransitionLauncher.with((Activity) v.getContext()).image(bitmap).from(holder.imageView).launch(intent);
            }
        });
        Glide.with(holder.imageView.getContext())
                .load(photoShowInfo.url)
                .asBitmap()
                .into(new BitmapImageViewTarget(holder.imageView));
    }

    @Override
    public int getItemCount() {
        return photoShowInfoList.size();
    }

    private void runEnterAnimation(View view, int position) {
        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            view.setTranslationY(UIUtils.getScreenHeight(view.getContext()));
            view.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(3.f))
                    .setDuration(700)
                    .start();
        }
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;


        public ViewHolder(View itemView, int ViewType) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.rowText);
            imageView = (ImageView) itemView.findViewById(R.id.rowCover);
        }


    }

}