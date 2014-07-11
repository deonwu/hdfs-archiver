package org.jvnet.hudson.queue;

import java.util.List;
import java.util.Map;

public class MessageTopic {
	//public static final int MAX_QUEUE_SIZE = 100;
	public enum TopicStatus {INIT, LOADING, NORMAL};
	public static final int MAX_QUEUE_SIZE = 100000;
	//public enum status 
	public String name;
	public int maxQueueSize = MAX_QUEUE_SIZE;
	//public long startTime;
	//public long latestTime;
	public String[] titles = null;
	
	public DataDiskStorage storage = null;
	public TopicStatus status = TopicStatus.INIT;
	
	//private Map<Long, Message> cache = null;
	public ArrayListMessageQueue queue = new ArrayListMessageQueue(MAX_QUEUE_SIZE, null);
	public boolean flushed = true;
	private int messageID = -1;
	private Message lastPut = null;
	public MessageTopic(String name){
		this.name = name;
	}
	
	public void setMaxQueueSize(int maxSize){
		this.queue.maxCapacity = maxSize;
		this.maxQueueSize = maxSize;
	}
	
	public void setDataDiskStorage(DataDiskStorage storage){
		this.storage = storage;
		queue.lazyloader = storage;
	}
	
	public Message addMessage(Map<String, String> message){
		Message m = new Message();
		m.createTime = System.currentTimeMillis();
		m.data = message;
		
		/**
		 * to make sure the message is order by message ID.
		 */
		synchronized(this){
			if(status == TopicStatus.NORMAL){
				m.id = nextID();
				queue.add(m);
			}
		}
		return m;
	}
	
	/**
	 * only used load message from disk at queue starting.
	 * @param m
	 */
	public void putMessage(Message m){
		if(this.lastPut != null && m.id != this.lastPut.id % Integer.MAX_VALUE + 1){
			this.queue = new ArrayListMessageQueue(this.maxQueueSize, null);
			this.queue.lazyloader = this.storage;
		}
		this.queue.add(m);
		this.messageID = m.id - 1;
		this.lastPut = m;
	}
	
	public List<Message> fetchMessage(long startTime,
			int startId, int offset, int limit){
		List<Message> data = null;
		synchronized(this){
			data = queue.searchTailMessage(startTime, startId, offset, limit);		
		}
		return data;
	}
	
	public List<Message> getLastList(int count){
		return queue.lastMessage(count);
	}
	
	public List<Message> getMemoryMessage(){
		return null;
	}
	
	public int getQueueSize(){
		return queue.size();
	}

	public int getBufferSize(){
		return queue.bufferSize();
	}
	
	public int getMaxQueueSize(){
		return this.maxQueueSize;
	}
	
	public int remainSize(){
		return this.maxQueueSize - queue.size();
	}
	
	public String getName(){
		return this.name;
	}
	
	public int queueHashCode(){
		return this.queue.hashCode();
	}
	
	public int curMessageId(){
		return this.messageID >= 0 ? this.messageID + 1 : 1;
	}
	
	public int getCurMessageId(){
		return curMessageId();
	}

	
	public Message head(){
		return queue.headMessage();
	}
	
	public Message tail(){
		return queue.tail();
	}
	
	/**
	 * start from 1 ~ Integer.MAX_VALUE;
	 * @return
	 */
	private int nextID(){
		int tmp = 0;
		this.messageID++;
		messageID = messageID % Integer.MAX_VALUE;
		tmp = messageID + 1;
		return tmp;
	}
}
