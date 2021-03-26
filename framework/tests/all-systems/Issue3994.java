public class Issue3994 {
  interface MyInterface {}

  interface OkRecursive<T extends OkRecursive> {}

  interface Recursive<T extends MyInterface & Recursive> {}
}
