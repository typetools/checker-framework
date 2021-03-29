import java.io.*;
import java.util.*;

abstract class EditLogInputStream implements Closeable {
  public abstract boolean isLocalLog();
}

interface JournalSet extends Closeable {
  static final Comparator<EditLogInputStream> LOCAL_LOG_PREFERENCE_COMPARATOR =
      Comparator.comparing(EditLogInputStream::isLocalLog).reversed();
}
