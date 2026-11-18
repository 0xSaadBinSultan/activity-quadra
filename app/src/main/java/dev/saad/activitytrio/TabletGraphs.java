package dev.saad.activitytrio;

import android.graphics.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

/** Full-year graph that wraps whole weeks to use the large tablet panel's height. */
final class TabletGraphs {
    static final class Layout {
        final LocalDate start;
        final int year,weeks,columns,bands;
        final float step,left,top,label,gap,density;
        Layout(int year,int width,int height,float density){
            this.year=year;this.density=density;LocalDate first=LocalDate.of(year,1,1);
            start=first.minusDays(first.getDayOfWeek().getValue()%7);
            weeks=(int)((LocalDate.of(year+1,1,1).toEpochDay()-start.toEpochDay()+6)/7);
            label=18*density;gap=9*density;float availableWidth=Math.max(1,width-26*density);
            int bestColumns=weeks,bestBands=1;float best=0;
            for(int candidate:new int[]{weeks,(weeks+1)/2,(weeks+2)/3,(weeks+3)/4}){
                int rows=(weeks+candidate-1)/candidate;
                float availableHeight=Math.max(1,height-rows*label-(rows-1)*gap-4*density);
                float cell=Math.min(availableWidth/candidate,availableHeight/(rows*7));
                if(cell>best||(bestBands==1&&rows==2&&cell>=best*.94f)){best=cell;bestColumns=candidate;bestBands=rows;}
            }
            columns=bestColumns;bands=bestBands;step=Math.max(.1f,best);
            left=22*density+Math.max(0,(availableWidth-columns*step)/2);
            float content=bands*(label+7*step)+(bands-1)*gap;
            top=Math.max(0,(height-content)/2);
        }
        RectF cell(LocalDate date){
            if(date.getYear()!=year)return null;long index=date.toEpochDay()-start.toEpochDay();int week=(int)(index/7),day=(int)(index%7),band=week/columns,column=week%columns;
            float x=left+column*step,y=top+band*(label+7*step+gap)+label+day*step;
            float side=Math.max(.1f,step-Math.min(step*.3f,Math.max(1,density)));
            return new RectF(x,y,x+side,y+side);
        }
    }
    static Bitmap bitmap(ActivityData data,int width,int height,float density){
        Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);Layout grid=new Layout(data.year,width,height,density);
        Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);text.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));text.setTextSize(10*density);text.setColor(Graphs.MUTED);
        for(int band=0;band<grid.bands;band++){
            float y=grid.top+band*(grid.label+7*grid.step+grid.gap),right=-Float.MAX_VALUE;
            LocalDate bandStart=grid.start.plusWeeks((long)band*grid.columns);LocalDate first=bandStart.isBefore(LocalDate.of(data.year,1,1))?LocalDate.of(data.year,1,1):bandStart;
            for(int month=first.getMonthValue();month<=12;month++){
                LocalDate date=month==first.getMonthValue()?first:LocalDate.of(data.year,month,1);int week=(int)((date.toEpochDay()-grid.start.toEpochDay())/7);
                if(week/grid.columns!=band)break;
                String name=date.getMonth().getDisplayName(TextStyle.SHORT,Locale.ENGLISH);float x=grid.left+(week%grid.columns)*grid.step;
                x=Math.min(x,width-text.measureText(name)-3*density);
                if(x>=right+4*density){canvas.drawText(name,x,y+12*density,text);right=x+text.measureText(name);}
            }
            text.setTextSize(8*density);
            for(int[] entry:new int[][]{{1,'M'},{3,'W'},{5,'F'}})canvas.drawText(String.valueOf((char)entry[1]),grid.left-18*density,y+grid.label+(entry[0]+.7f)*grid.step,text);
            text.setTextSize(10*density);
        }
        LocalDate today=LocalDate.now(ZoneId.of(data.zone));Paint cells=new Paint();
        for(LocalDate date=LocalDate.of(data.year,1,1);date.getYear()==data.year;date=date.plusDays(1)){
            cells.setColor(Graphs.color(data.provider,data.day(date).level));if(date.isAfter(today))cells.setAlpha(76);canvas.drawRect(grid.cell(date),cells);
        }
        return image;
    }
}
