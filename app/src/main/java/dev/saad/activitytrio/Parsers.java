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
    private static int nonnegative(Object value){
        if(!(value instanceof Integer)&&!(value instanceof Long))throw new IllegalArgumentException("Invalid LeetCode count");
        long number=((Number)value).longValue();if(number<0||number>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid LeetCode count");return (int)number;
    }
    private static Map<String,Integer> problemCounts(JSONArray rows)throws JSONException{
        Map<String,Integer> counts=new HashMap<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);String difficulty=row.getString("difficulty");
            if(Arrays.asList("All","Easy","Medium","Hard").contains(difficulty)){
                if(counts.containsKey(difficulty))throw new IllegalArgumentException("Duplicate LeetCode difficulty");counts.put(difficulty,nonnegative(row.get("count")));
            }
        }
        if(!counts.containsKey("All"))throw new IllegalArgumentException("LeetCode problem totals unavailable");return counts;
    }
    public static ActivityData leetcode(JSONObject response,String username,int year,String zone,String updated)throws JSONException{
        if(response.has("errors")&&!response.isNull("errors")&&response.getJSONArray("errors").length()>0)throw new IllegalArgumentException("LeetCode query unavailable");
        JSONObject payload=response.getJSONObject("data"),user=payload.getJSONObject("matchedUser");
        if(!user.getString("username").equalsIgnoreCase(username))throw new IllegalArgumentException("LeetCode profile does not match");
        Map<String,Integer> solved=problemCounts(user.getJSONObject("submitStatsGlobal").getJSONArray("acSubmissionNum")),available=problemCounts(payload.getJSONArray("allQuestionsCount"));
        Object raw=user.getJSONObject("userCalendar").get("submissionCalendar");if(!(raw instanceof String))throw new IllegalArgumentException("LeetCode calendar unavailable");
        JSONObject calendar=new JSONObject((String)raw);
        TreeMap<LocalDate,ActivityData.Day> days=new TreeMap<>();Iterator<String> keys=calendar.keys();int total=0;
        while(keys.hasNext()){
            String key=keys.next();if(!key.matches("[0-9]{1,12}"))throw new IllegalArgumentException("Invalid LeetCode calendar date");int count=nonnegative(calendar.get(key));
            // The epoch encodes the calendar day at UTC midnight, not the viewer's timezone.
            LocalDate day=Instant.ofEpochSecond(Long.parseLong(key)).atZone(ZoneOffset.UTC).toLocalDate();if(day.getYear()!=year)continue;
            int combined=Math.addExact(count,days.containsKey(day)?days.get(day).count:0);int level=combined==0?0:1+(combined>1?1:0)+(combined>4?1:0)+(combined>9?1:0);
            days.put(day,new ActivityData.Day(combined,level));total=Math.addExact(total,count);
        }
        JSONObject problems=new JSONObject().put("solved",solved.get("All")).put("available",available.get("All")),difficulty=new JSONObject();
        for(String name:new String[]{"Easy","Medium","Hard"})if(solved.containsKey(name)&&available.containsKey(name))difficulty.put(name,new JSONObject().put("solved",solved.get(name)).put("available",available.get(name)));
        problems.put("difficulty",difficulty);return new ActivityData("leetcode",username,year,total,updated,zone,false,days,problems);
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
