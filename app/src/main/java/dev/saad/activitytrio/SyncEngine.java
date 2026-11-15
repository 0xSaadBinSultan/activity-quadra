package dev.saad.activitytrio;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;

public final class SyncEngine {
    public interface Fetcher {String get(String url)throws Exception;}
    public interface Poster {String post(String url,JSONObject body)throws Exception;}
    static final String LEETCODE_QUERY="query ActivityTrio($username: String!, $year: Int) { allQuestionsCount { difficulty count } matchedUser(username: $username) { username submitStatsGlobal { acSubmissionNum { difficulty count } } userCalendar(year: $year) { submissionCalendar } } }";
    private final Context context;
    public SyncEngine(Context context){this.context=context.getApplicationContext();}
    private static final class HttpProblem extends IOException {final int code;HttpProblem(int code){super("HTTP "+code);this.code=code;}}
    public static String download(String address)throws Exception{
        return request(address,null);
    }
    public static String request(String address,JSONObject body)throws Exception{
        URL url=new URL(address);if(!url.getProtocol().equals("https"))throw new IOException("HTTPS is required");
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(20000);connection.setRequestProperty("User-Agent","ActivityQuadra/1.1 (Android; public activity)");
        try{
            if(body!=null){
                byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");connection.setFixedLengthStreamingMode(bytes.length);
                try(OutputStream out=connection.getOutputStream()){out.write(bytes);}
            }
            int status=connection.getResponseCode();if(status!=200)throw new HttpProblem(status);
            try(InputStream in=connection.getInputStream()){return new String(Store.bytes(in,5*1024*1024),StandardCharsets.UTF_8);}
        }finally{connection.disconnect();}
    }
    private static String encoded(String value)throws Exception{return URLEncoder.encode(value,"UTF-8");}
    public ActivityData leetcode(Poster poster)throws Exception{
        String user=Store.user(context,"leetcode"),zone=Store.zone(context);int year=Store.year(context);
        JSONObject body=new JSONObject().put("query",LEETCODE_QUERY).put("variables",new JSONObject().put("username",user).put("year",year));
        return Parsers.leetcode(new JSONObject(poster.post("https://leetcode.com/graphql/",body)),user,year,zone,Instant.now().toString());
    }
    public ActivityData github(Fetcher fetcher)throws Exception{
        String user=Store.user(context,"github"),zone=Store.zone(context);int year=Store.year(context);
        String url="https://github.com/users/"+encoded(user)+"/contributions?from="+year+"-01-01&to="+year+"-12-31";
        return Parsers.github(fetcher.get(url),user,year,zone,Instant.now().toString());
    }
    public ActivityData codeforces(Fetcher fetcher)throws Exception{
        String user=Store.user(context,"codeforces"),zone=Store.zone(context);int year=Store.year(context);JSONArray all=new JSONArray();
        long cutoff=LocalDate.of(year,1,1).atStartOfDay(ZoneId.of(zone)).toEpochSecond();boolean complete=false;
        for(int page=0;page<20;page++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();if(page>0)Thread.sleep(2200);
            String url="https://codeforces.com/api/user.status?handle="+encoded(user)+"&from="+(page*1000+1)+"&count=1000";
            JSONObject result=new JSONObject(fetcher.get(url));if(!result.optString("status").equals("OK"))throw new IOException("Codeforces is unavailable or rate limited");
            JSONArray batch=result.getJSONArray("result");for(int i=0;i<batch.length();i++)all.put(batch.getJSONObject(i));
            if(batch.length()<1000||batch.getJSONObject(batch.length()-1).getLong("creationTimeSeconds")<cutoff){complete=true;break;}
        }
        if(!complete)throw new IOException("Submission history exceeds the request budget");
        return Parsers.codeforces(all,user,year,zone,Instant.now().toString());
    }
    public String refresh(){return refresh(SyncEngine::download,SyncEngine::request);}
    String refresh(Fetcher fetcher,Poster poster){
        int success=0,failures=0;
        for(String id:new String[]{"github","codeforces","leetcode"}){
            if(Store.user(context,id).isEmpty())continue;
            long next=Store.prefs(context).getLong(id+".next",0);
            if(System.currentTimeMillis()<next){failures++;continue;}
            try{
                ActivityData data=id.equals("github")?github(fetcher):id.equals("leetcode")?leetcode(poster):codeforces(fetcher);Store.save(context,data);Store.error(context,id,"");success++;
                Store.prefs(context).edit().putLong(id+".next",System.currentTimeMillis()+60_000).apply();
            }catch(Exception error){
                if(error instanceof InterruptedException){Thread.currentThread().interrupt();break;}
                boolean limited=error instanceof HttpProblem&&(((HttpProblem)error).code==429||((HttpProblem)error).code==403);
                Store.error(context,id,limited?"Server limited access · keeping cached activity":"Refresh unavailable · keeping cached activity");
                Store.prefs(context).edit().putLong(id+".next",System.currentTimeMillis()+(limited?3_600_000:300_000)).apply();failures++;
            }
        }
        return success+" feeds refreshed"+(failures>0?" · cached data kept where needed":"")+" · TryHackMe uses its snapshot";
    }
}
