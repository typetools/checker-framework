// Based on a Zookeeper false positive that requires unconnected socket support.

import java.io.IOException;
import java.nio.channels.SocketChannel;

class ZookeeperReport6 {
  SocketChannel createSock() throws IOException {
    SocketChannel sock;
    sock = SocketChannel.open();
    sock.configureBlocking(false);
    sock.socket().setSoLinger(false, -1);
    sock.socket().setTcpNoDelay(true);
    return sock;
  }
}
