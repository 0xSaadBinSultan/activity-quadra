package dev.saad.activitytrio;

import java.time.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public final class Parsers {
    private static final Pattern TAG=Pattern.compile("<td\\b([^>]*)>",Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR=Pattern.compile("([\\w:-]+)\\s*=\\s*[\"']([^\"']*)[\"']");
    private static final Pattern TIP=Pattern.compile("<tool-tip\\b([^>]*)>(.*?)</tool-tip>",Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNT=Pattern.compile("([\\d,]+) contributions?",Pattern.CASE_INSENSITIVE);
    private static Map<String,String> attrs(String text){Map<String,String> result=new HashMap<>();Matcher m=ATTR.matcher(text);while(m.find())result.put(m.group(1),m.group(2));return result;}
    public static ActivityData github(String html,String username,int year,String zone,String updated){
        TreeMap<LocalDate,ActivityData.Day> days=new TreeMap<>();Map<String,LocalDate> ids=new HashMap<>();Matcher tags=TAG.matcher(html);
        while(tags.find()){
            Map<String,String> a=attrs(tags.group(1));if(!a.containsKey("data-date"))continue;
            LocalDate date=LocalDate.parse(a.get("data-date"));if(date.getYear()!=year)continue;
            int level=Integer.parseInt(a.getOrDefault("data-level","0"));if(level<0||level>4)throw new IllegalArgumentException("GitHub activity format changed");
            days.put(date,new ActivityData.Day(null,level));if(a.containsKey("id"))ids.put(a.get("id"),date);
        }
        Matcher tips=TIP.matcher(html);
        while(tips.find()){
            LocalDate date=ids.get(attrs(tips.group(1)).get("for"));if(date==null)continue;
            String content=tips.group(2).replaceAll("<[^>]*>"," ");Matcher count=COUNT.matcher(content);Integer number=null;
            if(count.find())number=Integer.valueOf(count.group(1).replace(",",""));else if(content.toLowerCase(Locale.ROOT).contains("no contributions"))number=0;
            if(number!=null)days.put(date,new ActivityData.Day(number,days.get(date).level));
        }
        if(days.isEmpty())throw new IllegalArgumentException("GitHub activity is unavailable");int total=0;
        for(ActivityData.Day day:days.values()){if(day.count==null)throw new IllegalArgumentException("GitHub activity format changed");total+=day.count;}
        return new ActivityData("github",username,year,total,updated,zone,false,days);
    }
    public static ActivityData codeforces(JSONArray submissions,String username,int year,String zone,String updated)throws JSONException{
        TreeMap<LocalDate,Integer> counts=new TreeMap<>();Set<Long> seen=new HashSet<>();ZoneId tz=ZoneId.of(zone);
        for(int i=0;i<submissions.length();i++){
            JSONObject row=submissions.getJSONObject(i);if(row.has("id")&&!seen.add(row.getLong("id")))continue;
            LocalDate day=Instant.ofEpochSecond(row.getLong("creationTimeSeconds")).atZone(tz).toLocalDate();if(day.getYear()==year)counts.put(day,counts.getOrDefault(day,0)+1);
        }
        TreeMap<LocalDate,ActivityData.Day> days=new TreeMap<>();int total=0;
        for(Map.Entry<LocalDate,Integer> row:counts.entrySet()){int count=row.getValue(),level=1+(count>1?1:0)+(count>4?1:0)+(count>9?1:0);days.put(row.getKey(),new ActivityData.Day(count,level));total+=count;}
        return new ActivityData("codeforces",username,year,total,updated,zone,false,days);
    }
    public static ActivityData tryhackme(JSONObject snapshot,String username,String zone)throws JSONException{
        if(!snapshot.optString("provider").equals("tryhackme")||!snapshot.getString("username").equalsIgnoreCase(username))throw new IllegalArgumentException("Snapshot belongs to another TryHackMe profile");
        int year=snapshot.getInt("year");if(year<2000||year>2200)throw new IllegalArgumentException("Invalid snapshot year");
        JSONArray colors=snapshot.getJSONArray("colors");LocalDate first=LocalDate.of(year,1,1);if(colors.length()!=first.lengthOfYear())throw new IllegalArgumentException("Snapshot must contain the complete year");
        String[] palette={"rgb(255,255,255)","rgb(186,227,210)","rgb(88,187,146)","rgb(62,147,112)","rgb(47,111,85)"};TreeMap<LocalDate,ActivityData.Day> days=new TreeMap<>();
        for(int i=0;i<colors.length();i++){
            String color=colors.getString(i).replaceAll("\\s+","");int level=Arrays.asList(palette).indexOf(color);if(level<0)throw new IllegalArgumentException("Snapshot color format is not supported");
            days.put(first.plusDays(i),new ActivityData.Day(null,level));
        }
        int total=snapshot.getInt("total");if(total<0)throw new IllegalArgumentException("Invalid activity total");String updated=snapshot.getString("captured");Instant.parse(updated);
        return new ActivityData("tryhackme",username,year,total,updated,zone,true,days);
    }
}
