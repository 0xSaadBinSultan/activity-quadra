package dev.saad.activitytrio;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

/** Small native dashboard. Private cache only; no login, embedded browser or analytics. */
public final class MainActivity extends Activity {
    private LinearLayout body;
    private String selected="all";
    private boolean registered;
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){render();toast(intent.getStringExtra("message"));}};
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(dp(3),1);if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    private TextView label(LinearLayout parent,String value,int size,int color,boolean bold){TextView v=text(value,size,color,bold);parent.addView(v);return v;}
    private void gap(LinearLayout parent,int amount){Space v=new Space(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(amount)));}
    private Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextColor(Graphs.TEXT);b.setTextSize(13);b.setMinHeight(dp(44));b.setMinimumHeight(dp(44));b.setPadding(dp(10),0,dp(10),0);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.rgb(69,84,119)),background(Color.rgb(38,48,67),12),null));b.setOnClickListener(v->action.run());return b;}
    private LinearLayout row(LinearLayout parent){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setOrientation(LinearLayout.HORIZONTAL);parent.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;}
    private void rowButton(LinearLayout row,String title,Runnable action){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);params.setMargins(dp(2),0,dp(2),0);row.addView(button(title,action),params);}
    private void toast(String value){if(value!=null&&!value.isEmpty())Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private static String provider(String value){for(String id:ActivityData.PROVIDERS)if(id.equals(value))return id;return "all";}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(Color.rgb(16,21,31));getWindow().setNavigationBarColor(Color.rgb(16,21,31));
        selected=provider(state==null?getIntent().getStringExtra("provider"):state.getString("provider"));render();SyncJob.maintain(this);
        if(state==null&&!Store.configured(this))getWindow().getDecorView().post(this::settings);
        if(Store.liveConfigured(this)&&System.currentTimeMillis()-Store.prefs(this).getLong("last-request",0)>3_600_000)Widgets.refresh(this);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);selected=provider(intent.getStringExtra("provider"));render();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("provider",selected);}
    @Override protected void onResume(){super.onResume();IntentFilter filter=new IntentFilter(SyncJob.UPDATED);androidx.core.content.ContextCompat.registerReceiver(this,updates,filter,androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);registered=true;Widgets.updateAll(this);render();}
    @Override protected void onPause(){if(registered){unregisterReceiver(updates);registered=false;}super.onPause();}
    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(16,21,31));setContentView(root);
        // Android 15 enforces edge-to-edge: keep controls clear of status/navigation bars.
        root.setOnApplyWindowInsetsListener((view,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());view.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});root.requestApplyInsets();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(22),dp(20),dp(28));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        label(body,"ACTIVITY TRIO",12,Color.rgb(169,187,255),true);gap(body,5);label(body,"A little progress, every day.",26,Graphs.TEXT,true);gap(body,9);
        label(body,"Your public coding & learning activity, together.",14,Graphs.MUTED,false);gap(body,16);
        LinearLayout actions=row(body);rowButton(actions,"Refresh",()->{Widgets.updateAll(this);boolean started=Widgets.refresh(this);toast(started?"Refreshing public activity…":!Store.liveConfigured(this)?"Add a GitHub, Codeforces or LeetCode username in Profiles first.":"Please wait a minute between refresh requests.");});rowButton(actions,"Add widget",this::pinWidget);rowButton(actions,"Profiles",this::settings);gap(body,14);
        if(!selected.equals("all")){body.addView(button("‹ All three",()->{selected="all";render();}));gap(body,8);}
        boolean tablet=selected.equals("all")&&getResources().getConfiguration().screenWidthDp>=700;
        LinearLayout cards=body;
        for(int i=0;i<ActivityData.PROVIDERS.length;i++){
            String id=ActivityData.PROVIDERS[i];if(!selected.equals("all")&&!selected.equals(id))continue;
            if(tablet&&i%2==0){cards=row(body);cards.setGravity(Gravity.TOP);}
            card(id,cards,tablet&&i+1<ActivityData.PROVIDERS.length);
        }
        label(body,"Automatic refresh about every 6 hours while a home-screen widget is present. Android may delay it to save battery. Cached data works offline.",12,Graphs.MUTED,false);gap(body,10);
        label(body,"Public profiles only · no accounts, API keys, ads or analytics. Codeforces counts submission attempts, including unsuccessful ones. LeetCode shows all-time unique solved/total problems and the selected year’s submissions, with UTC calendar dates.",12,Graphs.MUTED,false);
        gap(body,12);body.addView(button("Widget help",this::widgetHelp));
    }
    private void card(String id,LinearLayout parent,boolean tablet){
        ActivityData data=Store.read(this,id);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(background(Color.rgb(25,31,43),22));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(tablet?0:-1,-2,tablet?1:0);cp.bottomMargin=dp(16);if(tablet){cp.leftMargin=dp(5);cp.rightMargin=dp(5);}parent.addView(card,cp);
        LinearLayout header=row(card);TextView title=text(ActivityData.name(id),21,Graphs.color(id,4),true);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView badge=text("PUBLIC",10,Graphs.MUTED,true);header.addView(badge);gap(card,5);
        label(card,(Store.user(this,id).isEmpty()?"Choose your public username in Profiles":Store.user(this,id))+(data==null?"":" · "+data.year),13,Graphs.MUTED,false);gap(card,12);
        if(data==null){label(card,Store.user(this,id).isEmpty()?"Tap Profiles to set up this service. You can leave unused services blank.":"No activity cached yet. Tap Refresh when online.",15,Graphs.TEXT,false);if(!Store.error(this,id).isEmpty()){gap(card,5);label(card,Store.error(this,id),12,Color.rgb(246,200,124),false);}gap(card,14);}
        else{
            label(card,data.summary(),id.equals("leetcode")?19:23,Graphs.TEXT,true);gap(card,4);label(card,Widgets.status(this,id,data),12,Graphs.MUTED,false);
            if(id.equals("leetcode")&&data.problems!=null){org.json.JSONObject breakdown=data.problems.optJSONObject("difficulty");if(breakdown!=null){StringBuilder detail=new StringBuilder();for(String name:new String[]{"Easy","Medium","Hard"}){org.json.JSONObject n=breakdown.optJSONObject(name);if(n!=null){if(detail.length()>0)detail.append(" · ");detail.append(name).append(" ").append(n.optInt("solved")).append("/").append(n.optInt("available"));}}label(card,detail.toString(),12,Graphs.MUTED,false);}}
            if(!data.snapshot&&!Store.error(this,id).isEmpty()){gap(card,4);label(card,Store.error(this,id),12,Color.rgb(246,200,124),false);}
            HeatmapView graph=new HeatmapView(this,data);card.addView(graph,new LinearLayout.LayoutParams(-1,dp(selected.equals("all")?92:210)));
            TextView day=label(card,"Tap a square to see its date"+(selected.equals("all")?"":" · pinch to zoom"),13,Graphs.MUTED,false);graph.onDay(date->{day.setText(data.description(date));day.setTextColor(Graphs.TEXT);});gap(card,8);
            LinearLayout controls=row(card);rowButton(controls,"Choose date",()->chooseDate(data,day));rowButton(controls,selected.equals("all")?"Expand":"Profile",()->{if(selected.equals("all")){selected=id;render();}else openProfile(id);});
        }
        if(selected.equals("all")){gap(card,4);card.addView(button("Open public profile ↗",()->openProfile(id)));}
    }
    private void chooseDate(ActivityData data,TextView result){
        LocalDate now=LocalDate.now(ZoneId.of(data.zone));LocalDate initial=now.getYear()==data.year?now:LocalDate.of(data.year,1,1);
        DatePickerDialog picker=new DatePickerDialog(this,(view,year,month,day)->{result.setText(data.description(LocalDate.of(year,month+1,day)));result.setTextColor(Graphs.TEXT);},initial.getYear(),initial.getMonthValue()-1,initial.getDayOfMonth());
        picker.getDatePicker().setMinDate(LocalDate.of(data.year,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());picker.getDatePicker().setMaxDate(LocalDate.of(data.year,12,31).atTime(23,59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());picker.show();
    }
    private void openProfile(String id){if(Store.user(this,id).isEmpty()){settings();return;}String user=Uri.encode(Store.user(this,id));String url=id.equals("github")?"https://github.com/"+user:id.equals("codeforces")?"https://codeforces.com/profile/"+user:"https://leetcode.com/u/"+user+"/";try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){toast("No browser is available.");}}
    private void pinWidget(){
        String[] names={"All three together","GitHub","Codeforces","LeetCode"};Class<?>[] types=Widgets.TYPES;
        new AlertDialog.Builder(this).setTitle("Add to your home screen").setItems(names,(dialog,which)->{AppWidgetManager manager=AppWidgetManager.getInstance(this);if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new ComponentName(this,types[which]),null,null);else new AlertDialog.Builder(this).setTitle("Add Activity Trio").setMessage("Long-press an empty area of your home screen → Widgets → Activity Trio. Drag the combined or individual widget onto your home screen. Long-press the widget to resize it.").setPositiveButton("OK",null).show();}).setNeutralButton("Preview combined",(d,w)->previewWidget()).setNegativeButton("Cancel",null).show();
    }
    private void previewWidget(){
        FrameLayout holder=new FrameLayout(this);float width=Math.max(250,Math.min(900,getResources().getConfiguration().screenWidthDp-64));
        View widget=Widgets.render(this,0,TrioWidget.class,width,340).apply(this,holder);holder.addView(widget,new FrameLayout.LayoutParams(-1,dp(340)));disablePreviewActions(widget);
        new AlertDialog.Builder(this).setTitle("All three together").setView(holder).setPositiveButton("Close",null).show();
    }
    private void disablePreviewActions(View view){view.setOnClickListener(null);view.setClickable(false);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)disablePreviewActions(((ViewGroup)view).getChildAt(i));}
    private void widgetHelp(){
        StringBuilder message=new StringBuilder("If graphs appear here but not on Home, open the app once to resend cached graphs. After an update, remove and re-add only the combined widget if your launcher still keeps its old layout.\n\nUse Add widget → Preview combined to check the compact three-graph layout view.\n\nLocal status (no tokens or usernames):\n");
        for(String id:ActivityData.PROVIDERS)message.append(ActivityData.name(id)).append(": ").append(Store.read(this,id)==null?"no graph cached":"graph cached").append('\n');
        message.append("Widget send failures: ").append(Store.prefs(this).getInt("widget-update-failures",0));
        new AlertDialog.Builder(this).setTitle("Widget help").setMessage(message).setPositiveButton("OK",null).setNeutralButton("Preview combined",(d,w)->previewWidget()).show();
    }
    private void settings(){
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(dp(22),dp(12),dp(22),0);EditText[] users=new EditText[ActivityData.PROVIDERS.length];
        for(int i=0;i<ActivityData.PROVIDERS.length;i++){String id=ActivityData.PROVIDERS[i];label(fields,ActivityData.name(id)+" public username",13,Graphs.MUTED,false);EditText e=new EditText(this);e.setSingleLine(true);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);e.setText(Store.user(this,id));e.setHint("Optional · your username");e.setContentDescription(ActivityData.name(id)+" public username");fields.addView(e);users[i]=e;}
        label(fields,"Timezone (IANA name)",13,Graphs.MUTED,false);EditText zone=new EditText(this);zone.setSingleLine(true);zone.setText(Store.zone(this));fields.addView(zone);
        label(fields,"Year (0 = current year)",13,Graphs.MUTED,false);EditText year=new EditText(this);year.setSingleLine(true);year.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);year.setText(String.valueOf(Store.prefs(this).getInt("year",0)));fields.addView(year);gap(fields,8);
        label(fields,"Enter at least one username; leave unused services blank. No login or API key is needed. GitHub’s contribution dates and LeetCode’s UTC dates are preserved. The timezone groups Codeforces submissions.",12,Graphs.MUTED,false);
        ScrollView scroll=new ScrollView(this);scroll.addView(fields);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Public profiles").setView(scroll).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                String[] values=new String[ActivityData.PROVIDERS.length];for(int i=0;i<ActivityData.PROVIDERS.length;i++){values[i]=users[i].getText().toString().trim();if(!Store.validUsername(ActivityData.PROVIDERS[i],values[i]))throw new IllegalArgumentException("Check the "+ActivityData.name(ActivityData.PROVIDERS[i])+" username.");}
                boolean any=false;for(String value:values)if(!value.isEmpty())any=true;if(!any)throw new IllegalArgumentException("Enter at least one public username.");
                String tz=zone.getText().toString().trim();ZoneId.of(tz);int chosen=Integer.parseInt(year.getText().toString().trim());if(chosen!=0&&(chosen<2008||chosen>Year.now(ZoneId.of(tz)).getValue()))throw new IllegalArgumentException("Choose 0 or a year from 2008 through this year.");
                // Preserve rate-limit backoff when nothing changed; clear it only for a different query.
                SharedPreferences.Editor edit=Store.prefs(this).edit();boolean datesChanged=!tz.equals(Store.zone(this))||chosen!=Store.prefs(this).getInt("year",0);
                for(int i=0;i<ActivityData.PROVIDERS.length;i++){String id=ActivityData.PROVIDERS[i];if(datesChanged||!values[i].equalsIgnoreCase(Store.user(this,id))){edit.remove(id+".next").remove(id+".error");}edit.putString(id,values[i]);}
                edit.putString("timezone",tz).putInt("year",chosen).remove("last-request").apply();SyncJob.maintain(this);Widgets.updateAll(this);render();dialog.dismiss();Widgets.refresh(this);toast("Profiles saved. Public activity will refresh when online.");
            }catch(Exception e){toast(e instanceof IllegalArgumentException&&e.getMessage()!=null&&! (e instanceof DateTimeException)?e.getMessage():"Enter valid usernames, timezone and year.");}
        }));dialog.show();
    }
}
