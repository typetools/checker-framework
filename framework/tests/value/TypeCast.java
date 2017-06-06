import org.checkerframework.common.value.qual.*;

class TypeCast {

    public void charIntDoubleTest() {
        int a = 98;
        long b = 98;
        double c = 98.0;
        float d = 98.0f;
        char e = 'b';
        short f = 98;
        byte g = 98;

        @IntVal({'b'}) char h = (char) a;
        h = (char) b;
        //:: warning: (cast.unsafe)
        h = (char) c;
        //:: warning: (cast.unsafe)
        h = (char) d;
        h = (char) f;
        h = (char) g;

        @IntVal({98}) int i = (int) b;
        //:: warning: (cast.unsafe)
        i = (int) c;
        //:: warning: (cast.unsafe)
        i = (int) d;
        i = (int) e;
        i = (int) f;
        i = (int) g;

        @DoubleVal({98.0}) double j = (double) a;
        j = (double) b;
        j = (double) d;
        j = (double) e;
        j = (double) f;
        j = (double) g;
    }

    void otherCast() {

        byte[] b = (byte[]) null;
        @BoolVal(true) boolean bool = (boolean) true;
    }

    void rangeCast(@IntRange(from = 127, to = 128) int a, @IntRange(from = 128, to = 129) int b) {
        @IntRange(from = 0, to = 128)
        //:: error: (assignment.type.incompatible) :: warning: (cast.unsafe)
        byte c = (byte) a;
        // (byte) a is @IntRange(from = -128, to = 127) because of casting

        @IntRange(from = -128, to = -127)
        //:: warning: (cast.unsafe)
        byte d = (byte) b;
    }
}
