/*
 * Copyright (C) 2012 EaseTheWorld
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * https://github.com/EaseTheWorld/ZOUI
 */

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
	
	// move forward and return the available item.
	// make new instance if necessary.
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