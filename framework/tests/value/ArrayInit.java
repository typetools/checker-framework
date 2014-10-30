import org.checkerframework.common.value.qual.*;

class ArrayInit {

    public void numberInit() {
        int @ArrayLen({1})[] a = new int[1];
    }

    public void listInit() {
        int @ArrayLen({1})[] a = new int[]{4};
    }

    public void varInit() {
        int i = 1;
        int @ArrayLen({1})[] a = new int[i];
    }

    public void multiDim() {
        int i = 2;
        int j = 3;
        int @ArrayLen({2})[] @ArrayLen({3})[] a = new int[2][3];
        int @ArrayLen({2})[] @ArrayLen({3})[] b = new int[i][j];
    }
    
    public void initilizer(){
        int @ArrayLen(3) [] ints = new int[]{2,2,2};
        byte @StringVal("d%")[] bytes = new byte[]{100,'%'};
        char @StringVal("-A%")[] chars = new char[]{45,'A','%'};
    }
}