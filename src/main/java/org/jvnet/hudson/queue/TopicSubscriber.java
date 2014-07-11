package org.jvnet.hudson.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;

public class TopicSubscriber {
	public MessageTopic topic = null;
	public MessageTopic active = null;
	private ExpressionFactory factory = ExpressionFactoryImpl.getInstance();
	
	public TopicSubscriber(MessageTopic topic, MessageTopic active){
		this.topic = topic;
		this.active = active;
		active.setMaxQueueSize(100);
		
		/**
		 * need not save the subscribe data. 
		 */
		active.setDataDiskStorage(new FullStorage(active.storage.dataPath));
	}
	
	public List<Message> fetchWithLock(int limit, int timeout, String filter){
		List<Message> result = new ArrayList<Message>(limit);
		synchronized(this){
			fetchFromOriginTopic(filter);
			long expried = System.currentTimeMillis() + timeout;
			long cur = System.currentTimeMillis();
			for(Message msg : active.queue.allMessage()){
				if(msg.status == null || 
				   msg.status.trim().equals("") ||
				   (msg.status.equals(Message.LOCKED) && msg.expired < cur)){
					result.add(msg);
					msg.expired = expried;
					msg.status = Message.LOCKED;
					msg.retry++;
					active.flushed = false;
					if(result.size() >= limit){ 
						break;
					}
				}
			}
		}
		
		return result;
	}
	
	public void fetchFromOriginTopic(String filter){
		Expression el = null; //factory.create("test2 * 1 && true || false");
		int activeSize = active.remainSize();
		Message m = active.tail();
		if(m != null && topic.queue.size() > 0 && m.id == topic.tail().id){
			return;
		}
		
		int startID = 1;
		if(m != null){ startID = m.id + 1; }
		
		if(filter != null && filter.trim().length() > 0){
			el = factory.create(filter + "  && true || false");
		}
		
		List<Message> result = topic.fetchMessage(0, startID, 0, activeSize);
		for(Message msg : result){
			if(el == null || (Boolean)el.evaluate(msg.getData())){
				m = msg.copy();
				active.putMessage(m);
			}
		}
	}
	
	public void ack(String ids){
		ids = ids == null ? "" : ids;
		synchronized(this){
			Set<Integer> keys = new TreeSet<Integer>();
			for(String k :ids.split(",")){
				keys.add(Integer.parseInt(k.trim()));
			}
			
			int removeHeadCount = 0;
			boolean stopMove = false;
			
			for(Message msg : active.queue.allMessage()){
				if(keys.contains(msg.id)){
					keys.remove(msg.id);
					msg.status = Message.PROCESSED;
					active.flushed = false;
					if(!stopMove){
						removeHeadCount++;
					}
				}else if(!stopMove && (msg.status != null && msg.status.equals(Message.PROCESSED) ||msg.retry > 3)){
					removeHeadCount++;
				}else if(msg.status == null || !msg.status.equals(Message.PROCESSED)){
					stopMove = true;
				}
			}
			if(removeHeadCount >= active.queue.size()){
				removeHeadCount = active.queue.size() - 1;
			}			
			active.queue.removeHead(removeHeadCount);
		}
	}
}
