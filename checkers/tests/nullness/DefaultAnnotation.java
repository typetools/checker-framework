import checkers.nullness.quals.*;
import checkers.quals.*;
import java.util.*;
public class DefaultAnnotation {

    public void testNoDefault() {

        String s = null;

    }

    @DefaultQualifiers(@DefaultQualifier(value="checkers.nullness.quals.NonNull", locations={DefaultLocation.ALL}))
    public void testDefault() {

        //:: error: (assignment.type.incompatible)
        String s = null;                                // error
        List<String> lst = new List<String>();          // valid
        //:: error: (argument.type.incompatible)
        lst.add(null);                                  // error
    }

    @DefaultQualifier(value="checkers.nullness.quals.NonNull", locations={DefaultLocation.ALL})
    public class InnerDefault {

        public void testDefault() {
            //:: error: (assignment.type.incompatible)
            String s = null;                                // error
            List<String> lst = new List<String>();          // valid
            //:: error: (argument.type.incompatible)
            lst.add(null);                                  // error
            s = lst.get(0);                                 // valid

            List<@Nullable String> nullList
                = new List<@Nullable String>();             // valid
            nullList.add(null);                             // valid
            //:: error: (assignment.type.incompatible)
            s = nullList.get(0);                            // error
        }
    }

    @DefaultQualifier(value="checkers.nullness.quals.NonNull", locations={DefaultLocation.ALL})
    public static class DefaultDefs {

        public String getNNString() {
            return "foo";                               // valid
        }

        public String getNNString2() {
            //:: error: (return.type.incompatible)
            return null;                                // error
        }

        public <T extends @Nullable Object> T getNull(T t) {
            //:: error: (return.type.incompatible)
            return null;                                // invalid
        }

        public <T extends @NonNull Object> T getNonNull(T t) {
            //:: error: (return.type.incompatible)
            return null;                                // error
        }
    }

    public class DefaultUses {

        public void test() {

            DefaultDefs d = new DefaultDefs();

            @NonNull String s = d.getNNString();        // valid

        }

        @DefaultQualifier(value="checkers.nullness.quals.NonNull", locations={DefaultLocation.ALL})
        public void testDefaultArgs() {

            DefaultDefs d = new DefaultDefs();

            //:: error: (assignment.type.incompatible)
            String s1 = d.<@Nullable String>getNull(null);      // error
            String s2 = d.<String>getNonNull("foo");            // valid
            //:: error: (type.argument.type.incompatible) :: error: (assignment.type.incompatible)
            String s3 = d.<@Nullable String>getNonNull("foo");  // error
        }

    }

    @DefaultQualifier(value="checkers.nullness.quals.NonNull")
    static class DefaultExtends implements Iterator<String>, Iterable<String> {

        @Override public boolean hasNext() { throw new UnsupportedOperationException(); }
        @Override public void remove() { throw new UnsupportedOperationException(); }
        @Override public String next() { throw new UnsupportedOperationException(); }

        @Override
        public Iterator<String> iterator() {
            return this;
        }
    }

    class List<E extends @Nullable Object> {
        public E get(int i) { throw new RuntimeException(); }
        public boolean add(E e) { throw new RuntimeException(); }
    }

    @DefaultQualifier(value="NonNull")
    public void testDefaultUnqualified() {

        //:: error: (assignment.type.incompatible)
        String s = null;                                // error
        List<String> lst = new List<String>();          // valid
        //:: error: (argument.type.incompatible)
        lst.add(null);                                  // error
    }

}
