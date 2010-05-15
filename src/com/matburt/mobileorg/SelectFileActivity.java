package com.matburt.mobileorg;

import android.app.ListActivity;
import android.app.Application;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.util.Log;

import java.util.List;
import java.io.File;

public class SelectFileActivity extends ListActivity
{
    protected String[] fileNames;
    private static final String LT = "MobileOrg";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        Log.i(LT,"Retrieving folder contents for:" + getIntent().getStringExtra("FOLDER"));
        File folder = new File(getIntent().getStringExtra("FOLDER"));
        fileNames = folder.list();

        setListAdapter(new ArrayAdapter<String>(this,
                                                android.R.layout.simple_list_item_1, fileNames));
        getListView().setTextFilterEnabled(true);        
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("FILE",fileNames[position]);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

}