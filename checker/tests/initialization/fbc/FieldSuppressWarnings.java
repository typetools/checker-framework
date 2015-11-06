import org.checkerframework.checker.nullness.qual.*;

import java.util.*;

//:: error: (initialization.fields.uninitialized)
public class FieldSuppressWarnings {

  private Object notInitialized;

  @SuppressWarnings("initialization.fields.uninitialized")
  private Object notInitializedButSuppressed1;

  @SuppressWarnings("initialization")
  private Object notInitializedButSuppressed2;

  private Object initialized1;

  private Object initialized2 = new Object();

  {
    initialized1 = new Object();
  }

}
