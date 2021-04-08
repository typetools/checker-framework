import org.checkerframework.checker.signedness.qual.*;

public class ShiftPropogation {

  public void ShiftOperationTests(@Unsigned int unsigned, @Signed int signed) {
    @Unsigned int uur = unsigned >>> unsigned;
    @Unsigned int usr = unsigned >>> signed;

    @Signed int sur = signed >> unsigned;
    @Signed int ssr = signed >> signed;

    @Unsigned int uul = unsigned << unsigned;
    @Unsigned int usl = unsigned << signed;
    @Signed int sul = signed << unsigned;
    @Signed int ssl = signed << signed;
  }

  public void ShiftAssignmentTests(@Unsigned int unsigned, @Signed int signed) {
    @Unsigned int uur = unsigned >>>= unsigned;
    @Unsigned int usr = unsigned >>>= signed;

    @Signed int sur = signed >>= unsigned;
    @Signed int ssr = signed >>= signed;

    @Unsigned int uul = unsigned <<= unsigned;
    @Unsigned int usl = unsigned <<= signed;
    @Signed int sul = signed <<= unsigned;
    @Signed int ssl = signed <<= signed;
  }
}
