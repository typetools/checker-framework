// A path that leaves a disposal loop by an exception does not fulfill the collection's obligation;
// the loop still fulfills it on the loop's normal exit.

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class DisposalLoopExceptionalExit {

  void closeAll(@OwningCollection List<Socket> list) throws IOException {
    for (Socket s : list) {
      s.close();
    }
  }

  void closeAgainInHandler(@OwningCollection List<Socket> list) {
    try {
      for (Socket s : list) {
        s.close();
      }
    } catch (IOException e) {
      for (Socket s : list) {
        try {
          s.close();
        } catch (IOException e2) {
        }
      }
    }
  }

  // :: error: unfulfilled.collection.obligations
  void ignoreException(@OwningCollection List<Socket> list) {
    try {
      for (Socket s : list) {
        s.close();
      }
    } catch (IOException e) {
    }
  }

  // If closeAll throws, this method still owns the list.
  void callerPropagates() throws IOException {
    List<Socket> list = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new Socket());
    closeAll(list);
  }
}
