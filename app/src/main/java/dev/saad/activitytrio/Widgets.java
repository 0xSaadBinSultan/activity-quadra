package dev.saad.activitytrio;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.*;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Widgets {
    static final String REFRESH="dev.saad.activitytrio.REFRESH";
    static final Class<?>[] TYPES={TrioWidget.class,GithubWidget.class,CodeforcesWidget.class,TryHackMeWidget.class,LeetcodeWidget.class};
    static String kind(Class<?> type){return type==GithubWidget.class?"github":type==CodeforcesWidget.class?"codeforces":type==TryHackMeWidget.class?"tryhackme":type==LeetcodeWidget.class?"leetcode":"all";}
    public static boolean hasWidgets(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);for(Class<?> type:TYPES)if(manager.getAppWidgetIds(new ComponentName(context,type)).length>0)return true;return false;}
    public static void updateAll(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);for(Class<?> type:TYPES)for(int id:manager.getAppWidgetIds(new ComponentName(context,type)))update(context,manager,id,type);}
    public static boolean refresh(Context context){
        if(!Store.liveConfigured(context))return false;
        long last=Store.prefs(context).getLong("last-request",0);if(System.currentTimeMillis()-last<60_000)return false;
        Store.prefs(context).edit().putLong("last-request",System.currentTimeMillis()).apply();SyncJob.schedule(context,true);return true;
    }
    public static String stamp(ActivityData data){
        try{return DateTimeFormatter.ofPattern("dd MMM, HH:mm",Locale.ENGLISH).format(OffsetDateTime.parse(data.updated).atZoneSameInstant(ZoneId.systemDefault()));}catch(Exception ignored){return data.updated;}
    }
    public static String status(Context context,String provider,ActivityData data){
        if(Store.user(context,provider).isEmpty())return "Open app → Profiles to add your username";
        if(data==null)return provider.equals("tryhackme")?"Import a public-profile snapshot in the app":"Open app to refresh public activity";
        if(data.snapshot)return "Snapshot · "+stamp(data);
        return Store.error(context,provider).isEmpty()?"Updated · "+stamp(data):"Cached · "+stamp(data)+" · refresh unavailable";
    }
    private static PendingIntent open(Context context,int id,String provider){
        Intent intent=new Intent(context,MainActivity.class).putExtra("provider",provider).setAction("dev.saad.activitytrio.OPEN."+provider+"."+id);
        return PendingIntent.getActivity(context,id*10+provider.hashCode(),intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    static boolean wide(float width,float height){return width>=600&&height>=300;}
    static RemoteViews render(Context context,int id,Class<?> type,float dpWidth,float dpHeight){
        String kind=kind(type);boolean all=kind.equals("all"),wide=all&&wide(dpWidth,dpHeight);
        RemoteViews views=new RemoteViews(context.getPackageName(),all?(wide?R.layout.widget_trio_wide:R.layout.widget_trio):R.layout.widget_single);
        float density=context.getResources().getDisplayMetrics().density,fontScale=context.getResources().getConfiguration().fontScale;
        float sectionWidth=(dpWidth-28)/(wide?2:1)-(wide?12:0),sectionHeight=all?(dpHeight-82)/(wide?2:4)-(wide?12:0):dpHeight-82;
        int width=Math.max(100,Math.min(1600,Math.round(sectionWidth*density)));
        int height=Math.max(24,Math.min(360,Math.round(Math.max(20,sectionHeight-30*fontScale)*density)));
        views.setOnClickPendingIntent(R.id.widget_title,open(context,id,"all"));
        Intent refresh=new Intent(context,type).setAction(REFRESH);views.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(context,id,refresh,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
        String[] providers=all?ActivityData.PROVIDERS:new String[]{kind};
        int[] titles={R.id.github_title,R.id.codeforces_title,R.id.tryhackme_title,R.id.leetcode_title};int[] summaries={R.id.github_summary,R.id.codeforces_summary,R.id.tryhackme_summary,R.id.leetcode_summary};int[] graphs={R.id.github_graph,R.id.codeforces_graph,R.id.tryhackme_graph,R.id.leetcode_graph};int[] sections={R.id.github_section,R.id.codeforces_section,R.id.tryhackme_section,R.id.leetcode_section};
        for(int i=0;i<providers.length;i++){
            String provider=providers[i],user=Store.user(context,provider);ActivityData data=Store.read(context,provider);int title=all?titles[i]:R.id.widget_title,summary=all?summaries[i]:R.id.widget_summary,graph=all?graphs[i]:R.id.widget_graph;
            String heading=ActivityData.name(provider)+(data!=null?" · "+data.year:"")+(provider.equals("tryhackme")?" · snapshot":"");views.setTextViewText(title,heading);
            String summaryText=data==null?(user.isEmpty()?"Add your username in Profiles":user+" · waiting for data"):(all?(Store.error(context,provider).isEmpty()?"":"Cached · ")+data.summary()+" · "+stamp(data):data.username+" · "+data.summary());
            views.setTextViewText(summary,summaryText);views.setContentDescription(graph,ActivityData.name(provider)+" · "+(data==null?"":data.summary()+". ")+status(context,provider,data)+". Open for date details.");
            views.setTextColor(title,Graphs.color(provider,4));views.setOnClickPendingIntent(graph,open(context,id,provider));views.setOnClickPendingIntent(summary,open(context,id,provider));if(all)views.setOnClickPendingIntent(sections[i],open(context,id,provider));
            if(data!=null)views.setImageViewBitmap(graph,Graphs.bitmap(data,width,height,density));else views.setImageViewBitmap(graph,(Bitmap)null);
            if(!all)views.setTextViewText(R.id.widget_status,status(context,provider,data));
        }
        if(all)views.setTextViewText(R.id.widget_status,Store.configured(context)?"Tap a graph for date details · TryHackMe is a manual snapshot":"Open Activity Quadra → Profiles to get started");
        return views;
    }
    @android.annotation.TargetApi(31)
    private static RemoteViews exact(Context context,int id,Class<?> type,Bundle options){
        ArrayList<SizeF> sizes=androidx.core.os.BundleCompat.getParcelableArrayList(options,AppWidgetManager.OPTION_APPWIDGET_SIZES,SizeF.class);
        if(sizes==null||sizes.isEmpty()||sizes.size()>16)return null;
        Map<SizeF,RemoteViews> layouts=new LinkedHashMap<>();
        for(SizeF size:sizes){
            if(size==null||!Float.isFinite(size.getWidth())||!Float.isFinite(size.getHeight())||size.getWidth()<=0||size.getHeight()<=0)continue;
            layouts.put(size,render(context,id,type,size.getWidth(),size.getHeight()));
        }
        return layouts.isEmpty()?null:new RemoteViews(layouts);
    }
    static RemoteViews forOptions(Context context,int id,Class<?> type,Bundle options){
        if(Build.VERSION.SDK_INT>=31){RemoteViews exact=exact(context,id,type,options);if(exact!=null)return exact;}
        boolean all=kind(type).equals("all");
        int minWidth=Math.max(100,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320)),minHeight=Math.max(100,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,all?420:150));
        int maxWidth=Math.max(minWidth,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,minWidth)),maxHeight=Math.max(minHeight,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,minHeight));
        // Older launchers report the two orientation ranges instead of exact sizes.
        return new RemoteViews(render(context,id,type,maxWidth,minHeight),render(context,id,type,minWidth,maxHeight));
    }
    public static void update(Context context,AppWidgetManager manager,int id,Class<?> type){manager.updateAppWidget(id,forOptions(context,id,type,manager.getAppWidgetOptions(id)));}
}
