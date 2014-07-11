package org.jvnet.hudson.hadoop.servlet;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jvnet.hudson.hadoop.servlet.DistributeLockService.LockObject;


public class LockStatus {
	public Date start = new Date();
	public Date lastActive = new Date();
	public int reqiredLockCount = 0;
	public int failedLockCount = 0;
	public int timeoutLockCount = 0;
	
	public Queue<DistributeLockService.LockObject> recentLock = null;
	
	public LockStatus(){
		recentLock = new ConcurrentLinkedQueue<DistributeLockService.LockObject>();
	}
	
	public void activeLock(DistributeLockService.LockObject lock){
		recentLock.add(lock);
		while(recentLock.size() > 20){
			recentLock.poll();
		}
	}
	
	public Date getStart(){return start;}
	public Date getLastActive(){return lastActive;}
	public int getReqiredLockCount(){return reqiredLockCount;}
	public int getFailedLockCount(){return failedLockCount;}
	public int getTimeoutLockCount(){return timeoutLockCount;}
	public Collection<DistributeLockService.LockObject> getRecentLock(){
		Comparator<LockObject> comparator = new Comparator<LockObject>(){
			@Override
			public int compare(LockObject arg0, LockObject arg1) {
				return arg0.lastActiveTime > arg1.lastActiveTime ? -1: 1;
			}};
		Collection<LockObject> locks = new TreeSet<LockObject>(comparator);
		locks.addAll(recentLock);	
		return locks;
	}
}
