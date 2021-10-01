package bestsss.map;

import java.io.IOException;

/*Written by S. Simeonoff and released to the public domain, as explained at http://creativecommons.org/publicdomain/zero/1.0/
 * 2018
 */

import java.util.*;

/**
* Fast and compact hash table. Open address/pow2 capacity, linear probe with fill factor of 1 for lower sizes and 66%-75% upwards.
* The impl. does not use extra Node objects per element, nor stores hashCodes.  
* Hence, the hash table performs best with key whose hash codes are well distributed and easy to calculate hashes. (String/Integer/Long, anything or cached hashCode)
* Expected memory consumption for larger table is ~2.5 reference size per key/value entry (that's ~10.2bytes with compressed pointers on <32GB heaps)
* 
* In most cases CompactHashMap is a drop in replacement of HashMap with better memory footprint and possibly better performance under low collision scenarios.
* Please note: there is no support for fast fail iterators and ConcurrentModificatoinException. There is no support for null keys and values, either. Maximum held entries
* by HashMap is always higher. CompactHashMap can hold up to 1<<29 items, yet filling up would degrade performance greatly.
* 
* Code should be released under public domain cc0 - https://creativecommons.org/publicdomain/zero/1.0/ 
*
* @author Stanimir Simeonoff
*/
public class CompactHashMap<K, V> implements Map<K, V>, Cloneable, java.io.Serializable{
    private static final long serialVersionUID = 1L;

    private static final Object[] EMPTY = {};
    private static final int MAXIMUM_CAPACITY = 1 << 29;

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    private static int smear(int hashCode) {//"murmur3" smear; https://en.wikipedia.org/wiki/MurmurHash (see c1 and c2)
        return C2 * Integer.rotateLeft(hashCode * C1, 15);
    }
    private static int hash(Object key, int len) {
        return smear(key.hashCode()) & (len - 1) & ~1;//mask the for the key (always 0th bit has to be zero) {len - 2, should be ok}
    }
    //only 2 fields, no 'caching' for entrySet/keySet/etc.; the benefits are minuscule and better be created/dropped on each call, similar to iterators 
    transient int size = 0;
    transient Object[] table = EMPTY;//key at even pos, value at odd, no nulls
    
    private static boolean needGrow(int len, int size){        
        if (len <= 32)//less than 16 elements 
            return len >> 1 < size;
        if (len <= 128)//less than 64 (~40) elements 
            return len  < size * 3; //length is capacity, so .66 fill factor mid size; trade off for optimal performance
        
        return len  < size + size + (size >> 1); //length is capacity, so .75 fill factor for larger ones; large sizes, compact it; save memory
    }
    
    public V put(K key, V value) {
        final Object k = Objects.requireNonNull(key);//null checks, we do not support null
        Objects.requireNonNull(value);
        
        for (;;) {
            final Object[] tab = table;
            final int len = tab.length;
            if (len == 0){//special case for the 1st put -- the zero length is effectively a lattice and --5nanos(!!!) shave off due to lattice constraints!
                this.table = new Object[]{key, value};//start extra small
                size = 1;
                return null;
            }     
            
            int i = hash(k, len);
            final int start=i;
            for (Object item; (item = tab[i]) != null; ) {
                if (k==item || k.equals(item)) {
                    @SuppressWarnings("unchecked")
                    V result = (V) tab[i + 1];
                    if (result != value)
                      tab[i + 1] = value;
                    return result;
                }
                if ((i = nextKeyIndex(i, len)) == start){//loop the loop
                    break;
                }
            }

            final int s = size + 1;
            if (needGrow(len, s) && resize(len))
                continue;//resized, try again

            tab[i] = k;
            tab[i + 1] = value;
            size = s;
            return null;
        }               
    }
    
    
    public V remove(Object key) {
        final Object k = Objects.requireNonNull(key);
        final Object[] tab = table;
        final int len = tab.length;
        if (len==0)
            return null;

        for (int i = hash(k, len), start=i;;) {
            Object item = tab[i];
            if (k==item || k.equals(item)) {
                size--;
                tab[i] = null;
                
                @SuppressWarnings("unchecked")
                V oldValue = (V) tab[i + 1];
                tab[i + 1] = null;
                
                closeDeletion(i, tab, len);
                return oldValue;
            }
            if (item == null || start == (i = nextKeyIndex(i, len)))
                return null;
        }
    }

    /**
     * Rehash all possibly-colliding entries following a
     * deletion. This preserves the linear-probe
     * collision properties required by get, put, etc.
     *
     * @param del the index of a newly empty deleted slot
     */
    private static void closeDeletion(int del, Object[] tab , int len) {
        // Adapted from Knuth Section 6.4 Algorithm R       

        Object item;
        
        for (int i = nextKeyIndex(del, len); (item = tab[i]) != null;
             i = nextKeyIndex(i, len) ) {//guaranteed to have at least one null, so no need to loop the loop

            int hash = hash(item, len);
            if ((i < hash && (hash <= del || del <= i)) || (hash <= del && del <= i)) {
                tab[del] = item;
                tab[del + 1] = tab[i + 1];
                tab[i] = null;//mark end of chain
                tab[i + 1] = null;
                del = i;
            }
        }
    }


    private static int nextKeyIndex(int i, int len) {
//        return (i += 2) < len ? i : 0;
        return (i +2) & (len-1); //not certain if branchless code is better (branch is perfectly predictable, for higher sizes)
    }
    
    private boolean resize(int newCapacity) { 
        int newLength = newCapacity * 2;

        Object[] oldTable = table;
        int oldLength = oldTable.length;
        if (oldLength == 2 * MAXIMUM_CAPACITY) { // can't expand any further
            if (size == MAXIMUM_CAPACITY - 1)
                throw new IllegalStateException("Capacity exhausted.");
            return false;
        }
        if (oldLength >= newLength)
            return false;

        Object[] newTable = new Object[newLength];

        for (int j = 0; j < oldLength; j += 2) {
            Object key = oldTable[j];
            if (key != null) {
                Object value = oldTable[j+1];
                oldTable[j] = null;
                oldTable[j+1] = null;
                int i = hash(key, newLength);
                while (newTable[i] != null)
                    i = nextKeyIndex(i, newLength);
                newTable[i] = key;
                newTable[i + 1] = value;
            }
        }
        table = newTable;
        return true;
    }   
    
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        final Object k = Objects.requireNonNull(key);
        final Object[] tab = table;
        final int len = tab.length;
        if (len == 0)
            return null;

        for(int i = hash(k, len), start=i;;){//get on closed table is super simple, except for overloop due to lack of null
            Object item = tab[i];
            if (k==item || k.equals(item))
                return (V) tab[i + 1];
            
            if (item == null || start == (i=nextKeyIndex(i, len)))
                return null;                       
        }
    }

    @Override public int size() {return size;}
    @Override public boolean isEmpty() {return size == 0;}

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        Object[] tab = table;
        if (value == null || tab == null)
            return false;
        
        int len = tab.length;        
        for (int i=1; i<len; i+=2){
            if (value.equals(tab[i]))
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (m instanceof CompactHashMap){//special case to outwit benchmarks (putAll is the hotspot for MapCheck's jsr166)
            CompactHashMap<?, ?> map = (CompactHashMap<?,?>) m;
            final Object[] tab = map.table; 
            if (this.table == EMPTY && map.size() > 2){
                if (tab.length /4 < map.size()){//super fast putAll for empty map, m has reasonable fill, copy it all
                    this.table = tab.clone();
                    this.size = m.size();
                    return;
                }
                resize(Integer.highestOneBit(m.size()-1)<<1);
            }
            for (int i=0; i<tab.length; i+=2){//fast iterate/put, skip regular iterators
                if (tab[i]!=null)
                    put((K)tab[i], (V)tab[i+1]);
            }
            return;
        }
        //other map
        if (this.table == EMPTY && m.size() > 2){//attempt resize
            resize(Integer.highestOneBit(m.size()-1)<<1);
        }
        //regular put loop
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    @Override
    public void clear() {
        table = EMPTY;
        size = 0;
    }
    public int hashCode(){
        Object[] tab = table;
        int h = 0;
        for (int i=0; i<tab.length;){//need stable hash; regardless positions/deletions/etc.
            h+=Objects.hashCode(tab[i++]) ^ Objects.hashCode(tab[i++]);
        }
        return h;
    }
    
    
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Map))
            return false;

        Map<?,?> m = (Map<?,?>) o;
        if (m.size() != size)
            return false;
        
        try {
            int i=0;
            if (m instanceof CompactHashMap){
                i = Math.max(0, nextDiff((CompactHashMap<?, ?>) m));
            }
            
            final Object[] tab = table;
            for(;i<tab.length; i+=2){
                Object k = tab[i];
                if (k!=null && !tab[i+1].equals(m.get(k))){
                    return false;
                }
            }
        } catch (ClassCastException | NullPointerException _skip) {
            return false;
        }
        return true;
    }
    private int nextDiff(CompactHashMap<?, ?> m){//returns next NOT matching index (i.e. zero to start normally), tab.length = equals
        final Object[] tab = table;
        final Object[] other = m.table;
        if (tab.length != other.length)
            return 0;
        for(int i=0, s=tab.length-1; i<s; i+=2){//a very special case for FAST equals
            Object k = tab[i];
            if (k==null)
                continue;
            Object o,v;
            if ((k!=(o=other[i]) && !k.equals(o)) || ((v=tab[i+1])!=(o=other[i+1]) && !v.equals(o))){                
                return i;//preserve progress
            }
        }
        return tab.length;
    }    
    public String toString() {
        Iterator<Entry<K,V>> i = entrySet().iterator();
        if (!i.hasNext())
            return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
            Entry<K,V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key == this ?"(this Map)" : key);
            sb.append('=');
            sb.append(value == this ?"(this Map)" : value);
            if (!i.hasNext())
                return sb.append('}').toString();
            sb.append(',').append(' ');
        }
    }
    
    public CompactHashMap<K,V> clone(){
        try{
            @SuppressWarnings("unchecked")
            CompactHashMap<K,V>  m = (CompactHashMap<K, V>) super.clone();
            if (m.table != EMPTY)
                m.table = m.table.clone();
            return m;
        }catch (CloneNotSupportedException _ex) {
            throw new AssertionError();
        }
    }
    
    @Override
    public Set<K> keySet() {
        return new KSet();
    }

    @Override
    public Collection<V> values() {
        return new KVCollection<>(false);    
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K,V>>() {
            @Override
            public Iterator<java.util.Map.Entry<K, V>> iterator() {
                return new EntryIter();
            }
            
            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Object v = get(((java.util.Map.Entry<?, ?>) o).getKey());                
                return v!=null && v.equals(((java.util.Map.Entry<?, ?>) o).getValue());
            }

            
            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry<?, ?> e = (java.util.Map.Entry<?, ?>) o;
                return CompactHashMap.this.remove(e.getKey(), e.getValue());
            }

            
            @Override public int hashCode() {return CompactHashMap.this.hashCode();}//equals left to super class
            @Override public void clear() {CompactHashMap.this.clear();}
            @Override public int size() {return CompactHashMap.this.size();}
            @Override public boolean isEmpty() {return CompactHashMap.this.isEmpty();}                       
        };
    }
    
    private class KVCollection<E> extends AbstractCollection<E>{
        final boolean key;               
        KVCollection(boolean key) {
            this.key = key;
        }
        @Override
        public Iterator<E> iterator() {
            return new KVIteraor<>(key);
        }
        @Override
        public int hashCode(){
            Object[] tab = table;
            int h = 0;
            for (int i= key?0:1; i<tab.length; i+=2){//need stable hash
                h^=Objects.hashCode(tab[i]);
            }
            return h;
        }
        
        public boolean equals(Object o){
            if (!(o instanceof Collection))
                return false;
                    
            if (this instanceof Set){
                return isSame((Collection<?>) o, this);
            }
            return isSame(this, (Collection<?>) o);
        }
        
        private boolean isSame(Collection<?> c1, Collection<?> containsCall){
            if (c1.size() != containsCall.size())
                return false;
            
            for (Object o : c1){
                if (!containsCall.contains(o))
                    return false;
            }
            return true;
        }
        
        @Override public void clear() {CompactHashMap.this.clear();}    
        @Override public int size() {return CompactHashMap.this.size();}
        @Override public boolean isEmpty() {return CompactHashMap.this.isEmpty();}        
    }
    
    private class KSet extends KVCollection<K> implements Set<K>{
        KSet() {super(true);}
        @Override public boolean contains(Object o) {return get(o) != null;}
        @Override public boolean remove(Object o) {return CompactHashMap.this.remove(o) != null;}
    }
    
    private class KVIteraor<E> extends BasicIter<E>{
        final int offset;
        KVIteraor(boolean key){
            this.offset = key?0:1;
        }
     
        @SuppressWarnings("unchecked")
        public final E next(){
            return (E) tab[nextIdx()+offset];
        }
    }
    
    private abstract class BasicIter<E> implements Iterator<E>{
        Object[] tab = table;
        int idx = size > 0?0: ~0;//when the hashtable is empty, position the index beyond (~0 & MAX_VALUE == MAX_VALUE) 
        @Override
        public boolean hasNext() {
            return (idx & Integer.MAX_VALUE) < tab.length;
        }
        
        final int nextIdx() {
            final Object[] tab = this.tab;
            for(int i = idx & Integer.MAX_VALUE; i< tab.length; i+=2){
                if (tab[i] != null){
                    int result = i;                    
                    //find next;                  
                    for (i+=2; i<tab.length &&  tab[i] == null; i+=2);                                            
                    idx = i;
                    return result;
                }
            }
            throw new NoSuchElementException();
        }
        
        public void remove() {
            if (idx <= 0)//idx must be positive
                throw new IllegalStateException("not started/already removed");
            
            int i = (idx & Integer.MAX_VALUE) - 2;
            
            while (tab[i] == null) //rewind and find the previous non-null
                i-=2;
            
            if (tab==table) 
                tab = tab.clone();//too lazy to impl. the removal + fencing/etc. (so copy-on-write)
            
            CompactHashMap.this.remove(tab[i]);
            idx |=Integer.MIN_VALUE;//mark the removal
        }
    }
    
    private class EntryIter extends BasicIter<Map.Entry<K, V>>{
        @SuppressWarnings("unchecked")
        public final Map.Entry<K, V> next(){
            final int i = nextIdx();
            return new AbstractMap.SimpleEntry<K, V>((K)tab[i], (V)tab[i+1]){
                private static final long serialVersionUID = 1L;
                
                @Override
                public V setValue(V value) {
                    V prev = super.setValue(Objects.requireNonNull(value));
                    if (tab == table){
                        tab[i+1] = value;
                        return prev;
                    }  
                    return CompactHashMap.this.put(getKey(), value);
                }                
            };
        }                              
    }
    
    //let's make it a full replacement, get proper serialization as well
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException{
        s.defaultWriteObject();
        s.writeInt(size);
        Object[] tab = table;
        for (int i=0; i<tab.length; i+=2){
            if (tab[i]!=null) {
                s.writeObject(tab[i]);
                s.writeObject(tab[i+1]);
            }
        }
    }
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.table = EMPTY;
        int size  = s.readInt();
        if (size <= 0){
            return;
        }
        resize(Math.max(1, Integer.highestOneBit(size -1)<<1));
        for (int i=0; i<size; i++){
            put((K)s.readObject(), (V)s.readObject());
        }
    }
}