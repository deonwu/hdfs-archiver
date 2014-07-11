package org.jvnet.hudson.queue.tasks;

import java.io.File;

import org.jvnet.hudson.queue.MessageQueue;
import org.jvnet.hudson.queue.MessageTopic;

public class InitMessageQueue implements Runnable {
	private MessageQueue queue = null;
	
	public InitMessageQueue(MessageQueue queue){
		this.queue = queue; 
	}
	
	@Override
	public void run() {
		File root = queue.dataPath;
		String topicName = null;
		MessageTopic topic = null;
		for(String file: root.list()){
			if(file.endsWith(".data")){
				//queue.threadPool.execute(new WriteMessageToDisk(t));
				topicName = this.getTopicName(file);
				topic = queue.getTopic(topicName);
				topic.status = MessageTopic.TopicStatus.LOADING;
				if(topic != null && topic.storage != null){
					queue.threadPool.execute(new ReadMessageFromDisk(topic));
				}
			}
		}
	}
	
	private String getTopicName(String f){		
		int i = f.lastIndexOf('.');
		return f.substring(0, i);
	}

}
