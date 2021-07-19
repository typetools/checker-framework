interface Issue3884 {
    String go(Kind kind);

    Issue3884 FOO =
            kind -> {
                switch (kind) {
                    case A:
                        break;
                }
                return "";
            };

    enum Kind {
        A,
        B,
        C;
    }
}
