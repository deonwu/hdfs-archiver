package org.jvnet.hudson.queue;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.Map;


public class Message implements Serializable{
	private static final long serialVersionUID = 1L;
	public static final String LOCKED = "locked";
	public static final String PROCESSED = "processed";
	public static final String ERROR = "error";
	
	public String status = "";
	public long expired = 0;
	public int retry = 0;
	
	public int id;
	public long createTime;
	public Map<String, String> data = null;
	private transient SoftReference<Map<String, String>> ref = null;
	
	public Map<String, String> getData(){
		if(data != null){
			return data;
		}else if(ref != null){
			return ref.get();
		}
		return null;
	}
	
	/**
	 * It called by lazyloader of MessageTopic.
	 * @param ref
	 */
	public void fillData(SoftReference<Map<String, String>> ref){
		this.ref = ref;
	}	
	
	/*
	public int hashCode(){
		return id;
	}*/
	
	public boolean equals(Object m){
		if(m instanceof Message){
			Message o = (Message)m;
			return this.id == o.id && o.createTime == this.createTime;
		}
		return false;
	}
	
	public Date getCreateTime(){
		return new Date(this.createTime);
	}
	
	public int getId(){
		return id;
	}
	
	public String getStatus(){
		return this.status;
	}
	
	public Message copy(){
		Message m = new Message();
		m.id = id;
		m.createTime = createTime;
		m.ref = new SoftReference<Map<String, String>>(this.getData());		
		return m;
	}
	
	public String toString(){
		return String.format("#%s@%s", id, this.createTime);
	}
}
