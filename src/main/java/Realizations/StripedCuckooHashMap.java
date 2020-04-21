package Realizations;

import Models.ElementOfMap;
import Models.PhasedCuckooHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StripedCuckooHashMap<K, V> extends PhasedCuckooHashMap<K, V> {
    final ReentrantLock[][] lock;

    public StripedCuckooHashMap(int capacity) {
        super(capacity);
        lock = new ReentrantLock[2][capacity];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < capacity; j++) {
                lock[i][j] = new ReentrantLock();
            }
        }
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void acquire(K key) {
        lock[0][hashFunctionOne(key) % lock[0].length].lock();
        lock[1][hashFunctionTwo(key) % lock[1].length].lock();
    }

    @Override
    public void release(K key) {
        lock[0][hashFunctionOne(key) % lock[0].length].unlock();
        lock[1][hashFunctionTwo(key) % lock[1].length].unlock();
    }

    @Override
    public void resize() {
        int lastCapacity = capacity;
        for (Lock aLock : lock[0]) {
            aLock.lock();
        }
        try {
            if (capacity != lastCapacity) {
                return;
            }
            List<ElementOfMap<K, V>>[][] lastMap = map;
            capacity = 2 * capacity;
            map = (List<ElementOfMap<K, V>>[][]) new List[2][capacity];
            for (List<ElementOfMap<K, V>>[] row : map) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = new ArrayList<ElementOfMap<K, V>>(PROBE_SIZE);
                }
            }
            for (List<ElementOfMap<K, V>>[] row : lastMap) {
                for (List<ElementOfMap<K, V>> mp : row) {
                    for (ElementOfMap<K, V> elementOfMap : mp) {
                        put(elementOfMap.key, elementOfMap.value);
                    }
                }
            }
        } finally {
            for (Lock aLock : lock[0]) {
                aLock.unlock();
            }
        }
    }
}
