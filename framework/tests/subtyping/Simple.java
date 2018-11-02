import java.util.LinkedList;
import java.util.List;
import testlib.util.Encrypted;

abstract class BasicFunctionality {

    @Encrypted String encrypt(String s) {
        byte[] b = s.getBytes();
        for (int i = 0; i < b.length; b[i++]++) ;
        // :: warning: (cast.unsafe)
        return (@Encrypted String) new String(b);
    }

    abstract void sendOverTheInternet(@Encrypted String s);

    void test() {
        @Encrypted String s = encrypt("foo"); // valid
        sendOverTheInternet(s); // valid

        String t = encrypt("bar"); // valid (subtype)
        sendOverTheInternet(t); // valid (flow)

        List<@Encrypted String> lst = new LinkedList<>();
        lst.add(s);
        lst.add(t);

        for (@Encrypted String str : lst) {
            sendOverTheInternet(str);
        }

        //        for (String str : lst)
        //            sendOverTheInternet(str);           // should be valid!
    }
}
