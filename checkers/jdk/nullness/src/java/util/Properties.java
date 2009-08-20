package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Properties extends java.util.Hashtable<java.lang.Object, java.lang.Object> {
  private static final long serialVersionUID = 0;
  public Properties() { throw new RuntimeException("skeleton method"); }
  public Properties(java.util.Properties a1) { throw new RuntimeException("skeleton method"); }
  public synchronized @Nullable java.lang.Object setProperty(java.lang.String a1, java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void load(java.io.Reader a1)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void load(java.io.InputStream a1)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void save(java.io.OutputStream a1, @Nullable java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public void store(java.io.Writer a1, @Nullable java.lang.String a2)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void store(java.io.OutputStream a1, @Nullable java.lang.String a2)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void loadFromXML(java.io.InputStream a1)throws java.io.IOException, java.util.InvalidPropertiesFormatException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(java.io.OutputStream a1, @Nullable java.lang.String a2)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(java.io.OutputStream a1, @Nullable java.lang.String a2, java.lang.String a3)throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getProperty(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getProperty(java.lang.String a1, java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public java.util.Enumeration<?> propertyNames() { throw new RuntimeException("skeleton method"); }
  public java.util.Set<java.lang.String> stringPropertyNames() { throw new RuntimeException("skeleton method"); }
  public void list(java.io.PrintStream a1) { throw new RuntimeException("skeleton method"); }
  public void list(java.io.PrintWriter a1) { throw new RuntimeException("skeleton method"); }
}
