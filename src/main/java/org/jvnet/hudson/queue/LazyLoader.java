package org.jvnet.hudson.queue;

public interface LazyLoader {
	
	/**
	 * load Message from disk.
	 * @param startID -- the first message loading from.
	 * @param count 
	 * @return
	 */
	public Message[] load(long startTime, long startID, long count);
}
