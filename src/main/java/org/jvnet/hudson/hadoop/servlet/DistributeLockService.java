package org.jvnet.hudson.hadoop.servlet;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jvnet.hudson.hadoop.Version;

public class DistributeLockService extends BaseServlet {
	private static Map<String, LockObject> lockPool = new HashMap<String, LockObject>();
	//only used for status report.
	private static LockStatus status = new LockStatus();
	private Version ver = new Version();
	private static long lastCleanUp = 0;
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException{
		String name = request.getParameter("name");
		String owner = request.getParameter("owner");
		String timeOut = request.getParameter("timeout");
		LockObject lock = null;
		if(name != null && owner != null){
			status.lastActive = new Date(System.currentTimeMillis());
			long timeout = 30000;
			try{
				timeout = Integer.parseInt(timeOut);
			}catch(Throwable e){};
			lock = new LockObject(name, owner, timeout);
			lock.remoteIp = request.getRemoteAddr();
			this.tryGetLock(lock);
			if(request.getParameter("release") != null && lock.isLocked){
				releaseLock(lock);				
			}
			if(request.getParameter("release") == null){
				if(lock.isLocked){
					status.reqiredLockCount++;
				}else {
					status.failedLockCount++;
				}
				LockObject tmp = new LockObject(name, owner, timeout);
				tmp.remoteIp = lock.remoteIp;
				if(!lock.isLocked){
					tmp.curOnwer = lock.onwer;
				}
				tmp.isLocked = lock.isLocked;
				status.activeLock(tmp);
			}
			response.setHeader("locked", lock.isLocked ? "ok": "failed");
			response.setHeader("lock_name", lock.name);
			response.setHeader("lock_owner", lock.onwer);
			response.setHeader("lock_ip", lock.remoteIp);
			response.setHeader("lock_timeout", lock.timeout + "");
			response.setHeader("lock_lastActiveTime", lock.lastActiveTime + "");
		}
		if(request.getParameter("ajax") != null || request.getHeader("ajax") != null){
			response.getWriter().write(lock != null && lock.isLocked? "ok": "failed");
		}else {
	    	response.setContentType("text/html");
	    	response.setCharacterEncoding("utf8");			
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("locks", this.activeLockList());
			context.put("lock", lock);
			context.put("now", new Date(System.currentTimeMillis()));
			context.put("status", status);
			context.put("version", ver);
			this.renderTemplate("active_lock_list.html", context, response.getWriter());			
		}
	}
	
	public synchronized void tryGetLock(LockObject lock){
		long curTime = System.currentTimeMillis();
		if(curTime - lastCleanUp > 10 * 1000){
			this.cleanLock(curTime);
			lastCleanUp = curTime;
		}
		LockObject tmp = lockPool.get(lock.name);
		if(tmp == null){
			lockPool.put(lock.name, lock);
			lock.isLocked = true;
		}else {
			if(curTime - tmp.lastActiveTime > tmp.timeout){
				status.timeoutLockCount++;
				lockPool.put(lock.name, lock);
				lock.isLocked = true;
			}else if(tmp.onwer.equals(lock.onwer) &&
					tmp.remoteIp.equals(lock.remoteIp)
					){
				tmp.lastActiveTime = System.currentTimeMillis();
				lock.isLocked = true;
			}else {
				lock.onwer = tmp.onwer;
				lock.remoteIp = tmp.remoteIp;
				lock.timeout = tmp.timeout;
			}
		}
	}
	
	public synchronized void releaseLock(LockObject lock){
		if (lockPool.containsKey(lock.name)){
			lockPool.remove(lock.name);
		}
	}
	
	public Collection<LockObject> activeLockList(){
		Comparator<LockObject> comparator = new Comparator<LockObject>(){
			@Override
			public int compare(LockObject arg0, LockObject arg1) {
				if(arg0.name.equals(arg1.name)){
					return 0;
				}else {
					return arg0.lastActiveTime > arg1.lastActiveTime ? -1: 1;
				}
			}};
		Collection<LockObject> locks = new TreeSet<LockObject>(comparator);
		locks.addAll(lockPool.values());
		int count = 0;
		for(Iterator<LockObject> iter = locks.iterator(); iter.hasNext(); count++){
			iter.next();
			if(count > 1000){
				iter.remove();
			}
		}
		return locks;
	}
	
	public void cleanLock(long curTime){
		Iterator<Entry<String, LockObject>> iter = lockPool.entrySet().iterator();
		while(iter.hasNext()){
			LockObject obj = iter.next().getValue();
			if(curTime - obj.lastActiveTime > obj.timeout){
				status.timeoutLockCount++;
				iter.remove();
			}
		}
	}
	
	public static class LockObject{
		public String name;
		public long timeout;
		public long lastActiveTime = System.currentTimeMillis();
		public String onwer;
		public String curOnwer = "";
		public String remoteIp;
		public boolean isLocked = false;
		public LockObject(String name, String owner, long timeout){
			this.name = name;
			this.onwer = owner;
			this.timeout = timeout;
		}
		//for View
		public Date getLastActive(){
			return new Date(this.lastActiveTime);
		}
		public String getName(){return name;}
		public long getTimeout(){return timeout;}
		public String getOwner(){return onwer;}
		public String getCurOwner(){return curOnwer;}
		public String getRemoteIp(){return remoteIp;}
		public boolean getIsLocked(){return isLocked;}
	}
}
