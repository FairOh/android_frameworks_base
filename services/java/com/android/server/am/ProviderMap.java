/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.server.am;

import android.content.ComponentName;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Keeps track of content providers by authority (name) and class. It separates the mapping by
 * user and ones that are not user-specific (system providers).
 */
public class ProviderMap {

    private static final String TAG = "ProviderMap";

    private static final boolean DBG = false;

    private final ActivityManagerService mAm;

    private final HashMap<String, ContentProviderRecord> mSingletonByName
            = new HashMap<String, ContentProviderRecord>();
    private final HashMap<ComponentName, ContentProviderRecord> mSingletonByClass
            = new HashMap<ComponentName, ContentProviderRecord>();

    private final SparseArray<HashMap<String, ContentProviderRecord>> mProvidersByNamePerUser
            = new SparseArray<HashMap<String, ContentProviderRecord>>();
    private final SparseArray<HashMap<ComponentName, ContentProviderRecord>> mProvidersByClassPerUser
            = new SparseArray<HashMap<ComponentName, ContentProviderRecord>>();

    ProviderMap(ActivityManagerService am) {
        mAm = am;
    }

    ContentProviderRecord getProviderByName(String name) {
        return getProviderByName(name, -1);
    }

    ContentProviderRecord getProviderByName(String name, int userId) {
        if (DBG) {
            Slog.i(TAG, "getProviderByName: " + name + " , callingUid = " + Binder.getCallingUid());
        }
        // Try to find it in the global list
        ContentProviderRecord record = mSingletonByName.get(name);
        if (record != null) {
            return record;
        }

        // Check the current user's list
        return getProvidersByName(userId).get(name);
    }

    ContentProviderRecord getProviderByClass(ComponentName name) {
        return getProviderByClass(name, -1);
    }

    ContentProviderRecord getProviderByClass(ComponentName name, int userId) {
        if (DBG) {
            Slog.i(TAG, "getProviderByClass: " + name + ", callingUid = " + Binder.getCallingUid());
        }
        // Try to find it in the global list
        ContentProviderRecord record = mSingletonByClass.get(name);
        if (record != null) {
            return record;
        }

        // Check the current user's list
        return getProvidersByClass(userId).get(name);
    }

    void putProviderByName(String name, ContentProviderRecord record) {
        if (DBG) {
            Slog.i(TAG, "putProviderByName: " + name + " , callingUid = " + Binder.getCallingUid()
                + ", record uid = " + record.appInfo.uid);
        }
        if (record.singleton) {
            mSingletonByName.put(name, record);
        } else {
            final int userId = UserHandle.getUserId(record.appInfo.uid);
            getProvidersByName(userId).put(name, record);
        }
    }

    void putProviderByClass(ComponentName name, ContentProviderRecord record) {
        if (DBG) {
            Slog.i(TAG, "putProviderByClass: " + name + " , callingUid = " + Binder.getCallingUid()
                + ", record uid = " + record.appInfo.uid);
        }
        if (record.singleton) {
            mSingletonByClass.put(name, record);
        } else {
            final int userId = UserHandle.getUserId(record.appInfo.uid);
            getProvidersByClass(userId).put(name, record);
        }
    }

    void removeProviderByName(String name, int userId) {
        ContentProviderRecord removedRecord = null;

        if (mSingletonByName.containsKey(name)) {
            if (DBG)
                Slog.i(TAG, "Removing from globalByName name=" + name);
            removedRecord = mSingletonByName.remove(name);
        } else {
            if (userId < 0) throw new IllegalArgumentException("Bad user " + userId);
            if (DBG)
                Slog.i(TAG,
                        "Removing from providersByName name=" + name + " user=" + userId);
            HashMap<String, ContentProviderRecord> map = getProvidersByName(userId);
            // map returned by getProvidersByName wouldn't be null
            removedRecord = map.remove(name);
            if (map.size() == 0) {
                mProvidersByNamePerUser.remove(userId);
            }
        }

        // Clean up references to external process.
        if (removedRecord != null) {
            removedRecord.removeExternalProcessHandles();
        }
    }

    void removeProviderByClass(ComponentName name, int userId) {
        ContentProviderRecord removedRecord = null;

        if (mSingletonByClass.containsKey(name)) {
            if (DBG)
                Slog.i(TAG, "Removing from globalByClass name=" + name);
            removedRecord = mSingletonByClass.remove(name);
        } else {
            if (userId < 0) throw new IllegalArgumentException("Bad user " + userId);
            if (DBG)
                Slog.i(TAG,
                        "Removing from providersByClass name=" + name + " user=" + userId);
            HashMap<ComponentName, ContentProviderRecord> map = getProvidersByClass(userId);
            // map returned by getProvidersByClass wouldn't be null
            removedRecord = map.remove(name);
            if (map.size() == 0) {
                mProvidersByClassPerUser.remove(userId);
            }
        }

        // Clean up references to external process.
        if (removedRecord != null) {
            removedRecord.removeExternalProcessHandles();
        }
    }

    private HashMap<String, ContentProviderRecord> getProvidersByName(int userId) {
        if (userId < 0) throw new IllegalArgumentException("Bad user " + userId);
        final HashMap<String, ContentProviderRecord> map = mProvidersByNamePerUser.get(userId);
        if (map == null) {
            HashMap<String, ContentProviderRecord> newMap = new HashMap<String, ContentProviderRecord>();
            mProvidersByNamePerUser.put(userId, newMap);
            return newMap;
        } else {
            return map;
        }
    }

    HashMap<ComponentName, ContentProviderRecord> getProvidersByClass(int userId) {
        if (userId < 0) throw new IllegalArgumentException("Bad user " + userId);
        final HashMap<ComponentName, ContentProviderRecord> map
                = mProvidersByClassPerUser.get(userId);
        if (map == null) {
            HashMap<ComponentName, ContentProviderRecord> newMap
                    = new HashMap<ComponentName, ContentProviderRecord>();
            mProvidersByClassPerUser.put(userId, newMap);
            return newMap;
        } else {
            return map;
        }
    }

    private boolean collectForceStopProvidersLocked(String name, int appId,
            boolean doit, boolean evenPersistent, int userId,
            HashMap<ComponentName, ContentProviderRecord> providers,
            ArrayList<ContentProviderRecord> result) {
        boolean didSomething = false;
        for (ContentProviderRecord provider : providers.values()) {
            if ((name == null || provider.info.packageName.equals(name))
                    && (provider.proc == null || evenPersistent || !provider.proc.persistent)) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                result.add(provider);
            }
        }
        return didSomething;
    }

    boolean collectForceStopProviders(String name, int appId,
            boolean doit, boolean evenPersistent, int userId,
            ArrayList<ContentProviderRecord> result) {
        boolean didSomething = collectForceStopProvidersLocked(name, appId, doit,
                evenPersistent, userId, mSingletonByClass, result);
        if (!doit && didSomething) {
            return true;
        }
        if (userId == UserHandle.USER_ALL) {
            for (int i=0; i<mProvidersByClassPerUser.size(); i++) {
                if (collectForceStopProvidersLocked(name, appId, doit, evenPersistent,
                        userId, mProvidersByClassPerUser.valueAt(i), result)) {
                    if (!doit) {
                        return true;
                    }
                    didSomething = true;
                }
            }
        } else {
            HashMap<ComponentName, ContentProviderRecord> items
                    = getProvidersByClass(userId);
            if (items != null) {
                didSomething |= collectForceStopProvidersLocked(name, appId, doit,
                        evenPersistent, userId, items, result);
            }
        }
        return didSomething;
    }

    private void dumpProvidersByClassLocked(PrintWriter pw, boolean dumpAll,
            HashMap<ComponentName, ContentProviderRecord> map) {
        Iterator<Map.Entry<ComponentName, ContentProviderRecord>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ComponentName, ContentProviderRecord> e = it.next();
            ContentProviderRecord r = e.getValue();
            pw.print("  * ");
            pw.println(r);
            r.dump(pw, "    ", dumpAll);
        }
    }

    private void dumpProvidersByNameLocked(PrintWriter pw,
            HashMap<String, ContentProviderRecord> map) {
        Iterator<Map.Entry<String, ContentProviderRecord>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ContentProviderRecord> e = it.next();
            ContentProviderRecord r = e.getValue();
            pw.print("  ");
            pw.print(e.getKey());
            pw.print(": ");
            pw.println(r.toShortString());
        }
    }

    void dumpProvidersLocked(PrintWriter pw, boolean dumpAll) {
        if (mSingletonByClass.size() > 0) {
            pw.println("  Published single-user content providers (by class):");
            dumpProvidersByClassLocked(pw, dumpAll, mSingletonByClass);
        }

        pw.println("");
        for (int i = 0; i < mProvidersByClassPerUser.size(); i++) {
            HashMap<ComponentName, ContentProviderRecord> map = mProvidersByClassPerUser.valueAt(i);
            pw.println("");
            pw.println("  Published user " + mProvidersByClassPerUser.keyAt(i)
                    + " content providers (by class):");
            dumpProvidersByClassLocked(pw, dumpAll, map);
        }

        if (dumpAll) {
            pw.println("");
            pw.println("  Single-user authority to provider mappings:");
            dumpProvidersByNameLocked(pw, mSingletonByName);

            for (int i = 0; i < mProvidersByNamePerUser.size(); i++) {
                pw.println("");
                pw.println("  User " + mProvidersByNamePerUser.keyAt(i)
                        + " authority to provider mappings:");
                dumpProvidersByNameLocked(pw, mProvidersByNamePerUser.valueAt(i));
            }
        }
    }

    protected boolean dumpProvider(FileDescriptor fd, PrintWriter pw, String name, String[] args,
            int opti, boolean dumpAll) {
        ArrayList<ContentProviderRecord> allProviders = new ArrayList<ContentProviderRecord>();
        ArrayList<ContentProviderRecord> providers = new ArrayList<ContentProviderRecord>();

        synchronized (mAm) {
            allProviders.addAll(mSingletonByClass.values());
            for (int i=0; i<mProvidersByClassPerUser.size(); i++) {
                allProviders.addAll(mProvidersByClassPerUser.valueAt(i).values());
            }

            if ("all".equals(name)) {
                providers.addAll(allProviders);
            } else {
                ComponentName componentName = name != null
                        ? ComponentName.unflattenFromString(name) : null;
                int objectId = 0;
                if (componentName == null) {
                    // Not a '/' separated full component name; maybe an object ID?
                    try {
                        objectId = Integer.parseInt(name, 16);
                        name = null;
                        componentName = null;
                    } catch (RuntimeException e) {
                    }
                }

                for (int i=0; i<allProviders.size(); i++) {
                    ContentProviderRecord r1 = allProviders.get(i);
                    if (componentName != null) {
                        if (r1.name.equals(componentName)) {
                            providers.add(r1);
                        }
                    } else if (name != null) {
                        if (r1.name.flattenToString().contains(name)) {
                            providers.add(r1);
                        }
                    } else if (System.identityHashCode(r1) == objectId) {
                        providers.add(r1);
                    }
                }
            }
        }

        if (providers.size() <= 0) {
            return false;
        }

        boolean needSep = false;
        for (int i=0; i<providers.size(); i++) {
            if (needSep) {
                pw.println();
            }
            needSep = true;
            dumpProvider("", fd, pw, providers.get(i), args, dumpAll);
        }
        return true;
    }

    /**
     * Invokes IApplicationThread.dumpProvider() on the thread of the specified provider if
     * there is a thread associated with the provider.
     */
    private void dumpProvider(String prefix, FileDescriptor fd, PrintWriter pw,
            final ContentProviderRecord r, String[] args, boolean dumpAll) {
        String innerPrefix = prefix + "  ";
        synchronized (mAm) {
            pw.print(prefix); pw.print("PROVIDER ");
                    pw.print(r);
                    pw.print(" pid=");
                    if (r.proc != null) pw.println(r.proc.pid);
                    else pw.println("(not running)");
            if (dumpAll) {
                r.dump(pw, innerPrefix, true);
            }
        }
        if (r.proc != null && r.proc.thread != null) {
            pw.println("    Client:");
            pw.flush();
            try {
                TransferPipe tp = new TransferPipe();
                try {
                    r.proc.thread.dumpProvider(
                            tp.getWriteFd().getFileDescriptor(), r.provider.asBinder(), args);
                    tp.setBufferPrefix("      ");
                    // Short timeout, since blocking here can
                    // deadlock with the application.
                    tp.go(fd, 2000);
                } finally {
                    tp.kill();
                }
            } catch (IOException ex) {
                pw.println("      Failure while dumping the provider: " + ex);
            } catch (RemoteException ex) {
                pw.println("      Got a RemoteException while dumping the service");
            }
        }
    }
}
