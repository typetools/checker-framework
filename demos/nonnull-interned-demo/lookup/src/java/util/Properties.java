package java.util;

import java.io.Reader;
import java.io.Writer;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import checkers.quals.*;

public class Properties extends java.util.Hashtable<java.lang.Object, java.lang.Object> {
  public Properties() { throw new RuntimeException("skeleton method"); }
  public Properties(@NonNull Properties a1) { throw new RuntimeException("skeleton method"); }
  public synchronized  @Nullable Object setProperty(@NonNull String a1, @NonNull String a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void load(@NonNull Reader a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void load(@NonNull InputStream a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void save(@NonNull OutputStream a1, @Nullable String a2) { throw new RuntimeException("skeleton method"); }
  public void store(@NonNull Writer a1, @Nullable String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void store(@NonNull OutputStream a1, @Nullable String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void loadFromXML(@NonNull InputStream a1) throws java.io.IOException, java.util.InvalidPropertiesFormatException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(@NonNull OutputStream a1, @Nullable String a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void storeToXML(@NonNull OutputStream a1, @Nullable String a2, @NonNull String a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public @Nullable String getProperty(@NonNull String a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull String getProperty(@NonNull String a1, @NonNull String a2) { throw new RuntimeException("skeleton method"); }
  public @NonNull Enumeration<? extends @NonNull Object> propertyNames() { throw new RuntimeException("skeleton method"); }
  public @NonNull Set<@NonNull String> stringPropertyNames() { throw new RuntimeException("skeleton method"); }
  public void list(@NonNull PrintStream a1) { throw new RuntimeException("skeleton method"); }
  public void list(@NonNull PrintWriter a1) { throw new RuntimeException("skeleton method"); }
}
