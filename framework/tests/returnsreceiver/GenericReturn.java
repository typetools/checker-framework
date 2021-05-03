import org.checkerframework.common.returnsreceiver.qual.This;

public class GenericReturn {

  abstract static class Builder<B extends Builder<?>> {
    abstract @This B setFoo(String foo);

    @SuppressWarnings("unchecked")
    @This B retThis() {
      return (@This B) this;
    }

    @This B dontRetThis() {
      // :: error: return
      return null;
    }
  }

  static class Builder1 extends Builder<Builder1> {

    @This Builder1 setFoo(String foo) {
      return this;
    }
  }

  static class Builder2 extends Builder<Builder2> {

    @This Builder2 setFoo(String foo) {
      // :: error: return
      return null;
    }
  }
}
