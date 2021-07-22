import java.net.*;

class CommonModuleCrash {
    Socket bar = new Socket();

    static void baz(Socket s) {}

    static {
        baz(new Socket());
    }
}
