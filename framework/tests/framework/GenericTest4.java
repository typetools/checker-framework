import java.util.Map;
import tests.util.*;

// Test case for Issue 134:
// https://github.com/typetools/checker-framework/issues/134
// Handling of generics from different enclosing classes.
// TODO: revisit with nested types in 1.3.
// @skip-test
class GenericTest4 {
    public interface Foo {}

    class Outer<O> {
        O getOuter() {
            return null;
        }

        class Inner<I> {
            O getInner() {
                return null;
            }

            I setter1(O p) {
                return null;
            }

            O setter2(I p) {
                return null;
            }

            Map<O, I> wow(Map<O, I> p) {
                return null;
            }
        }
    }

    class OuterImpl extends Outer<Foo> {
        void test() {
            Foo foo = getOuter();
        }

        class InnerImpl extends Inner<@Odd String> {
            void test() {
                Foo foo = getInner();
                String s = setter1(foo);
                foo = setter2(s);
            }

            void testWow(Map<Foo, String> p) {
                p = wow(p);
            }
        }
    }

    // Add uses from outside of both classes.
}
