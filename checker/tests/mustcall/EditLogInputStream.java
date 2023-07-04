import java.io.*;
import java.util.*;

abstract class EditLogInputStream implements Closeable {
  public abstract boolean isLocalLog();
}

interface JournalSet extends Closeable {
  static final Comparator<? extends EditLogInputStream> LOCAL_LOG_PREFERENCE_COMPARATOR =
      // This is an undesirable false positive that occurs because of the defaulting
      // that the Must Call Checker uses for generics.
      // :: error: type.argument
      Comparator.comparing(EditLogInputStream::isLocalLog).reversed();
}
