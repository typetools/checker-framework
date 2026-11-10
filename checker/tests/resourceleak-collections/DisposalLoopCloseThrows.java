// A disposal loop whose disposal method throws a checked exception is still a disposal loop:  if
// the loop exits normally, every element has been closed.

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class DisposalLoopCloseThrows {

  void enhancedFor(@OwningCollection List<Socket> list) throws IOException {
    for (Socket s : list) {
      s.close();
    }
  }
}
