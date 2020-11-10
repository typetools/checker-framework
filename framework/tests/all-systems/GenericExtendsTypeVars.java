/*
 * Sets up recursive bounds where the bounds themselves are type variables.
 */

interface MMyList<LL> {}

interface MMyMap<KEY, VALUE> {}

class Tester<EE extends TT, TT extends MMyList<EE>> {}

class WithWildcard<ZZ extends QQ, QQ extends YY, YY extends MMyMap<QQ, ZZ>> {
    void context() {
        ZZ zz = null;
        QQ qq = null;
        YY yy = null;
    }
}

class Test<KK extends FF, FF extends MMyMap<KK, KK>> {
    KK kk;
    FF ff;

    Test(KK kk, FF ff) {
        this.kk = kk;
        this.ff = ff;
    }
}

class RecursiveTypevarClass<T extends RecursiveTypevarClass<T>> {
    T t;

    RecursiveTypevarClass(T t) {
        this.t = t;
    }
}

class RecursiveImplements implements MMyList<RecursiveImplements> {}
