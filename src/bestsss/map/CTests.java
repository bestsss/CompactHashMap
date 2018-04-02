package bestsss.map;

/*Written by S. Simeonoff and released to the public domain, as explained at http://creativecommons.org/publicdomain/zero/1.0/
 * 2018
 */
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CTests {
    public static void main(String[] args) {
        final int n = 997;
        Map<Long, String> compact = newMap(n, CompactHashMap::new);
        Map<Long, String> hash = newMap(n, HashMap::new);
        
        assertEquals(compact.entrySet(), hash.entrySet());
        
        testMap(hash, n);
        testMap(compact, n);
        
        assertEquals(compact, hash);
        
        testIteratorRemove(newMap(11, CompactHashMap::new), 11);
        testIteratorRemove(newMap(11, HashMap::new), 11);
        
        lowSize(new CompactHashMap<>());
        lowSize(new HashMap<>());
        
        evenLower(new CompactHashMap<>());
        evenLower(new HashMap<>());       
    }  
    
    private static void assertEquals(Object actual, Object expected){
        if (!Objects.equals(actual, expected) || !Objects.equals(expected, actual)){//ensure transitive props for equals
            throw new AssertionError(String.format("Fail. Actual %s, expected: %s", actual, expected));
        }
    }
    
    private static void evenLower(Map<Integer, Integer> map) {        
        Integer[] key={2017, 19, 2018};
        for (int i=0; i<key.length; i++){
            for (int j=i; j-->0;){
                map.put(key[j], key[j]<<1);
            }
            for (int j=i; j-->0;){
                assertEquals(map.containsKey(key[j]) && map.containsValue(key[j]<<1), true);
            }
            
            assertEquals(map.size(), i);
            assertEquals(map.entrySet().stream().map(e -> e.getValue() >> 1).collect(Collectors.toSet()).equals(map.keySet()), true);
            assertEquals(map.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()).equals(new java.util.ArrayList<>(map.values())), true);

            map.clear();
        }
    }

    private static void lowSize(Map<URI, String>  map) {
        try{
            for (int i =0; i<8; i++){
                String x = String.format("sss://%d?value", i);
                map.put(new URI(x), x);
                assertEquals(map.get(new URI(x)), x);
                assertEquals(map.get(new URI(x+"x")), null);

            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testIteratorRemove(Map<Long, String> map, int n) {
        int count = 0;
        for (Iterator<Long> i = map.keySet().iterator(); i.hasNext(); count++){
            i.next();
            i.remove();
        }
        assertEquals(count, n);        
        assertEquals(map.isEmpty(), true);
    }

    private static long p2(long v){
        return v*v;
    }

    private static void testMap(Map<Long, String> map, long max) {
        assertEquals(map.keySet().stream().max(Long::compareTo).get().longValue(), p2(max-1));//find max and test
        
        assertEquals(map.keySet().stream().filter(Objects::isNull).count(), 0L);//no null
        assertEquals(map.values().stream().filter(Objects::isNull).count(), 0L);
        
        map.entrySet().stream().forEach(e -> assertEquals((long)e.getKey(), p2(Long.parseLong(e.getValue())) ) );
        for (long i=0; i<max; i++){
            assertEquals(map.remove(i*i), String.valueOf(i));
        } 
        assertEquals(map.size(), 0);
    }


    private static Map<Long, String> newMap(int count, Supplier<Map<Long, String>> f){
        Map<Long, String> map = f.get();
        fillUp(map, count);
        assertEquals(map.size(), count);
        return map;
    }
    private static void fillUp(Map<Long, String> map, int count) {
        for (long i=0; i<count; i++){
            map.put(i*i, String.valueOf(i));
        }
    }
}