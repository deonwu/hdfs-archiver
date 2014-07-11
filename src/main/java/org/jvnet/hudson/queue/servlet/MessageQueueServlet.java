package org.jvnet.hudson.queue.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONValue;
import org.jvnet.hudson.hadoop.Version;
import org.jvnet.hudson.hadoop.servlet.BaseServlet;
import org.jvnet.hudson.queue.Message;
import org.jvnet.hudson.queue.MessageQueue;
import org.jvnet.hudson.queue.MessageTopic;
import org.jvnet.hudson.queue.TopicSubscriber;

public class MessageQueueServlet extends BaseServlet {
	private Log log = LogFactory.getLog("query");
	private MessageQueue queue = MessageQueue.getInstance();
	private Version ver = new Version();
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException{
		String name = getTopicName(request);
		MessageTopic topic = queue.getTopic(name);
		String q = request.getParameter("action");
		if(request.getMethod().equals("POST") && (q == null || q.trim().equals("post"))){
			postMessage(topic, request, response);
		}else if(q != null && q.trim().equals("fetch")){
			fetchMessage(topic, request, response);
		}else if(q != null && q.trim().equals("ack")){
			ackMessage(topic, request, response);
		}else{
			queryMessage(topic, request, response);
		}
	}
	
	private String getTopicName(HttpServletRequest request){
		String name = request.getParameter("topic");
	   	//S request.getPathInfo()
    	if(name != null){
    		name = name.trim().split("\\.")[0];
    		if ("".equals(name)) name = null;
    	}else {
    		Pattern pa = Pattern.compile("q/([[^\\./]]+)");
    		Matcher ma = pa.matcher(request.getPathInfo());
    		if(ma.find()){
    			name = ma.group(1);
    		}
    	}
    	if(name == null) name = MessageQueue.STATUS_QUEUE;
		return name;
	}
	
	private void postMessage(MessageTopic topic, HttpServletRequest request, HttpServletResponse response) throws IOException{
		response.setContentType("text/plain");
		Map<String, String> msg = new HashMap<String, String>();
		Set<String> keys = request.getParameterMap().keySet();
		for(String k: keys){
			msg.put(k, request.getParameter(k));
		}
		if(topic.status == MessageTopic.TopicStatus.NORMAL){
			Message m = topic.addMessage(msg);
			MessageQueue.logPostMessage(topic.name, m.id);		
			response.getWriter().write("ok");
		}else {
			response.getWriter().write("err, topic in " + topic.status.name());
		}
	}
	
	private void queryMessage(MessageTopic topic, HttpServletRequest request, HttpServletResponse response) throws IOException{
		response.setContentType("text/plain");
		String receiver = request.getParameter("receiver");
		long startTime = 0; 
		int startID = 0, offset = 0, limit = 100;
		try{
			startTime = Long.parseLong(request.getParameter("startTime"));
		}catch(Exception e){ }
		try{
			startID = Integer.parseInt(request.getParameter("startID"));
		}catch(Exception e){ }
		try{
			offset = Integer.parseInt(request.getParameter("offset"));
		}catch(Exception e){ }
		try{
			limit = Integer.parseInt(request.getParameter("limit"));
		}catch(Exception e){ }		
		List<Message> messages = null;
		MessageQueue.logQueryMessage(topic.name, receiver, startTime + "", startID + "", offset +"", limit + "");
		if(request.getParameter("action") != null && request.getParameter("action").equals("last")){
			messages = topic.getLastList(limit);
		}else {
			messages = topic.fetchMessage(startTime, startID, offset, limit);
		}
		
		outputMessageQueue(topic, messages, request, response);
	}
	
	private void fetchMessage(MessageTopic topic, HttpServletRequest request, HttpServletResponse response) throws IOException{
		response.setContentType("text/plain");
		String receiver = request.getParameter("receiver"); 
		if(receiver == null) receiver = "guest";
		int limit = 100;
		try{
			limit = Integer.parseInt(request.getParameter("limit"));
		}catch(Exception e){ }
		
		int timeout = 60 * 1000 * 5;
		try{
			timeout = Integer.parseInt(request.getParameter("timeout"));
		}catch(Exception e){ }
		String filter = request.getParameter("filter");

		
		List<Message> messages = null;
		
		MessageQueue.log(topic.name, "fetch", String.format("receiver:%s, limit:%s, filter:%s", receiver, limit, filter));		
		TopicSubscriber subscriber = this.queue.getSubscriber(topic, receiver);	
		messages = subscriber.fetchWithLock(limit, timeout, filter);
		
		outputMessageQueue(subscriber.active, messages, request, response);		
	}

	private void ackMessage(MessageTopic topic, HttpServletRequest request, HttpServletResponse response) throws IOException{
		String receiver = request.getParameter("receiver"); 
		if(receiver == null) receiver = "guest";
		int limit = 100;
		try{
			limit = Integer.parseInt(request.getParameter("limit"));
		}catch(Exception e){ }
		List<Message> messages = null;
		
		MessageQueue.log(topic.name, "ack", String.format("receiver:%s, limit:%s", receiver, limit));		
		TopicSubscriber subscriber = this.queue.getSubscriber(topic, receiver);		
		subscriber.ack(request.getParameter("msg_id"));
		
		response.getWriter().println("ok");		
	}
	
	/**
	 * output the message with different output format:
	 * 1. html
	 * 2. json
	 * 3. csv
	 * @param message
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void outputMessageQueue(MessageTopic topic, List<Message> messages, HttpServletRequest request, HttpServletResponse response)
	throws IOException
	{
		String f = request.getParameter("format");
		if(f != null && f.trim().equals("json")){
			outputJsonMessageQueue(topic, messages, request, response);
		}
		else {
			response.setContentType("text/html");
			Map<String, Object> context = new HashMap<String, Object>();
			
			
			Message head = null;
			if(topic.titles != null){
				context.put("title", topic.titles);
			}else if(messages.size() > 0){
				head = messages.get(0);
				context.put("title", head.getData().keySet());
			}			
			context.put("message_list", messages);
			context.put("version", ver);
			context.put("topic", topic);

			this.renderTemplate("queue_message_list.html", context, response.getWriter());			
		}
	}
	
	private void outputJsonMessageQueue(MessageTopic topic, List<Message> message, HttpServletRequest request, HttpServletResponse response)
	throws IOException
	{
		response.setContentType("application/json");
		PrintWriter writer = response.getWriter();
		writer.append("{\"data\":[");
		Message last = null;
		Map<String, String> data = null;
		for(Iterator<Message> iter = message.iterator(); iter.hasNext(); ){
			if(last != null) {
				writer.append(',');
			}
			last = iter.next();
			data = new HashMap<String, String>();
			data.putAll(last.getData());
			data.put("id", last.id + "");
			JSONValue.writeJSONString(data, writer);
		}
		writer.append("]");
		if(last != null){
			writer.append(",\"lastTime\":" + last.createTime);
			writer.append(",\"lastID\":" + last.id);
		}
		writer.append("}");
	}
}
