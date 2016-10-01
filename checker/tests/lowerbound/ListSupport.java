import java.util.*;

class ListSupport {

    void testGet() {

        List<Integer> list = new ArrayList<Integer>();
        int i = -1;
        int j = 0;

        //try and use a negative to get, should fail
        //:: error: (argument.type.incompatible)
        Integer m = list.get(i);

        //try and use a nonnegative, should work
        Integer l = list.get(j);
    }
}
//a comment
