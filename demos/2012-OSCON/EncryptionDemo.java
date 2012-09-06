package encrypted;

import encrypted.quals.*;

public class EncryptionDemo {
    void send(@Encrypted String s) {}

    void calls(@Encrypted String arg) {
        send("nono");
        send(arg);
    }
}
