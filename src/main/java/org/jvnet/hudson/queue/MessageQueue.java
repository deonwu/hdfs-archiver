package org.jvnet.hudson.queue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jvnet.hudson.queue.tasks.InitMessageQueue;
import org.jvnet.hudson.queue.tasks.SyncMonitor;

/**
 * create a message queue, can post message and recieve message.
 * @author deon
 *
 */
public class MessageQueue {
	private Log log = LogFactory.getLog("queue");
	public static final String STATUS_QUEUE = "status_queue";
	public static MessageTopic status_queue = null;
	public static MessageTopic internal_log = null;
	private static MessageQueue ins = null;
	
	public Map<String, MessageTopic> topics = new HashMap<String, MessageTopic>();
	public Map<String, TopicSubscriber> subscriber = new HashMap<String, TopicSubscriber>();
	
	public final ReentrantLock syncLock = new ReentrantLock();
	public transient ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
			10, 30, 60, TimeUnit.SECONDS, 
			new LinkedBlockingDeque<Runnable>(10000)
			);

	public File dataPath = null;
	private transient Timer timer = new Timer();	
	
	public static MessageQueue getInstance(){
		return ins;
	}
	
	public static void start(File dataRoot){
		ins = new MessageQueue();
		ins.dataPath = dataRoot;
		ins.timer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				ins.syncMonitor();
			}}, 10, 1000);
		ins.log.info("start message queue, root path:" + ins.dataPath.getAbsolutePath());
		if(!ins.dataPath.exists()){
			ins.dataPath.mkdirs();
		}
		
		status_queue = ins.getTopic(STATUS_QUEUE);
		internal_log = ins.getTopic("internal_log");
		status_queue.titles = new String[]{"topic", "action", "message", };
		internal_log.titles = new String[]{"topic", "action", "message", };
		status_queue.storage = null;
		internal_log.storage = null;
		
		ins.threadPool.execute(new InitMessageQueue(ins));
	}
	
	public MessageTopic getTopic(String name){
		MessageTopic topic = null;
		synchronized(this){
			topic = topics.get(name);
			if(topic == null){
				topic = new MessageTopic(name);
				topic.setDataDiskStorage(new DataDiskStorage(new File(this.dataPath, name + ".data")));
				if(topic.storage.dataPath.isFile()){
					topic.status = MessageTopic.TopicStatus.LOADING;
				}else {
					topic.status = MessageTopic.TopicStatus.NORMAL;
				}
				topics.put(name, topic);
			}
		}
		return topic;
	}
	
	public TopicSubscriber getSubscriber(MessageTopic topic, String name){		
		String key = topic.name + "_" + name;
		TopicSubscriber sub = this.subscriber.get(key);
		if (sub == null){
			sub = new TopicSubscriber(topic, getTopic(key));
			this.subscriber.put(key, sub);
		}
		
		return sub;
	}
	
	private void syncMonitor(){
		threadPool.execute(new SyncMonitor(this));
	}
	
	public static void internalLog(String topic, String action, String message){
		Map<String, String> l = new HashMap<String, String>();
		l.put("topic", topic);
		l.put("action", action);
		l.put("message", message);		
		internal_log.addMessage(l);	
		ins.log.info(topic + " " + action + " " + message);
	}
	
	public static void log(String topic, String action, String message){
		Map<String, String> l = new HashMap<String, String>();
		l.put("topic", topic);
		l.put("action", action);
		l.put("message", message);
		
		status_queue.addMessage(l);
	}
	
	public static void logPostMessage(String topic, int newId){
		log(topic, "post", "new id:" + newId);
	}
	
	public static void logQueryMessage(String topic, String receiver, String startTime, String startID, String offset, String limit){
		log(topic, "query", String.format("receiver=%s, startTime=%s, startID=%s, offset=%s, limit=%s", 
				receiver, startTime, startID, offset, limit));
	}
}
