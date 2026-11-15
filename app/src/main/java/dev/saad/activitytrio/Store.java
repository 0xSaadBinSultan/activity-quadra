package dev.saad.activitytrio;

import android.content.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;

public final class Store {
    public static android.content.SharedPreferences prefs(Context context){return context.getSharedPreferences("activity-trio",Context.MODE_PRIVATE);}
    public static String user(Context context,String provider){for(String id:ActivityData.PROVIDERS)if(provider.equals(id))return prefs(context).getString(provider,"");throw new IllegalArgumentException("Unknown provider");}
    public static boolean configured(Context context){for(String id:ActivityData.PROVIDERS)if(!user(context,id).isEmpty())return true;return false;}
    public static boolean liveConfigured(Context context){for(String id:new String[]{"github","codeforces","leetcode"})if(!user(context,id).isEmpty())return true;return false;}
    public static String zone(Context context){return prefs(context).getString("timezone",ZoneId.systemDefault().getId());}
    public static boolean validUsername(String provider,String value){return value.isEmpty()||value.matches(provider.equals("github")?"[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?":"[A-Za-z0-9_.-]{1,50}");}
    public static int year(Context context){int chosen=prefs(context).getInt("year",0);return chosen>0?chosen:Year.now(ZoneId.of(zone(context))).getValue();}
    private static File file(Context context,String provider){for(String id:ActivityData.PROVIDERS)if(id.equals(provider))return new File(context.getFilesDir(),provider+".json");throw new IllegalArgumentException("Unknown provider");}
    public static synchronized void save(Context context,ActivityData data)throws Exception{
        AtomicFile file=new AtomicFile(file(context,data.provider));FileOutputStream out=null;
        try{out=file.startWrite();out.write(data.json().toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception error){if(out!=null)file.failWrite(out);throw error;}
    }
    public static ActivityData read(Context context,String provider){
        if(user(context,provider).isEmpty())return null;
        try{
            ActivityData data=ActivityData.fromJson(new JSONObject(new String(new AtomicFile(file(context,provider)).readFully(),StandardCharsets.UTF_8)));
            if(!data.username.equalsIgnoreCase(user(context,provider)))return null;
            if(!data.snapshot&&(data.year!=year(context)||!data.zone.equals(zone(context))))return null;return data;
        }catch(Exception ignored){return null;}
    }
    public static byte[] bytes(InputStream in,int limit)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
        while((count=in.read(buffer))!=-1){if(out.size()+count>limit)throw new IOException("Response is too large");out.write(buffer,0,count);}return out.toByteArray();
    }
    public static void error(Context context,String provider,String text){prefs(context).edit().putString(provider+".error",text==null?"":text).apply();}
    public static String error(Context context,String provider){return prefs(context).getString(provider+".error","");}
}
