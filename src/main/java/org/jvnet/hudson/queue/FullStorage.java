package org.jvnet.hudson.queue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class FullStorage extends DataDiskStorage {

	public FullStorage(File path) {
		super(path);
	}
	
	public void removeHeadTo(long startTime, long startID) throws Exception{		
	}

	protected Writer getWriter() throws UnsupportedEncodingException, FileNotFoundException{
		return new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(dataPath, false), 64 * 1024), "utf8");
	}
	
	public int getLastMessageId(){
		return 0;
	}
}
