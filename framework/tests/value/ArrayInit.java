import org.checkerframework.common.value.qual.*;

class ArrayInit {
    public void raggedArrays() {
        int @ArrayLen(4) []@ArrayLen({1,3,4})[]@ArrayLen({1,2,3,4,7})[] alpha = new int[][][]{
                {{1,1},{1,1,1},{1},{1}},
                {{1},{1},{1},{1,1}},
                {{1,2,3,4,5,6,7}},
                {{1},{1},{1,1,1,1}}
                };

        int @ArrayLen(4)[] @ArrayLen({1,2,3})[] a = {{1,1},{1,1,1},{1},{1}};
        int @ArrayLen(4)[] @ArrayLen({1,2})[] b =  {{1},{1},{1},{1,1}};
        int @ArrayLen(1)[] @ArrayLen(7)[] c = {{1,2,3,4,5,6,7}};
        int @ArrayLen(3)[] @ArrayLen({1,4})[] d =  {{1},{1},{1,1,1,1}};

        int @ArrayLen(4) []@ArrayLen({1,3,4})[]@ArrayLen({1,2,3,4,7})[] beta = {a,b,c,d};
    }

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

        int @ArrayLen({2}) [][] c = new int[][] {{2},{3}};
    }

    public void initilizer() {
        int @ArrayLen(3) [] ints = new int[]{2,2,2};
        byte @StringVal("d%")[] bytes = new byte[]{100,'%'};
        char @StringVal("-A%")[] chars = new char[]{45,'A','%'};
        int @ArrayLen(3) [] ints2 = {2,2,2};
    }
    public void vargsTest() {
        // type of arg should be @UnknownValue Object @BottomVal[]
        vargs((Object[])null);

        // type of arg should be @UnknownValue int @BottomVal[]
        vargs((int[]) null);

        // type of arg is, but maybe shouldn't be @UnknownVal byte @StringVal("null") []
        //:: error: (argument.type.incompatible)
        vargs((byte[]) null);
    }
    public void vargs( Object @ArrayLen(0)... ints) {
    }
}
