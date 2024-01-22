// Test case for https://github.com/typetools/checker-framework/issues/5739 .

import java.io.IOException;
import java.net.*;

class ConnectingSockets2 {

  void run(InetSocketAddress isa) {
    try (Socket serverSocket = new Socket()) {
      serverSocket.close();
      serverSocket.connect(isa);
    } catch (IOException e) {
      // do nothing
    }
  }
}
