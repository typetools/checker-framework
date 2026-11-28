// A `while` disposal loop whose disposal method throws a checked exception is still a disposal
// loop:  if the loop exits normally, every element has been closed.

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import org.checkerframework.checker.collectionownership.qual.*;

class WhileDisposalLoopCloseThrows {

  void iteratorLoop(@OwningCollection List<Socket> list) throws IOException {
    Iterator<Socket> it = list.iterator();
    while (it.hasNext()) {
      Socket s = it.next();
      s.close();
    }
  }

  void drainLoop(@OwningCollection Queue<Socket> queue) throws IOException {
    while (!queue.isEmpty()) {
      Socket s = queue.poll();
      if (s != null) {
        s.close();
      }
    }
  }
}
