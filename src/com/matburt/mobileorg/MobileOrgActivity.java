package com.matburt.mobileorg;

import android.app.ListActivity;
import android.app.Application;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Toast;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.lang.Runnable;
import java.lang.reflect.InvocationTargetException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.File;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

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
            if(thisNode != null) {
                return this.thisNode.subNodes.size();
            }
            else {
                return 0;
            }
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

    private static final int OP_MENU_SETTINGS = 1;
    private static final int OP_MENU_SYNC = 2;
    private static final int OP_MENU_OUTLINE = 3;
    private static final int OP_MENU_CAPTURE = 4;
    private static final int ACTIVITY_EXPAND = 1;
    private static final int ACTIVITY_CAPTURE = 3;
    private static final int ACTIVITY_SYNCHRONIZE = 4;
    private static final String LT = "MobileOrg";
    private ProgressDialog syncDialog;
    private MobileOrgDatabase appdb;
    private ReportableError syncError;
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
        ListView lv = this.getListView();
        this.appdb = new MobileOrgDatabase((Context)this);
        appSettings = PreferenceManager.getDefaultSharedPreferences(
                                       getBaseContext());
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener
                                      (){
                    public boolean onItemLongClick(AdapterView<?> av, View v,
                                                   int pos, long id) {
                    onLongListItemClick(v,pos,id);
                    return true;
                }
            });
        if ((this.appSettings.getString("syncSource","").equals("webdav") &&
             this.appSettings.getString("webUrl","").equals("")) ||
            (this.appSettings.getString("syncSource","").equals("sdcard") &&
             this.appSettings.getString("indexFilePath","").equals(""))) {
            this.onShowSettings();
        }
    }

    @Override
    public void onDestroy() {
        this.appdb.close();
        super.onDestroy();
    }

    public void runParser() {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        ArrayList<String> allOrgList = this.appdb.getOrgFiles();

        OrgFileParser ofp = new OrgFileParser(allOrgList,
                                              getStorageLocation(),
                                              this.appdb,
                                              MobileOrgApplication.getOrgBasePath(appSettings));
        try {
        	ofp.parse();
        	appInst.rootNode = ofp.rootNode;
        }
        catch(Throwable e) {
        	ErrorReporter.displayError(this, "An error occurred during parsing: " + e.toString());
        }
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
        menu.add(0, MobileOrgActivity.OP_MENU_OUTLINE, 0, R.string.menu_outline);
        menu.add(0, MobileOrgActivity.OP_MENU_CAPTURE, 0, R.string.menu_capture);
        menu.add(0, MobileOrgActivity.OP_MENU_SYNC, 0, R.string.menu_sync);
        menu.add(0, MobileOrgActivity.OP_MENU_SETTINGS, 0, R.string.menu_settings);
        return true;
    }

    protected void onLongListItemClick(View av, int pos, long id) {
        Intent dispIntent = new Intent();
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        dispIntent.setClassName("com.matburt.mobileorg",
                                "com.matburt.mobileorg.OrgContextMenu");

        appInst.pushSelection(pos);

        dispIntent.putIntegerArrayListExtra("nodePath", appInst.nodeSelection);
        startActivity(dispIntent);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();

        appInst.pushSelection(position);
        Node thisNode = appInst.getSelectedNode();

        if(thisNode.encrypted && !thisNode.parsed)
        {
            //if suitable APG version is installed
            if(Encryption.isAvailable((Context)this))
            {
                //retrieve the encrypted file data
                byte[] rawData = OrgFileParser.getRawFileData(MobileOrgApplication.getOrgBasePath(appSettings), thisNode.nodeName);
                //and send it to APG for decryption
                Encryption.decrypt(this, rawData);
            }
            else
            {
                appInst.popSelection();
            }
            return;
        }

        if (thisNode.subNodes.size() < 1) {
            appInst.popSelection();
            Intent textIntent = new Intent();

            String docBuffer = thisNode.nodeName + "\n\n" +
                thisNode.nodePayload;
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SimpleTextDisplay");
            textIntent.putExtra("txtValue", docBuffer);
            startActivity(textIntent);
        }
        else {
            expandSelection(appInst.nodeSelection);
        }
    }

    public void expandSelection(ArrayList<Integer> selection)
    {
        Intent dispIntent = new Intent();
        dispIntent.setClassName("com.matburt.mobileorg",
                                "com.matburt.mobileorg.MobileOrgActivity");
        dispIntent.putIntegerArrayListExtra("nodePath", selection);
        startActivityForResult(dispIntent, ACTIVITY_EXPAND);        
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        if (requestCode == ACTIVITY_CAPTURE) {
            this.runParser();
        }
        else if(requestCode == Encryption.DECRYPT_MESSAGE)
        {
            if (resultCode != Activity.RESULT_OK || data == null)
            {
                appInst.popSelection();
                return;
            }
            
            Node thisNode = appInst.getSelectedNode();
            String decryptedData = data.getStringExtra(Encryption.EXTRA_DECRYPTED_MESSAGE);
            OrgFileParser ofp = new OrgFileParser(appdb.getOrgFiles(),
                                                  getStorageLocation(),
                                                  appdb,
                                                  MobileOrgApplication.getOrgBasePath(appSettings));

            ofp.parse(thisNode, new BufferedReader(new StringReader(decryptedData)));
            expandSelection(appInst.nodeSelection);
        }
        else if(requestCode == ACTIVITY_SYNCHRONIZE)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                Log.i("MobileOrg","Synchronization completed successfully");
                addOrUpdateFiles();
                runParser();
                onResume();
            }
            else
            {
                ErrorReporter.displayError(this, data.getStringExtra("error"));
            }
        }
        else
        {
            appInst.popSelection();
        }
    }

    public boolean onShowSettings() {
        Intent settingsIntent = new Intent();
        settingsIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SettingsActivity");
        startActivity(settingsIntent);
        return true;
    }

    public void runSynchronizer() {
        String userSynchro = this.appSettings.getString("syncSource","");

        Intent syncIntent = new Intent(MobileOrgApplication.SYNCHRONIZER_PLUGIN_ACTION_SYNC);
        String[] params = userSynchro.split(":");
        syncIntent.setClassName(params[0], params[1]);
        syncIntent.putExtra("storageDir", this.appSettings.getString("storageDir",""));
        startActivityForResult(syncIntent, ACTIVITY_SYNCHRONIZE);
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
        if(this.syncError != null) {
            ErrorReporter.displayError(this, this.syncError);
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
        }
        return false;
    }

    public String getStorageLocation() {
        return this.appSettings.getString("storageMode", "");
    }

    protected void addOrUpdateFiles() {
        File folder = new File(MobileOrgApplication.getOrgBasePath(this.appSettings));
        File[] files = folder.listFiles();
        MobileOrgDatabase appdb = new MobileOrgDatabase((Context)this);
        for(File file : files)
        {
            if(!file.getName().endsWith(".org") &&
               !file.getName().endsWith(".org.gpg"))
            {
                continue;
            }
            
            appdb.addOrUpdateFile(file.getName(), file.getName());
        }
        appdb.close();
    }

}
