package com.matburt.mobileorg;

import android.app.ListActivity;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.AdapterView;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Runnable;
import java.io.File;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.content.SharedPreferences;

public class MobileOrgActivity extends ListActivity
{
    private static class OrgViewAdapter extends BaseAdapter {

        public Node topNode;
        public Node thisNode;
        public ArrayList<Integer> nodeSelection;
        private Context context;
        private LayoutInflater lInflator;

        public OrgViewAdapter(Context context, Node ndx,
                              ArrayList<Integer> selection) {
            this.topNode = ndx;
            this.thisNode = ndx;
            this.lInflator = LayoutInflater.from(context);
            this.nodeSelection = selection;
            this.context = context;
            Log.d("OVA", "Selection Stack");
            if (selection != null) {
                for (int idx = 0; idx < selection.size(); idx++) {
                    this.thisNode = this.thisNode.subNodes.get(
                                          selection.get(idx));
                    Log.d("OVA", this.thisNode.nodeName);
                }
            }
        }

        public int getCount() {
            return this.thisNode.subNodes.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.lInflator.inflate(R.layout.main, null);
            }

            TextView thisView = (TextView)convertView.findViewById(R.id.orgItem);
            TextView todoView = (TextView)convertView.findViewById(R.id.todoState);
            LinearLayout tagsLayout = (LinearLayout)convertView.findViewById(R.id.tagsLayout);
            String todo = this.thisNode.subNodes.get(position).todo;
            if (TextUtils.isEmpty(todo)) {
            	todoView.setVisibility(View.GONE);
            } else {
            	todoView.setText(todo);
            	todoView.setVisibility(View.VISIBLE);
            }
            thisView.setText(this.thisNode.subNodes.get(position).nodeName);
            tagsLayout.removeAllViews();
            for (String tag : this.thisNode.subNodes.get(position).tags) {
				TextView tagView = new TextView(this.context);
				tagView.setText(tag);
				tagView.setPadding(0, 0, 5, 0);
				tagsLayout.addView(tagView);
			}
            Log.d("MobileOrg", "Returning view item: " +
                  this.thisNode.subNodes.get(position).nodeName);
            convertView.setTag(thisView);
            return convertView;
        }
    }

    private static final int ACTIVITY_DISPLAY = 1;
    private static final int ACTIVITY_CAPTURE = 3;
    private static final int ACTIVITY_FILE = 4;

    private static final int OP_MENU_SETTINGS = 1;
    private static final int OP_MENU_SYNC = 2;
    private static final int OP_MENU_OUTLINE = 3;
    private static final int OP_MENU_CAPTURE = 4;
    private static final int OP_MENU_FILE = 5;
    private static final String LT = "MobileOrg";
    private ProgressDialog syncDialog;
    public boolean syncResults;
    public SharedPreferences appSettings;
    final Handler syncHandler = new Handler();
    final Runnable syncUpdateResults = new Runnable() {
        public void run() {
            postSynchronize();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.initializeTables();
        ListView lv = this.getListView();
        appSettings = PreferenceManager.getDefaultSharedPreferences(
                                       getBaseContext());
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener
                                      (){
                @Override
                    public boolean onItemLongClick(AdapterView<?> av, View v,
                                                   int pos, long id) {
                    onLongListItemClick(v,pos,id);
                    return true;
                }
            });
        if (this.appSettings.getString("webUrl","").equals("")) {
            this.onShowSettings();
        }
    }

    public void runParser() {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        ArrayList<String> allOrgList = this.getOrgFiles();
        String storageMode = this.getStorageLocation();
        OrgFileParser ofp = new OrgFileParser(allOrgList, storageMode);
        ofp.parse();
        appInst.rootNode = ofp.rootNode;
    }

    @Override
    public void onResume() {
        super.onResume();
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        if (appInst.rootNode == null) {
            this.runParser();
        }

        Intent nodeIntent = getIntent();
        appInst.nodeSelection = nodeIntent.getIntegerArrayListExtra("nodePath");
        this.setListAdapter(new OrgViewAdapter(this,
                                               appInst.rootNode,
                                               appInst.nodeSelection));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MobileOrgActivity.OP_MENU_OUTLINE, 0, "Outline");
        menu.add(0, MobileOrgActivity.OP_MENU_CAPTURE, 0, "Capture");
        menu.add(0, MobileOrgActivity.OP_MENU_SYNC, 0, "Sync");
        menu.add(0, MobileOrgActivity.OP_MENU_SETTINGS, 0, "Settings");
        menu.add(0, MobileOrgActivity.OP_MENU_FILE, 0, "File");
        return true;
    }

    protected void onLongListItemClick(View av, int pos, long id) {
        Intent dispIntent = new Intent();
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        dispIntent.setClassName("com.matburt.mobileorg",
                                "com.matburt.mobileorg.OrgContextMenu");
        if (appInst.nodeSelection == null) {
            appInst.nodeSelection = new ArrayList<Integer>();
        }

        appInst.nodeSelection.add(new Integer(pos));
        dispIntent.putIntegerArrayListExtra("nodePath", appInst.nodeSelection);
        startActivity(dispIntent);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent dispIntent = new Intent();
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        dispIntent.setClassName("com.matburt.mobileorg",
                                "com.matburt.mobileorg.MobileOrgActivity");
        if (appInst.nodeSelection == null) {
            appInst.nodeSelection = new ArrayList<Integer>();
        }

        ArrayList<Integer> selection = new ArrayList<Integer>(appInst.nodeSelection);
        selection.add(new Integer(position));

        Node thisNode = appInst.rootNode;
        if (selection != null) {
            for (int idx = 0; idx < selection.size(); idx++) {
                thisNode = thisNode.subNodes.get(selection.get(idx));
            }
        }
        if (thisNode.subNodes.size() < 1) {
            Intent textIntent = new Intent();

            String docBuffer = thisNode.nodeName + "\n\n" +
                thisNode.nodePayload;
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SimpleTextDisplay");
            textIntent.putExtra("txtValue", docBuffer);
            startActivity(textIntent);
        }
        else {
            dispIntent.putIntegerArrayListExtra("nodePath", selection);
            startActivityForResult(dispIntent, ACTIVITY_DISPLAY);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(LT, "Sub activity " + requestCode + " completed");
        super.onActivityResult(requestCode, resultCode, data); 
        switch(requestCode) {            
            case (ACTIVITY_FILE): 
            { 
                if (resultCode == Activity.RESULT_OK) { 
                    String file = data.getStringExtra("FILE");
                    Log.i(LT, "Selected file: " + file);
                }
            } 
            break;
            case (ACTIVITY_CAPTURE):
            {
                this.runParser();
            }
            break;
            case (ACTIVITY_DISPLAY):
            {
                MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
                appInst.nodeSelection.remove(appInst.nodeSelection.size()-1);
            }
            break;
        }
    }

    public boolean onShowSettings() {
        Intent settingsIntent = new Intent();        
        settingsIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SettingsActivity");
        startActivity(settingsIntent);
        return true;
    }

    protected String getStorageFolder()
    {
        File root = Environment.getExternalStorageDirectory();   
        File morgDir = new File(root, "mobileorg");
        return morgDir.getAbsolutePath() + "/";
    }

    public boolean onSelectFile()
    {

        Intent fileIntent = new Intent();
        fileIntent.putExtra("FOLDER",getStorageFolder());
        fileIntent.setClassName("com.matburt.mobileorg",
                                "com.matburt.mobileorg.SelectFileActivity");
        startActivityForResult(fileIntent, ACTIVITY_FILE);
        return true;
    }

    public void runSynchronizer() {
        final Synchronizer appSync = new SVNSynchronizer(this, appSettings.getString("webUrl",""), getStorageFolder());
        Thread syncThread = new Thread() {
                public void run() {
                    boolean pullResult = appSync.pull();
                    boolean pushResult = false;
                    syncResults = true;
                    if (!pullResult) {
                        Log.e(LT, "Pull Synchronization fail");
                        syncResults = false;
                    }
                    else {
                        pushResult = appSync.push();
                    }
                    if (!pushResult) {
                        Log.e(LT, "Push Synchronization fail");
                        syncResults = false;
                    }
                    syncHandler.post(syncUpdateResults);
            }
        };
        syncThread.start();
        syncDialog = ProgressDialog.show(this, "",
                        "Synchronizing. Please wait...", true);
    }

    public boolean runCapture() {
        Intent captureIntent = new Intent();
        captureIntent.setClassName("com.matburt.mobileorg",
                                   "com.matburt.mobileorg.Capture");
        startActivityForResult(captureIntent, ACTIVITY_CAPTURE);
        return true;
    }

    public void postSynchronize() {
        syncDialog.dismiss();
        if (!this.syncResults) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Synchronization Failed, try again?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            runSynchronizer();
                        }
                    })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            this.runParser();
            this.onResume();
        }
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MobileOrgActivity.OP_MENU_SYNC:
            this.runSynchronizer();
            return true;
        case MobileOrgActivity.OP_MENU_SETTINGS:
            return this.onShowSettings();
        case MobileOrgActivity.OP_MENU_OUTLINE:
            return true;
        case MobileOrgActivity.OP_MENU_CAPTURE:
            return this.runCapture();
        case MobileOrgActivity.OP_MENU_FILE:
            return this.onSelectFile();
        }
        return false;
    }

    public String getStorageLocation() {
        return this.appSettings.getString("storageMode", "");
    }

    public ArrayList<String> getOrgFiles() {
        ArrayList<String> allFiles = new ArrayList<String>();
        SQLiteDatabase appdb = this.openOrCreateDatabase("MobileOrg",
                                                         MODE_PRIVATE, null);
        Cursor result = appdb.rawQuery("SELECT file FROM files", null);
        if (result != null) {
            if (result.getCount() > 0) {
                result.moveToFirst();
                do {
                    Log.d(LT, "pulled " + result.getString(0));
                    allFiles.add(result.getString(0));
                } while(result.moveToNext());
            }
        }
        appdb.close();
        result.close();
        return allFiles;
    }

    public void initializeTables() {
        SQLiteDatabase appdb = this.openOrCreateDatabase("MobileOrg",
                                                         MODE_PRIVATE, null);
        appdb.execSQL("CREATE TABLE IF NOT EXISTS files"
                      + " (file VARCHAR, name VARCHAR,"
                      + " checksum VARCHAR);");
        appdb.close();

    }
}
