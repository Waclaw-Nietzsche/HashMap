package Interfaces;

public interface Map<K,V>
{
    V put(K key, V value);

    V get(K key);

    V remove(K key);

    boolean containsKey(K key);
}
