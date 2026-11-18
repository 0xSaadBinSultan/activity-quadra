package dev.saad.activitytrio;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Bundle;

/** Separate provider identity avoids reusing a launcher's cached compact widget span. */
public final class TabletTrioWidget extends BaseWidget {
    @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,Bundle options){
        Widgets.updateTablet(context,manager,id,options);
    }
}
