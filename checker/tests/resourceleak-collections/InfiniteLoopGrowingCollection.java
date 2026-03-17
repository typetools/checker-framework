// import java.io.IOException;
// import java.nio.channels.SocketChannel;
// import java.util.ArrayList;
// import java.util.List;
//
// import org.checkerframework.checker.calledmethods.qual.*;
// import org.checkerframework.checker.collectionownership.qual.*;
// import org.checkerframework.checker.mustcall.qual.*;
//
// class InfiniteLoopGrowingCollection {
//
//  void serverLoop() {
//    @OwningCollection List<SocketChannel> socketChannelList = new ArrayList<>();
//
//    while (true) {
//      SocketChannel sc = null;
//      try {
//        // Something that creates an owning resource repeatedly
//        sc = SocketChannel.open();
//        sc.configureBlocking(false);
//      } catch (IOException e) {
//        // swallow: no checked exceptional exit from the method
//      }
//
//      if (sc != null) {
//        // :: error: collection.obligation.never.enforced
//        socketChannelList.add(sc);
//      }
//    }
//  }
// }

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class InfiniteLoopGrowingCollection {

  public static void main(String[] args) throws IOException {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(8080));
    @OwningCollection List<SocketChannel> socketChannelList = new ArrayList<>();
    byte[] bytes = new byte[1024];
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    serverSocketChannel.configureBlocking(false);
    while (true) {
      SocketChannel socketChannel = serverSocketChannel.accept();
      if (socketChannel == null) {
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        System.out.println("无人连接");
        for (SocketChannel item : socketChannelList) {
          int len = item.read(byteBuffer);
          if (len > 0) {
            byteBuffer.flip();
            System.out.println("读取到的数据" + new String(byteBuffer.array(), 0, len));
          }
          byteBuffer.clear();
        }
      } else {
        socketChannel.configureBlocking(false);
        // :: error: collection.obligation.never.enforced
        socketChannelList.add(socketChannel);
        for (SocketChannel item : socketChannelList) {
          int len = item.read(byteBuffer);
          if (len > 0) {
            byteBuffer.flip();
            System.out.println("读取到的数据" + new String(byteBuffer.array(), 0, len));
          }
          byteBuffer.clear();
        }
      }
    }
  }
}
