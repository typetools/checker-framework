
class Lambda {

    void context(Lambda c) {
        FuncParam<String, String> func = c::aMethod;
        FuncParam<Lambda, String> func2 = Lambda::aMethod;
        FuncParam<String, String> func3 = Lambda::staticMethod;
        Creator newLambda = Lambda::new;
    }

    void aMethod(String a, String b) { }

    void aMethod(String s) { }

    static void staticMethod(String a, String s) { }

    interface FuncParam<T, V> {
        void method(T a, V b);
    }

    interface Creator {
        Lambda create();
    }
}