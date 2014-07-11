package org.jvnet.hudson.queue;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A thread-safe array queue, This queue orders elements FIFO (first-in-first-out).
 * 
 * Key feature:
 * 1. It's support search elements from tail.
 * 2. auto increase capacity.
 * 3. thread-safe, iterator without ConcurrentModificationException.
 * 
 * @author deon
 *
 */
public class ArrayListMessageQueue {
	public int maxCapacity = 0;
	
	public LazyLoader lazyloader = null;
	private Message[] buffer = new Message[256];
	private int head = 0;
	private int tail = 0;
	
	/**
	 * the message id is generated in Message topic more reasonable,
	 * but it's can 
	 */
	
	public ArrayListMessageQueue(int capacity, LazyLoader loader){
		this.maxCapacity = capacity;
	}
	
	public void add(Message e){
		if(tail >= buffer.length){
			this.reallocate();
		}
		buffer[this.tail] = e;
		this.tail++;
		
		//move the head to next.
		head = Math.max(tail - this.maxCapacity, head);
	}
	
	public Message headMessage(){
		return this.buffer[head];
	}
	
	/**
	 * Search message from tail.
	 * @param startTime
	 * @param startId
	 * @param skip -- count of skip the result.
	 * @param limit -- the max result.
	 * @return
	 */
	public List<Message> searchTailMessage(long startTime,
			int startId, int skip, int limit){
		
		ReadOnlyList result = null;
		if(head < tail){
			//Message last = buffer[tail - 1];
			//100, 101, 102, 1, 2, 3, 4
			int index = tail - 1;
			if(startId > 0){
				int messageId = buffer[index].id; 
				if(messageId < startId){ //is restart.
					index = Math.max(index - messageId, head);
					messageId = buffer[index].id;
				}
				index = Math.max(index - (messageId - startId), head);
			}
			if(startTime > 0){
				for(; index > head; index--){
					if(buffer[index].createTime <= startTime) break;
				}
			}
			index = Math.min(index + skip, tail - 1);
			int indexTail = Math.min(index + limit, tail);
			
			result = new ReadOnlyList(this.buffer, index, indexTail, null);
		}else {
			result = new ReadOnlyList(this.buffer, 0, 0, null);
		}
		
		this.lazyLoadMessageData(result);
		return result;
	}
	
	public void removeHead(int i){
		//if(i < size())
		i = Math.min(i, size());
		head += i;
	}

	public List<Message> lastMessage(int count){
		int index = Math.max(tail - count, head);
		return new ReadOnlyList(this.buffer, index, tail, null);
	}
	
	public List<Message> allMessage(){
		return new ReadOnlyList(this.buffer, head, tail, null);
	}
	
	public Message tail(){
		if (this.size() > 0){
			return this.buffer[tail -1];
		}else {
			return null;
		}
	}
	
	private void lazyLoadMessageData(ReadOnlyList messages){
		Message m = null;
		int i = 0;
		for(Message msg : messages){
			if(msg.getData() == null){
				m = msg; 
				break;
			}
			i++;
		}
		if(m != null && this.lazyloader != null){
			Message[] _tmp = this.lazyloader.load(m.createTime, m.id, messages.size() - i);
			messages.setRef(_tmp);
			for(int j = 0; i < messages.size(); i++, j++){
				if(j >= _tmp.length)break;
				if(messages.get(i).id == _tmp[j].id){
					messages.get(i).fillData(new SoftReference<Map<String, String>>(_tmp[j].getData()));
				}
			}
		}
	}
	
	public int size(){
		return tail - head;
	}
	
	public int bufferSize(){
		return buffer.length;
	}

	public int capacity(){
		return buffer.length - tail;
	}	
	
	public int hashCode(){
		return this.buffer.hashCode();
	}
	
	/**
	 * Get message list that only in memory. the message data will covert as
	 * soft reference if the data is wrote into disk. the data may be removed
	 * from memory by GC, and the data can be loaded if need read.
	 * 
	 * @return 
	 */
	public Iterator<Message> getMemoryMessage(){
		return null;
	}
	
	public void reallocate(){
		int curSize = tail - head;
		int size = Math.min(tail - head, this.maxCapacity) * 2;		
		Message[] tmp = new Message[size];
		System.arraycopy(buffer, head, tmp, 0, tail - head);
		head = 0;
		tail = curSize;
		this.buffer = tmp;
	}

}
