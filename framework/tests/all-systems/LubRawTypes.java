// @above-java17-jdk-skip-test TODO: reinstate on JDK 18, false positives may be due to issue #979

@SuppressWarnings("unchecked")
public class LubRawTypes {
  public static boolean flag = false;

  class MyGen<T> {}

  MyGen<MyGen<MyGen<String>>> test(MyGen myGen1, MyGen myGen2) {
    return flag ? myGen1 : myGen2;
  }

  MyGen<MyGen<MyGen<String>>> test2(MyGen myGen1, MyGen<MyGen<MyGen<String>>> myGen2) {
    return flag ? myGen1 : myGen2;
  }

  MyGen<MyGen<MyGen<String>>> test3(MyGen myGen1, MyGen<MyGen<MyGen<String>>> myGen2) {
    return flag ? myGen2 : myGen1;
  }
}
