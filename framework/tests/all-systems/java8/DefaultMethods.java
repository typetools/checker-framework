
interface DefaultMethods {

    default String method(String s) {
        return s.toString();
    }
}

interface DefaultMethods2 extends DefaultMethods {

    @Override
    default String method(String s) {
        return s;
    }
}