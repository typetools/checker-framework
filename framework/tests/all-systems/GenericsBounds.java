import java.util.LinkedList;
import java.util.List;

interface A<ID> {}

class B1<ID> implements A<ID> {}

interface B2 extends A<Long> {}

class C extends B1<Long> implements B2 {}

class Upper<ID, X extends A<ID>, Y extends X> {}

class Lower extends Upper<Long, B2, C> {}

class GenericsBounds {
  Upper<Long, B2, C> f = new Upper<>();
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
