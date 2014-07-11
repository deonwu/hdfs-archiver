package org.jvnet.hudson.queue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DataDiskStorage implements LazyLoader{
	private Log log = LogFactory.getLog("stoarge");
	
	public final ReentrantLock writeLock = new ReentrantLock();
	public long lastWriteTime = System.currentTimeMillis();
	public long lastMessageTime = 0;
	public int lastMessageId = 0;
	public String topicName = "";
	public boolean writeWaiting = false;
	
	public int messageBufferhashCode = 0;
	
	public File dataPath = null;

	public DataDiskStorage(File path){
		this.dataPath = path;
	}
	
	public int getMessageBufferhashCode(){
		return this.messageBufferhashCode;
	}
	public int getLastMessageId(){
		return this.lastMessageId;
	}
	
	@Override
	public Message[] load(long startTime, long startID, long count) {
		JSONReader reader = null;
		Message[] tmp = null;
		try {
			//InputStreamReader(InputStream in) 
			MessageQueue.internalLog(topicName, "load", String.format("startTime=%s, startID=%s, count=%s", startTime, startID, count));
			synchronized(dataPath){
				reader = new JSONReader(dataPath);
			}
			Message obj = reader.nextMessage();
			for(; obj != null; obj = reader.nextMessage()){
				if(obj.createTime >= startTime && obj.id >= startID){
					break;
				}
			}			
			ArrayList<Message> result = new ArrayList<Message>();
			for(int i = 0; i < count && obj != null; obj = reader.nextMessage(), i++){
				result.add((Message)obj);
			}
			tmp = result.toArray(new Message[]{});
		} catch (Exception e) {
			log.error(e.toString(), e);
		} finally{
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					log.error(e.toString(), e);
				}
			}
		}
		
		return tmp;
	}
	
	public int initMessageTopicQueue(MessageTopic topic){
		JSONReader reader = null;
		Message obj = null;
		int count = 0;
		try{
			reader = new JSONReader(dataPath);			
			obj = reader.nextMessage(); 
			for(; obj != null; obj = reader.nextMessage()){
				obj.fillData(new SoftReference<Map<String, String>>(obj.getData()));
				obj.data = null;
				topic.putMessage(obj);
				this.lastMessageId = obj.id;
				this.lastMessageTime = obj.createTime;
				count++;
			}
		}catch(EOFException e){
		}catch(IOException e){
			log.error(e.toString(), e);
		}		
		
		return count;
	}
	
	public void writeMessageToDisk(List<Message> messages) throws IOException{
		log.debug("write message to:" + dataPath.getAbsolutePath());
		Writer os = getWriter();
		for(Message msg : messages){
			writeMessage(msg, os);
			msg.fillData(new SoftReference<Map<String, String>>(msg.getData()));
			msg.data = null;
		}
		os.flush();
		os.close();
	}
	
	protected Writer getWriter() throws UnsupportedEncodingException, FileNotFoundException{
		return new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(dataPath, true), 64 * 1024), "utf8");
	}
	
	private void writeMessage(Message m, Writer out) throws IOException{
		//log.info("write message:" + m.id);
		out.append("\n{\"id\":" + m.id);
		out.append(",\"createTime\":" + m.createTime);
		if(m.status != null){
			out.append(",\"status\":\"" + m.status + "\"");
		}
		out.append(",\"data\":");
		JSONValue.writeJSONString(m.getData(), out);
		out.append("}");
	}

	public void removeHeadTo(long startTime, long startID) throws Exception{
		if(!dataPath.isFile()) return;
		log.debug("remove head message from:" + dataPath.getAbsolutePath());
		
		Message obj = null;
		Writer os = null;
		JSONReader reader = null;
		try{
			reader = new JSONReader(dataPath);
			
			obj = reader.nextMessage(); 
			for(; obj != null; obj = reader.nextMessage()){
				//log.debug(String.format("move head start time:%s start id:%s, mid:%s, mt:%s", startTime, startID, obj.id, obj.createTime));
				if(obj.createTime >= startTime && obj.id >= startID){
					break;
				}
			}
		}catch(EOFException e){
		}catch(Exception e){
			log.error(e.toString(), e);
		}
		
		File tmp = new File(dataPath.getParentFile(), dataPath.getName() + ".tmp");		
		if(reader != null){
			try{
				os = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(tmp), 64 * 1024), "utf8");
				for(; obj != null; obj = reader.nextMessage()){
					//log.debug("write message id:" + obj.id);
					this.writeMessage((Message)obj, os);
				}
			}catch(EOFException e){
			}catch(IOException e){
				log.error(e.toString(), e);
			}finally{
				if(reader != null)reader.close();
				if(os != null){
					os.flush();
					os.close();					
				}
			}
		}
		if(tmp.isFile()){
			synchronized(dataPath){
				dataPath.delete();
				tmp.renameTo(dataPath);
			}		
		}
	}	
	
	class JSONReader {
		private JSONParser parser = null;
		
		private Reader reader = null;		
		private Transformer transformer = new Transformer();

		public JSONReader(File path){
			try {
				reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(path), 64 * 1024), "utf8");
				parser = new JSONParser();
			} catch (Exception e) {
				log.error(e.toString(), e);
			} 
		}
		
		public Map next() throws IOException {
			this.transformer.reset();
			//parser.reset();
			try {
				Field f = parser.getClass().getDeclaredField("status");
				f.setAccessible(true);
				f.setInt(parser, 0);
			} catch (Exception e) {
				log.error(e.toString(), e);
			}
			
			try{
				parser.parse(reader, transformer, true);
			}catch(ParseException e){
				if (e.toString().indexOf("END OF FILE") == -1){
					log.error("load data:" + e.toString(), e);
				}
			}
			
			return (Map)transformer.getResult();
		}
		
		public Message nextMessage() throws IOException{
			Message m = null;
			Map o = next();
			if(o != null){
				try{
					m = new Message();
					m.id = Integer.parseInt(o.get("id").toString());
					m.createTime = Long.parseLong(o.get("createTime").toString());
					m.data = (Map<String, String>)o.get("data");
					if(o.containsKey("status")){
						m.status = o.get("status").toString();
					}
					//log.info("load msg:"+ m.id + ", time:" + m.createTime);
				}catch(Throwable e){
					m = null;
					log.error(e.toString(), e);
				}
			}else {
				log.warn("not found data");
			}
			return m;
		}
		
		public void close() throws IOException{
			if(reader != null){
				reader.close();
			}
		}		
	}
	
	class Transformer implements ContentHandler{
        private Stack valueStack;
        
        public Object getResult(){
            if(valueStack == null || valueStack.size() == 0)
                return null;
            return valueStack.peek();
            
        }
        
        public void reset(){
        	if(this.valueStack != null){
        		this.valueStack.clear();
        	}
        }
        
        public boolean endArray () throws ParseException, IOException {
            trackBack();
            return true;
        }

        public void endJSON () throws ParseException, IOException {}

        public boolean endObject () throws ParseException, IOException {
        	trackBack();  
        	boolean isTopLevel = valueStack.size() == 1;
            return !isTopLevel;
        }

        public boolean endObjectEntry () throws ParseException, IOException {
            Object value = valueStack.pop();
            Object key = valueStack.pop();
            Map parent = (Map)valueStack.peek();
            parent.put(key, value);
            return true;
        }

        private void trackBack(){
            if(valueStack.size() > 1){
                Object value = valueStack.pop();
                Object prev = valueStack.peek();
                if(prev instanceof String){
                    valueStack.push(value);
                }
            }
        }
        
        private void consumeValue(Object value){
            if(valueStack.size() == 0)
                valueStack.push(value);
            else{
                Object prev = valueStack.peek();
                if(prev instanceof List){
                    List array = (List)prev;
                    array.add(value);
                }
                else{
                    valueStack.push(value);
                }
            }
        }
        
        public boolean primitive (Object value) throws ParseException, IOException {
            consumeValue(value);
            return true;
        }

        public boolean startArray () throws ParseException, IOException {
            List array = new JSONArray();
            consumeValue(array);
            valueStack.push(array);
            return true;
        }

        public void startJSON () throws ParseException, IOException {
            valueStack = new Stack();
        }

        public boolean startObject () throws ParseException, IOException {
            Map object = new JSONObject();
            consumeValue(object);
            valueStack.push(object);
            return true;
        }

        public boolean startObjectEntry (String key) throws ParseException, IOException {
            valueStack.push(key);
            return true;
        }
        
    }	
}
