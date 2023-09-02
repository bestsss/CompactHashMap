# CompactHashMap key features.
- A java.util.Map implementation, non-concurrent
- Very dense, esp. on low sizes. In lots of java applications, a lot of memory is wasted of java.util.HashMap$Entry and its arrays. This implementation uses a power of two backing array, and a linear probe search, plus a fill factor of one for smaller maps.
- A drop-in replacement of java.util.HashMap, except no support for _null_
- An empty CompactHashMap is extemeley cheap, no other obeject allocated - one array reference + an int
- Much faster key/values iteration
- Fast putAll
- No storage for 'hashCode' unlike java.util.HashMap as many common keys do have hashCode that's trivial to calculate (or it is cached)
