import java.util.List;

public class RawTypeTest {
  public void m(ClassLoader cl) throws ClassNotFoundException {
    Class clazz = cl.loadClass("");
  }
}

interface I {
  public void m(List<? extends String> l);
}

class C implements I {
  public void m(List l) {
    
  }
}