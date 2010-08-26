package com.matburt.mobileorg;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.os.Environment;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.IOException;

import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNDebugLogAdapter;
import org.tmatesoft.svn.util.SVNLogType;
import java.util.logging.Level;
import java.io.PrintWriter;
import java.io.StringWriter;

class CustomSVNKitLogger extends SVNDebugLogAdapter {

    public void log(SVNLogType logType, byte[] data, Level logLevel) {
    }

    public void log(SVNLogType logType, String message, byte[] data) {
        //        Log.i("mobileorg",message + " " + new String(data));    
    }

    public void log(SVNLogType logType, String message, Level logLevel) {
       Log.i("MobileOrg",message);    
    }
    public void log(SVNLogType logType, Throwable th, Level logLevel) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        th.printStackTrace(pw);
        pw.flush();
        sw.flush();
        Log.e("MobileOrg","Exc:" + th + sw.toString());        
    }
}

public class SVNSynchronizer implements Synchronizer
{
    private Map<String, String> appSettings;
    private Activity activity;
    private static SVNClientManager ourClientManager;
    private static final String LT = "MobileOrg";
    private String storageDir;
    private String webUrl;

    SVNSynchronizer(Activity parentActivity, String webUrl, String storageDir)
    {
        DAVRepositoryFactory.setup();
        ISVNOptions options = SVNWCUtil.createDefaultOptions( true );
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager( );
        ourClientManager = SVNClientManager.newInstance( options , authManager );
        this.activity = parentActivity;
        this.appSettings = appSettings;
        this.webUrl = webUrl;
        this.storageDir = storageDir;
        ISVNDebugLog customLogger = new CustomSVNKitLogger();
        SVNDebugLog.setDefaultLog(customLogger);
    }

    public void close()
    {}

    public void pull()
    {
        Log.i(LT,"pull from " + webUrl);
        SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        SVNURL repositoryURL = null;
        File morgDir = null;
        try {
            repositoryURL = SVNURL.parseURIEncoded(webUrl);
        } 
        catch (SVNException e) {
            Log.e(LT, "Exception: " + e);
            return;
        }

        try {
            morgDir = new File(storageDir);
            morgDir.mkdir();

            updateClient.doCheckout(repositoryURL, morgDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
        }
        catch(SVNException e) {
            Log.e(LT, "SVNException: " + e + ":" + e.getMessage());
            return;
        }
        addOrUpdateFiles(morgDir);
    }

    protected void addOrUpdateFiles(File folder) {
        File[] files = folder.listFiles();
        MobileOrgDatabase appdb = new MobileOrgDatabase((Context)activity);
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

    public void push()
    {

        Log.i(LT,"Commiting repository at " + storageDir);
        try {
            File file = new File(storageDir + "mobileorg.org");
            if(!file.exists())
            {
                return;
            }
            ourClientManager.getWCClient().doAdd(file, false, false, false, true);
            long rev = ourClientManager.getCommitClient().doCommit(new File[] { file }, false,
                                                                   "MobileOrg commit", false, true).getNewRevision();
            Log.i(LT, "Commited revision:" + rev);
        }
        catch(SVNException e) {
            Log.e(LT, "SVNException: " + e + ":" + e.getMessage());
        }
    }
}