import java.util.Date;

public class ArrayLengthLBC {

  public static Date[] add_date(Date[] dates, Date new_date) {
    Date[] new_dates = new Date[dates.length + 1];
    System.arraycopy(dates, 0, new_dates, 0, dates.length);
    new_dates[dates.length] = new_date;
    Date[] new_dates_cast = new_dates;
    return new_dates_cast;
  }
}
// a comment
