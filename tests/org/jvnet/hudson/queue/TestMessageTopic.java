package org.jvnet.hudson.queue;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;


public class TestMessageTopic {
	
	@Test
	public void test_ProcessSimpleClientPackage(){
		assertEquals("test demo", 1, 1);
	}
	
	@Test
	public void test_MessageTopicIDOverload(){
		MessageTopic topic = new MessageTopic("test");
		Message m = new Message();
		int startID = Integer.MAX_VALUE - 2;
		
		m.id = startID;
		m.data = new HashMap<String, String>();
		topic.putMessage(m);
		
		topic.addMessage(nextMessage());
		topic.addMessage(nextMessage());
		topic.addMessage(nextMessage());
		topic.addMessage(nextMessage());
		
		List<Message> list = topic.fetchMessage(0, startID, 0, 1000);
		
		//topic.fetchMessage(startTime, startId, offset, limit)
		
		assertEquals("check message size", 5, list.size());		
		assertEquals("check message id", startID, list.get(0).id);
		assertEquals("check message id", startID + 1, list.get(1).id);		
		assertEquals("check message id", Integer.MAX_VALUE, list.get(2).id);		
		assertEquals("check message id", 1, list.get(3).id);
		assertEquals("check message id", 2, list.get(4).id);

		List<Message> list2 = topic.fetchMessage(0, 1, 0, 1000);
		assertEquals("check message size", 2, list2.size());
		assertEquals("check message id", 1, list2.get(0).id);
		assertEquals("check message id", 2, list2.get(1).id);		
	}
	
	private Map<String, String> nextMessage(){
		Map<String, String> m = new HashMap<String, String>();
		return m;
	}
	
}
