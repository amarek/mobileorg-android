package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.io.IOException;

public class NullSynchronizer implements SynchronizerInterface {

    public NullSynchronizer() {
    }

    public boolean isConfigured() {
        return true;
    }

    public void putRemoteFile(String filename, String contents) {
    }

    public BufferedReader getRemoteFile(String filename) {
        return null;
    }

    public ArrayList<String> listRemoteFiles() throws IOException
    {
        return new ArrayList<String>();
    }

    @Override
	public void postSynchronize() {
    }
}