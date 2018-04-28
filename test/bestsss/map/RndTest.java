package bestsss.map;
/*Written by S. Simeonoff and released to the public domain, as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */
 
import java.util.*;
import java.util.Map.Entry;

/**
 *  Random test adding, removing and reading from {@link CompactHashMap} and {@link HashMap}, checking for equality 
 * @author Stanimir Simeonoff
 */
public class RndTest {
  
  public static void main(String[] args) {
    int iterations = (int) 1e6;
    int maxKey = (int) 1e7;
    float read = 0.8f;
    float add = read + 0.15f;
    
    int removals = 0;
    int puts = 0;
    //remove -> remainder to 1
    CompactHashMap<Integer, Long> c = new CompactHashMap<>();
    Map<Integer, Long> h = new HashMap<>();
    Random r = new Random(111);
    
    for (int i=0; i< iterations; i++){
      float f = r.nextFloat();
      
      if (f < read){
        Integer k = randomKey(r, c);
        if (k==null){
          continue;
        }
        Long v = c.get(k);
        assertEquals(h.get(k), v);
        assertEquals(val(k), v);
        if (f < 0.01){
          assertEquals(c, h);
          if (f < 0.0001){
            System.out.print('.');//single dot; we are moving
          }
        }
        continue;
      }
      
      if (f<add){
        int k = r.nextInt(maxKey);
        Long v = val(k);
        c.put(k, v);
        h.put(k, v);
        puts++;
        continue;
      }
      //remove
      if (c.isEmpty())
        continue;
      
      removals++;
      if (r.nextBoolean()){//remove 1st
        Iterator<Map.Entry<Integer, Long>> it = c.entrySet().iterator();
        Entry<Integer, Long> e = it.next();
        assertEquals(e.getValue(), h.remove(e.getKey()));
        it.remove();
      } else{
        Integer k = randomKey(r, c);        
        assertEquals(k!=null, true);        
        assertEquals(c.remove(k), h.remove(k));
      }
      
    }
    
    System.out.printf("%nSize: %d, puts: %d, removals: %d%n", c.size, puts, removals);
    assertEquals(h, c);
  }
  static long val(long k){
     return k * (k+2);
  }
  
  static Integer randomKey(Random r, CompactHashMap<Integer, ?> c){
    Object[] tab = c.table;
    if (tab.length == 0)
      return null;
    
    int idx = r.nextInt(tab.length) & ~1;
    for (int i=idx; ;){
      if (tab[i]!=null)
        return (Integer) tab[i];
      
      if (idx == (i=(i+2) & (tab.length-1))){//loop
        return null;
      }
    }
  }
  
  private static void assertEquals(Object actual, Object expected){
    if (!Objects.equals(actual, expected) || !Objects.equals(expected, actual)){//ensure transitive props for equals
      throw new AssertionError(String.format("Fail. Actual %s, expected: %s", actual, expected));
    }
  }
}