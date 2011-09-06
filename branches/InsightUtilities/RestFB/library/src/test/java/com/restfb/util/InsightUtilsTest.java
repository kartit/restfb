package com.restfb.util;

import static com.restfb.util.InsightUtils.buildQueries;
import static com.restfb.util.InsightUtils.convertToMidnightInPacificTimeZone;
import static com.restfb.util.InsightUtils.convertToUnixTimeAtPacificTimeZoneMidnight;
import static com.restfb.util.InsightUtils.createBaseQuery;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.restfb.util.InsightUtils.Period;

/**
 * Unit tests that exercise {@link com.restfb.util.InsightUtils}.
 * 
 * @author Andrew Liles
 */
public class InsightUtilsTest {

  private static Locale DEFAULT_LOCALE;
  private static TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("Etc/UTC");
  private static TimeZone PST_TIMEZONE = TimeZone.getTimeZone("PST");
  private static SimpleDateFormat sdfUTC;
  private static SimpleDateFormat sdfPST;

  @BeforeClass
  public static void beforeClass() {
    for (Locale locale : Locale.getAvailableLocales()) {
      if (locale.toString().equals("en_US")) {
        DEFAULT_LOCALE = locale;
        break;
      }
    }
    Assert.assertNotNull(DEFAULT_LOCALE);
    sdfUTC = newSimpleDateFormat("yyyyMMdd_HHmm", DEFAULT_LOCALE, UTC_TIMEZONE);
    sdfPST = newSimpleDateFormat("yyyyMMdd_HHmm", DEFAULT_LOCALE, PST_TIMEZONE);
  }

  @Test
  public void convertToMidnightInPacificTimeZone1() throws ParseException {
    Date d20030630_0221utc = sdfUTC.parse("20030630_0221");
    Date d20030629_0000pst = sdfPST.parse("20030629_0000");

    Date actual = convertToMidnightInPacificTimeZone(d20030630_0221utc);
    Assert.assertEquals(d20030629_0000pst, actual);
  }

  @Test
  public void convertToMidnightInPacificTimeZone2() throws ParseException {
    Date d20030630_1503utc = sdfUTC.parse("20030630_1503");
    Date d20030630_0000pst = sdfPST.parse("20030630_0000");

    Date actual = convertToMidnightInPacificTimeZone(d20030630_1503utc);
    Assert.assertEquals(d20030630_0000pst, actual);
  }

  @Test
  public void convertToMidnightInPacificTimeZoneSet1() throws ParseException {
    Date d20030630_0221utc = sdfUTC.parse("20030630_0221");
    Date d20030629_0000pst = sdfPST.parse("20030629_0000");
    Date d20030630_1503utc = sdfUTC.parse("20030630_1503");
    Date d20030630_0000pst = sdfPST.parse("20030630_0000");
    Set<Date> inputs = new HashSet<Date>();
    inputs.add(d20030630_0221utc);
    inputs.add(d20030630_1503utc);

    SortedSet<Date> actuals = convertToMidnightInPacificTimeZone(inputs);
    Assert.assertEquals(2, actuals.size());
    Iterator<Date> it = actuals.iterator();
    Assert.assertEquals(d20030629_0000pst, it.next());
    Assert.assertEquals(d20030630_0000pst, it.next());
  }

  @Test
  public void getUnixTimeAtPSTMidnight1() throws ParseException {
    // From http://developers.facebook.com/docs/reference/fql/insights/
    // Example: To obtain data for the 24-hour period starting on
    // September 15th at 00:00 (i.e. 12:00 midnight) and ending on
    // September 16th at 00:00 (i.e. 12:00 midnight),
    // specify 1284620400 as the end_time and 86400 as the period.

    Date d20100916_1800utc = sdfUTC.parse("20100916_1800");

    long actual = convertToUnixTimeAtPacificTimeZoneMidnight(d20100916_1800utc);
    Assert.assertEquals(1284620400L, actual);
  }

  @Test
  public void getUnixTimeAtPSTMidnight2() throws ParseException {
    // in this test we are still in the previous PST day - the difference is 7
    // hours from UTC to PST

    Date d20100917_0659utc = sdfUTC.parse("20100917_0659");

    long actual = convertToUnixTimeAtPacificTimeZoneMidnight(d20100917_0659utc);
    Assert.assertEquals(1284620400L, actual);

    Date d20100917_0700utc = sdfUTC.parse("20100917_0700");

    actual = convertToUnixTimeAtPacificTimeZoneMidnight(d20100917_0700utc);
    Assert.assertEquals(1284620400L + (60 * 60 * 24), actual);
  }

  @Test
  public void createBaseQuery0metrics() {
    Set<String> metrics = Collections.emptySet();
    Assert.assertEquals("SELECT metric, value FROM insights WHERE object_id=31698190356 AND "
        + "period=604800 AND end_time=", createBaseQuery(Period.WEEK, 31698190356L, metrics));
  }

  @Test
  public void createBaseQuery1metric() {
    Set<String> metrics = Collections.singleton("page_active_users");
    Assert.assertEquals("SELECT metric, value FROM insights WHERE object_id=31698190356 AND metric IN "
        + "('page_active_users') AND period=604800 AND end_time=", createBaseQuery(Period.WEEK, 31698190356L, metrics));
  }

  @Test
  public void createBaseQuery3metrics() {
    Set<String> metrics = new LinkedHashSet<String>();
    metrics.add("page_comment_removes");
    metrics.add("page_active_users");
    metrics.add("page_like_adds_source_unique");
    Assert.assertEquals("SELECT metric, value FROM insights WHERE object_id=31698190356 AND metric IN "
        + "('page_comment_removes','page_active_users','page_like_adds_source_unique') AND period=86400 AND end_time=",
      createBaseQuery(Period.DAY, 31698190356L, metrics));
  }

  @Test
  public void buildQueries1() throws ParseException {
    Date d20101205_0000pst = sdfPST.parse("20101205_0000");
    long t20101205_0000 = 1291536000L;
    Assert.assertEquals(t20101205_0000, convertToUnixTimeAtPacificTimeZoneMidnight(d20101205_0000pst));
    Assert.assertEquals(t20101205_0000, sdfPST.parse("20101205_0000").getTime() / 1000L);

    List<Date> datesByQueryIndex = new ArrayList<Date>();
    datesByQueryIndex.add(d20101205_0000pst);

    String baseQuery =
        "SELECT metric, value FROM insights WHERE object_id=31698190356 AND metric IN "
            + "('page_active_users') AND period=604800 AND end_time=";

    Map<String, String> fqlByQueryIndex = buildQueries(baseQuery, datesByQueryIndex);
    Assert.assertEquals(1, fqlByQueryIndex.size());

    String fql = fqlByQueryIndex.values().iterator().next();
    Assert
      .assertEquals(
        "SELECT metric, value FROM insights WHERE object_id=31698190356 AND metric IN ('page_active_users') AND period=604800 AND end_time="
            + t20101205_0000, fql);
  }

  @Test
  public void prepareQueries30() throws Exception {
    // produce a set of days in UTC timezone from 1st Nov 9am to 30th Nov 9am
    // inclusive
    Date d20101101_0900utc = sdfUTC.parse("20101101_0900");
    Calendar c = new GregorianCalendar();
    c.setTimeZone(UTC_TIMEZONE);
    c.setTime(d20101101_0900utc);
    Set<Date> utcDates = new TreeSet<Date>();
    for (int dayNum = 1; dayNum <= 30; dayNum++) {
      utcDates.add(c.getTime());
      c.add(Calendar.DAY_OF_MONTH, 1);
    }
    Assert.assertEquals(30, utcDates.size());

    // convert into PST and convert into a list
    List<Date> datesByQueryIndex = new ArrayList<Date>(convertToMidnightInPacificTimeZone(utcDates));
    Assert.assertEquals(30, datesByQueryIndex.size());

    // Mon Nov 01 00:00:00 2010 PST
    long day0 = sdfPST.parse("20101101_0000").getTime() / 1000L;
    // Sun Nov 07 00:00:00 2010 PST
    long day6 = sdfPST.parse("20101107_0000").getTime() / 1000L;
    // Tue Nov 30 00:00:00 2010 PST
    long day29 = sdfPST.parse("20101130_0000").getTime() / 1000L;

    String baseQuery =
        "SELECT metric, value FROM insights WHERE object_id=316981903568 AND metric IN "
            + "('page_active_users','page_audio_plays') AND period=86400 AND end_time=";

    Map<String, String> fqlByQueryIndex = buildQueries(baseQuery, datesByQueryIndex);
    Assert.assertEquals(30, fqlByQueryIndex.size());

    Assert.assertEquals("SELECT metric, value FROM insights WHERE object_id=316981903568 AND metric IN "
        + "('page_active_users','page_audio_plays') AND period=86400 AND end_time=" + day0, fqlByQueryIndex.get("0"));
    Assert.assertEquals("SELECT metric, value FROM insights WHERE object_id=316981903568 AND metric IN "
        + "('page_active_users','page_audio_plays') AND period=86400 AND end_time=" + day6, fqlByQueryIndex.get("6"));
    Assert.assertEquals("SELECT metric, value FROM insights WHERE object_id=316981903568 AND metric IN "
        + "('page_active_users','page_audio_plays') AND period=86400 AND end_time=" + day29, fqlByQueryIndex.get("29"));
  }

  /**
   * As there is no easy constructor for making a SimpleDateFormat specifying
   * both a Locale and Timezone a utility is provided here
   * 
   * @param pattern
   * @param locale
   * @param timezone
   * @return
   */
  public static SimpleDateFormat newSimpleDateFormat(String pattern, Locale locale, TimeZone timezone) {
    SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
    sdf.setTimeZone(timezone);
    return sdf;
  }
}
