class Crash {
    void crash(Sub o) {
        Sub.SubInner<?> x = o.a().b().b();
        o.a().b().b().c();
    }
}
