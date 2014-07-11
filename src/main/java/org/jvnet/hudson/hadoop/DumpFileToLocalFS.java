package org.jvnet.hudson.hadoop;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;

import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class DumpFileToLocalFS implements Runnable{
	private Log log = LogFactory.getLog("hdfs.archiver");	
	private GridFSDBFile file = null;
	private GridFS fs = null;
	private File localRoot = null;
	
	public DumpFileToLocalFS(GridFSDBFile file, GridFS fs, File localRoot){
		this.file = file;
		this.fs = fs;
		this.localRoot = localRoot;
	}

	@Override
	public void run() {
		try{
			File localPath = new File(localRoot, file.getFilename());
			log.info("Save to local file:" + localPath.getAbsolutePath());
			File dirName = localPath.getParentFile();
			if(!dirName.exists()){
				dirName.mkdirs();
			}
			file.writeTo(localPath);
			GridFSInputFile newFile = fs.createFile(new byte[]{0, 0,});
			newFile.setMetaData(file.getMetaData());
			newFile.setFilename(file.getFilename());
			newFile.put("localLength", file.getLength());
			newFile.save(10);
			//log.info("remove:%s" + file.getId() + ", fn:" + file.getFilename());
			fs.remove((ObjectId)file.getId());
		}catch(Throwable e){
			log.error("Failed to dump file to local fs, error:" + e.toString(), e);
		}
	}
}