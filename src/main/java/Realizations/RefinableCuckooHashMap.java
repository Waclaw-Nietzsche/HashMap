package Realizations;

import Models.ElementOfMap;
import Models.PhasedCuckooHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

public class RefinableCuckooHashMap<K,V> extends PhasedCuckooHashMap<K,V>
{
    volatile ReentrantLock[][] locks;

    AtomicMarkableReference<Thread> owner;

    public RefinableCuckooHashMap(int size)
    {
        super(size);
        locks = new ReentrantLock[2][size];
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < size; j++)
            {
                locks[i][j] = new ReentrantLock();
            }
        }
        owner = new AtomicMarkableReference<Thread>(null, false);
    }

    protected void quiesce()
    {
        for (ReentrantLock lock : locks[0])
        {
            while (lock.isLocked()) {}
        }
    }

    @Override
    public void acquire(K key)
    {
        boolean[] mark = {true};
        Thread me = Thread.currentThread();
        Thread who;
        while (true)
        {
            do
            {
                who = owner.get(mark);
            }
            while (mark[0] && who != me);

            ReentrantLock[][] lastLocks = this.locks;
            ReentrantLock lastLock0 = lastLocks[0][hashFunctionOne(key) % lastLocks[0].length];
            ReentrantLock lastLock1 = lastLocks[1][hashFunctionTwo(key) % lastLocks[1].length];
            lastLock0.lock();
            lastLock1.lock();
            who = owner.get(mark);
            if ((!mark[0] || who == me) && this.locks == lastLocks)
            {
                return;
            }
            else
            {
                lastLock0.unlock();
                lastLock1.unlock();
            }
        }
    }

    @Override
    public void release(K key)
    {
        locks[0][hashFunctionOne(key) % locks[0].length].unlock();
        locks[1][hashFunctionTwo(key) % locks[1].length].unlock();
    }

    @Override
    public void resize()
    {
        int lastCapacity = capacity;
        Thread me = Thread.currentThread();
        if (owner.compareAndSet(null, me, false, true))
        {
            try
            {
                if (capacity != lastCapacity)
                {
                    return;
                }
                quiesce();

                capacity = capacity * 2;
                List<ElementOfMap<K,V>>[][] oldMap = map;
                map = (List<ElementOfMap<K,V>>[][]) new List[2][capacity];
                locks = new ReentrantLock[2][capacity];
                for (int i = 0; i < 2; i++)
                {
                    for (int j = 0; j < capacity; j++)
                    {
                        locks[i][j] = new ReentrantLock();
                    }
                }
                for (List<ElementOfMap<K,V>>[] row : map)
                {
                    for (int i = 0; i < row.length; i++)
                    {
                        row[i] = new ArrayList<ElementOfMap<K,V>>(PROBE_SIZE);
                    }
                }
                for (List<ElementOfMap<K,V>>[] row : oldMap)
                {
                    for (List<ElementOfMap<K,V>> map0 : row)
                    {
                        for (ElementOfMap<K,V> elementOfMap : map0)
                        {
                            put(elementOfMap.key, elementOfMap.value);
                        }
                    }
                }
            }
            finally
            {
                owner.set(null, false);
            }
        }
    }
}
