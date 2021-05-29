import org.checkerframework.dataflow.qual.Pure;

public class GetInterfacesPurity {

  @Pure
  public static boolean isSubtype(Class<?> sub, Class<?> sup) {
    // :: error: (purity.not.deterministic.call)
    Class<?>[] interfaces = sub.getInterfaces();
    return interfaces.length == 0;
  }
}
