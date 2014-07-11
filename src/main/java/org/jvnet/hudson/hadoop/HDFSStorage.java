package org.jvnet.hudson.hadoop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.PrivilegedExceptionAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;

public class HDFSStorage {
	private Log log = LogFactory.getLog("hdfs.archiver");
	
	private String url = "hdfs://127.0.0.1:9000/";
	private String localFs = "/tmp/hdfs_work";
	private FileSystem fs = null;	
	
	
	public HDFSStorage(String url, File rootFS){
		this.url = url;
		localFs = new File(rootFS, "hdfs_work").getAbsolutePath();
	}
	
	public boolean uploadFile(final String path, final InputStream ins) throws IOException, InterruptedException{
		boolean result = true;
		
		UserGroupInformation ugi = UserGroupInformation.createUserForTesting("www-data", new String[]{"www-data"});		
		/*
		new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
              //Submit a job
              JobClient jc = new JobClient(conf);
              jc.submitJob(conf);
              //OR access hdfs
              FileSystem fs = FileSystem.get(conf);
              fs.mkdir(someFilePath); 
            }         
           */
		result = ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
			@Override
			public Boolean run() throws Exception {
				FileSystem curFS = null;
				FSDataOutputStream os = null;				
				try{
					curFS = getFileSystem();
					os = curFS.create(new Path(path), true);
					
					byte[] ioBuffer = new byte[1024 * 1024];
					int readLen = ins.read(ioBuffer);
					while(-1 != readLen){
						os.write(ioBuffer, 0, readLen); 
						readLen = ins.read(ioBuffer);
					}					
					
				}catch(IOException e){
					log.error(e.toString(), e);
					return false;
				}finally{
					if(os != null){
						try {
							os.flush();
							os.close();
						} catch (IOException e) {
							log.error(e.toString(), e);
						}
					}
				}
				return true;
			}});
		

		
		return result;
	}
	
	public ZipFileWrapper downloadFile(String path){
		log.info("load file from HDFS:" + path);
		
		ZipFileWrapper cache = new ZipFileWrapper();
		FSDataInputStream hdfsInStream = null; 
		OutputStream out = null;
		FileSystem curFS = null;
		try{
			curFS = getFileSystem();
			if(curFS != null){
				hdfsInStream = curFS.open(new Path(path));		
				cache.rawFile = File.createTempFile("archive", "tmp");
				cache.rawFile.deleteOnExit();
				log.debug("cache zip file:" + path + "-->" + cache.rawFile.getAbsolutePath());
				//file.writeTo(cache.rawFile);
				out = new FileOutputStream(cache.rawFile);
				
				byte[] ioBuffer = new byte[1024 * 1024];
				int readLen = hdfsInStream.read(ioBuffer);
				while(-1 != readLen){
					out.write(ioBuffer, 0, readLen); 
					readLen = hdfsInStream.read(ioBuffer);
				}
			}
		}catch(IOException e){
			cache = null;
			log.error(e.toString(), e);
		}finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
					log.error(e.toString(), e);
				}
			}
			if(hdfsInStream != null){
				try {
					hdfsInStream.close();
				} catch (IOException e) {
					log.error(e.toString(), e);
				}
			}
		}
		return cache;
	}
	
	
	public FileSystem getFileSystem(){
		/*
		File rootPath = new File(localFs).getAbsoluteFile();
		if(!rootPath.isDirectory()){
			rootPath.mkdirs();
		} 
		log.info("Local root:" + rootPath.getAbsolutePath());
		*/
		
		
		if(fs == null){
			Configuration conf = new Configuration(true);
			conf.set("hadoop.tmp.dir", localFs);		
			conf.set("fs.default.name", url);	
			
			try {
				fs = FileSystem.get(URI.create(url), conf);
				fs.setWorkingDirectory(new Path("/"));
			} catch (IOException e) {
				log.error(e.toString(), e);
			}
		}
		return fs;
	}
}
