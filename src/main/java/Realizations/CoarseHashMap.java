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
        int lastCapacity = table.length;

        lock.lock();
        try
        {
            if (lastCapacity != table.length)
            {
                return;
            }
            int newCapacity = lastCapacity * 2;
            List<ElementOfMap<K, V>>[] oldTable = table;
            table = (List<ElementOfMap<K,V>>[]) new List[newCapacity];
            for (int i = 0; i < newCapacity; i++)
            {
                table[i] = new ArrayList<ElementOfMap<K,V>>();
            }
            for (List<ElementOfMap<K,V>> bucket : oldTable)
            {
                for (ElementOfMap<K,V> elementOfMap : bucket)
                {
                    int hashcode = elementOfMap.key.hashCode();
                    int index = (hashcode & 0x7FFFFFFF) % table.length;
                    table[index].add(elementOfMap);
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
        return size / table.length > 4;
    }
}
