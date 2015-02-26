import org.checkerframework.checker.regex.qual.*;

class Box<T extends @Regex(1) Object> {
    T t;
}

class TypeVarMemberSelect<V extends Box<@Regex(2) String>> {

    void test(V v) {
        //previously the type of the right hand side would have been T which is wrong
        //this test was added to make sure we call viewpoint adaptation when type variables are the receiver
        @Regex(2) String str = v.t;
    }
}