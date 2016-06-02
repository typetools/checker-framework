import org.checkerframework.common.value.qual.*;

class Unaries {

    public void complement() {
        boolean a = false;
        @BoolVal({ true }) boolean b = !a;

        @IntVal({ -5 }) int c = ~4;

        @IntVal({ -123456789 }) long d = ~123456788;
    }

    public void prefix() {
        byte a = 1;
        @IntVal({ 2 }) byte b = ++a;

        @IntVal({ 3 }) short c = ++a;

        @IntVal({ 4 }) int d = ++a;

        @IntVal({ 5 }) long e = ++a;
        ++a;
        e = --a;
        d = --a;
        c = --a;
        b = --a;
    }

    public void postfix() {
        int a = 0;
        @IntVal({ 0 }) int b = a++;
        @IntVal({ 1 }) int c = a--;
        b = a++;

        @IntVal({ 1 }) long d = a--;

        double e = 0.25;
        @DoubleVal({ 0.25 }) double f = e++;
        @DoubleVal({ 1.25 }) double g = e--;
        f = e;

    }

    public void plusminus() {
        @IntVal({ 48 }) int a = +48;
        @IntVal({ -49 }) int b = -49;

        @IntVal({ 34 }) long c = +34;
        @IntVal({ -34 }) long d = -34;
    }
}
