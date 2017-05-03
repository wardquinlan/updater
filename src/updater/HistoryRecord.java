package updater;

import java.text.DecimalFormat;

public class HistoryRecord {
  private DecimalFormat df4;
  private DecimalFormat df2;
  private DecimalFormat dfD;
  private int year;
  private int month;
  private int day;
  private double open;
  private double high;
  private double low;
  private double close;
  private double volume;

  public HistoryRecord() {
    df4 = new DecimalFormat("0000");
    df2 = new DecimalFormat("00");
    dfD = new DecimalFormat(".0000");
    year   = 0;
    month  = 0;
    day    = 0;
    open   = 0;
    high   = 0;
    low    = 0;
    close  = 0;
    volume = 0;
  }

  public String toYmd() {
    return df4.format(year) + df2.format(month) + df2.format(day);
  }

  public String toString() {
    return toYmd() + "," +
           dfD.format(open)  + "," +
           dfD.format(high)  + "," +
           dfD.format(low)   + "," +
           dfD.format(close) + "," +
           dfD.format(volume);
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public int getMonth() {
    return month;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public int getDay() {
    return day;
  }

  public void setDay(int day) {
    this.day = day;
  }

  public double getOpen() {
    return open;
  }

  public void setOpen(double open) {
    this.open = open;
  }

  public double getHigh() {
    return high;
  }

  public void setHigh(double high) {
    this.high = high;
  }

  public double getLow() {
    return low;
  }

  public void setLow(double low) {
    this.low = low;
  }

  public double getClose() {
    return close;
  }

  public void setClose(double close) {
    this.close = close;
  }

  public double getVolume() {
    return volume;
  }

  public void setVolume(double volume) {
    this.volume = volume;
  }
}
