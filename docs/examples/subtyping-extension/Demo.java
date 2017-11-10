import java.util.LinkedList;
import java.util.List;
import qual.Encrypted;

abstract class EncryptionDemo {

    public @Encrypted String encrypt(String text) {
        byte[] b = text.getBytes();
        for (int i = 0; i < b.length; b[i++]++) {
            // side effect is in increment expression of for loop
        }
        // :: warning: (cast.unsafe)
        return (@Encrypted String) new String(b);
    }

    // Only send encrypted data!
    abstract void sendOverTheInternet(@Encrypted String msg);

    void sendText() {
        @Encrypted String s = encrypt("foo"); // valid
        sendOverTheInternet(s); // valid

        String t = encrypt("bar"); // valid (subtype)
        sendOverTheInternet(t); // valid (flow)

        List<@Encrypted String> lst = new LinkedList<@Encrypted String>();
        lst.add(s);
        lst.add(t);

        for (String str : lst) // valid
        sendOverTheInternet(str);
    }

    void sendPassword() {
        String password = "unencrypted";
        sendOverTheInternet(password); // invalid
    }
}
