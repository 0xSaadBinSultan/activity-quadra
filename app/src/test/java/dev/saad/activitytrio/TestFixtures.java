package dev.saad.activitytrio;

import android.content.Context;
import org.json.*;
import java.time.*;
import java.util.*;

/** Synthetic profiles, used only by tests and clearly labelled documentation previews. */
final class TestFixtures {
    static final String USER="demo-user";
    static JSONObject leetcode()throws Exception{
        JSONArray available=new JSONArray(),solved=new JSONArray();String[] names={"All","Easy","Medium","Hard"};int[] totals={4000,1000,2000,1000},counts={23,7,12,4};
        for(int i=0;i<names.length;i++){available.put(new JSONObject().put("difficulty",names[i]).put("count",totals[i]));solved.put(new JSONObject().put("difficulty",names[i]).put("count",counts[i]));}
        JSONObject calendar=new JSONObject().put(String.valueOf(Instant.parse("2026-01-01T00:00:00Z").getEpochSecond()),7).put(String.valueOf(Instant.parse("2026-06-02T00:00:00Z").getEpochSecond()),4);
        return new JSONObject().put("data",new JSONObject().put("allQuestionsCount",available).put("matchedUser",new JSONObject().put("username",USER).put("submitStatsGlobal",new JSONObject().put("acSubmissionNum",solved)).put("userCalendar",new JSONObject().put("submissionCalendar",calendar.toString()))));
    }
    static void cache(Context context){try{
        android.content.SharedPreferences.Editor edit=Store.prefs(context).edit().putInt("year",2026).putString("timezone","UTC");for(String provider:ActivityData.PROVIDERS)edit.putString(provider,USER);edit.apply();
        for(String provider:ActivityData.PROVIDERS){
            if(provider.equals("leetcode")){Store.save(context,Parsers.leetcode(leetcode(),USER,2026,"UTC","2026-11-13T19:30:51Z"));continue;}
            TreeMap<LocalDate,ActivityData.Day> days=new TreeMap<>();LocalDate start=LocalDate.of(2026,1,1);
            for(int i=0;i<365;i++){int count=(i*7+3)%13;days.put(start.plusDays(i),new ActivityData.Day(count,count==0?0:1+(count>1?1:0)+(count>4?1:0)+(count>9?1:0)));}
            Store.save(context,new ActivityData(provider,USER,2026,2190,"2026-11-13T19:30:51Z","UTC",false,days));
        }
    }catch(Exception e){throw new AssertionError(e);}}
}
