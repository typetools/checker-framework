package java.util;
import org.checkerframework.checker.lock.qual.*;

public class Properties extends Hashtable<Object, Object> {
  private static final long serialVersionUID = 0;
  public Properties() { throw new RuntimeException("skeleton method"); }
  public Properties(Properties a1) { throw new RuntimeException("skeleton method"); }
  public synchronized Object setProperty(@GuardSatisfied Properties this, String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void load(java.io.Reader a1)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void load(java.io.InputStream a1)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void save(java.io.OutputStream a1, String a2) { throw new RuntimeException("skeleton method"); }
  public void store(java.io.Writer a1, String a2)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void store(java.io.OutputStream a1, String a2)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void loadFromXML(java.io.InputStream a1)throws java.io.IOException, InvalidPropertiesFormatException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(java.io.OutputStream a1, String a2)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(java.io.OutputStream a1, String a2, String a3)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public String getProperty(@GuardSatisfied Properties this, String a1) { throw new RuntimeException("skeleton method"); }
  public String getProperty(@GuardSatisfied Properties this, String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public Enumeration<?> propertyNames() { throw new RuntimeException("skeleton method"); }
  public Set<String> stringPropertyNames() { throw new RuntimeException("skeleton method"); }
  public void list(java.io.PrintStream a1) { throw new RuntimeException("skeleton method"); }
  public void list(java.io.PrintWriter a1) { throw new RuntimeException("skeleton method"); }
}
