import java.util.List;
import java.util.Map;

@interface Bla {}

public class WildcardAnnoBound<X extends List<? extends Object>> {
  WildcardAnnoBound(WildcardAnnoBound<X> n, X p) {}
}

class NoBound<X> {}

class Bounds<X extends Object & Comparable<int[]> & Map<? extends Object, ?>, Y> {}
