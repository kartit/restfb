/*
 * Copyright (c) 2010-2011 Mark Allen.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.restfb.util;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import com.restfb.FacebookClient;
import com.restfb.json.JsonArray;

/**
 * A set of utilities to ease querying <a
 * href="http://developers.facebook.com/docs/reference/fql/insights/">Insight
 * Metrics</a> over several dates
 * 
 * @author Andrew Liles
 * @since 1.6.xxx TODO: fix version numbering
 */
public class InsightUtils {
  private static TimeZone PST_TIMEZONE = TimeZone.getTimeZone("PST");

  /**
   * Queries Facebook via FQL for several Insights at different date points
   * 
   * The output groups the result by Metric and then by Date, matching the input
   * arguments. Maps entries may be null if Facebook does not return
   * corresponding data, e.g. when you query a metric which is not available at
   * the chosen period. The inner Map's Object value may be a strongly typed
   * value for some metrics in other cases Facebook returns a JsonObject.
   * 
   * TODO: fix formatting in below
   * 
   * Sample output, assuming 2 metrics were queried for 5 dates {
   * page_active_users : 2011-jan-01 : 7 : 2011-jan-02 : 26 : 2011-jan-02 : 15 :
   * 2011-jan-02 : 10 : 2011-jan-02 : 687 page_tab_views_login_top_unique :
   * 2011-jan-01 : {"photos":1,"wall":7} : 2011-jan-02 :
   * {"photos":8,"wall":25,"info":3} : 2011-jan-03 :
   * {"app_237307273":1,"photos":
   * 1,"app_494975287":1,"app_234747185":1,"wall":14,"info":2} : 2011-jan-04 :
   * {"app_237307273":1,"photos":2,"app_234747185":1,"wall":10,"info":1} :
   * 2011-jan-05 :
   * {"app_494975287":2,"app_237307273":2,"photos":6,"wall":35,"info":6} }
   * 
   * @param client
   * @param pageObjectId
   *          mandatory object_id to query
   * @param metrics
   *          if the set is is null/empty then all metrics will be queried for
   *          the given period
   * @param period
   *          mandatory period
   * @param periodEndDates
   *          the set should be not null/empty and each date should be
   *          normalized to be midnight in the PST timezone; see
   *          {@link #convertToMidnightInPSTTimeZone(Set)}
   * @return a map of maps: the outer keys will be all the metrics requested
   *         that were available at the period/times requested. The innert keys
   *         will be the set periodEndDates
   * @see #convertToMidnightInPSTTimeZone(Set)
   * @see #executeInsightQueriesByDate(FacebookClient, long, Set, Period, Set)
   */
  public static Map<String, SortedMap<Date, Object>> executeInsightQueriesByMetricByDate(FacebookClient client,
      long pageObjectId, Set<String> metrics, Period period, Set<Date> periodEndDates) {
    /*
     * pseudo code: Map<Date, JsonArray> result = executeInsightQueriesRaw(..)
     * Dissect the JsonArrays which contain one element for each of the metrics
     * queried, rebuild into a map of maps.
     */
    return null;
  }

  /**
   * Queries Facebook via FQL for several Insights at different date points
   * 
   * A variation on {
   * {@link #executeInsightQueriesByMetricByDate(FacebookClient, long, Set, Period, Set)}
   * , this method returns the raw output from the Facebook, keying the output
   * by date alone. The JsonArray value will contain the metrics that were
   * requested and available at the date.
   * 
   * TODO: fix formatting in below
   * 
   * Sample output, assuming 2 metrics were queried for 5 dates {2011-jan-01 :
   * [{"metric":"page_active_users","value":7},{"metric":
   * "page_tab_views_login_top_unique","value":{"photos":1,"wall":7}}],
   * 2011-jan-02 : [{"metric":"page_active_users","value":26},{"metric":
   * "page_tab_views_login_top_unique"
   * ,"value":{"photos":8,"wall":25,"info":3}}], 2011-jan-03 :
   * [{"metric":"page_active_users"
   * ,"value":15},{"metric":"page_tab_views_login_top_unique"
   * ,"value":{"app_237307273"
   * :1,"photos":1,"app_494975287":1,"app_234747185":1,"wall":14,"info":2}}],
   * 2011-jan-04 : [{"metric":"page_active_users","value":10},{"metric":
   * "page_tab_views_login_top_unique"
   * ,"value":{"app_237307273":1,"photos":2,"app_234747185"
   * :1,"wall":10,"info":1}}], 2011-jan-05 :
   * [{"metric":"page_active_users","value"
   * :687},{"metric":"page_tab_views_login_top_unique"
   * ,"value":{"app_494975287":2
   * ,"app_237307273":2,"photos":6,"wall":35,"info":6}}] }
   * 
   * @param client
   * @param pageObjectId
   *          mandatory object_id to query
   * @param metrics
   *          if the set is is null/empty then all metrics will be queried for
   *          the given period
   * @param period
   *          mandatory period
   * @param periodEndDates
   *          the set should be not null/empty and each date should be
   *          normalized to be midnight in the PST timezone; see
   *          {@link #convertToMidnightInPSTTimeZone(Set)}
   * @return
   * @see #convertToMidnightInPSTTimeZone(Set)
   * @see #executeInsightQueriesByMetricByDate(FacebookClient, long, Set,
   *      Period, Set)
   */
  public static SortedMap<Date, JsonArray> executeInsightQueriesByDate(FacebookClient client, long pageObjectId,
      Set<String> metrics, Period period, Set<Date> periodEndDates) {
    /*
     * pseudo code: String baseQuery = createBaseQuery(..); Map<String,String>
     * queries = iterate through dates, add to end of baseQuery, put in map with
     * an index associated with periodEndDates
     * 
     * JsonObject response = client.executeMultiquery(queries,
     * JsonObject.class);
     * 
     * Map<Date, JsonArray> result = TreeMap... iterate over the result using
     * the expected multiquery key indices cast each key value to JsonArray
     * resolve the key index back into a Date, store output in the outgoing map
     */
    return null;
  }

  private static String createBaseQuery(Period period, long pageObjectId, String metricInList) {
    StringBuilder q = new StringBuilder();
    q.append("SELECT metric, value ");
    q.append("FROM insights ");
    q.append("WHERE object_id=");
    q.append(pageObjectId);
    q.append(" AND metric IN (");
    q.append(metricInList);
    q.append(")");
    q.append(" AND period=");
    q.append(period.getPeriodLength());
    q.append(" AND end_time=");
    return q.toString();
  }

  /**
   * Slide this time back to midnight in the PST timezone fit for the Facebook
   * Query Language
   * 
   * @param input
   * @return
   * @see http://developers.facebook.com/docs/reference/fql/insights/
   * @see {@link #convertToMidnightInPacificTimeZone(Set)}
   */
  public static Date convertToMidnightInPacificTimeZone(Date input) {
    if (input == null) {
      throw new IllegalArgumentException("Provide a date");
    }
    Set<Date> convertedDates = convertToMidnightInPacificTimeZone(Collections.singleton(input));
    if (convertedDates.size() != 1) {
      throw new RuntimeException("Internal error, expected 1 date");
    }
    return convertedDates.iterator().next();
  }

  /**
   * Slide this time back to midnight in the PST timezone fit for the Facebook
   * Query Language
   * 
   * @param input
   * @return
   * @see http://developers.facebook.com/docs/reference/fql/insights/
   * @see {@link #convertToMidnightInPacificTimeZone(Date)}
   */
  public static SortedSet<Date> convertToMidnightInPacificTimeZone(Set<Date> dates) {
    Calendar calendar = Calendar.getInstance(PST_TIMEZONE);
    SortedSet<Date> convertedDates = new TreeSet<Date>();
    for (Date d : dates) {
      calendar.setTime(d);
      calendar.set(Calendar.HOUR_OF_DAY, 0);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      convertedDates.add(calendar.getTime());
    }
    return convertedDates;
  }

  /**
   * Convert into a "unix time" which means convert into the number of seconds
   * (NOT milliseconds) from the Epoc
   * 
   * @param input
   * @return
   */
  static long convertToUnixTime(Date input) {
    return input.getTime() / 1000L;
  }

  /**
   * slide this time back to midnight in the PST timezone and convert into a
   * "unix time" which means convert into the number of seconds (NOT
   * milliseconds) from the Epoc fit for the Facebook Query Language
   * 
   * @param input
   * @return
   */
  static long getUnixTimeAtPSTMidnight(Date input) {
    return convertToUnixTime(convertToMidnightInPacificTimeZone(input));
  }

  public enum Period {

    DAY(86400), WEEK(604800), MONTH(2592000), LIFETIME(0);

    private int periodLength;

    private Period(int periodLength) {
      this.periodLength = periodLength;
    }

    public int getPeriodLength() {
      return periodLength;
    }

  }

}
