// If conservativeUninferredTypeArguments option is used, then the lines marked
// "TODO: Issue 802", will issue a methodref.inference.unimplemented warning.

interface Supplier<R> {
    R supply();
}

interface FunctionMR<T, R> {
    R apply(T t);
}

interface Consumer<T> {
    void consume(T t);
}

interface BiFunctionMR<T, U, R> {
    R apply(T t, U u);
}

/** super # instMethod */
// SUPER(ReferenceMode.INVOKE, false),
class Super {

    Object func1(Object o) {
        return o;
    }

    <T> T func2(T o) {
        return o;
    }

    class Sub extends Super {
        void context() {
            FunctionMR<Object, Object> f1 = super::func1;
            // TODO: Issue 802: type argument inference
            FunctionMR f2 = super::func2;
            // Top level wildcards are ignored when type checking
            FunctionMR<? extends String, ? extends String> f3 = super::<String>func2;
        }
    }
}

class SuperWithArg<U> {

    void func1(U o) {}

    class Sub extends SuperWithArg<Number> {
        void context() {
            Consumer<Integer> f1 = super::func1;
        }
    }
}

/** Type # instMethod. */
// UNBOUNDED(ReferenceMode.INVOKE, true),
class Unbound {
    <T> T func1(T o) {
        return o;
    }

    void context() {
        FunctionMR<String, String> f1 = String::toString;
        // TODO: Issue 802: type argument inference
        BiFunctionMR<Unbound, String, String> f2 = Unbound::func1;
        @SuppressWarnings("nullness:type.argument.type.incompatible")
        BiFunctionMR<? extends Unbound, ? super Integer, ? extends Integer> f3 =
                Unbound::<Integer>func1;
    }
}

abstract class UnboundWithArg<U> {
    abstract U func1();

    void context() {
        // TODO: Issue 802: type argument inference
        FunctionMR<UnboundWithArg<String>, String> f1 = UnboundWithArg::func1;
        FunctionMR<UnboundWithArg<String>, String> f2 = UnboundWithArg<String>::func1;
        // TODO: Issue 802: type argument inference
        FunctionMR<? extends UnboundWithArg<String>, String> f3 = UnboundWithArg::func1;
        FunctionMR<? extends UnboundWithArg<String>, String> f4 = UnboundWithArg<String>::func1;
    }
}

/** Type # staticMethod. */
// STATIC(ReferenceMode.INVOKE, false),
class Static {
    static <T> T func1(T o) {
        return o;
    }

    void context() {
        // TODO: Issue 802: type argument inference
        FunctionMR<String, String> f1 = Static::func1;
        FunctionMR<String, String> f2 = Static::<String>func1;
    }
}

/** Expr # instMethod. */
// BOUND(ReferenceMode.INVOKE, false),
class Bound {
    <T> T func1(T o) {
        return o;
    }

    void context(Bound bound) {
        // TODO: Issue 802: type argument inference
        FunctionMR<String, String> f1 = bound::func1;
        // TODO: Issue 802: type argument inference
        FunctionMR<String, String> f2 = this::func1;
        FunctionMR<String, String> f3 = this::<String>func1;
        FunctionMR<? extends String, ? extends String> f4 = this::<String>func1;
    }
}

class BoundWithArg<U> {
    void func1(U param) {}

    void context(BoundWithArg<Number> bound) {
        Consumer<Number> f1 = bound::func1;
        Consumer<Integer> f2 = bound::func1;
    }
}

/** Inner # new. */
// IMPLICIT_INNER(ReferenceMode.NEW, false),
class Outer {
    void context(Outer other) {
        Supplier<Inner> f1 = Inner::new;
    }

    class Inner extends Outer {}
}

class OuterWithArg {
    void context() {
        // TODO: Issue 802: type argument inference
        Supplier<Inner<String>> f1 = Inner::new;
        Supplier<? extends Inner<Number>> f2 = Inner<Number>::new;
        Supplier<? extends Inner<? extends Number>> f3 = Inner<Integer>::new;
    }

    class Inner<T> extends OuterWithArg {}
}

/** Toplevel # new. */
// TOPLEVEL(ReferenceMode.NEW, false),
class TopLevel {
    TopLevel() {}

    <T> TopLevel(T s) {}

    void context() {
        Supplier<TopLevel> f1 = TopLevel::new;
        // TODO: Issue 802: type argument inference
        FunctionMR<String, TopLevel> f2 = TopLevel::new;
        FunctionMR<String, TopLevel> f3 = TopLevel::<String>new;
    }
}

class TopLevelWithArg<T> {
    TopLevelWithArg() {}

    <U> TopLevelWithArg(U s) {}

    void context() {
        // TODO: Issue 802: type argument inference
        Supplier<TopLevelWithArg<String>> f1 = TopLevelWithArg::new;
        Supplier<TopLevelWithArg<String>> f2 = TopLevelWithArg<String>::new;
        FunctionMR<String, TopLevelWithArg<String>> f3 = TopLevelWithArg<String>::<String>new;
    }
}

/** ArrayType # new. */
// ARRAY_CTOR(ReferenceMode.NEW, false);

class ArrayType {
    void context() {
        // TODO: Signedness Checker does not default boxed primitives correctly.
        // See Issue #797: https://github.com/typetools/checker-framework/issues/797
        @SuppressWarnings({"signedness"})
        FunctionMR<Integer, String[]> string = String[]::new;
        FunctionMR<String[], String[]> clone = String[]::clone;
        FunctionMR<String[], String> toString = String[]::toString;
    }
}
