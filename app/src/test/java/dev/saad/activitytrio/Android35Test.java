package dev.saad.activitytrio;

import android.graphics.*;
import android.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w412dp-h915dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class Android35Test {
    @Test public void dashboardOnTargetSdkRendersAndReceivesUpdates()throws Exception{
        android.content.Context context=RuntimeEnvironment.getApplication();TestFixtures.cache(context);
        try(org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            MainActivity activity=c.get();View view=activity.getWindow().getDecorView();view.measure(View.MeasureSpec.makeMeasureSpec(1236,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2745,View.MeasureSpec.EXACTLY));view.layout(0,0,1236,2745);
            Bitmap image=Bitmap.createBitmap(1236,2745,Bitmap.Config.ARGB_8888);view.draw(new Canvas(image));File folder=new File(System.getProperty("user.dir"),"build/reports/screenshots");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,"dashboard.png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}
            String text=ActivityTrioTest.allText(view);assertTrue(text.contains("GitHub"));assertTrue(text.contains("Codeforces"));assertFalse(text.contains("TryHackMe"));assertTrue(text.contains("LeetCode"));assertTrue(text.contains("ACTIVITY TRIO"));
            android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(context);org.robolectric.shadows.ShadowAppWidgetManager widgets=org.robolectric.Shadows.shadowOf(manager);int id=widgets.createWidget(TrioWidget.class,R.layout.widget_trio);android.os.Bundle options=new android.os.Bundle();options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,380);options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,420);manager.updateAppWidgetOptions(id,options);Widgets.update(context,manager,id,TrioWidget.class);View widget=widgets.getViewFor(id);widget.measure(View.MeasureSpec.makeMeasureSpec(1140,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1260,View.MeasureSpec.EXACTLY));widget.layout(0,0,1140,1260);Bitmap widgetImage=Bitmap.createBitmap(1140,1260,Bitmap.Config.ARGB_8888);widget.draw(new Canvas(widgetImage));try(FileOutputStream out=new FileOutputStream(new File(folder,"combined-widget.png"))){assertTrue(widgetImage.compress(Bitmap.CompressFormat.PNG,100,out));}
            activity.sendBroadcast(new android.content.Intent(SyncJob.UPDATED).setPackage(activity.getPackageName()).putExtra("message","Test refresh"));org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        }
    }
}
