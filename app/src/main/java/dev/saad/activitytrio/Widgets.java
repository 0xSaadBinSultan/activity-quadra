package dev.saad.activitytrio;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Widgets {
    static final String REFRESH="dev.saad.activitytrio.REFRESH";
    private static final Class<?>[] TYPES={TrioWidget.class,GithubWidget.class,CodeforcesWidget.class,TryHackMeWidget.class};
    static String kind(Class<?> type){return type==GithubWidget.class?"github":type==CodeforcesWidget.class?"codeforces":type==TryHackMeWidget.class?"tryhackme":"all";}
    public static boolean hasWidgets(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);for(Class<?> type:TYPES)if(manager.getAppWidgetIds(new ComponentName(context,type)).length>0)return true;return false;}
    public static void updateAll(Context context){Store.seed(context);AppWidgetManager manager=AppWidgetManager.getInstance(context);for(Class<?> type:TYPES)for(int id:manager.getAppWidgetIds(new ComponentName(context,type)))update(context,manager,id,type);}
    public static boolean refresh(Context context){
        long last=Store.prefs(context).getLong("last-request",0);if(System.currentTimeMillis()-last<60_000)return false;
        Store.prefs(context).edit().putLong("last-request",System.currentTimeMillis()).apply();SyncJob.schedule(context,true);return true;
    }
    public static String stamp(ActivityData data){
        try{return DateTimeFormatter.ofPattern("dd MMM, HH:mm",Locale.ENGLISH).format(OffsetDateTime.parse(data.updated).atZoneSameInstant(ZoneId.systemDefault()));}catch(Exception ignored){return data.updated;}
    }
    public static String status(Context context,String provider,ActivityData data){
        if(data==null)return provider.equals("tryhackme")?"Import a public-profile snapshot in the app":"Open app to refresh public activity";
        if(data.snapshot)return "Snapshot · "+stamp(data);
        return Store.error(context,provider).isEmpty()?"Updated · "+stamp(data):"Cached · "+stamp(data)+" · refresh unavailable";
    }
    private static PendingIntent open(Context context,int id,String provider){
        Intent intent=new Intent(context,MainActivity.class).putExtra("provider",provider).setAction("dev.saad.activitytrio.OPEN."+provider+"."+id);
        return PendingIntent.getActivity(context,id*10+provider.hashCode(),intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    public static void update(Context context,AppWidgetManager manager,int id,Class<?> type){
        String kind=kind(type);boolean all=kind.equals("all");RemoteViews views=new RemoteViews(context.getPackageName(),all?R.layout.widget_trio:R.layout.widget_single);
        Bundle options=manager.getAppWidgetOptions(id);int dpWidth=Math.max(180,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320));int dpHeight=Math.max(all?260:110,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,all?340:150));float density=context.getResources().getDisplayMetrics().density;
        int width=Math.max(120,Math.min(1200,Math.round((dpWidth-28)*density)));int graphDp=all?Math.max(30,(dpHeight-160)/3):Math.max(34,dpHeight-96);int height=Math.max(40,Math.min(480,Math.round(graphDp*density)));
        PendingIntent root=open(context,id,"all");views.setOnClickPendingIntent(R.id.widget_title,root);
        Intent refresh=new Intent(context,type).setAction(REFRESH);views.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(context,id,refresh,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
        String[] providers=all?ActivityData.PROVIDERS:new String[]{kind};
        int[] titles={R.id.github_title,R.id.codeforces_title,R.id.tryhackme_title};int[] summaries={R.id.github_summary,R.id.codeforces_summary,R.id.tryhackme_summary};int[] graphs={R.id.github_graph,R.id.codeforces_graph,R.id.tryhackme_graph};int[] sections={R.id.github_section,R.id.codeforces_section,R.id.tryhackme_section};
        for(int i=0;i<providers.length;i++){
            String provider=providers[i];ActivityData data=Store.read(context,provider);int title=all?titles[i]:R.id.widget_title,summary=all?summaries[i]:R.id.widget_summary,graph=all?graphs[i]:R.id.widget_graph;
            String heading=ActivityData.name(provider)+(data!=null?" · "+data.year:"")+(provider.equals("tryhackme")?" · snapshot":"");views.setTextViewText(title,heading);
            String summaryText=data==null?Store.user(context,provider)+" · waiting for data":(all?(Store.error(context,provider).isEmpty()?"":"Cached · ")+data.total+" "+data.unit()+" · "+stamp(data):data.username+" · "+data.total+" "+data.unit()+(data.snapshot?" · "+stamp(data):""));
            views.setTextViewText(summary,summaryText);
            views.setContentDescription(graph,ActivityData.name(provider)+" · "+status(context,provider,data)+". Open for date details.");
            views.setTextColor(title,Graphs.color(provider,4));views.setOnClickPendingIntent(graph,open(context,id,provider));views.setOnClickPendingIntent(summary,open(context,id,provider));if(all)views.setOnClickPendingIntent(sections[i],open(context,id,provider));
            if(data!=null)views.setImageViewBitmap(graph,Graphs.bitmap(data,width,height,density));else views.setImageViewBitmap(graph,(Bitmap)null);
            if(!all)views.setTextViewText(R.id.widget_status,status(context,provider,data));
        }
        if(all){ActivityData thm=Store.read(context,"tryhackme");views.setTextViewText(R.id.widget_status,"Tap a graph for date details · "+(thm==null?"TryHackMe needs an import":"THM snapshot "+stamp(thm)));}
        manager.updateAppWidget(id,views);
    }
}
