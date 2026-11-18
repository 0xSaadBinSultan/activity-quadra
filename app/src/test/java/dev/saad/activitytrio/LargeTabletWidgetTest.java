package dev.saad.activitytrio;

import android.app.Application;
import android.appwidget.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.lang.reflect.Method;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="sw800dp-w800dp-h1280dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LargeTabletWidgetTest {
    Application context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();Store.prefs(context).edit().clear().apply();TestFixtures.cache(context);}
    @Test public void tabletPickerUsesFreshLargeProviderAndKeepsThreeIndividuals()throws Exception{
        assertTrue(Widgets.tablet(context));assertArrayEquals(new Class<?>[]{TabletTrioWidget.class,GithubWidget.class,CodeforcesWidget.class,LeetcodeWidget.class},Widgets.pickerTypes(context));
        assertTrue(context.getPackageManager().getReceiverInfo(new ComponentName(context,TabletTrioWidget.class),PackageManager.GET_META_DATA).enabled);
        android.content.res.XmlResourceParser parser=context.getResources().getXml(R.xml.widget_merged_tablet);while(parser.next()!=org.xmlpull.v1.XmlPullParser.START_TAG){}
        String ns="http://schemas.android.com/apk/res/android";assertEquals(3,parser.getAttributeIntValue(ns,"resizeMode",0));assertEquals(8,parser.getAttributeIntValue(ns,"targetCellWidth",0));assertEquals(5,parser.getAttributeIntValue(ns,"targetCellHeight",0));assertNotNull(parser.getAttributeValue(ns,"maxResizeWidth"));assertNotNull(parser.getAttributeValue(ns,"maxResizeHeight"));parser.close();
    }
    @Test @Config(qualifiers="sw412dp-w412dp-h915dp-xxhdpi") public void phoneKeepsItsExistingMergedAndIndividualPicker()throws Exception{
        assertFalse(Widgets.tablet(context));assertArrayEquals(new Class<?>[]{TrioWidget.class,GithubWidget.class,CodeforcesWidget.class,LeetcodeWidget.class},Widgets.pickerTypes(context));
        android.content.res.XmlResourceParser parser=context.getResources().getXml(R.xml.widget_merged_tablet);while(parser.next()!=org.xmlpull.v1.XmlPullParser.START_TAG){}assertTrue((parser.getAttributeIntValue("http://schemas.android.com/apk/res/android","widgetFeatures",0)&AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER)!=0);parser.close();
    }
    @Test public void wrappedYearContainsEveryDateOnceWithSquareCellsInsideBounds(){
        for(int year:new int[]{2024,2026})for(int[] size:new int[][]{{740,270},{390,450},{1200,600},{600,160},{300,140}}){
            TabletGraphs.Layout grid=new TabletGraphs.Layout(year,size[0],size[1],1);Set<String> positions=new HashSet<>();int count=0;
            for(LocalDate day=LocalDate.of(year,1,1);day.getYear()==year;day=day.plusDays(1)){
                RectF cell=grid.cell(day);assertTrue(cell.width()>0);assertEquals(cell.width(),cell.height(),.001f);assertTrue(cell.left>=0&&cell.top>=0&&cell.right<=size[0]+.01f&&cell.bottom<=size[1]+.01f);assertTrue(positions.add(cell.toString()));count++;
            }
            assertEquals(Year.of(year).length(),count);assertNull(grid.cell(LocalDate.of(year-1,12,31)));
        }
        assertTrue(new TabletGraphs.Layout(2026,740,270,1).bands>1);
    }
    @Test public void largeMergedFitsTabletPortraitAndLandscapeAndStaysWithinBudget()throws Exception{
        for(int[] size:new int[][]{{800,1100},{1280,620},{1000,800}}){
            RemoteViews views=Widgets.render(context,5,TabletTrioWidget.class,size[0],size[1]);Method estimate=RemoteViews.class.getDeclaredMethod("estimateMemoryUsage");estimate.setAccessible(true);assertTrue(((Number)estimate.invoke(views)).longValue()<=Widgets.tabletBitmapBudget(context));
            View view=views.apply(context,new FrameLayout(context));TabletTest.layout(view,size[0],size[1]);String text=ActivityTrioTest.allText(view);assertTrue(text.contains("Tablet Large"));assertTrue(text.contains("23/4000 solved"));
            Rect[] panels=new Rect[3];int i=0;
            for(int id:new int[]{R.id.github_graph,R.id.codeforces_graph,R.id.leetcode_graph}){
                ImageView graph=view.findViewById(id);assertNotNull(((BitmapDrawable)graph.getDrawable()).getBitmap());Rect rect=new Rect();graph.getDrawingRect(rect);((ViewGroup)view).offsetDescendantRectToMyCoords(graph,rect);assertTrue(rect.width()>200&&rect.height()>150);assertTrue(rect.right<=size[0]&&rect.bottom<=size[1]);panels[i++]=rect;
            }
            if(Widgets.tabletWide(size[0],size[1])){assertEquals(panels[0].top,panels[1].top);assertTrue(panels[1].left>panels[0].left);}else assertTrue(panels[1].top>panels[0].top);
            TabletTest.screenshot(view,"tablet-large-"+size[0]+"x"+size[1]+".png");
        }
    }
    @Test public void resizeCallbackUsesTheNewSizeEvenWhenManagerOptionsAreStale(){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);org.robolectric.shadows.ShadowAppWidgetManager host=Shadows.shadowOf(manager);int id=host.createWidget(TabletTrioWidget.class,R.layout.widget_merged_tablet_portrait);
        Bundle fresh=new Bundle();fresh.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,1280);fresh.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,620);new TabletTrioWidget().onAppWidgetOptionsChanged(context,manager,id,fresh);
        View view=host.getViewFor(id);TabletTest.layout(view,1280,620);Rect a=new Rect(),b=new Rect();View va=view.findViewById(R.id.github_section),vb=view.findViewById(R.id.codeforces_section);va.getDrawingRect(a);vb.getDrawingRect(b);((ViewGroup)view).offsetDescendantRectToMyCoords(va,a);((ViewGroup)view).offsetDescendantRectToMyCoords(vb,b);assertEquals(a.top,b.top);assertTrue(b.left>a.left);
    }
}
