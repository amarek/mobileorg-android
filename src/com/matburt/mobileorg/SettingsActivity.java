package com.matburt.mobileorg;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageItemInfo;
import android.content.pm.ActivityInfo;
import android.util.Log;
import java.util.List;

public class SettingsActivity extends PreferenceActivity
{
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);                
        populateSyncSources();
    }

    protected void populateSyncSources()
    {
        List<ActivityInfo> synchronizers = MobileOrgApplication.discoverSynchronizerPlugins((Context)this);

        ListPreference syncSource = (ListPreference)findPreference("syncSource");

        //save the items for built-in synchronizer originally
        //retrieved from xml resources
        CharSequence[] entries = new CharSequence[synchronizers.size() + syncSource.getEntries().length];
        CharSequence[] values = new CharSequence[synchronizers.size() + syncSource.getEntryValues().length];
        System.arraycopy(syncSource.getEntries(), 0, entries, 0, syncSource.getEntries().length);
        System.arraycopy(syncSource.getEntryValues(), 0, values, 0, syncSource.getEntryValues().length);

        //populate the sync source list and prepare Intents for
        //discovered synchronizers
        int offset = syncSource.getEntries().length;
        for (ActivityInfo info : synchronizers)
        {
            entries[offset] = info.nonLocalizedLabel;
            values[offset] = info.packageName + ":" + info.name;
            Intent syncIntent = new Intent(MobileOrgApplication.SYNCHRONIZER_PLUGIN_ACTION_SETTINGS);
            syncIntent.setClassName(info.packageName, info.name);
            SynchronizerPreferences.syncIntents.put(info.packageName + ":" + info.name,syncIntent);
            offset++;
        }

        //fill in the Intents for built-in synchronizers
        Intent synchroIntent = new Intent();
        synchroIntent.setClassName("com.matburt.mobileorg",
                                   "com.matburt.mobileorg.WebDAVSettingsActivity");
         SynchronizerPreferences.syncIntents.put("webdav",synchroIntent);
            
        synchroIntent = new Intent();
        synchroIntent.setClassName("com.matburt.mobileorg",
                                   "com.matburt.mobileorg.SDCardSettingsActivity");
        SynchronizerPreferences.syncIntents.put("sdcard", synchroIntent);

        //populate the sync source list with updated data
        syncSource.setEntries(entries);
        syncSource.setEntryValues(values);
    }        
}