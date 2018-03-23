package bestsss.map;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
* @copyright Playtech 2018
* @author Stanimir Simeonoff
*/
public class Memer {
    public static void main(String[] args) {
        int n = (int) 1.9e5;
        Object[] k = newArray(n);
        Object[] v = newArray(n);
        new CompactHashMap<>().put(1,1);//load class
        gc();
        long start =  mem();
        
        Map<Object, Object> m = new CompactHashMap<>();
        fill(m, k, v);
        gc();
        long delta = mem() - start;
        System.out.printf("Consumed: %d, per entry: %.2f%n", delta, delta /(double)n);
        System.out.println(k.length + v.length);//reachability for k & v
    }

    private static void gc() {
        System.gc();
        System.runFinalization();
        System.gc();
        Thread.yield();
    }

    private static long mem() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private static void fill(Map<Object, Object> m, Object[] k, Object[] v) {
        for (int i = 0; i < k.length; i++) {
            m.put(k[i], v[i]);
        }
    }

    private static Object[] newArray(int n) {
        Object[] k = new Object[n];
        for (int i = 0; i < k.length; i++) {
            k[i]=new Integer(i);
        }
        return k;
    }
}
