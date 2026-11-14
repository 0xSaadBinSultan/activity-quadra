package dev.saad.activitytrio;

import android.graphics.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

public final class Graphs {
    public static final int BG=Color.rgb(19,22,30),TEXT=Color.rgb(237,241,250),MUTED=Color.rgb(163,175,196);
    public static final class Layout {
        public final LocalDate start;public final int year,weeks;public final float step,x,y;
        public Layout(int year,int width,int height,float density){
            this.year=year;LocalDate first=LocalDate.of(year,1,1);start=first.minusDays(first.getDayOfWeek().getValue()%7);weeks=(int)((LocalDate.of(year+1,1,1).toEpochDay()-start.toEpochDay()+6)/7);
            float left=19*density,top=18*density;step=Math.max(1,Math.min((width-left-3*density)/weeks,(height-top-2*density)/7));
            x=left+Math.max(0,(width-left-3*density-weeks*step)/2);y=top+Math.max(0,(height-top-2*density-7*step)/2);
        }
        public LocalDate hit(float px,float py){
            int column=(int)Math.floor((px-x)/step),row=(int)Math.floor((py-y)/step);if(column<0||column>=weeks||row<0||row>=7)return null;
            LocalDate date=start.plusDays(column*7L+row);return date.getYear()==year?date:null;
        }
    }
    public static int color(String provider,int level){
        int[][] values={{0x283041,0x174d36,0x236d41,0x37a85b,0x72df91},{0x283041,0x283f64,0x315d94,0x4682c4,0x83b7ee},{0x283041,0x465936,0x687c40,0x91a64a,0xb5d962}};
        int index=provider.equals("github")?0:provider.equals("codeforces")?1:2;return 0xff000000|values[index][Math.max(0,Math.min(4,level))];
    }
    public static void draw(Canvas canvas,ActivityData data,int width,int height,float density){
        Layout g=new Layout(data.year,width,height,density);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(8*density);paint.setColor(MUTED);
        for(int[] label:new int[][]{{1,'M'},{3,'W'},{5,'F'}})canvas.drawText(String.valueOf((char)label[1]),g.x-18*density,g.y+(label[0]+.7f)*g.step,paint);
        for(int month=1;month<=12;month++){
            int week=(int)((LocalDate.of(data.year,month,1).toEpochDay()-g.start.toEpochDay())/7);String name=Month.of(month).getDisplayName(TextStyle.SHORT,Locale.ENGLISH);
            canvas.drawText(name,g.x+week*g.step,g.y-5*density,paint);
        }
        LocalDate today=LocalDate.now(ZoneId.of(data.zone));paint.setAntiAlias(false);
        for(int week=0;week<g.weeks;week++)for(int day=0;day<7;day++){
            LocalDate date=g.start.plusDays(week*7L+day);if(date.getYear()!=data.year)continue;paint.setColor(color(data.provider,data.day(date).level));if(date.isAfter(today))paint.setAlpha(76);
            float side=Math.max(.5f,g.step-Math.max(1,density));canvas.drawRect(g.x+week*g.step,g.y+day*g.step,g.x+week*g.step+side,g.y+day*g.step+side,paint);
        }
    }
    public static Bitmap bitmap(ActivityData data,int width,int height,float density){Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);draw(new Canvas(bitmap),data,width,height,density);return bitmap;}
}
