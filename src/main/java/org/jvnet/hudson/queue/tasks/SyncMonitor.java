package org.jvnet.hudson.queue.tasks;

import java.util.ArrayList;
import java.util.Collection;

import org.jvnet.hudson.queue.MessageQueue;
import org.jvnet.hudson.queue.MessageTopic;

/**
 * To Monitor which topic needs to sync into disk.
 *  
 * @author deon
 */
public class SyncMonitor implements Runnable {
	private MessageQueue queue = null;
	
	public SyncMonitor(MessageQueue queue){
		this.queue = queue; 
	}

	@Override
	public void run() {
		if(queue.syncLock.tryLock()) {
			try {
				Collection<MessageTopic> topics = new ArrayList<MessageTopic>(queue.topics.values());				
				//检查是否需要写数据
				SyncStatus st = null;
				for(MessageTopic t: topics){
					if(t.getQueueSize() == 0)continue;
					st = this.checkNeedSyncTopic(t);
					if(st.canSync){
						queue.threadPool.execute(new WriteMessageToDisk(t));
						MessageQueue.internalLog(t.name, "schedule_sync", st.cause);
						t.storage.writeWaiting = true;
					}
				}
	        } finally {
	        	queue.syncLock.unlock();
	        }
	    }
	}
	
	public SyncStatus checkNeedSyncTopic(MessageTopic m){
		SyncStatus st = new SyncStatus();
		st.canSync = false;
		if(m.storage != null && m.status == MessageTopic.TopicStatus.NORMAL){
			int dirty = 0;
			if(m.curMessageId() > m.storage.lastMessageId){
				dirty = m.curMessageId() - m.storage.lastMessageId;
			}else if(m.curMessageId() < m.storage.lastMessageId){
				dirty = m.curMessageId();
				dirty += (Integer.MAX_VALUE - m.storage.lastMessageId);
			}else if(!m.flushed){
				dirty = Integer.MAX_VALUE;
			}
			
			if(dirty > m.getQueueSize() * 0.1){
				st.canSync = true;
				st.cause = String.format("write the queue, too many new message:%s, queue size:%s", dirty, m.getQueueSize());				
			}else if(dirty > 0 && System.currentTimeMillis() - m.storage.lastWriteTime > 1000 * 5){
				long s = System.currentTimeMillis() - m.storage.lastWriteTime;
				st.canSync = true;
				st.cause = String.format("write the queue, too long time, new message:%s, last write time:%s", dirty, s / 1000);
			}
		}
		return st;
	}
	
	class SyncStatus{
		public boolean canSync = false;
		public String cause = null;
	}
}
