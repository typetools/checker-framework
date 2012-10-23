package java.util;

import checkers.quals.*;

public class Properties extends java.util.Hashtable<java.lang.Object, java.lang.Object> {
  public Properties() { throw new RuntimeException("skeleton method"); }
  public Properties(@NonNull java.util.Properties a1) { throw new RuntimeException("skeleton method"); }
  public synchronized  @Nullable Object setProperty(@NonNull java.lang.String a1, @NonNull java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void load(@NonNull java.io.Reader a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void load(@NonNull java.io.InputStream a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void save(@NonNull java.io.OutputStream a1, @Nullable java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public void store(@NonNull java.io.Writer a1, @Nullable java.lang.String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void store(@NonNull java.io.OutputStream a1, @Nullable java.lang.String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void loadFromXML(@NonNull java.io.InputStream a1) throws java.io.IOException, java.util.InvalidPropertiesFormatException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(@NonNull java.io.OutputStream a1, @Nullable java.lang.String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(@NonNull java.io.OutputStream a1, @Nullable java.lang.String a2, @NonNull java.lang.String a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getProperty(@NonNull java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.String getProperty(@NonNull java.lang.String a1, @NonNull java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.Enumeration<? extends @NonNull Object> propertyNames() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.Set<@NonNull java.lang.String> stringPropertyNames() { throw new RuntimeException("skeleton method"); }
  public void list(@NonNull java.io.PrintStream a1) { throw new RuntimeException("skeleton method"); }
  public void list(@NonNull java.io.PrintWriter a1) { throw new RuntimeException("skeleton method"); }
}
