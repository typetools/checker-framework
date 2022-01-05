public class Issue4996 {

  abstract static class CaptureOuter<T extends CharSequence> {
    abstract T get();

    abstract class Inner {
      abstract T get();
    }
  }

  static class Client {

    CharSequence getFrom(CaptureOuter<?> o) {
      return o.get();
    }

    CharSequence getFrom(CaptureOuter<?>.Inner i) {
      return i.get();
    }
  }
}
