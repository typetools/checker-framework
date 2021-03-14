public class Issue3001 {

  private <T> T getMember(Class<T> type) {
    T sym = getMember(type);
    return sym;
  }
}
