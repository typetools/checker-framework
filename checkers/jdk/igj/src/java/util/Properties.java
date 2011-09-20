package java.util;
import checkers.igj.quals.*;

@I
public class Properties extends @I Hashtable<Object, Object> {
    private static final long serialVersionUID = 0L;
  public Properties(@AssignsFields Properties this) { throw new RuntimeException("skeleton method"); }
  public Properties(@AssignsFields Properties this, @ReadOnly Properties a1) { throw new RuntimeException("skeleton method"); }
  public synchronized Object setProperty(@Mutable Properties this, String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void load(@Mutable Properties this, java.io.Reader a1)  throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void load(@Mutable Properties this, java.io.InputStream a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void save(@ReadOnly Properties this, java.io.OutputStream a1, String a2) { throw new RuntimeException("skeleton method"); }
  public void store(@ReadOnly Properties this, java.io.Writer a1, String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void store(@ReadOnly Properties this, java.io.OutputStream a1, String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void loadFromXML(@Mutable Properties this, java.io.InputStream a1) throws java.io.IOException, InvalidPropertiesFormatException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(@ReadOnly Properties this, java.io.OutputStream a1, String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(@ReadOnly Properties this, java.io.OutputStream a1, String a2, String a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public String getProperty(@ReadOnly Properties this, String a1) { throw new RuntimeException("skeleton method"); }
  public String getProperty(@ReadOnly Properties this, String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public Enumeration<?> propertyNames(@ReadOnly Properties this) { throw new RuntimeException("skeleton method"); }
  public @I("T") Set<String> stringPropertyNames(@ReadOnly Properties this) { throw new RuntimeException("skeleton method"); }
  public void list(@ReadOnly Properties this, java.io.PrintStream a1) { throw new RuntimeException("skeleton method"); }
  public void list(@ReadOnly Properties this, java.io.PrintWriter a1) { throw new RuntimeException("skeleton method"); }
}
