package org.jvnet.hudson.hadoop;

import com.mongodb.BasicDBObject;

public class Client extends BasicDBObject{
	public Client(){}
	public Client(String id){
		this.put("_id", id);
	}
	public Client(String id, long updateTime, double qouta){
		this.put("_id", id);
		this.put("lastUpdated", updateTime);
		this.put("qouta", qouta);
	}
	
	public long getLastUpdateTime(){
		return this.getLong("lastUpdated");
	}
	
	public double getQouta(){
		//可能在Web接口重启后，修改了参数。已保留的Qouta比修改参数后的还大。
		//在两者间取一个小的值。
		return Math.min(this.getLong("qouta"), Integer.MAX_VALUE);
	}
	public void reduceQouta(long size){
		this.put("qouta", this.getQouta() - size);
	}
} 