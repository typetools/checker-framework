import org.checkerframework.common.aliasing.qual.Unique;

@Unique class Data {
    @SuppressWarnings("unique.leaked")
    Data() {} // All objects of Data are now @Unique
}

class Demo {
    void check(Data p) { // p is @Unique Data Object
        // :: error: (unique.leaked)
        Data y = p; // @Unique p is leaked
    }
}
