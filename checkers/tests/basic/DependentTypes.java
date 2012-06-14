import checkers.quals.*;
import checkers.nullness.quals.*;

import tests.util.SuperQual;
import tests.util.SubQual;

// @skip-test
public class DependentTypes {

  @SuperQual Object supero = null;
  // A trivial error, so there is at least one in the output.
  //:: error: (type.incompatible)
  @SubQual Object subo = supero;


  // This signature is a type error.  A client of the supertype (SuperQual)
  // can set publicDepSubQual to null, but a client of the subtype cannot.
  //:: error: (type.incompatible)
  public @NonNull @Dependent(result = Nullable.class, when=SuperQual.class) String publicDepSubQual;
  // This signature is a type error.  A client of the supertype need not
  // handle null as a value of publicDepSuperQual, but the subtype might
  // contain that.
  //:: error: (type.incompatible)
  public @Nullable @Dependent(result = NonNull.class, when=SuperQual.class) String publicDepSuperQual;


  private @NonNull @Dependent(result = Nullable.class, when=SuperQual.class) String privateDepSubQual;
  private @Nullable @Dependent(result = NonNull.class, when=SuperQual.class) String privateDepSuperQual;

  public @NonNull @Dependent(result = Nullable.class, when=SuperQual.class) String getDepSubQual() {
    return privateDepSubQual;
  }
  public @NonNull String getDepSubQualOTHERGOOD() {
    return privateDepSubQual;
  }
  // This signature is a type error.  Clients of the supertype
  // (SuperQual) can pass in null, but clients of the subtype cannot.
  //:: error: (type.incompatible)
  public void setDepSubQualBAD(@NonNull @Dependent(result = Nullable.class, when=SuperQual.class) String s) {
    privateDepSubQual = s;
  }
  public void setDepSubQualGOOD1(@NonNull String s) {
    privateDepSubQual = s;
  }
  public void setDepSubQualGOOD2(@SuperQual DependentTypes this, @Nullable String s) {
    privateDepSubQual = s;
  }

  // This signature is a type error.  Clients of the supertype need not
  // handle null as a return value, but the subtype may return null.
  //:: error: (type.incompatible)
  public @Nullable @Dependent(result = NonNull.class, when=SuperQual.class) String getDepSuperQualBAD() {
    return privateDepSuperQual;
  }
  public @Nullable String getDepSuperQualGOOD1() {
    return privateDepSuperQual;
  }
  public @NonNull String getDepSuperQualGOOD2(@SuperQual DependentTypes this) {
    return privateDepSuperQual;
  }
  public void setDepSuperQual(@Nullable @Dependent(result = NonNull.class, when=SuperQual.class) String s) {
    privateDepSuperQual = s;
  }
  public void setDepSuperQualOTHERGOOD(@SuperQual DependentTypes this, @NonNull String s) {
    privateDepSuperQual = s;
  }

}
