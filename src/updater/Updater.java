package updater;

import java.util.Properties;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class Updater {
  // logger
  private static Log log = LogFactory.getFactory().getInstance(Updater.class);

  // empty string
  public static final String EMPTY = "";

  // version
  public static final String VERSION = "1.20";

  // properties
  private Properties props = null;

  // the hash map
  private HashMap hashMap = new HashMap();

  // flags
  private static final int FLAG_TICKER = 0;
  private static final int FLAG_YEAR   = 1;
  private static final int FLAG_MONTH  = 2;
  private static final int FLAG_DAY    = 3;
  private static final int FLAG_OPEN   = 4;
  private static final int FLAG_HIGH   = 5;
  private static final int FLAG_LOW    = 6;
  private static final int FLAG_CLOSE  = 7;
  private static final int FLAG_VOLUME = 8;
  private static final int FLAG_YMD    = 9;
  private static final int FLAG_IGNORE = 10;

  // constructor
  public Updater(ArrayList argList) {
    loadProperties();
    parseArgs(argList);
  }

  private HistoryRecord createHistoryRecord(String historyLine) {
    HistoryRecord rec = new HistoryRecord();
    StringTokenizer st = new StringTokenizer(historyLine, ",");
    try {
      String tok = st.nextToken();
      if (tok.length() != 8) {
        log.warn("invalid history line [YMD invalid]: " + historyLine);
        return null;
      }
      String year  = tok.substring(0, 4);
      String month = tok.substring(4, 6);
      String day   = tok.substring(6, 8);
      rec.setYear(Integer.parseInt(year));
      rec.setMonth(Integer.parseInt(month));
      rec.setDay(Integer.parseInt(day));

      tok = st.nextToken();
      rec.setOpen(Double.parseDouble(tok));

      tok = st.nextToken();
      rec.setHigh(Double.parseDouble(tok));

      tok = st.nextToken();
      rec.setLow(Double.parseDouble(tok));

      tok = st.nextToken();
      rec.setClose(Double.parseDouble(tok));

      tok = st.nextToken();
      rec.setVolume(Double.parseDouble(tok));

      if (st.hasMoreTokens()) {
        log.warn("invalid history line [too many tokens]: " + historyLine);
        return null;
      }
    } catch(NumberFormatException e) {
      log.warn("invalid history line [number format]: " + historyLine);
      return null;
    } catch(NoSuchElementException e) {
      log.warn("invalid history line [too few tokens]: " + historyLine);
      return null;
    }
    return rec;
  }

  private UpdateRecord createUpdateRecord(String updateLine, ArrayList updateMap) {
    int n = 0;
    UpdateRecord rec = new UpdateRecord(props);
    String delimit = props.getProperty("updater.delimit");
    StringTokenizer st = new StringTokenizer(updateLine, delimit);
    try {
      while (st.hasMoreTokens()) {
        if (n == updateMap.size()) {
          log.warn("invalid update line [too many tokens]: " + updateLine);
          return null;
        }
        String tok = st.nextToken();
        Integer val = (Integer) updateMap.get(n);
        if (val.intValue() == FLAG_TICKER) {
          rec.setTicker(tok);
          n++; 
        } else if (val.intValue() == FLAG_YEAR) {
          rec.setYear(Integer.parseInt(tok));
          n++;
        } else if (val.intValue() == FLAG_MONTH) {
          rec.setMonth(Integer.parseInt(tok));
          n++;
        } else if (val.intValue() == FLAG_DAY) {
          rec.setDay(Integer.parseInt(tok));
          n++;
        } else if (val.intValue() == FLAG_OPEN) {
          rec.setOpen(Double.parseDouble(tok));
          n++;
        } else if (val.intValue() == FLAG_HIGH) {
          rec.setHigh(Double.parseDouble(tok));
          n++;
        } else if (val.intValue() == FLAG_LOW) {
          rec.setLow(Double.parseDouble(tok));
          n++;
        } else if (val.intValue() == FLAG_CLOSE) {
          rec.setClose(Double.parseDouble(tok));
          n++;
        } else if (val.intValue() == FLAG_VOLUME) {
          rec.setVolume(Double.parseDouble(tok));
          n++;
        } else if (val.intValue() == FLAG_YMD) {
          if (tok.length() != 8) {
            log.warn("invalid update line [YMD invalid]: " + updateLine);
            return null;
          }
          String year  = tok.substring(0, 4);
          String month = tok.substring(4, 6);
          String day   = tok.substring(6, 8);
          rec.setYear(Integer.parseInt(year));
          rec.setMonth(Integer.parseInt(month));
          rec.setDay(Integer.parseInt(day));
          n++;
        } else if (val.intValue() == FLAG_IGNORE) {
          n++;
        } else {
          log.error("panic: unexpected value in updateMap: " + val.intValue());
          System.exit(1);
        }
      } 
      if (n < updateMap.size()) {
        log.warn("invalid update line [too few tokens]: " + updateLine);
        return null;
      }
    } catch(NumberFormatException e) {
      log.warn("invalid update line [number format]: " + updateLine);
      return null;
    }
    return rec;
  }

  // read and parse the update file
  private void readUpdateFile(String updateFile, ArrayList updateMap) {
    BufferedReader rdr = null;
    String line = null;
    try {
      rdr = new BufferedReader(new FileReader(updateFile));
      while ((line = rdr.readLine()) != null) {
        if (line.equals(EMPTY)) {
          continue;
        }
        UpdateRecord rec = createUpdateRecord(line, updateMap);
        if (rec != null && rec.checkIntegrity()) {
          hashMap.put(rec.getTicker(), rec);
        }
      }
    } catch(IOException e) {
      log.error("exception during read of update file", e);
      System.exit(1);
    } finally {
      try {
        if (rdr != null) {
          rdr.close();
        }
      } catch(IOException e) {
        log.warn("could not close input stream while reading update file", e);
      }
    }
  }

  // delete from a file
  private void deleteFromFile(File file, String date) {
    log.info("deleting from " + file.getAbsolutePath());
    File dir = new File(props.getProperty("updater.work"));
    if (!dir.exists()) {
      log.error("working directory '" + dir.getAbsolutePath() + 
                "' does not exist");
      System.exit(1);
    }
    if (!dir.isDirectory()) {
      log.error("working directory '" + dir.getAbsolutePath() + 
                "' is not a directory");
      System.exit(1);
    }
    String fileNew = props.getProperty("updater.work") + 
                     System.getProperty("file.separator") +
                     file.getName() + ".work";
    BufferedReader rdr = null;
    BufferedWriter wrt = null;
    String line = null;
    boolean written = false;
    boolean error = false;
    try {
      rdr = new BufferedReader(new FileReader(file));
      wrt = new BufferedWriter(new FileWriter(fileNew));
      while ((line = rdr.readLine()) != null) {
        if (line.equals(EMPTY)) {
          continue;
        }
        HistoryRecord hrec = createHistoryRecord(line);
        if (hrec == null) {
          error = true;
          break;
        }
        if (date.equals(hrec.toYmd())) {
          log.info("found '" + date + "'; deleting");
          written = true;
        } else {
          wrt.write(line);
          wrt.newLine();
        }
      }
    } catch(IOException e) {
      log.error("exception during deletion", e);
      System.exit(1);
    } finally {
      if (rdr != null) {
        try {
          rdr.close();
        } catch(IOException e) {
          log.warn("could not close input stream during deletion", e);
        }
      }
      if (wrt != null) {
        try {
          wrt.close();
        } catch(IOException e) {
          log.warn("could not close output stream during deletion", e);
        }
      }
    }

    if (!written) {
      log.warn("'" + date + "' not found in file; ignoring");
      File newFile = new File(fileNew);
      if (!newFile.delete()) {
        log.error("could not delete working file");
        System.exit(1);
      }
    } else if (error) {
      File newFile = new File(fileNew);
      if (!newFile.delete()) {
        log.error("could not delete working file");
        System.exit(1);
      }
    } else {
      File newFile = new File(fileNew);
      if (!newFile.renameTo(file)) {
        log.error("could not move file back to database directory");
        System.exit(1);
      }
    }
  }

  // update a file
  private void updateFile(String updateFile, File file) {
    log.info("updating " + file.getAbsolutePath());
    String ticker = file.getName();
    String ext = "." + props.getProperty("updater.extension");
    int last = ticker.lastIndexOf(ext);
    if (last <= 0) {
      log.error("panic: cannot find extension: " + ticker);
      System.exit(1);
    }
    ticker = ticker.substring(0, last).toUpperCase();
    UpdateRecord urec = (UpdateRecord) hashMap.get(ticker);
    if (urec == null) {
      log.warn("could not find '" + ticker + "' in update file");
      return;
    }

    File dir = new File(props.getProperty("updater.work"));
    if (!dir.exists()) {
      log.error("working directory '" + dir.getAbsolutePath() + 
                "' does not exist");
      System.exit(1);
    }
    if (!dir.isDirectory()) {
      log.error("working directory '" + dir.getAbsolutePath() + 
                "' is not a directory");
      System.exit(1);
    }
    String fileNew = props.getProperty("updater.work") + 
                     System.getProperty("file.separator") +
                     file.getName() + ".work";
    BufferedReader rdr = null;
    BufferedWriter wrt = null;
    String line = null;
    boolean written = false;
    boolean error = false;
    try {
      rdr = new BufferedReader(new FileReader(file));
      wrt = new BufferedWriter(new FileWriter(fileNew));
      while ((line = rdr.readLine()) != null) {
        if (line.equals(EMPTY)) {
          continue;
        }
        HistoryRecord hrec = createHistoryRecord(line);
        if (hrec == null) {
          error = true;
          break;
        }
        int cmp = urec.toYmd().compareTo(hrec.toYmd());
        if (cmp < 0) {
          if (!written) {
            wrt.write(urec.toHistory());
            wrt.newLine();
            written = true;
          }
          wrt.write(line);
          wrt.newLine();
        } else if (cmp == 0) {
          String overWrite = props.getProperty("updater.overwrite");
          if (overWrite.equals("1") || 
              overWrite.equals("true") || 
              overWrite.equals("yes")) {
            log.warn("record already exists; overwriting");
            wrt.write(urec.toHistory());
            wrt.newLine();
            written = true;
          } else {
            log.warn("record already exists; ignoring");
            wrt.write(line);
            wrt.newLine();
            written = true;
          }
        } else {
          wrt.write(line);
          wrt.newLine();
        }
      }
      if (!error && !written) {
        wrt.write(urec.toHistory());
        wrt.newLine();
        written = true;
      }
    } catch(IOException e) {
      log.error("exception during update", e);
      System.exit(1);
    } finally {
      if (rdr != null) {
        try {
          rdr.close();
        } catch(IOException e) {
          log.warn("could not close input stream during update", e);
        }
      }
      if (wrt != null) {
        try {
          wrt.close();
        } catch(IOException e) {
          log.warn("could not close output stream during update", e);
        }
      }
    }

    if (!error) {
      File newFile = new File(fileNew);
      if (!newFile.renameTo(file)) {
        log.error("could not move file back to database directory");
        System.exit(1);
      }
    } else {
      File newFile = new File(fileNew);
      if (!newFile.delete()) {
        log.error("could not delete working file");
        System.exit(1);
      }
    }
  }

  // create the update map
  private ArrayList createUpdateMap() {
    ArrayList map = new ArrayList();
    boolean flagTicker = false;
    boolean flagYear   = false;
    boolean flagMonth  = false;
    boolean flagDay    = false;
    boolean flagOpen   = false;
    boolean flagHigh   = false;
    boolean flagLow    = false;
    boolean flagClose  = false;
    boolean flagVolume = false;
    boolean flagYmd    = false;

    String update = props.getProperty("updater.update");
    String delimit = props.getProperty("updater.delimit");
    StringTokenizer st = new StringTokenizer(update, delimit);
    while (st.hasMoreTokens()) {
      String tok = st.nextToken();
      if ("$tk".equals(tok)) {
        if (flagTicker) {
          log.error("update sequence: ticker defined more than once");
          System.exit(1);
        }
        map.add(FLAG_TICKER);
        flagTicker = true;
      } else if ("$mn".equals(tok)) {
        if (flagMonth) {
          log.error("update sequence: month defined more than once");
          System.exit(1);
        }
        map.add(FLAG_MONTH);
        flagMonth = true;
      } else if ("$dy".equals(tok)) {
        if (flagDay) {
          log.error("update sequence: day defined more than once");
          System.exit(1);
        }
        map.add(FLAG_DAY);
        flagDay = true;
      } else if ("$yr".equals(tok)) {
        if (flagYear) {
          log.error("update sequence: year defined more than once");
          System.exit(1);
        }
        map.add(FLAG_YEAR);
        flagYear = true;
      } else if ("$op".equals(tok)) {
        if (flagOpen) {
          log.error("update sequence: open defined more than once");
          System.exit(1);
        }
        map.add(FLAG_OPEN);
        flagOpen = true;
      } else if ("$hi".equals(tok)) {
        if (flagHigh) {
          log.error("update sequence: high defined more than once");
          System.exit(1);
        }
        map.add(FLAG_HIGH);
        flagHigh = true;
      } else if ("$lo".equals(tok)) {
        if (flagLow) {
          log.error("update sequence: low defined more than once");
          System.exit(1);
        }
        map.add(FLAG_LOW);
        flagLow = true;
      } else if ("$cl".equals(tok)) {
        if (flagClose) {
          log.error("update sequence: close defined more than once");
          System.exit(1);
        }
        map.add(FLAG_CLOSE);
        flagClose = true;
      } else if ("$vl".equals(tok)) {
        if (flagVolume) {
          log.error("update sequence: volume defined more than once");
          System.exit(1);
        }
        map.add(FLAG_VOLUME);
        flagVolume = true;
      } else if ("$ymd".equals(tok)) {
        if (flagYmd) {
          log.error("update sequence: ymd defined more than once");
          System.exit(1);
        }
        map.add(FLAG_YMD);
        flagYmd = true;
      } else if ("$ig".equals(tok)) {
        map.add(FLAG_IGNORE);
      } else {
        log.error("update sequence: unexpected token: " + tok);
        System.exit(1);
      }
    }

    if (!flagTicker ||
        !flagOpen   ||
        !flagHigh   ||
        !flagLow    ||
        !flagClose  ||
        !flagVolume) {
      log.error("incomplete update sequence");
      System.exit(1);
    }

    if (flagYmd) {
      if (flagYear || flagMonth || flagDay) {
        log.error("inconsistent update sequence");
        System.exit(1);
      }
    } else {
      if (!flagYear || !flagMonth || !flagDay) {
        log.error("incomplete update sequence");
        System.exit(1);
      }
    }
    return map;
  }

  // delete
  private void doDelete(ArrayList argList) {
    File[] files;
    log.info("updater version " + VERSION);
    String date = null;
    String filebase = null;
    while (argList.size() > 0) {
      String arg = (String) argList.remove(0);
      if (arg.equals("--date")) {
        if (argList.size() == 0) {
          usage();
          System.exit(1);
        }
        date = (String) argList.remove(0);
      } else if (arg.equals("--filebase")) {
        if (argList.size() == 0) {
          usage();
          System.exit(1);
        }
        filebase = (String) argList.remove(0);
      } else {
        usage();
        System.exit(1);
      } 
    }
    if (date == null) {
      usage();
      System.exit(1);
    }
    if (date.length() != 8) {
      usage();
      System.exit(1);
    }
    if (filebase == null) {
      files = getFileList();
    } else {
      files = getSingleFileList(filebase);
    }
    for (int i = 0; i < files.length; i++) {
      deleteFromFile(files[i], date);
    }
  }

  // update
  private void doUpdate(ArrayList argList) {
    log.info("updater version " + VERSION);
    String updateFile = null;
    while (argList.size() > 0) {
      String arg = (String) argList.remove(0);
      if (arg.equals("--overwrite")) {
        if (argList.size() == 0) {
          usage();
          System.exit(1);
        }
        arg = (String) argList.remove(0);
        props.setProperty("updater.overwrite", arg);
      } else if (arg.equals("--update-file")) {
        if (argList.size() == 0) {
          usage();
          System.exit(1);
        }
        arg = (String) argList.remove(0);
        updateFile = arg;
      } else {
        usage();
        System.exit(1);
      }
    }
    if (updateFile == null) {
      usage();
      System.exit(1);
    }

    File f = new File(updateFile);
    if (!f.exists()) {
      log.error(updateFile + " does not exist");
      System.exit(1);
    }
    if (!f.isFile()) {
      log.error(updateFile + " is not a file");
      System.exit(1);
    }
    log.info("updating against " + updateFile);
    ArrayList updateMap = createUpdateMap();
    readUpdateFile(updateFile, updateMap);

    File[] files = getFileList();
    for (int i = 0; i < files.length; i++) {
      updateFile(updateFile, files[i]);
    }
  }

  private File[] getSingleFileList(String filebase) {
    String database = props.getProperty("updater.database");
    String extension = "." + props.getProperty("updater.extension");
    String path = database + System.getProperty("file.separator") + 
                  filebase + extension;
    File file = new File(path);
    if (!file.exists()) {
      log.error("file '" + file.getAbsolutePath() + "' does not exist");
      System.exit(1);
    }
    if (!file.isFile()) {
      log.error("file '" + file.getAbsolutePath() + "' is not a file");
      System.exit(1);
    }
    File[] files = new File[1];
    files[0] = file;
    return files;
  }

  private File[] getFileList() {
    File[] files = null;
    final String database = props.getProperty("updater.database");
    final String extension = "." + props.getProperty("updater.extension");
    try {
      File dir = new File(database);
      if (!dir.exists()) {
        log.error("database '" + dir.getAbsolutePath() + "' does not exist");
        System.exit(1);
      }
      if (!dir.isDirectory()) {
        log.error("database '" + dir.getAbsolutePath() + "' is not a directory");
        System.exit(1);
      }
      files = dir.listFiles(new FileFilter() {
        public boolean accept(File pathname) {
          if (!pathname.getAbsolutePath().endsWith(extension)) {
            return false;
          }
          if (!pathname.isFile()) {
            return false;
          }
          return true;
        }
      });
      if (files == null) {
        log.error("could not get the file list");
        System.exit(1);
      }
    } catch (Exception e) {
      log.error("could not get the file list: ", e);
      System.exit(1);
    }
    return files;
  }

  // shows usage
  private void usage() {
    System.out.println("updater version " + VERSION);
    System.out.println("--------------------");
    System.out.println("updater update [--overwrite false|true] --update-file file");
    System.out.println("  updates database directory against 'file'\n");
    System.out.println("updater delete --date yyyymmdd [--filebase file]");
    System.out.println("  deletes 'yyyymmdd' from 'file' / all files\n");
    System.out.println("updater help");
    System.out.println("  shows this screen\n");
  }

  // parse the arguments and call the right operation
  private void parseArgs(ArrayList argList) {
    if (argList.size() == 0) {
      usage();
      System.exit(1);
    } else if ("update".equals((String) argList.get(0))) {
      argList.remove(0);
      doUpdate(argList);
    } else if ("delete".equals((String) argList.get(0))) {
      argList.remove(0);
      doDelete(argList);
    } else if ("help".equals((String) argList.get(0))) {
      usage();
    } else {
      usage();
      System.exit(1);
    }
  }

  // load properties
  private void loadProperties() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    props = new Properties();
    try {
      InputStream is = cl.getResourceAsStream("updater.properties");
      if (is == null) {
        log.error("panic: cannot load properties");
        System.exit(1);
      }
      props.load(is);
    } catch(IOException e) {
      log.error("panic: cannot load properties", e);
      System.exit(1);
    }

    if (props.getProperty("updater.update") == null ||
        props.getProperty("updater.update").equals(EMPTY)) {
      log.error("updater.update not defined");
      System.exit(1);
    }
    if (props.getProperty("updater.delimit") == null ||
        props.getProperty("updater.delimit").equals(EMPTY)) {
      log.error("updater.delimit not defined");
      System.exit(1);
    }
    if (props.getProperty("updater.database") == null ||
        props.getProperty("updater.database").equals(EMPTY)) {
      log.error("updater.database not defined");
      System.exit(1);
    }
    if (props.getProperty("updater.extension") == null ||
        props.getProperty("updater.extension").equals(EMPTY)) {
      log.error("updater.extension not defined");
      System.exit(1);
    }
    if (props.getProperty("updater.overwrite") == null ||
        props.getProperty("updater.overwrite").equals(EMPTY)) {
      log.error("updater.overwrite not defined");
      System.exit(1);
    }
    if (props.getProperty("updater.work") == null ||
        props.getProperty("updater.work").equals(EMPTY)) {
      log.error("updater.work not defined");
      System.exit(1);
    }
  }

  public static void main(String args[]) {
    ArrayList argList = new ArrayList(args.length);
    for (int i = 0; i < args.length; i++) {
      argList.add(args[i]);
    }
    Updater updater = new Updater(argList);
    System.exit(0);
  }
}

