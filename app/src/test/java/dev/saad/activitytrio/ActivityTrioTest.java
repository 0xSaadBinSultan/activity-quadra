package dev.saad.activitytrio;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class ActivityTrioTest {
    private Application context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();
        // Android grants this app-defined signature permission on installation.
        // Robolectric requires that grant to be modeled explicitly on API 28.
        org.robolectric.Shadows.shadowOf(context).grantPermissions(context.getPackageName()+".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
        Store.prefs(context).edit().clear().putInt("year",2026).apply();for(String p:ActivityData.PROVIDERS)new File(context.getFilesDir(),p+".json").delete();}
    private String cell(String id,String date,int level,String tip){return "<td id='"+id+"' data-date='"+date+"' data-level='"+level+"'></td><tool-tip for='"+id+"'>"+tip+"</tool-tip>";}
    private JSONObject snapshot(int year,String user)throws Exception{JSONArray colors=new JSONArray();for(int i=0;i<Year.of(year).length();i++)colors.put(i%2==0?"rgb(255, 255, 255)":"rgb(88, 187, 146)");return new JSONObject().put("provider","tryhackme").put("username",user).put("year",year).put("total",126).put("captured","2026-11-13T19:30:51.930Z").put("colors",colors);}
    @Test public void githubCountsAndZeroAndOtherYear(){ActivityData d=Parsers.github(cell("a","2026-01-01",4,"<b>1,234 contributions</b> on Jan 1")+cell("b","2026-01-02",0,"No contributions on Jan 2")+cell("c","2025-01-01",4,"5 contributions"),"user",2026,"UTC","2026-11-14T12:00:00Z");assertEquals(1234,d.total);assertEquals(2,d.cells.size());assertEquals(Integer.valueOf(0),d.day(LocalDate.of(2026,1,2)).count);}
    @Test public void githubSingleContribution(){assertEquals(1,Parsers.github(cell("a","2026-01-01",1,"1 contribution on Jan 1"),"a",2026,"UTC","2026-01-01T00:00:00Z").total);}
    @Test(expected=IllegalArgumentException.class) public void githubDoesNotInventMissingCounts(){Parsers.github(cell("a","2026-01-01",1,"format changed"),"a",2026,"UTC","2026-01-01T00:00:00Z");}
    @Test(expected=IllegalArgumentException.class) public void githubEmptyIsNotZero(){Parsers.github("<html>Rate limited</html>","a",2026,"UTC","2026-01-01T00:00:00Z");}
    private JSONObject submission(long id,String instant,String verdict)throws Exception{return new JSONObject().put("id",id).put("creationTimeSeconds",Instant.parse(instant).getEpochSecond()).put("verdict",verdict);}
    @Test public void codeforcesTimezoneDuplicatesAndUnsuccessfulAttempts()throws Exception{
        JSONArray rows=new JSONArray().put(submission(1,"2026-01-01T04:30:00Z","OK")).put(submission(2,"2026-01-01T05:30:00Z","WRONG_ANSWER")).put(submission(2,"2026-01-01T05:30:00Z","WRONG_ANSWER")).put(submission(3,"2027-01-01T04:59:00Z","COMPILATION_ERROR")).put(submission(4,"2027-01-01T05:01:00Z","OK"));
        ActivityData d=Parsers.codeforces(rows,"saadbinsultan",2026,"America/New_York","2026-11-14T12:00:00Z");assertEquals(2,d.total);assertEquals(Integer.valueOf(1),d.day(LocalDate.of(2026,1,1)).count);assertEquals(Integer.valueOf(1),d.day(LocalDate.of(2026,12,31)).count);assertEquals(Integer.valueOf(0),d.day(LocalDate.of(2026,6,1)).count);
    }
    @Test public void tryhackmeLeapYearAndNoInventedCounts()throws Exception{ActivityData d=Parsers.tryhackme(snapshot(2024,"0xsaadbinsultan"),"0xSaadBinSultan","UTC");assertEquals(366,d.cells.size());assertTrue(d.snapshot);assertNull(d.day(LocalDate.of(2024,2,29)).count);assertTrue(d.description(LocalDate.of(2024,2,29)).contains("Activity level"));}
    @Test(expected=IllegalArgumentException.class) public void tryhackmeWrongProfileRejected()throws Exception{Parsers.tryhackme(snapshot(2026,"someoneelse"),"0xsaadbinsultan","UTC");}
    @Test(expected=IllegalArgumentException.class) public void tryhackmeIncompleteRejected()throws Exception{JSONObject s=snapshot(2026,"u");s.getJSONArray("colors").remove(0);Parsers.tryhackme(s,"u","UTC");}
    @Test(expected=IllegalArgumentException.class) public void tryhackmeUnknownColorRejected()throws Exception{JSONObject s=snapshot(2026,"u");s.getJSONArray("colors").put(0,"rgb(1,2,3)");Parsers.tryhackme(s,"u","UTC");}
    @Test public void actualBundledSnapshotValid()throws Exception{try(InputStream in=context.getAssets().open("tryhackme-snapshot.json")){ActivityData d=Parsers.tryhackme(new JSONObject(new String(Store.bytes(in,256*1024),StandardCharsets.UTF_8)),"0xsaadbinsultan","America/New_York");assertEquals(126,d.total);assertEquals(365,d.cells.size());assertTrue(d.updated.startsWith("2026-11-13"));}}
    @Test public void jsonRoundtripPreservesUnknownCounts()throws Exception{ActivityData a=Parsers.tryhackme(snapshot(2026,"u"),"u","UTC"),b=ActivityData.fromJson(a.json());assertEquals(a.json().toString(),b.json().toString());assertNull(b.day(LocalDate.of(2026,1,2)).count);}
    @Test public void switchingProfileDoesNotDisplayPreviousUsersGraph(){Store.seed(context);assertNotNull(Store.read(context,"github"));Store.prefs(context).edit().putString("github","another-person").apply();assertNull(Store.read(context,"github"));Store.seed(context);assertNull(Store.read(context,"github"));}
    @Test public void switchingYearRejectsOldLiveDataButRetainsDatedSnapshot(){Store.seed(context);Store.prefs(context).edit().putInt("year",2025).apply();assertNull(Store.read(context,"github"));assertNotNull(Store.read(context,"tryhackme"));assertEquals(2026,Store.read(context,"tryhackme").year);}
    @Test(expected=IOException.class) public void importSizeBounded()throws Exception{Store.bytes(new ByteArrayInputStream(new byte[257]),256);}
    @Test public void everyDateHitsItsSquareAtMultipleAspectRatios(){for(int year:new int[]{2024,2026})for(int[] size:new int[][]{{320,90},{1000,180},{250,500},{640,120},{1200,480}}){Graphs.Layout g=new Graphs.Layout(year,size[0],size[1],1);for(LocalDate d=LocalDate.of(year,1,1);d.getYear()==year;d=d.plusDays(1)){long index=d.toEpochDay()-g.start.toEpochDay();assertEquals(d,g.hit(g.x+(index/7+.4f)*g.step,g.y+(index%7+.4f)*g.step));}assertNull(g.hit(-1,-1));assertTrue(g.x>=19);assertTrue(g.y>=18);assertTrue(g.x+g.weeks*g.step<=size[0]+.01f);assertTrue(g.y+7*g.step<=size[1]+.01f);}}
    @Test public void widgetLayoutsAndResizeRenderWithoutDataAndWithCache(){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);org.robolectric.shadows.ShadowAppWidgetManager shadow=org.robolectric.Shadows.shadowOf(manager);
        @SuppressWarnings("unchecked") Class<? extends android.appwidget.AppWidgetProvider>[] types=new Class[]{TrioWidget.class,GithubWidget.class,CodeforcesWidget.class,TryHackMeWidget.class};
        for(boolean seed:new boolean[]{false,true}){
            for(String provider:ActivityData.PROVIDERS)Store.prefs(context).edit().putString(provider,seed?(provider.equals("github")?"0xSaadBinSultan":provider.equals("codeforces")?"saadbinsultan":"0xsaadbinsultan"):"uncached-user").apply();
            if(seed)Store.seed(context);
            for(int i=0;i<types.length;i++){
                int id=shadow.createWidget(types[i],types[i]==TrioWidget.class?R.layout.widget_trio:R.layout.widget_single);Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280+i*70);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,types[i]==TrioWidget.class?340:140);manager.updateAppWidgetOptions(id,options);Widgets.update(context,manager,id,types[i]);
                View view=shadow.getViewFor(id);String text=allText(view);assertTrue(seed?!text.contains("waiting for data"):text.contains("waiting for data"));if(seed&&types[i]==TrioWidget.class){assertTrue(text.contains("126 events"));assertNotNull(((ImageView)view.findViewById(R.id.github_graph)).getDrawable());}
            }
        }
    }
    @Test public void refreshRequestsAreThrottled(){assertTrue(Widgets.refresh(context));assertFalse(Widgets.refresh(context));}
    @Test public void dashboardOpensAndDetailsUseRealSnapshot(){try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class,new Intent(context,MainActivity.class).putExtra("provider","tryhackme")).setup()){assertTrue(allText(c.get().getWindow().getDecorView()).contains("126 events"));assertTrue(allText(c.get().getWindow().getDecorView()).contains("SNAPSHOT"));assertTrue(allText(c.get().getWindow().getDecorView()).contains("13 Nov"));}}
    static String allText(View v){StringBuilder b=new StringBuilder();if(v instanceof TextView)b.append(((TextView)v).getText()).append('\n');if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)b.append(allText(((ViewGroup)v).getChildAt(i)));return b.toString();}
}
