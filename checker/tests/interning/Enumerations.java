public class Enumerations {

  // All enumeration instances are interned; there should be no need for an annotation.
  enum StudentYear {
    FRESHMAN,
    SOPHOMORE,
    JUNIOR,
    SENIOR;

    // check that receiver is OK
    @org.checkerframework.dataflow.qual.Pure
    public String toString() {
      return "StudentYear: ...";
    }
  }

  public boolean isSophomore(StudentYear sy) {
    return sy == StudentYear.SOPHOMORE;
  }

  public boolean flow(StudentYear s) {
    StudentYear m = StudentYear.SOPHOMORE;
    return s == m;
  }

  StudentYear cast(Object o) {
    return (StudentYear) o;
  }
}
