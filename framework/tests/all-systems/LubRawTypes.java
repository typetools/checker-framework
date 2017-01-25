@SuppressWarnings("unchecked")
public class LubRawTypes {
    public static boolean flag = false;

    class MyGen<T> {}

    MyGen<MyGen<MyGen<String>>> test(MyGen myGen1, MyGen myGen2) {
        return flag ? myGen1 : myGen2;
    }
}
