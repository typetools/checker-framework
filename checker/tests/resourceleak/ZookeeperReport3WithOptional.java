// Based on a Zookeeper false positive that requires unconnected socket support
// and support for java.util.Optional.

// @skip-test until Optional is supported. For now, users should use null instead.

import java.io.*;
import java.net.*;
import java.util.Optional;
import org.checkerframework.checker.mustcall.qual.*;

class ZookeeperReport3WithOptional {

  // This is a simpler version of case 3.
  Optional<ServerSocket> createServerSocket_easy(
      InetSocketAddress address, boolean portUnification, boolean sslQuorum) {
    ServerSocket serverSocket;
    try {
      serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
      serverSocket.bind(address);
      return Optional.of(serverSocket);
    } catch (IOException e) {
      System.err.println("Couldn't bind to " + address.toString() + e);
    }
    return Optional.empty();
  }

  Optional<ServerSocket> createServerSocket(
      InetSocketAddress address, boolean portUnification, boolean sslQuorum) {
    ServerSocket serverSocket;
    try {
      if (portUnification || sslQuorum) {
        serverSocket = new UnifiedServerSocket(portUnification);
      } else {
        serverSocket = new ServerSocket();
      }
      serverSocket.setReuseAddress(true);
      serverSocket.bind(address);
      return Optional.of(serverSocket);
    } catch (IOException e) {
      System.err.println("Couldn't bind to " + address.toString() + e);
    }
    return Optional.empty();
  }

  class UnifiedServerSocket extends ServerSocket {
    // A human has to verify that this constructor actually does produce an unconnected socket.
    public @MustCall({}) UnifiedServerSocket(boolean b) throws IOException {
      super();
    }
  }
}
