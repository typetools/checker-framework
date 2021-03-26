package android.os;

/** stub to avoid bringing in Android dependence */
public interface Parcelable {
  public interface Creator<T> {

    public T createFromParcel(Parcel source);

    public T[] newArray(int size);
  }

  public int describeContents();

  public void writeToParcel(Parcel dest, int flags);
}
