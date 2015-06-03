/*@skip-test*/
public class Issue345 {
    String f1;
    String f2;

    {
        f1 = f2;
        f2 = f1;
        f2.toString();   //Null pointer exception here
    }

    public static void main(String [] args) {
        Issue345 a = new Issue345();
    }
}