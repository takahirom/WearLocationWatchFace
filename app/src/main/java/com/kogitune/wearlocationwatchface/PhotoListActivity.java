package com.kogitune.wearlocationwatchface;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.kogitune.wearlocationwatchface.adapter.MenuAdapter;

public class PhotoListActivity extends ActionBarActivity {
    
    

    String TITLES[] = {"Home","Settings"};
    int ICONS[] = {R.drawable.ic_photo_library_grey600_36dp, R.drawable.ic_settings_grey600_36dp};


    private Toolbar toolbar;                              

    RecyclerView mRecyclerView;                           
    RecyclerView.Adapter mAdapter;                        
    RecyclerView.LayoutManager mLayoutManager;            
    DrawerLayout Drawer;                                  

    ActionBarDrawerToggle mDrawerToggle;                  




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.RecyclerView); 

        mRecyclerView.setHasFixedSize(true);                            

        mAdapter = new MenuAdapter(TITLES,ICONS);       
        
        

        mRecyclerView.setAdapter(mAdapter);                              

        mLayoutManager = new LinearLayoutManager(this);                 

        mRecyclerView.setLayoutManager(mLayoutManager);                 


        Drawer = (DrawerLayout) findViewById(R.id.DrawerLayout);        

        mDrawerToggle = new ActionBarDrawerToggle(this,Drawer,toolbar,R.string.open_drawer,R.string.close_drawer){

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                
                
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                
            }



        }; 
        Drawer.setDrawerListener(mDrawerToggle); 
        mDrawerToggle.syncState();               

    }
}
