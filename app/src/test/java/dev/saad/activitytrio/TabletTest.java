package dev.saad.activitytrio;

import android.app.*;
import android.appwidget.*;
import android.graphics.*;
import android.os.*;
import android.util.SizeF;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w800dp-h1280dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TabletTest {
    Application context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();Store.prefs(context).edit().clear().apply();TestFixtures.cache(context);}
    static void layout(View view,int width,int height){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}
    static void screenshot(View view,String name)throws Exception{Bitmap image=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(image));File folder=new File(System.getProperty("user.dir"),"build/reports/screenshots");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}}
    void dashboard(String name,int width,int height)throws Exception{
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            View root=c.get().getWindow().getDecorView();layout(root,width,height);String text=ActivityTrioTest.allText(root);for(String provider:ActivityData.PROVIDERS)assertTrue(text.contains(ActivityData.name(provider)));assertTrue(text.contains("23/4000 solved"));assertTrue(text.contains("11 submissions"));
            ArrayList<HeatmapView> graphs=new ArrayList<>();findGraphs(root,graphs);assertEquals(3,graphs.size());for(HeatmapView graph:graphs){assertTrue(graph.getWidth()>250);if(graph!=graphs.get(2))assertTrue(graph.getWidth()<width*.7);assertTrue(graph.getHeight()>0);}screenshot(root,name);
        }
    }
    void findGraphs(View view,List<HeatmapView> result){if(view instanceof HeatmapView)result.add((HeatmapView)view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)findGraphs(((ViewGroup)view).getChildAt(i),result);}
    @Test public void tabletPortraitUsesTwoColumns()throws Exception{dashboard("tablet-portrait.png",800,1280);}
    @Test @Config(qualifiers="w1280dp-h800dp-land-mdpi") public void tabletLandscapeUsesTwoColumns()throws Exception{dashboard("tablet-landscape.png",1280,800);}
    @Test public void widgetsFitPhoneAndTabletSizesWithoutCroppingOrStretching()throws Exception{
        for(int[] size:new int[][]{{300,280},{320,300},{320,340},{320,420},{600,420},{900,500},{1200,600}}){
            RemoteViews views=Widgets.render(context,42,TrioWidget.class,size[0],size[1]);View widget=views.apply(context,new FrameLayout(context));layout(widget,size[0],size[1]);
            int[] ids={R.id.github_graph,R.id.codeforces_graph,R.id.leetcode_graph};
            for(int id:ids){ImageView graph=widget.findViewById(id);assertNotNull(graph.getDrawable());assertEquals(ImageView.ScaleType.FIT_CENTER,graph.getScaleType());assertTrue(graph.getWidth()>0);assertTrue(graph.getHeight()>0);android.graphics.Rect bounds=new android.graphics.Rect();graph.getDrawingRect(bounds);((ViewGroup)widget).offsetDescendantRectToMyCoords(graph,bounds);assertTrue(bounds.right<=size[0]);assertTrue(bounds.bottom<=size[1]);}
            Rect a=new Rect(),b=new Rect(),c=new Rect();View va=widget.findViewById(R.id.github_section),vb=widget.findViewById(R.id.codeforces_section),vc=widget.findViewById(R.id.leetcode_section);va.getDrawingRect(a);vb.getDrawingRect(b);vc.getDrawingRect(c);((ViewGroup)widget).offsetDescendantRectToMyCoords(va,a);((ViewGroup)widget).offsetDescendantRectToMyCoords(vb,b);((ViewGroup)widget).offsetDescendantRectToMyCoords(vc,c);assertTrue(b.top>a.top);assertTrue(c.top>b.top);assertEquals(a.left,b.left);
            String text=ActivityTrioTest.allText(widget);assertTrue(text.contains("23/4000 solved"));assertFalse(text.contains("TryHackMe"));screenshot(widget,"widget-"+size[0]+"x"+size[1]+".png");
        }
    }
    @Test public void launcherReportedExactSizesAndFallbackRangesAreAccepted(){
        Bundle options=new Bundle();options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,new ArrayList<>(Arrays.asList(new SizeF(380,500),new SizeF(720,360))));
        RemoteViews views=Widgets.forOptions(context,42,TrioWidget.class,options);Parcel parcel=Parcel.obtain();try{views.writeToParcel(parcel,0);assertTrue(parcel.dataSize()>0);}finally{parcel.recycle();}
        options.remove(AppWidgetManager.OPTION_APPWIDGET_SIZES);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,380);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,720);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,360);options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,500);assertNotNull(Widgets.forOptions(context,42,TrioWidget.class,options));
    }
}
