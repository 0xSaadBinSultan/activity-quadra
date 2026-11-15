package dev.saad.activitytrio;

import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.time.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class LeetcodeTest {
    Application context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();Store.prefs(context).edit().clear().putString("leetcode",TestFixtures.USER).putInt("year",2026).putString("timezone","America/New_York").apply();}
    ActivityData parse(JSONObject response)throws Exception{return Parsers.leetcode(response,TestFixtures.USER,2026,"America/New_York","2026-11-15T12:00:00Z");}
    JSONObject user(JSONObject response)throws Exception{return response.getJSONObject("data").getJSONObject("matchedUser");}
    @Test public void solvedCountsAreNotSubmissionTotalsAndSurviveCacheRoundtrip()throws Exception{ActivityData data=parse(TestFixtures.leetcode());assertEquals(11,data.total);assertEquals(23,data.problems.getInt("solved"));assertEquals(4000,data.problems.getInt("available"));assertEquals(12,data.problems.getJSONObject("difficulty").getJSONObject("Medium").getInt("solved"));assertTrue(data.summary().contains("23/4000 solved"));assertEquals(data.json().toString(),ActivityData.fromJson(data.json()).json().toString());}
    @Test public void datesStayUtcAndOtherYearsAreFiltered()throws Exception{JSONObject response=TestFixtures.leetcode();JSONObject cal=new JSONObject(user(response).getJSONObject("userCalendar").getString("submissionCalendar"));cal.put(String.valueOf(Instant.parse("2025-12-31T00:00:00Z").getEpochSecond()),100);user(response).getJSONObject("userCalendar").put("submissionCalendar",cal.toString());ActivityData data=parse(response);assertEquals(Integer.valueOf(7),data.day(LocalDate.of(2026,1,1)).count);assertFalse(data.cells.containsKey(LocalDate.of(2025,12,31)));assertEquals(11,data.total);}
    @Test public void emptyCalendarIsValidButMissingCalendarIsNot()throws Exception{JSONObject response=TestFixtures.leetcode();user(response).getJSONObject("userCalendar").put("submissionCalendar","{}");assertEquals(0,parse(response).total);user(response).remove("userCalendar");try{parse(response);fail();}catch(JSONException expected){}}
    @Test public void malformedValuesCannotBeShownAsZero()throws Exception{for(Object count:new Object[]{true,-1,1.5,"3",JSONObject.NULL}){JSONObject response=TestFixtures.leetcode();user(response).getJSONObject("userCalendar").put("submissionCalendar",new JSONObject().put("1790000000",count).toString());try{parse(response);fail("Accepted invalid count "+count);}catch(IllegalArgumentException expected){}}}
    @Test public void wrongUserAndGraphqlErrorsAreRejected()throws Exception{JSONObject response=TestFixtures.leetcode();user(response).put("username","different-user");try{parse(response);fail();}catch(IllegalArgumentException expected){}response=TestFixtures.leetcode().put("errors",new JSONArray().put(new JSONObject().put("message","unavailable")));try{parse(response);fail();}catch(IllegalArgumentException expected){}}
    @Test public void missingOrInvalidProblemTotalsAreRejected()throws Exception{JSONObject response=TestFixtures.leetcode();response.getJSONObject("data").put("allQuestionsCount",new JSONArray());try{parse(response);fail();}catch(IllegalArgumentException expected){}response=TestFixtures.leetcode();user(response).getJSONObject("submitStatsGlobal").getJSONArray("acSubmissionNum").getJSONObject(0).put("count",true);try{parse(response);fail();}catch(IllegalArgumentException expected){}}
    @Test public void readOnlyPublicPostHasSelectedUsernameAndYear()throws Exception{ActivityData result=new SyncEngine(context).leetcode((url,body)->{assertEquals("https://leetcode.com/graphql/",url);assertTrue(body.getString("query").startsWith("query "));assertFalse(body.getString("query").contains("mutation"));assertEquals(TestFixtures.USER,body.getJSONObject("variables").getString("username"));assertEquals(2026,body.getJSONObject("variables").getInt("year"));return TestFixtures.leetcode().toString();});assertEquals(23,result.problems.getInt("solved"));}
    @Test public void failedRefreshKeepsLastGoodCacheAndUnconfiguredServicesNeverFetch()throws Exception{ActivityData original=parse(TestFixtures.leetcode());Store.save(context,original);final int[] posts={0};new SyncEngine(context).refresh(url->{fail("Unconfigured service fetched");return "";},(url,body)->{posts[0]++;return "{\"errors\":[{\"message\":\"unavailable\"}]}";});assertEquals(1,posts[0]);assertEquals(original.json().toString(),Store.read(context,"leetcode").json().toString());assertFalse(Store.error(context,"leetcode").isEmpty());Store.prefs(context).edit().clear().apply();new SyncEngine(context).refresh(url->{fail();return "";},(url,body)->{fail();return "";});}
    @Test public void upgradePreservesExplicitProfilesButDoesNotRestoreOldDefaults()throws Exception{Store.prefs(context).edit().putString("github","chosen-user").apply();assertEquals("chosen-user",Store.user(context,"github"));assertEquals("",Store.user(context,"codeforces"));assertTrue(Store.configured(context));}
    @Test public void leapDayAndZeroActivityAreAccurate()throws Exception{JSONObject response=TestFixtures.leetcode();user(response).getJSONObject("userCalendar").put("submissionCalendar",new JSONObject().put(String.valueOf(Instant.parse("2024-02-29T00:00:00Z").getEpochSecond()),0).toString());ActivityData data=Parsers.leetcode(response,TestFixtures.USER,2024,"America/New_York","2026-11-15T12:00:00Z");assertTrue(data.cells.containsKey(LocalDate.of(2024,2,29)));assertEquals(0,data.day(LocalDate.of(2024,2,29)).level);}
}
