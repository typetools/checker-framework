
class Lambda {

    void context() {
        //:: warning: (lambda.unimplemented)
        FuncParam<String, String> func = in -> {
            return  in.toString();
        };

        FuncParam<String, String> func2 = in -> in.toString();
    }

    interface FuncParam<T, V> {
        V method(T b);
    }
}

