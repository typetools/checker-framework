/*
 * Sets up recursive bounds where the bounds themselves are type variables.
 */

interface List<LL> {}
interface Map<KEY, VALUE> {}

class Tester<EE extends TT, TT extends List<EE>> {

}

class WithWildcard<ZZ extends QQ, QQ extends YY, YY extends Map<QQ, ZZ>> {
    void context() {
        ZZ zz = null;
        QQ qq = null;
        YY yy = null;
    }
}

@SuppressWarnings("initialization.fields.uninitialized")
class Test<KK extends FF, FF extends Map<KK, KK>> {
    KK kk;
    FF ff;
}

@SuppressWarnings("initialization.fields.uninitialized")
class RecursiveTypevarClass<T extends RecursiveTypevarClass<T>> {
    T t;
}

class RecursiveImplements implements List<RecursiveImplements> {

}