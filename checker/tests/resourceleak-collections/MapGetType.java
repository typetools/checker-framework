import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class ChannelManager {

  private Map<String, FileChannel> name2channel;

  @EnsuresCalledMethods(value = "#1", methods = "close")
  public void release(@Owning FileChannel chan) {
    try {
      chan.close();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void releaseAll(String prefix) {
    @OwningCollection List<FileChannel> x = new ArrayList<>();
    for (String fn : name2channel.keySet()) {
      if (prefix == null || fn.startsWith(prefix)) {
        x.add(name2channel.get(fn));
      }
    }
    for (FileChannel chan : x) release(chan);
  }
}
