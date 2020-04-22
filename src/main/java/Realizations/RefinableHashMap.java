package Realizations;

import Models.ElementOfMap;
import Models.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

public class RefinableHashMap<K, V> extends HashMap<K, V> {
    AtomicMarkableReference<Thread> owner;
    volatile ReentrantLock[] locks;

    public RefinableHashMap(int capacity) {
        super(capacity);
        locks = new ReentrantLock[capacity];
        for (int j = 0; j < capacity; j++) {
            locks[j] = new ReentrantLock();
        }
        owner = new AtomicMarkableReference<Thread>(null, false);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    protected void quiesce() {
        for (ReentrantLock lock : locks) {
            while (lock.isLocked()) {
            }
        }
    }

    private void initializeFrom(List<ElementOfMap<K, V>>[] lastMap) {
        for (List<ElementOfMap<K, V>> map0 : lastMap) {
            for (ElementOfMap<K, V> elementOfMap : map0) {
                int index = (elementOfMap.key.hashCode() & 0x7FFFFFFF) % map.length;
                map[index].add(elementOfMap);
            }
        }
    }

    @Override
    public void acquire(K key) {
        boolean[] mark = {true};
        Thread me = Thread.currentThread();
        Thread who;
        while (true) {
            do {
                who = owner.get(mark);
            }
            while (mark[0] && who != me);
            ReentrantLock[] lastLocks = this.locks;
            int index = (key.hashCode() & 0x7FFFFFFF) % map.length;
            ReentrantLock lastLock = lastLocks[index];
            lastLock.lock();
            who = owner.get(mark);
            if ((!mark[0] || who == me) && this.locks == lastLocks) {
                return;
            } else {
                lastLock.unlock();
            }
        }
    }

    @Override
    public void release(K key) {
        int index = (key.hashCode() & 0x7FFFFFFF) % map.length;
        locks[index].unlock();
    }

    @Override
    public void resize() {
        int lastCapacity = map.length;
        int newCapacity = lastCapacity * 2;
        Thread me = Thread.currentThread();
        if (owner.compareAndSet(null, me, false, true)) {
            try {
                if (map.length != lastCapacity) {
                    return;
                }
                quiesce();
                List<ElementOfMap<K, V>>[] lastMap = map;
                map = (List<ElementOfMap<K, V>>[]) new List[newCapacity];
                for (int i = 0; i < newCapacity; i++) {
                    map[i] = new ArrayList<ElementOfMap<K, V>>();
                }
                locks = new ReentrantLock[newCapacity];
                for (int j = 0; j < locks.length; j++) {
                    locks[j] = new ReentrantLock();
                }
                initializeFrom(lastMap);
            } finally {
                owner.set(null, false);       // restore prior state
            }
        }
    }

    @Override
    public boolean policy() {
        return size / map.length > 4;
    }
}
