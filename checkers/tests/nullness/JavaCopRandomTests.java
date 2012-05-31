import checkers.nullness.quals.*;

class RandomTests {
    final int a;
    final int b = 1;
    final int c;

    RandomTests(){
        String s = null;
        a = 2;
    }

    RandomTests(String s) throws Exception{
        //this();
        a = 2;
        if (a > 1){
            throw new Exception("dude");
        }
        throw new RuntimeException("dude");
    }

    // initializer block
    {
        c = 4;
        //throw new Exception("dude");
    }

}
