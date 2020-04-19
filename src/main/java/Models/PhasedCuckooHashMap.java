package Models;

import Interfaces.Map;
import Models.ElementOfMap;
import Models.HashMap;

import java.util.ArrayList;
import java.util.List;

public abstract class PhasedCuckooHashMap<K,V> implements Map<K,V>
{
    // Если достигнем предела, расширим таблицу
    protected static final int LIMIT = 8;
    protected static final int PROBE_SIZE = 8;
    protected static final int THRESHOLD = PROBE_SIZE / 2;

    protected volatile int capacity;
    protected volatile  List<ElementOfMap<K,V>>[][] map;

    // Замыкание
    public abstract void acquire(K key);
    // Размыкание
    public abstract void release(K key);
    // Изменение размера таблицы (удвоение)
    public abstract void resize();

    public PhasedCuckooHashMap(int size)
    {
        capacity = size;
        map = (List<ElementOfMap<K,V>>[][]) new ArrayList[2][capacity];
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < capacity; j++)
            {
                map[i][j] = new ArrayList<ElementOfMap<K,V>>(PROBE_SIZE);
            }
        }
    }

    final public int hashFunctionOne(K key)
    {
        return (key.hashCode() & 0x7FFFFFFF) % capacity;
    }

    final public int hashFunctionTwo(K key)
    {
        return (int) Math.abs((key.hashCode() * 0x5DEECE66DL + 0xBL) & ((1L << 31) - 1));
    }

    protected boolean relocate(int i, int hi)
    {
        int hj = 0;
        int j = 1 - i;
        for (int round = 0; round < LIMIT; round++)
        {
            List<ElementOfMap<K,V>> iMap = map[i][hi];
            ElementOfMap<K,V> entry = iMap.get(0);
            K key = entry.key;
            switch (i)
            {
                case 0:
                    hj = hashFunctionOne(entry.key) % capacity;
                    break;
                case 1:
                    hj = hashFunctionTwo(entry.key) % capacity;
                    break;
            }
            acquire(key);
            List<ElementOfMap<K,V>> jMap = map[j][hj];
            try
            {
                if (iMap.remove(entry))
                {
                    if (jMap.size() < THRESHOLD)
                    {
                        jMap.add(entry);
                        return true;
                    }
                    else if (jMap.size() < PROBE_SIZE)
                    {
                        jMap.add(entry);
                        i = 1 - i;
                        hi = hj;
                        j = 1 - j;
                    }
                    else
                    {
                        iMap.add(entry);
                        return false;
                    }
                }
                else if (iMap.size() >= THRESHOLD)
                {
                    continue;
                }
                else
                {
                    return true;
                }
            }
            finally
            {
                release(key);
            }
        }
        return false;
    }

    @Override
    public V put(K key, V value)
    {
        acquire(key);
        int hashOne = hashFunctionOne(key) % capacity;
        int hashTwo = hashFunctionTwo(key) % capacity;
        int i = -1;
        int h = -1;
        boolean mustResize = false;
        try
        {
            List<ElementOfMap<K,V>> map0 = map[0][hashOne];
            for(ElementOfMap<K,V> elementOfMap: map0)
            {
                if (elementOfMap.key.equals(key))
                {
                    V result = elementOfMap.value;
                    elementOfMap.value = value;
                    return result;
                }
            }
            List<ElementOfMap<K,V>> map1 = map[1][hashTwo];
            for(ElementOfMap<K,V> elementOfMap: map1)
            {
                if (elementOfMap.key.equals(key))
                {
                    V result = elementOfMap.value;
                    elementOfMap.value = value;
                    return result;
                }
            }
            ElementOfMap<K,V> entry = new ElementOfMap<K, V>(key.hashCode(), key, value);
            if (map0.size() < THRESHOLD)
            {
                map0.add(entry);
                return null;
            }
            else if (map1.size() < THRESHOLD)
            {
                map1.add(entry);
                return null;
            }
            else if (map0.size() < PROBE_SIZE)
            {
                map0.add(entry);
                i = 0; h = hashOne;
            }
            else if (map1.size() < PROBE_SIZE)
            {
                map1.add(entry);
                i = 1; h = hashTwo;
            }
            else
            {
                mustResize = true;
            }
        }
        finally
        {
            release(key);
        }
        if (mustResize)
        {
            resize();
            put(key, value);
        }
        else if (!relocate(i, h))
        {
            resize();
        }
        return null;  // x must have been present
    }

    @Override
    public V get(K key)
    {
        acquire(key);
        try
        {
            List<ElementOfMap<K,V>> map0 = map[0][hashFunctionOne(key) % capacity];
            for(ElementOfMap<K,V> elementOfMap: map0)
            {
                if (elementOfMap.key.equals(key))
                {
                    V result = elementOfMap.value;
                    return result;
                }
            }
            List<ElementOfMap<K,V>> map1 = map[1][hashFunctionTwo(key) % capacity];
            for(ElementOfMap<K,V> elementOfMap: map1)
            {
                if (elementOfMap.key.equals(key))
                {
                    V result = elementOfMap.value;
                    return result;
                }
            }
            return null;
        }
        finally
        {
            release(key);
        }
    }

    @Override
    public V remove(K key)
    {
        acquire(key);
        try
        {
            List<ElementOfMap<K,V>> map0 = map[0][hashFunctionOne(key) % capacity];
            for(ElementOfMap<K,V> elementOfMap: map0)
            {
                if (elementOfMap.key.equals(key))
                {
                    V result = elementOfMap.value;
                    map0.remove(elementOfMap);
                    return result;
                }
            }
            List<ElementOfMap<K,V>> map1 = map[1][hashFunctionTwo(key) % capacity];
            for(ElementOfMap<K,V> elementOfMap: map1)
            {
                if (elementOfMap.key.equals(key))
                {
                    V result = elementOfMap.value;
                    map1.remove(elementOfMap);
                    return result;
                }
            }
            return null;
        }
        finally
        {
            release(key);
        }
    }

    @Override
    public boolean containsKey(K key)
    {
        acquire(key);
        try
        {
            List<ElementOfMap<K,V>> map0 = map[0][hashFunctionOne(key) % capacity];
            for (ElementOfMap<K,V> elementOfMap : map0)
            {
                if (elementOfMap.key.equals(key))
                {
                    return true;
                }
            }
            List<ElementOfMap<K,V>> map1 = map[1][hashFunctionTwo(key) % capacity];
            for(ElementOfMap<K,V> elementOfMap: map1)
            {
                if (elementOfMap.key.equals(key))
                {
                    return true;
                }
            }
            return false;
        }
        finally
        {
            release(key);
        }
    }
}
