package org.jvnet.hudson.hadoop;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class QoutaInputStream extends FilterInputStream{
	private Log log = LogFactory.getLog("hdfs.archiver");	
	public long qouta = Long.MAX_VALUE, size = 0;
	
	//只是为了输出debug.
	private long lastCheckTime = System.currentTimeMillis(), lastSize = 0;
	private String fileName = null;
	public QoutaInputStream(InputStream in, long qouta, String file){
		super(in);
		this.qouta = qouta;
		this.fileName = file;
	}
	
	public int read() throws IOException{
		this.size++;
		this.checkQouta();
		return super.read();
	}
	
	public int read(byte[] b) throws IOException{
		int r = super.read(b);
		this.size += r;
		this.checkQouta();
		return r;
	}
	
	public int read(byte[] b, int off, int len) throws IOException{
		int r = super.read(b, off, len);
		this.size += r;
		this.checkQouta();
		return r;
	}		
	
	private void checkQouta() throws IOException{
		if(System.currentTimeMillis() - this.lastCheckTime > 5000){
			printSpeed();
			lastSize = this.size;
			this.lastCheckTime = System.currentTimeMillis();
		}
		if(this.size > this.qouta) throw new IOException("violated the qouta limitation, size:" + this.qouta);
	}
	
	public void printSpeed(){
		long length = this.size - lastSize;			
		log.debug(String.format("The file '%s', upload speed %1.2f bytes/s.", this.fileName,
				length / 1.024 / (System.currentTimeMillis() - this.lastCheckTime)));		
	}
}
