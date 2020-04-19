package Realizations;

import Models.ElementOfMap;
import Models.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CoarseHashMap<K,V> extends HashMap<K,V>
{
    final Lock lock;

    public CoarseHashMap(int capacity)
    {
        super(capacity);
        lock = new ReentrantLock();
    }

    @Override
    public void acquire(K key)
    {
        lock.lock();
    }

    @Override
    public void release(K key)
    {
        lock.unlock();
    }

    @Override
    public void resize()
    {
        int lastCapacity = map.length;

        lock.lock();
        try
        {
            if (lastCapacity != map.length)
            {
                return;
            }
            int newCapacity = lastCapacity * 2;
            List<ElementOfMap<K, V>>[] lastMap = map;
            map = (List<ElementOfMap<K,V>>[]) new List[newCapacity];
            for (int i = 0; i < newCapacity; i++)
            {
                map[i] = new ArrayList<ElementOfMap<K,V>>();
            }
            for (List<ElementOfMap<K,V>> bucket : lastMap)
            {
                for (ElementOfMap<K,V> elementOfMap : bucket)
                {
                    int hashcode = elementOfMap.key.hashCode();
                    int index = (hashcode & 0x7FFFFFFF) % map.length;
                    map[index].add(elementOfMap);
                }
            }
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public boolean policy()
    {
        return size / map.length > 4;
    }
}
