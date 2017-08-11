/*
 * Copyright (C) 2009 The Android Open Source Project
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
 */

package com.mikelau.croperino;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class BitmapManager {

    private static final String TAG = "BitmapManager";

    private enum State {CANCEL, ALLOW}

    private static class ThreadStatus {

        public State mState = State.ALLOW;
        public BitmapFactory.Options mOptions;

        @Override
        public String toString() {
            String s;
            if (mState == State.CANCEL) {
                s = "Cancel";
            } else if (mState == State.ALLOW) {
                s = "Allow";
            } else {
                s = "?";
            }
            s = "thread state = " + s + ", options = " + mOptions;
            return s;
        }
    }

    public static class ThreadSet implements Iterable<Thread> {

        private final WeakHashMap<Thread, Object> mWeakCollection =
                new WeakHashMap<Thread, Object>();

        public void add(Thread t) {

            mWeakCollection.put(t, null);
        }

        public void remove(Thread t) {

            mWeakCollection.remove(t);
        }

        public Iterator<Thread> iterator() {

            return mWeakCollection.keySet().iterator();
        }
    }

    private final WeakHashMap<Thread, ThreadStatus> mThreadStatus =
            new WeakHashMap<Thread, ThreadStatus>();

    private static BitmapManager sManager = null;

    private BitmapManager() {

    }

    private synchronized ThreadStatus getOrCreateThreadStatus(Thread t) {

        ThreadStatus status = mThreadStatus.get(t);
        if (status == null) {
            status = new ThreadStatus();
            mThreadStatus.put(t, status);
        }
        return status;
    }

    private synchronized void setDecodingOptions(Thread t, BitmapFactory.Options options) {
        getOrCreateThreadStatus(t).mOptions = options;
    }

    synchronized BitmapFactory.Options getDecodingOptions(Thread t) {

        ThreadStatus status = mThreadStatus.get(t);
        return status != null ? status.mOptions : null;
    }

    synchronized void removeDecodingOptions(Thread t) {

        ThreadStatus status = mThreadStatus.get(t);
        status.mOptions = null;
    }

    public synchronized void allowThreadDecoding(ThreadSet threads) {
        for (Thread t : threads) {
            allowThreadDecoding(t);
        }
    }

    public synchronized void cancelThreadDecoding(ThreadSet threads) {

        for (Thread t : threads) {
            cancelThreadDecoding(t);
        }
    }

    public synchronized boolean canThreadDecoding(Thread t) {
        ThreadStatus status = mThreadStatus.get(t);
        if (status == null) {
            // allow decoding by default
            return true;
        }

        return (status.mState != State.CANCEL);
    }

    public synchronized void allowThreadDecoding(Thread t) {

        getOrCreateThreadStatus(t).mState = State.ALLOW;
    }

    public synchronized void cancelThreadDecoding(Thread t) {

        ThreadStatus status = getOrCreateThreadStatus(t);
        status.mState = State.CANCEL;
        if (status.mOptions != null) {
            status.mOptions.requestCancelDecode();
        }

        notifyAll();
    }

    public synchronized void dump() {

        Iterator<Map.Entry<Thread, ThreadStatus>> i = mThreadStatus.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<Thread, ThreadStatus> entry = i.next();
            Log.v(TAG, "[Dump] Thread " + entry.getKey() + " ("
                    + entry.getKey().getId()
                    + ")'s status is " + entry.getValue());
        }
    }

    public static synchronized BitmapManager instance() {

        if (sManager == null) {
            sManager = new BitmapManager();
        }
        return sManager;
    }

    public Bitmap decodeFileDescriptor(FileDescriptor fd, BitmapFactory.Options options) {
        if (options.mCancel) {
            return null;
        }

        Thread thread = Thread.currentThread();
        if (!canThreadDecoding(thread)) {
            return null;
        }

        setDecodingOptions(thread, options);
        Bitmap b = BitmapFactory.decodeFileDescriptor(fd, null, options);

        removeDecodingOptions(thread);
        return b;
    }
}
