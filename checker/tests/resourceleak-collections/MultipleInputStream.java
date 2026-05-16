import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

// Tests constructor paths that transfer resource collections through List.addAll.
// TODO: Fix arguemnt errors for List.addAll call.
public class MultipleInputStream extends InputStream {

  private final List<InputStream> streams = new LinkedList<>();

  private InputStream currentInputStream;

  private int currentIndex;

  // ::error: unfulfilled.collection.obligations
  public MultipleInputStream(@OwningCollection Collection<InputStream> streams) {
    if (streams.size() == 0) {
      throw new IllegalArgumentException("At least one stream is required");
    }
    // ::error: argument
    this.streams.addAll(streams);
    incrementCurrent();
  }

  public MultipleInputStream(InputStream... streams) {
    if (streams.length == 0) {
      throw new IllegalArgumentException("At least one stream is required");
    }
    // ::error: argument
    this.streams.addAll(Arrays.asList(streams));
    incrementCurrent();
  }

  public int read() throws IOException {
    throw new java.lang.Error();
  }

  private boolean incrementCurrent() {
    if (++currentIndex >= streams.size()) {
      return false;
    }
    currentInputStream = streams.get(currentIndex);
    return true;
  }

  @CollectionFieldDestructor("this.streams")
  public void close() throws IOException {
    for (InputStream i : streams) {
      try {
        i.close();
      } catch (IOException e) {
      }
    }
  }
}
