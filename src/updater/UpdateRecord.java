package updater;

import java.util.Properties;
import java.text.DecimalFormat;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class UpdateRecord {
  // logger
  private static Log log = LogFactory.getFactory().getInstance(
                           UpdateRecord.class);

  private DecimalFormat df4;
  private DecimalFormat df2;
  private DecimalFormat dfD;
  private String ticker;
  private int year;
  private int month;
  private int day;
  private double open;
  private double high;
  private double low;
  private double close;
  private double volume;
  private Properties props;

  public UpdateRecord(Properties props) {
    df4 = new DecimalFormat("0000");
    df2 = new DecimalFormat("00");
    dfD = new DecimalFormat(".0000");
    ticker = Updater.EMPTY;
    year   = 0;
    month  = 0;
    day    = 0;
    open   = 0;
    high   = 0;
    low    = 0;
    close  = 0;
    volume = 0;
    this.props = props;
  }

  public boolean checkIntegrity() {
    boolean force = "true".equals(props.getProperty("updater.force"));

    if (year == 0) {
      log.warn("failed integrity check: year = 0");
      return false;
    }
    if (month == 0) {
      log.warn("failed integrity check: month = 0");
      return false;
    }
    if (day == 0) {
      log.warn("failed integrity check: day = 0");
      return false;
    }
    if (high < low) {
      log.warn("failed integrity check: " + ticker + 
               ": high < low");
      return false;
    }
    if (open < low || open > high) {
      log.warn("failed integrity check: " + ticker + 
               ": open not in [low..high]");
      if (force) {
        log.warn("force option set; auto-correcting");
        if (low > open) {
          low = open;
        }
        if (high < open) {
          high = open;
        }
      } else {
        return false;
      }
    }
    if (close < low || close > high) {
      log.warn("failed integrity check: " + ticker + 
               ": close not in [low..high]");
      if (force) {
        log.warn("force option set; auto-correcting");
        if (low > close) {
          low = close;
        }
        if (high < close) {
          high = close;
        }
      } else {
        return false;
      }
    }
    return true;
  }

  public String toYmd() {
    return df4.format(year) + df2.format(month) + df2.format(day);
  }

  public String toHistory() {
    return toYmd() + "," +
           dfD.format(open)  + "," +
           dfD.format(high)  + "," +
           dfD.format(low)   + "," +
           dfD.format(close) + "," +
           dfD.format(volume);
  }

  public String toString() {
    return ticker + "," + toHistory();
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker.toUpperCase();
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
