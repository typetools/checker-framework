import java.lang.SuppressWarnings;

// See gist: https://gist.github.com/JonathanBurke/6c1c1c28161a451611ad
// for more information on what was going wrong here
class Issue457<T extends Number> {
   void callMe(T t) {
   }
   
   @SuppressWarnings({"unused", "javari"})
   public void f(T t) {
      final T obj = t;
      
      Float objFloat = (obj instanceof Float) ? (Float) obj : null;

      callMe(obj);
   }
}