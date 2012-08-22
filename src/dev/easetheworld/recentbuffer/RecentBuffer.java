package dev.easetheworld.recentbuffer;

import java.lang.reflect.Array;

public class RecentBuffer<E> {
	private int mSize;
	private final Class<E> mClazz;
	private int mHead;
	private final E[] mData;
	
	public RecentBuffer(int size, Class<E> clazz) {
		mData = (E[]) Array.newInstance(clazz, size);
		mClazz = clazz;
		clear();
	}
	
	public int getSize() {
		return mSize;
	}
	
	public void clear() {
		mHead = -1;
		mSize = 0;
	}
	
	// smaller index is older
	public E getFromOldest(int index) {
		if (index >= mSize)
			return null;
		index = (mHead - mSize + 1 + index + mData.length) % mData.length;
		return mData[index];
	}
	
	// smaller index is newer
	public E get(int index) {
		if (index >= mSize)
			return null;
		index = (mHead - index + mData.length) % mData.length;
		return mData[index];
	}
	
	// move forward
	public E obtain() {
		mHead = (mHead + 1) % mData.length;
		if (mData[mHead] == null) {
			try {
				mData[mHead] = mClazz.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		if (mSize < mData.length)
			mSize++;
		return mData[mHead];
	}
	
	public void removeSince(int index) {
		if (mSize > index)
			mSize = index;
	}
}