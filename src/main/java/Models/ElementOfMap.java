package Models;

public class ElementOfMap<K,V>
{
    public int hashcode;
    public K key;
    public V value;

    public ElementOfMap(int hashcode, K key, V value)
    {
        this.hashcode = hashcode;
        this.key = key;
        this.value = value;
    }
}
