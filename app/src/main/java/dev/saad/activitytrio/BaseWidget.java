package dev.saad.activitytrio;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
public abstract class BaseWidget extends AppWidgetProvider {
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)Widgets.update(c,m,id,getClass());SyncJob.maintain(c);Widgets.refresh(c);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){Widgets.update(c,m,id,getClass());}
 @Override public void onEnabled(Context c){SyncJob.maintain(c);}
 @Override public void onDisabled(Context c){SyncJob.maintain(c);}
 @Override public void onReceive(Context c,Intent intent){super.onReceive(c,intent);if(Widgets.REFRESH.equals(intent.getAction())){Widgets.updateAll(c);Widgets.refresh(c);}}
}
