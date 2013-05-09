import java.util.*;

interface A<ID> {}

class B1<ID> implements A<ID> {}

interface B2 extends A<Long> {}

class C extends B1<Long> implements B2 {}

class Upper<ID, X extends A<ID>, Y extends X> {}

class Lower extends Upper<Long, B2, C> {}

class Test {
    Upper<Long, B2, C> f = new Upper<Long, B2, C>();
}

class Upper1<ID, X extends List<ID>> {}
class Lower1 extends Upper1<Long, List<Long>> {}

class Upper2<ID, X extends List<ID>, Y extends X> {}
class Lower2 extends Upper2<Long, List<Long>, LinkedList<Long>> {}

class GenericNull {
    /**
     * null has to be bottom. If not, the following legal java code
     * will not compile, because T may not be a super type of null's type.
     */
    <T> T f() {
        return null;
    }	

    <U> Class<? extends U> getClass(Class<?> orig, Class<U> cast) {
        return orig.asSubclass(cast);
    }
}
