package org.jvnet.hudson.hadoop;

import java.io.File;
import java.util.zip.ZipFile;

class ZipFileWrapper{
	ZipFile file = null;
	File rawFile = null; 
	boolean isLocal = false;
	protected void finalize() throws Throwable {
	    try {
	    	if(file != null){
	    		file.close();
	    	}
	    	if(!this.isLocal && rawFile != null){
	    		rawFile.delete();
	    	}
	    } finally {
	        super.finalize();
	    }
	}
}