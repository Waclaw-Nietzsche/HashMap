package Models;

import Interfaces.Map;

import java.util.ArrayList;
import java.util.List;

public abstract class HashMap<K,V> implements Map<K,V>
{
    protected List<ElementOfMap<K,V>>[] table;
    protected int size;

    // Замыкание
    public abstract void acquire(K key);
    // Размыкание
    public abstract void release(K key);
    // Изменение размера таблицы (удвоение)
    public abstract void resize();
    // Определяет - необходимо ли увеличение таблицы
    public abstract boolean policy();

    public HashMap(int capacity)
    {
        size = 0;
        table = (List<ElementOfMap<K,V>>[]) new List[capacity];
        for (int i = 0; i < capacity; i++)
        {
            table[i] = new ArrayList<ElementOfMap<K, V>>();
        }
    }

    // Добавление элемента
    @Override
    public V put(K key, V value)
    {
        acquire(key);
        try
        {
            if (value == null)
            {
                throw new NullPointerException();
            }
            else
            {
                int hashcode = key.hashCode();
                // Позволяет избежать проблем с отрицательными числами и выходом за предел (с формуов)
                int index = (hashcode & 0x7FFFFFFF) % table.length;
                // Это foreach
                for (ElementOfMap<K,V> elementOfMap : table[index])
                {
                    if (elementOfMap.hashcode == hashcode && elementOfMap.key.equals(key))
                    {
                        V lastValue = elementOfMap.value;
                        elementOfMap.value = value;

                        return lastValue;
                    }
                }
                table[index].add(new ElementOfMap<K,V>(hashcode, key, value));
                size++;
            }
        }
        finally
        {
            release(key);
        }

        if (policy())
        {
            resize();
        }

        return null;
    }

    // Получение элемента
    @Override
    public V get(K key)
    {
        acquire(key);
        try
        {
            int hashcode = key.hashCode();
            int index = (hashcode & 0x7FFFFFFF) % table.length;
            for (ElementOfMap<K,V> elementOfMap : table[index])
            {
                if (elementOfMap.hashcode == hashcode && elementOfMap.key.equals(key))
                {
                    return elementOfMap.value;
                }
            }
        }
        finally
        {
            release(key);
        }

        return null;
    }

    // Удаление элемента
    @Override
    public V remove(K key)
    {
        acquire(key);
        try
        {
            int hashcode = key.hashCode();
            int index = (hashcode & 0x7FFFFFFF) % table.length;
            for (ElementOfMap<K,V> elementOfMap : table[index])
            {
                if (elementOfMap.hashcode == hashcode && elementOfMap.key.equals(key))
                {
                    V result = elementOfMap.value;
                    table[index].remove(elementOfMap);
                    size--;

                    return result;
                }
            }
        }
        finally
        {
            release(key);
        }

        return null;
    }

    // Проверка наличия элемента
    @Override
    public boolean containsKey(K key)
    {
        acquire(key);
        try
        {
            int hashcode = key.hashCode();
            int index = (hashcode & 0x7FFFFFFF) % table.length;
            for (ElementOfMap<K,V> elementOfMap : table[index])
            {
                if (elementOfMap.hashcode == hashcode && elementOfMap.key.equals(key))
                {
                    return true;
                }
            }
        }
        finally
        {
            release(key);
        }

        return false;
    }
}
