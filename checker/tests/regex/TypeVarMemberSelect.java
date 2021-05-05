import org.checkerframework.checker.regex.qual.*;

class Box<T extends @Regex(1) Object> {
  @Regex(1) T t1;

  T t2;
}

class TypeVarMemberSelect<V extends Box<@Regex(2) String>> {

  void test(V v) {
    // :: error: (assignment)
    @Regex(2) String local1 = v.t1;

    // Previously the type of the right hand side would have been T which is wrong.  This test
    // was added to make sure we call viewpoint adaptation when type variables are the receiver.
    @Regex(2) String local2 = v.t2;
  }
}
