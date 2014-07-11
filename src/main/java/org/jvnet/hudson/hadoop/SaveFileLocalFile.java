package org.jvnet.hudson.hadoop;

import java.io.File;
import java.io.InputStream;

import com.mongodb.gridfs.GridFS;

public class SaveFileLocalFile implements Runnable {
	private InputStream in = null;
	private String path = null;
	private GridFS fs = null;
	private File localRoot = null;
	SaveFileLocalFile(InputStream in, String path, GridFS fs, File root){
		
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
