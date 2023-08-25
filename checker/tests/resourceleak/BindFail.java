import java.net.*;

public class BindFail {
  public void testtttt() throws Exception {
    // :: error: required.method.not.called
    Socket sssss = new Socket();
    //    SocketAddress addr = new InetSocketAddress("127.0.0.1", 6010);
    //    try {
    //      sssss.bind(addr);
    //    } catch(Exception e) {
    //      // socket might still be open on this path
    //      return;
    //    }
    //    sssss.close();
  }
}
