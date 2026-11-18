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
    static final Class<?>[] TYPES={TrioWidget.class,TabletTrioWidget.class,GithubWidget.class,CodeforcesWidget.class,LeetcodeWidget.class};
    static boolean tablet(Context context){return context.getResources().getBoolean(R.bool.offer_tablet_merged);}
    static Class<?>[] pickerTypes(Context context){return new Class<?>[]{tablet(context)?TabletTrioWidget.class:TrioWidget.class,GithubWidget.class,CodeforcesWidget.class,LeetcodeWidget.class};}
    static String kind(Class<?> type){return type==GithubWidget.class?"github":type==CodeforcesWidget.class?"codeforces":type==LeetcodeWidget.class?"leetcode":"all";}
    public static boolean hasWidgets(Context context){AppWidgetManager manager=AppWidgetManager.getInstance(context);for(Class<?> type:TYPES)if((type!=TabletTrioWidget.class||tablet(context))&&manager.getAppWidgetIds(new ComponentName(context,type)).length>0)return true;return false;}
    public static void updateAll(Context context){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);int failures=0;
        for(Class<?> type:TYPES){
            if(type==TabletTrioWidget.class&&!tablet(context))continue;
            try{for(int id:manager.getAppWidgetIds(new ComponentName(context,type)))if(!update(context,manager,id,type))failures++;}
            catch(RuntimeException error){failures++;}
        }
        Store.prefs(context).edit().putInt("widget-update-failures",failures).apply();
    }
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
        if(data==null)return "Open app to refresh public activity";
        if(data.snapshot)return "Snapshot · "+stamp(data);
        return Store.error(context,provider).isEmpty()?"Updated · "+stamp(data):"Cached · "+stamp(data)+" · refresh unavailable";
    }
    private static PendingIntent open(Context context,int id,String provider){
        Intent intent=new Intent(context,MainActivity.class).putExtra("provider",provider).setAction("dev.saad.activitytrio.OPEN."+provider+"."+id);
        return PendingIntent.getActivity(context,id*10+provider.hashCode(),intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    static long bitmapBudget(Context context){
        android.util.DisplayMetrics metrics=context.getResources().getDisplayMetrics();
        // Android budgets all bitmap variants together. Leave room below its 6*w*h ceiling.
        return Math.max(64*1024,Math.min(4*1024*1024L,2L*Math.max(1,metrics.widthPixels)*Math.max(1,metrics.heightPixels)));
    }
    private static Bitmap graph(Context context,ActivityData data,float dpWidth,int images,long budget){
        float density=context.getResources().getDisplayMetrics().density;
        int width=Math.max(64,Math.min(1600,Math.round(dpWidth*density)));
        int height=(int)Math.ceil(22*density+7*Math.max(1,(width-22*density)/53));
        long perImage=Math.max(1,budget/Math.max(1,images));
        double scale=Math.min(1,Math.sqrt(perImage/(5.0*width*height)));
        width=Math.max(1,(int)(width*scale));height=Math.max(1,(int)(height*scale));
        return Graphs.bitmap(data,width,height,(float)(density*scale));
    }
    static RemoteViews render(Context context,int id,Class<?> type,float dpWidth,float dpHeight){
        return render(context,id,type,dpWidth,dpHeight,type==TabletTrioWidget.class?tabletBitmapBudget(context):bitmapBudget(context));
    }
    private static RemoteViews render(Context context,int id,Class<?> type,float dpWidth,float dpHeight,long budget){
        if(type==TabletTrioWidget.class)return renderTablet(context,id,dpWidth,dpHeight,budget);
        String kind=kind(type);boolean all=kind.equals("all");
        // One weighted layout works in every orientation, without duplicating bitmap payloads.
        RemoteViews views=new RemoteViews(context.getPackageName(),all?R.layout.widget_trio:R.layout.widget_single);
        float sectionWidth=Math.max(40,dpWidth-28);
        views.setOnClickPendingIntent(R.id.widget_title,open(context,id,"all"));
        Intent refresh=new Intent(context,type).setAction(REFRESH);views.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(context,id,refresh,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
        String[] providers=all?ActivityData.PROVIDERS:new String[]{kind};
        int[] titles={R.id.github_title,R.id.codeforces_title,R.id.leetcode_title};int[] summaries={R.id.github_summary,R.id.codeforces_summary,R.id.leetcode_summary};int[] graphs={R.id.github_graph,R.id.codeforces_graph,R.id.leetcode_graph};int[] sections={R.id.github_section,R.id.codeforces_section,R.id.leetcode_section};
        for(int i=0;i<providers.length;i++){
            String provider=providers[i],user=Store.user(context,provider);ActivityData data=Store.read(context,provider);int title=all?titles[i]:R.id.widget_title,summary=all?summaries[i]:R.id.widget_summary,graph=all?graphs[i]:R.id.widget_graph;
            String heading=ActivityData.name(provider)+(data!=null?" · "+data.year:"");views.setTextViewText(title,heading);
            String summaryText=data==null?(user.isEmpty()?"Set username in Profiles":user+" · waiting for data"):(all?(Store.error(context,provider).isEmpty()?"":"Cached · ")+(data.problems==null?data.total+" "+data.unit():data.problems.optInt("solved")+"/"+data.problems.optInt("available")+" solved")+" · "+stamp(data):data.username+" · "+data.summary());
            views.setTextViewText(summary,summaryText);views.setContentDescription(graph,ActivityData.name(provider)+" · "+(data==null?"":data.summary()+". ")+status(context,provider,data)+". Open for date details.");
            views.setTextColor(title,Graphs.color(provider,4));views.setOnClickPendingIntent(graph,open(context,id,provider));views.setOnClickPendingIntent(summary,open(context,id,provider));if(all)views.setOnClickPendingIntent(sections[i],open(context,id,provider));
            if(data!=null)views.setImageViewBitmap(graph,graph(context,data,sectionWidth,providers.length,budget));else views.setImageViewBitmap(graph,(Bitmap)null);
            if(!all)views.setTextViewText(R.id.widget_status,status(context,provider,data));
        }
        if(all)views.setTextViewText(R.id.widget_status,Store.configured(context)?"Tap a graph for date details":"Open Activity Trio → Profiles to get started");
        return views;
    }
    static float widthForOptions(Bundle options){
        float width=Math.max(180,Math.max(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,0)));
        if(Build.VERSION.SDK_INT>=31)try{
            ArrayList<SizeF> sizes=androidx.core.os.BundleCompat.getParcelableArrayList(options,AppWidgetManager.OPTION_APPWIDGET_SIZES,SizeF.class);
            if(sizes!=null)for(SizeF size:sizes)if(size!=null&&Float.isFinite(size.getWidth())&&size.getWidth()>0)width=Math.max(width,size.getWidth());
        }catch(RuntimeException ignored){} // Some launchers omit or misencode the optional size list.
        return Math.min(2400,width);
    }
    static RemoteViews forOptions(Context context,int id,Class<?> type,Bundle options){
        if(type==TabletTrioWidget.class)return tabletForOptions(context,id,options);
        boolean all=kind(type).equals("all");
        return render(context,id,type,widthForOptions(options),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,all?340:150));
    }
    public static boolean update(Context context,AppWidgetManager manager,int id,Class<?> type){
        try{manager.updateAppWidget(id,forOptions(context,id,type,manager.getAppWidgetOptions(id)));return true;}
        catch(RuntimeException first){
            try{manager.updateAppWidget(id,render(context,id,type,280,300,128*1024));return true;}
            catch(RuntimeException retry){Store.prefs(context).edit().putString("widget-last-error",retry.getClass().getSimpleName()).apply();return false;}
        }
    }
    static long tabletBitmapBudget(Context context){
        android.util.DisplayMetrics metrics=context.getResources().getDisplayMetrics();
        return Math.max(64*1024,Math.min(12*1024*1024L,2L*Math.max(1,metrics.widthPixels)*Math.max(1,metrics.heightPixels)));
    }
    static boolean tabletWide(float width,float height){return width>=800&&width>=height*1.35f;}
    private static RemoteViews renderTablet(Context context,int id,float dpWidth,float dpHeight,long budget){
        boolean wide=tabletWide(dpWidth,dpHeight);RemoteViews views=new RemoteViews(context.getPackageName(),wide?R.layout.widget_merged_tablet_landscape:R.layout.widget_merged_tablet_portrait);
        views.setOnClickPendingIntent(R.id.widget_title,open(context,id,"all"));
        Intent refresh=new Intent(context,TabletTrioWidget.class).setAction(REFRESH);views.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(context,id,refresh,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
        float density=context.getResources().getDisplayMetrics().density;
        float panelWidth=Math.max(40,(dpWidth-40)/(wide?3:1)-16),panelHeight=Math.max(32,(dpHeight-104)/(wide?1:3)-62);
        int sourceWidth=Math.max(64,Math.min(2000,Math.round(panelWidth*density))),sourceHeight=Math.max(24,Math.min(2000,Math.round(panelHeight*density)));
        double scale=Math.min(1,Math.sqrt((budget/3.0)/(5.0*sourceWidth*sourceHeight)));
        int width=Math.max(1,(int)(sourceWidth*scale)),height=Math.max(1,(int)(sourceHeight*scale));float drawingDensity=(float)(density*scale);
        int[] titles={R.id.github_title,R.id.codeforces_title,R.id.leetcode_title},summaries={R.id.github_summary,R.id.codeforces_summary,R.id.leetcode_summary},graphs={R.id.github_graph,R.id.codeforces_graph,R.id.leetcode_graph},sections={R.id.github_section,R.id.codeforces_section,R.id.leetcode_section};
        for(int i=0;i<ActivityData.PROVIDERS.length;i++){
            String provider=ActivityData.PROVIDERS[i];ActivityData data=Store.read(context,provider);String user=Store.user(context,provider);
            views.setTextViewText(titles[i],ActivityData.name(provider)+(data==null?"":" · "+data.year));views.setTextColor(titles[i],Graphs.color(provider,4));
            views.setTextViewText(summaries[i],data==null?(user.isEmpty()?"Set username in Profiles":user+" · waiting for data"):data.summary()+"\n"+(Store.error(context,provider).isEmpty()?"Updated · ":"Cached · ")+stamp(data));
            views.setImageViewBitmap(graphs[i],data==null?(Bitmap)null:TabletGraphs.bitmap(data,width,height,drawingDensity));
            views.setContentDescription(graphs[i],ActivityData.name(provider)+" · "+status(context,provider,data)+". Full year; tap for date details.");
            for(int view:new int[]{titles[i],summaries[i],graphs[i],sections[i]})views.setOnClickPendingIntent(view,open(context,id,provider));
        }
        views.setTextViewText(R.id.widget_status,"Full-year activity · Tap a graph for date details");return views;
    }
    private static float positive(Bundle options,String key,float fallback){int value=options.getInt(key,0);return value>0?value:fallback;}
    static RemoteViews tabletForOptions(Context context,int id,Bundle options){
        boolean landscape=context.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        float minWidth=positive(options,AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,600),minHeight=positive(options,AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,600);
        float width=landscape?positive(options,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,minWidth):minWidth;
        float height=landscape?minHeight:positive(options,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,minHeight);
        if(Build.VERSION.SDK_INT>=31)try{
            ArrayList<SizeF> sizes=androidx.core.os.BundleCompat.getParcelableArrayList(options,AppWidgetManager.OPTION_APPWIDGET_SIZES,SizeF.class);SizeF best=null;float distance=Float.MAX_VALUE;
            if(sizes!=null)for(SizeF size:sizes)if(size!=null&&Float.isFinite(size.getWidth())&&Float.isFinite(size.getHeight())&&size.getWidth()>0&&size.getHeight()>0){float delta=Math.abs(size.getWidth()-width)+Math.abs(size.getHeight()-height);if(delta<distance){best=size;distance=delta;}}
            if(best!=null){width=best.getWidth();height=best.getHeight();}
        }catch(RuntimeException ignored){}
        return render(context,id,TabletTrioWidget.class,Math.max(240,Math.min(2400,width)),Math.max(260,Math.min(2400,height)));
    }
    static void updateTablet(Context context,AppWidgetManager manager,int id,Bundle options){
        try{manager.updateAppWidget(id,tabletForOptions(context,id,options));}
        catch(RuntimeException error){
            try{manager.updateAppWidget(id,render(context,id,TabletTrioWidget.class,400,600,128*1024));}
            catch(RuntimeException retry){Store.prefs(context).edit().putString("widget-last-error",retry.getClass().getSimpleName()).apply();}
        }
    }
}
