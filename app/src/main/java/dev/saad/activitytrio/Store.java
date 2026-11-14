package dev.saad.activitytrio;

import android.content.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;

public final class Store {
    private static final String[] DEFAULTS={"0xSaadBinSultan","saadbinsultan","0xsaadbinsultan"};
    public static android.content.SharedPreferences prefs(Context context){return context.getSharedPreferences("activity-trio",Context.MODE_PRIVATE);}
    public static String user(Context context,String provider){for(int i=0;i<ActivityData.PROVIDERS.length;i++)if(provider.equals(ActivityData.PROVIDERS[i]))return prefs(context).getString(provider,DEFAULTS[i]);throw new IllegalArgumentException("Unknown provider");}
    public static String zone(Context context){return prefs(context).getString("timezone","America/New_York");}
    public static int year(Context context){int chosen=prefs(context).getInt("year",0);return chosen>0?chosen:Year.now(ZoneId.of(zone(context))).getValue();}
    private static File file(Context context,String provider){for(String id:ActivityData.PROVIDERS)if(id.equals(provider))return new File(context.getFilesDir(),provider+".json");throw new IllegalArgumentException("Unknown provider");}
    public static synchronized void save(Context context,ActivityData data)throws Exception{
        AtomicFile file=new AtomicFile(file(context,data.provider));FileOutputStream out=null;
        try{out=file.startWrite();out.write(data.json().toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception error){if(out!=null)file.failWrite(out);throw error;}
    }
    public static ActivityData read(Context context,String provider){
        try{
            ActivityData data=ActivityData.fromJson(new JSONObject(new String(new AtomicFile(file(context,provider)).readFully(),StandardCharsets.UTF_8)));
            if(!data.username.equalsIgnoreCase(user(context,provider)))return null;
            if(!data.snapshot&&(data.year!=year(context)||!data.zone.equals(zone(context))))return null;return data;
        }catch(Exception ignored){return null;}
    }
    public static void seed(Context context){
        for(String provider:new String[]{"github","codeforces"}){
            if(read(context,provider)!=null)continue;
            try(InputStream input=context.getAssets().open(provider+"-seed.json")){
                ActivityData data=ActivityData.fromJson(new JSONObject(new String(bytes(input,256*1024),StandardCharsets.UTF_8)));
                if(data.username.equalsIgnoreCase(user(context,provider))&&data.year==year(context)&&data.zone.equals(zone(context)))save(context,data);
            }catch(Exception ignored){}
        }
        if(read(context,"tryhackme")!=null)return;
        try(InputStream in=context.getAssets().open("tryhackme-snapshot.json")){
            ActivityData data=Parsers.tryhackme(new JSONObject(new String(bytes(in,256*1024),StandardCharsets.UTF_8)),user(context,"tryhackme"),zone(context));save(context,data);
        }catch(Exception ignored){}
    }
    public static byte[] bytes(InputStream in,int limit)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
        while((count=in.read(buffer))!=-1){if(out.size()+count>limit)throw new IOException("Response is too large");out.write(buffer,0,count);}return out.toByteArray();
    }
    public static void error(Context context,String provider,String text){prefs(context).edit().putString(provider+".error",text==null?"":text).apply();}
    public static String error(Context context,String provider){return prefs(context).getString(provider+".error","");}
}
