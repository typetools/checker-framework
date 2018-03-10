public class NullnessExample {
    public static void main(String[] args) {
        Object myObject = null;

        if (args.length > 2) {
            myObject = new Object();
        }
        System.out.println(myObject.toString());
    }
}
