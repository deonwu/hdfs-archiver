package org.jvnet.hudson.queue.tasks;

import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jvnet.hudson.queue.Message;
import org.jvnet.hudson.queue.MessageTopic;

public class ReadMessageFromDisk implements Runnable {
	private Log log = LogFactory.getLog("queue");
	private MessageTopic topic = null;
	
	public ReadMessageFromDisk(MessageTopic topic){
		this.topic = topic;
	}	

	@Override
	public void run() {
		Lock lock = topic.storage.writeLock;		
		if(lock.tryLock()){
			try{
				log.info(String.format("recover topic '%s' from disk", topic.name));
				int count = 0;
				synchronized(this.topic){
					count = this.topic.storage.initMessageTopicQueue(topic);
					this.topic.storage.messageBufferhashCode = topic.queueHashCode();
					this.topic.storage.lastMessageId = topic.curMessageId();
					topic.status = MessageTopic.TopicStatus.NORMAL;
					if(topic.getQueueSize() > 0){
						Message m = topic.head();
						try {
							this.topic.storage.removeHeadTo(m.createTime, m.id);
						} catch (Exception e) {
							log.error("loading topic from disk, error:" + e.toString(),
									e);
						}
					}
				}
				
				log.info(String.format("loaded '%s' messages for topic '%s'", count, topic.name));				
			}finally{
				lock.unlock();
			}
		}
	}

}
