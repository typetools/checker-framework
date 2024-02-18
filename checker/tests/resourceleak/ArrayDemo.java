import java.io.IOException;

class ArrayDemo {

  public static void main(String[] args) throws IOException {
    // public static void main(String[] args) {
    @OwningArray Socket[] sockets = new Socket[5];
    sockets[0] = new Socket(InetAddress.getByName("server_ip_address"), 42);
    for (int i = 0; i < sockets.length; i++) {
      sockets[i] = new Socket(InetAddress.getByName("server_ip_address"), 42);
    }
    for (int i = 0; i < 5; i++) {
      sockets[i].close();
    }
  }
}
