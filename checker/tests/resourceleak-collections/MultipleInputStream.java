import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class MultipleInputStream extends InputStream {

  private final List<InputStream> streams = new LinkedList<>();

  private InputStream currentInputStream;

  private int currentIndex;

  public MultipleInputStream(@OwningCollection Collection<InputStream> streams) {
    if (streams.size() == 0) {
      throw new IllegalArgumentException("At least one stream is required");
    }
    this.streams.addAll(streams);
    incrementCurrent();
  }

  public MultipleInputStream(InputStream... streams) {
    if (streams.length == 0) {
      throw new IllegalArgumentException("At least one stream is required");
    }
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

  @NotOwning
  Resource nonOwn() {
    return null;
  }

  Resource[] test() {

    @NotOwningCollection List<Resource> col = new ArrayList<>();
    col.add(nonOwn());
    Resource[] streamsWithField = new Resource[col.size()];
    streamsWithField = col.toArray(streamsWithField);
    return streamsWithField;
  }
}
