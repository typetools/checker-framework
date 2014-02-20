import checkers.value.quals.*;

class Binaries {

    public void add() {
        int a = 1;
        if (true) {
            a = 2;
        }
        @IntVal({ 3, 4 }) int b = a + 2;

        double c = 1.0;
        if (true) {
            c = 2.0;
        }
        @DoubleVal({ 3.0, 4.0 }) double d = c + 2;

        char e = '1';
        if (true) {
            e = '2';
        }
        //:: warning: (cast.unsafe)
        @CharVal({ '3', '4' }) char f = (char) (e + 2);

        String g = "A";
        if (true) {
            g = "B";
        }
        @StringVal({ "AC", "BC" }) String h = g + "C";

    }

    public void subtract() {
        int a = 1;
        if (true) {
            a = 2;
        }
        @IntVal({ -1, 0 }) int b = a - 2;

        double c = 1.0;
        if (true) {
            c = 2.0;
        }
        @DoubleVal({ -1.0, 0.0 }) double d = c - 2;

        char e = '2';
        if (true) {
            e = '3';
        }
        //:: warning: (cast.unsafe)
        @CharVal({ '0', '1' }) char f = (char) (e - 2);

    }

    public void multiply() {
        int a = 1;
        if (true) {
            a = 2;
        }
        @IntVal({ 2, 4 }) int b = a * 2;

        double c = 1.0;
        if (true) {
            c = 2.0;
        }
        @DoubleVal({ 2.0, 4.0 }) double d = (double) (c * 2);

        //:: warning: (cast.unsafe)
        char e = (char) 25;
        if (true) {
            //:: warning: (cast.unsafe)
            e = (char) 26;
        }
        //:: warning: (cast.unsafe)
        @CharVal({ '2', '4' }) char f = (char) (e * 2);

    }

    public void divide() {
        int a = 2;
        if (true) {
            a = 4;
        }
        @IntVal({ 1, 2 }) int b = a / 2;

        double c = 1.0;
        if (true) {
            c = 2.0;
        }
        @DoubleVal({ 0.5, 1.0 }) double d = c / 2;

        //:: warning: (cast.unsafe)
        char e = (char) 96;
        if (true) {
            //:: warning: (cast.unsafe)
            e = (char) 98;
        }
        //:: warning: (cast.unsafe)
        @CharVal({ '0', '1' }) char f = (char) (e / 2);

    }

    public void remainder() {
        int a = 4;
        if (true) {
            a = 5;
        }
        @IntVal({ 1, 2 }) int b = a % 3;

        double c = 4.0;
        if (true) {
            c = 5.0;
        }
        @DoubleVal({ 1.0, 2.0 }) double d = c % 3;

        //:: warning: (cast.unsafe)
        char e = (char) 98;
        if (true) {
            //:: warning: (cast.unsafe)
            e = (char) 99;
        }
        //:: warning: (cast.unsafe)
        @CharVal({ '0', '1' }) char f = (char) (e % 50);

    }

    public void and() {
        boolean a = true;
        if (true) {
            a = false;
        }
        @BoolVal({ true, false }) boolean b = a & true;

        int c = 4;
        if (true) {
            c = 5;
        }
        @IntVal({ 0, 1 }) int d = c & 3;

        //:: warning: (cast.unsafe)
        char e = (char) 48;
        if (true) {
            //:: warning: (cast.unsafe)
            e = (char) 51;
        }
        //:: warning: (cast.unsafe)
        @CharVal({ '0', '2' }) char f = (char) (e & 50);
    }

    public void or() {
        boolean a = true;
        if (true) {
            a = false;
        }
        @BoolVal({ true, false }) boolean b = a | true;

        int c = 4;
        if (true) {
            c = 5;
        }
        @IntVal({ 7 }) int d = c | 3;

        //:: warning: (cast.unsafe)
        char e = (char) 48;
        if (true) {
            //:: warning: (cast.unsafe)
            e = (char) 51;
        }
        //:: warning: (cast.unsafe)
        @CharVal({ '1', '3' }) char f = (char) (e | 1);
    }

    public void xor() {
        boolean a = true;
        if (true) {
            a = false;
        }
        @BoolVal({ true, false }) boolean b = a ^ true;

        int c = 4;
        if (true) {
            c = 5;
        }
        @IntVal({ 7, 6 }) int d = c ^ 3;

        //:: warning: (cast.unsafe)
        char e = (char) 48;
        if (true) {
            //:: warning: (cast.unsafe)
            e = (char) 51;
        }
        //:: warning: (cast.unsafe)
        @CharVal({ '1', '2' }) char f = (char) (e ^ 1);
    }

    public void boolAnd() {
        @BoolVal({ false }) boolean a = true && false;
        @BoolVal({ true }) boolean b = false || true;

    }

    public void conditionals() {
        @BoolVal({ false }) boolean a = 1.0f == '1';
        @BoolVal({ true }) boolean b = 1 != 2.0;
        @BoolVal({ true }) boolean c = 1 > 0.5;
        @BoolVal({ true }) boolean d = 1 >= 1.0;
        @BoolVal({ true }) boolean e = 1 < 1.1f;
        //:: warning: (cast.unsafe)
        @BoolVal({ true }) boolean f = (char) 2 <= 2.0;
    }

    public void shifts() {
        int a = -8;
        if (true) {
            a = 4;
        }
        @IntVal({ 1, -2 }) int b = a >> 2;

        int c = 1;
        if (true) {
            c = 2;
        }
        @IntVal({ 4, 8 }) int d = c << 2;

        int e = -8;
        if (true) {
            e = 4;
        }
        @IntVal({ Integer.MAX_VALUE / 2 - 1, 1 }) int f = e >>> 2;

        //:: warning: (cast.unsafe)
        char g = (char) 24;
        if (true) {
            //:: warning: (cast.unsafe)
            g = (char) 25;
        }
        //:: warning: (cast.unsafe)
        @CharVal({ '0', '2' }) char h = (char) (g << 1);
    }

    public void chains() {
        char a = 2;
        int b = 3;
        double c = 5;

        @DoubleVal({ 1 }) double d = a * b - c;
        //:: warning: (cast.unsafe)
        @DoubleVal({ 3 }) double e = a * c - 2 * b - (char) 1;
    }
}