import java.util.Date;

@SuppressWarnings("ainfertest") // only check WPI for crashes
public class GetClassTest {

  // See AnnotatedTypeFactory.adaptGetClassReturnTypeToReceiver

  void context() {
    Integer i = 4;
    i.getClass();
    Class<?> a = i.getClass();
    Class<? extends Object> b = i.getClass();
    Class<? extends Integer> c = i.getClass();

    Class<?> d = i.getClass();
    // not legal Java; that is, does not type-check under Java rules
    // Class<Integer> e = i.getClass();
  }

  void m(Date d) {
    Class<? extends Date> c = d.getClass();
  }
}
