package Models;

public class ElementOfMap<K,V>
{
    int hashcode;
    K key;
    V value;

    protected ElementOfMap(int hashcode, K key, V value)
    {
        this.hashcode = hashcode;
        this.key = key;
        this.value = value;
    }
}
