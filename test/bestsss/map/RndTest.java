package bestsss.map;
/*Written by S. Simeonoff and released to the public domain, as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */
 
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

/**
 *  Random test adding, removing and reading from {@link CompactHashMap} and {@link HashMap}, checking for equality 
 * @author Stanimir Simeonoff
 */
public class RndTest {
  
  public static void main(String[] args) {
    run(10000, Integer.MAX_VALUE);//warm up    
        
    for (int i=1; i<67; i++){
      run((int)5.5e4, i);
    }
    
    timedRun((int)7.8e5, Integer.MAX_VALUE);
  }

  private static void timedRun(int iterations, int maxSize) {
    long nanos = -System.nanoTime();    
    int s = run(iterations, maxSize);
    nanos += System.nanoTime();
    System.out.printf("Time %.2fms; size: %d%n", BigDecimal.valueOf(nanos, 6), s);
  }
  
  static int run(int iterations, int maxSize){    
    final boolean print = maxSize > 200;//skip prints for lower sizes
    final int maxKey = iterations * 11;
    float read = 0.8f - (maxSize < 2000? 0.2f :0f);//lower sizes prefer more "remove/put" than read
    float add = read + (1f - read) * (maxSize <2000? .61f : .75f);//and more "remove" overall
    
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
          if (print && f < 0.0001){
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
        if (c.size() <= maxSize){
          continue;
        }
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
    if (print)
      System.out.printf("%nIterations: %d, size: %d, puts: %d, removals: %d%n", iterations, c.size, puts, removals);
    assertEquals(h, c);
    return c.size();
  }
  
  static long val(long k){
     return k * (k+2);
  }
  
//randomKey can be used as a tool for probabilistic expiration cache (i.e. gather several random keys [+values] and expire one of them [lowest score])
  static Integer randomKey(Random r, CompactHashMap<Integer, ?> c){
    final Object[] tab = c.table;
    if (tab.length == 0)
      return null;
    
    final int idx = r.nextInt(tab.length) & ~1;
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