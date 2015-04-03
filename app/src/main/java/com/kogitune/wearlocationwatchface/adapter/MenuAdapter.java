package com.kogitune.wearlocationwatchface.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kogitune.wearlocationwatchface.R;

import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final List<MenuItem> menuItemList;

    public static class MenuItem {
        Intent intent;
        String title;
        int icon;

        public MenuItem(Intent intent, String title, int icon) {
            this.intent = intent;
            this.title = title;
            this.icon = icon;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        int Holderid;

        TextView textView;
        ImageView imageView;


        public ViewHolder(View itemView,int ViewType) {                 
            super(itemView);
            if(ViewType == TYPE_ITEM) {
                textView = (TextView) itemView.findViewById(R.id.rowText); 
                imageView = (ImageView) itemView.findViewById(R.id.rowIcon);
                Holderid = 1;                                               
            }
            else{
                Holderid = 0;                                                
            }
        }


    }



    public MenuAdapter(List menuItemList){
        this.menuItemList = menuItemList;
    }

    @Override
    public MenuAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_item_row,parent,false);

            ViewHolder vhItem = new ViewHolder(v,viewType);

            return vhItem;

        } else if (viewType == TYPE_HEADER) {

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_header,parent,false);

            ViewHolder vhHeader = new ViewHolder(v,viewType);

            return vhHeader;


        }
        return null;

    }

    @Override
    public void onBindViewHolder(MenuAdapter.ViewHolder holder, int position) {
        if(holder.Holderid ==1) {
            final MenuItem item = menuItemList.get(position - 1);
            holder.textView.setText(item.title);
            holder.imageView.setImageResource(item.icon);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.getContext().startActivity(item.intent);
                }
            });
        }
        else{
            
        }
    }

    @Override
    public int getItemCount() {
        return menuItemList.size() +1;
    }


    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

}