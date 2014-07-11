package org.jvnet.hudson.queue.tasks;

import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jvnet.hudson.queue.Message;
import org.jvnet.hudson.queue.MessageQueue;
import org.jvnet.hudson.queue.MessageTopic;

/**
 * save the message data to disk.
 * 
 * @author deon
 */
public class WriteMessageToDisk implements Runnable {
	private Log log = LogFactory.getLog("queue");
	private MessageTopic topic = null;
	
	public WriteMessageToDisk(MessageTopic topic){
		this.topic = topic;
	}

	@Override
	public void run() {
		Lock lock = topic.storage.writeLock;
		
		if(lock.tryLock()){
			MessageQueue.internalLog(topic.name, "write", "Start to write message.");
			try{
				if(this.topic.storage.getMessageBufferhashCode() != topic.queueHashCode()){		
					Message head = topic.head();
					
					MessageQueue.internalLog(topic.name, "write", "move the message to new head, message id:" + head.id + ", hash code:" + topic.queueHashCode());

					topic.storage.removeHeadTo(head.createTime, head.id);
					topic.storage.messageBufferhashCode = topic.queueHashCode();
				}
				
				log.debug(String.format("start search dirty message, time:%s, id:%s", topic.storage.lastMessageTime, topic.storage.lastMessageId));
				List<Message> messages = topic.fetchMessage(0, 
							topic.storage.getLastMessageId() + 1, 0, topic.maxQueueSize);

				Message start = messages.get(0);
				
				MessageQueue.internalLog(topic.name, "write", String.format("write message from %s, size:%s", start.id, messages.size()));
				
				topic.storage.writeMessageToDisk(messages);
				Message m = messages.get(messages.size() - 1);
				
				topic.storage.lastMessageId = m.id;
				topic.storage.lastMessageTime = m.createTime;
				topic.storage.writeWaiting = false;
				topic.storage.lastWriteTime = System.currentTimeMillis();
				topic.flushed = true;
				
				MessageQueue.internalLog(topic.name, "write", String.format("write message done."));
			}catch(Exception e){
				MessageQueue.internalLog(topic.name, "write", "write message error:" + e.toString());
				log.error(e.toString(), e);
			}finally{
				lock.unlock();
			}
		}else {
			MessageQueue.internalLog(topic.name, "write", "failed to get writing lock.");
		}
	}
}
