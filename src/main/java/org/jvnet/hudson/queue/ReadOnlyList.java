package org.jvnet.hudson.queue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ReadOnlyList implements List<Message> {
	private final int lStart;
	private final int lEnd;
	private final Message[] buffer;
	private Message[] bufferRef;
	
	public ReadOnlyList(Message[] buffer, int start, int end, Message[] ref){
		this.buffer = buffer;
		this.lStart = start;
		this.lEnd = end;
		this.bufferRef = ref;		
	}
	
	@Override
	public int size() {
		return this.lEnd - this.lStart;
	}

	@Override
	public boolean isEmpty() {
		return this.lEnd == this.lStart;
	}

	@Override
	public boolean contains(Object o) {
		return false;
	}

	@Override
	public Iterator<Message> iterator() {
		return new MessageIterator(this.lStart, this.lEnd);
	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return null;
	}

	@Override
	public boolean add(Message e) {
		return false;
	}

	@Override
	public boolean remove(Object o) {
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Message> c) {
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends Message> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	@Override
	public void clear() {	
	}
	
	public void setRef(Message[] ref){
		this.bufferRef = ref;
	}

	@Override
	public Message get(int index) {
		return this.buffer[this.lStart + index];
	}

	@Override
	public Message set(int index, Message element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(int index, Message element) {
	}

	@Override
	public Message remove(int index) {
		return null;
	}

	@Override
	public int indexOf(Object o) {
		for(int i = lStart; i < lEnd; i++){
			if(buffer[i].equals(o)) return i - lStart;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		int index = -1;
		for(int i = lStart; i < lEnd; i++){
			if(buffer[i].equals(o)) index = i;
		}
		return index - lStart;
	}

	@Override
	public ListIterator<Message> listIterator() {
		return new MessageIterator(this.lStart, this.lEnd);
	}

	@Override
	public ListIterator<Message> listIterator(int index) {
		return new MessageIterator(this.lStart + index, this.lEnd);
	}

	@Override
	public List<Message> subList(int fromIndex, int toIndex) {
		return new ReadOnlyList(this.buffer, this.lStart + fromIndex, this.lStart + toIndex, this.bufferRef);
	}

	private class MessageIterator implements Iterator<Message>, ListIterator<Message>{
		private final int start;
		private final int end;
		private int index;
		public MessageIterator(int start, int end){
			this.start = start;
			this.end = end;
			this.index = start;
			if(start > end || start < 0 || end > buffer.length){
				throw new Error(String.format("error range, %s-%s, buffer len:%s", start, end, buffer.length));
			}
		}
		
		@Override
		public boolean hasNext() {
			return this.index < this.end;
		}
		@Override
		public Message next() {
			if(this.index < this.end){
				return buffer[this.index++];
			}
			return null;
		}
		
		@Override
		public void remove() {			
		}

		@Override
		public boolean hasPrevious() {
			return index > start;
		}

		@Override
		public Message previous() {
			this.index--;
			return this.index >= start ? buffer[this.index]: null;
		}

		@Override
		public int nextIndex() {
			return this.index;
		}

		@Override
		public int previousIndex() {
			return this.index - 1;
		}

		@Override
		public void set(Message e) {
		}

		@Override
		public void add(Message e) {			
		}
		
	}	
}
