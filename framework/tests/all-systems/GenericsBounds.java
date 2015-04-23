import java.util.*;

interface A<ID> {}

@SuppressWarnings({"javari", // No dependency desired; Javari logic stupid.
        "nullness"}) // Why is there a nullness warning?
class B1<ID> implements A<ID> {}

@SuppressWarnings("javari") // No dependency desired; Javari logic stupid.
interface B2 extends A<Long> {}

@SuppressWarnings("javari") // No dependency desired; Javari logic stupid.
class C extends B1<Long> implements B2 {}

@SuppressWarnings({"javari", // No dependency desired; Javari logic stupid.
        "nullness"}) // Why is there a nullness warning?
class Upper<ID, X extends A<ID>, Y extends X> {}

class Lower extends Upper<Long, B2, C> {}

class Test {
    Upper<Long, B2, C> f = new Upper<Long, B2, C>();
}

class Upper1<ID, X extends List<ID>> {}
class Lower1 extends Upper1<Long, List<Long>> {}

class Upper2<ID, X extends List<ID>, Y extends X> {}
class Lower2 extends Upper2<Long, List<Long>, LinkedList<Long>> {}


class GenericGetClass {

    <U extends Object> Class<? extends U> getClass(Class<?> orig, Class<U> cast) {
      return orig.asSubclass(cast);
    }

}
