import java.net.Socket;

class PaperExample {
    void test(String myHost, int myPort) throws Exception {
        Socket s = null;
        try {
            s = new Socket(myHost, myPort); /* 1 */
        } catch (Exception e) {
        } finally {
            if (s != null) {
                /* 2 */
                s.close();
            }
        }
    }
}
