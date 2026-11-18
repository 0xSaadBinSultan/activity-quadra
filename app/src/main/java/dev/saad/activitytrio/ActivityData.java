package dev.saad.activitytrio;

import org.json.*;
import java.time.*;
import java.util.*;

public final class ActivityData {
    public static final String[] PROVIDERS={"github","codeforces","leetcode"};
    public final String provider,username,updated,zone; public final int year,total;
    public final TreeMap<LocalDate,Day> cells; public final boolean snapshot;
    public final JSONObject problems;
    public static final class Day {
        public final Integer count; public final int level;
        public Day(Integer count,int level){this.count=count;this.level=Math.max(0,Math.min(4,level));}
    }
    public ActivityData(String provider,String username,int year,int total,String updated,String zone,boolean snapshot,TreeMap<LocalDate,Day> cells){
        this(provider,username,year,total,updated,zone,snapshot,cells,null);
    }
    public ActivityData(String provider,String username,int year,int total,String updated,String zone,boolean snapshot,TreeMap<LocalDate,Day> cells,JSONObject problems){
        if(!Arrays.asList(PROVIDERS).contains(provider))throw new IllegalArgumentException("Unsupported provider");
        this.provider=provider;this.username=username;this.year=year;this.total=total;this.updated=updated;this.zone=zone;this.snapshot=snapshot;this.cells=cells;this.problems=problems;
    }
    public String name(){return name(provider);}
    public static String name(String id){return id.equals("github")?"GitHub":id.equals("codeforces")?"Codeforces":"LeetCode";}
    public String unit(){return provider.equals("github")?"contributions":"submissions";}
    public String summary(){return problems==null?total+" "+unit():problems.optInt("solved")+"/"+problems.optInt("available")+" solved · "+total+" submissions";}
    public Day day(LocalDate date){Day found=cells.get(date);return found!=null?found:new Day(snapshot?null:0,0);}
    public String description(LocalDate day){Day value=day(day);return day.toString()+" · "+(value.count==null?"Activity level "+value.level+"/4":value.count+" "+unit());}
    public JSONObject json() throws JSONException {
        JSONObject result=new JSONObject().put("provider",provider).put("username",username).put("year",year).put("total",total).put("updated",updated).put("zone",zone).put("snapshot",snapshot);JSONObject days=new JSONObject();
        for(Map.Entry<LocalDate,Day> entry:cells.entrySet())days.put(entry.getKey().toString(),new JSONObject().put("count",entry.getValue().count==null?JSONObject.NULL:entry.getValue().count).put("level",entry.getValue().level));
        result.put("cells",days);if(problems!=null)result.put("problems",problems);return result;
    }
    public static ActivityData fromJson(JSONObject json) throws JSONException {
        TreeMap<LocalDate,Day> days=new TreeMap<>();JSONObject raw=json.getJSONObject("cells");Iterator<String> keys=raw.keys();
        while(keys.hasNext()){String key=keys.next();JSONObject day=raw.getJSONObject(key);days.put(LocalDate.parse(key),new Day(day.isNull("count")?null:day.getInt("count"),day.getInt("level")));}
        return new ActivityData(json.getString("provider"),json.getString("username"),json.getInt("year"),json.getInt("total"),json.getString("updated"),json.optString("zone","UTC"),json.optBoolean("snapshot"),days,json.optJSONObject("problems"));
    }
}
