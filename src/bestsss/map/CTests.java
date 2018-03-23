package bestsss.map;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.testng.Assert;

/**
 * @copyright Playtech 2018
 * @author Stanimir Simeonoff
 */
public class CTests {
    public static void main(String[] args) {
        final int n = 997;
        Map<Long, String> compact = newMap(n, CompactHashMap::new);
        Map<Long, String> hash = newMap(n, HashMap::new);

//        System.out.println(compact);

        Assert.assertEquals(compact.entrySet(), hash.entrySet());
        
        testMap(hash, n);
        testMap(compact, n);
        
        Assert.assertEquals(compact, hash);
        
        testIteratorRemove(newMap(11, CompactHashMap::new), 11);
        testIteratorRemove(newMap(11, HashMap::new), 11);
        
        lowSize(new CompactHashMap<>());
        lowSize(new HashMap<>());
    }  
    
    private static void lowSize(Map<URI, String>  map) {
        try{
            for (int i =0; i<8; i++){
                String x = String.format("sss://%d?value", i);
                map.put(new URI(x), x);
                Assert.assertEquals(map.get(new URI(x)), x);
                Assert.assertNull(map.get(new URI(x+"x")));

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
        Assert.assertEquals(count, n);
        Assert.assertTrue(map.isEmpty());
    }

    private static long p2(long v){
        return v*v;
    }

    private static void testMap(Map<Long, String> map, long max) {
        Assert.assertEquals(map.keySet().stream().max(Long::compareTo).get().longValue(), p2(max-1));//find max and test
        
        Assert.assertEquals(map.keySet().stream().filter(Objects::isNull).count(), 0);//no null
        Assert.assertEquals(map.values().stream().filter(Objects::isNull).count(), 0);
        
        map.entrySet().stream().forEach(e -> Assert.assertEquals((long)e.getKey(), p2(Long.parseLong(e.getValue())) ) );
        for (long i=0; i<max; i++){
            Assert.assertEquals(map.remove(i*i), String.valueOf(i));
        } 
        Assert.assertEquals(map.size(), 0);
    }


    private static Map<Long, String> newMap(int count, Supplier<Map<Long, String>> f){
        Map<Long, String> map = f.get();
        fillUp(map, count);
        Assert.assertEquals(map.size(), count);
        return map;
    }
    private static void fillUp(Map<Long, String> map, int count) {
        for (long i=0; i<count; i++){
            map.put(i*i, String.valueOf(i));
        }
    }
}
