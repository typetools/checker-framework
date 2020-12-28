public class Issue3055 {

    class C1<T extends C1<T>.Bound> {
        class Bound {}
    }

    class C2<T extends C2.Bound> {
        class Bound {}
    }

    class C3<T extends C3<?>.Bound> {
        class Bound {}
    }
}
