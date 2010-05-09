package com.matburt.mobileorg;

import android.app.Activity;
import android.util.Log;
import android.os.Environment;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.IOException;

import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;

public class SVNSynchronizer implements Synchronizer
{
    private Map<String, String> appSettings;
    private Activity activity;

    private static SVNClientManager ourClientManager;
    private static final String LT = "MobileOrg";

    SVNSynchronizer(Activity parentActivity, Map<String, String> appSettings)
    {
        DAVRepositoryFactory.setup();
        this.activity = parentActivity;
        this.appSettings = appSettings;
    }

    public boolean pull()
    {
        // Pattern checkUrl = Pattern.compile("http.*\\.(?:org|txt)$");
        // if (!checkUrl.matcher(this.appSettings.get("webUrl")).find()) {
        //     Log.e(LT, "Bad URL");
        //     return false;
        // }
        
        SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        SVNURL repositoryURL = null;
        try {
            repositoryURL = SVNURL.parseURIEncoded(appSettings.get("webUrl"));
        } 
        catch (SVNException e) {
            Log.e(LT, "Exception: " + e);
            return false;
        }

        try {
            File root = Environment.getExternalStorageDirectory();   
            File morgDir = new File(root, "mobileorg");
            morgDir.mkdir();

            updateClient.doCheckout(repositoryURL, morgDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
        }
        catch(SVNException e) {
            Log.e(LT, "SVNException: " + e);
            return false;
        }
            
        return true;
    }

    public boolean push()
    {
        return false;
    }
}