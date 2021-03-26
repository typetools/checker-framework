package daikon.chicory;

public class ChicoryPremain {
  public static void premain(ClassLoader loader) {
    Object transformer = null;
    try {
      transformer = loader.loadClass("Foo").getDeclaredConstructor().newInstance();
      transformer.getClass();
    } catch (Exception e1) {
      throw new RuntimeException("Exception", e1);
    }
  }
}
