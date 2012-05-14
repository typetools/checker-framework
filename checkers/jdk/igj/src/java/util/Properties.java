package java.util;
import checkers.igj.quals.*;

@I
public class Properties extends @I Hashtable<Object, Object> {
    private static final long serialVersionUID = 0L;
  public Properties() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Properties(@ReadOnly Properties a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public synchronized Object setProperty(String a1, String a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void load(java.io.Reader a1) @Mutable  throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void load(java.io.InputStream a1) @Mutable throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void save(java.io.OutputStream a1, String a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void store(java.io.Writer a1, String a2) @ReadOnly throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void store(java.io.OutputStream a1, String a2) @ReadOnly throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void loadFromXML(java.io.InputStream a1) @Mutable throws java.io.IOException, InvalidPropertiesFormatException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(java.io.OutputStream a1, String a2) @ReadOnly throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(java.io.OutputStream a1, String a2, String a3) @ReadOnly throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public String getProperty(String a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String getProperty(String a1, String a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public Enumeration<?> propertyNames() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("T") Set<String> stringPropertyNames() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void list(java.io.PrintStream a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void list(java.io.PrintWriter a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
}
