package dev.saad.activitytrio;

import android.content.Context;
import android.graphics.Canvas;
import android.view.*;
import java.time.LocalDate;

public final class HeatmapView extends android.view.View {
    private final ActivityData data;private final float density;private float zoom=1,pan=0,downX,downY;private boolean dragging=false,scaled=false;
    private final ScaleGestureDetector scale;private OnDay listener;
    public interface OnDay{void choose(LocalDate day);}
    public HeatmapView(Context context,ActivityData data){
        super(context);this.data=data;density=getResources().getDisplayMetrics().density;setContentDescription(data.name()+" activity for "+data.year+". Tap a cell or use Choose date for day details.");
        scale=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector detector){float maxZoom=Math.max(1,Math.min(8,((getHeight()-20*density)/7*53+22*density)/Math.max(1,getWidth())));zoom=Math.max(1,Math.min(maxZoom,zoom*detector.getScaleFactor()));pan=Math.min(0,Math.max(getWidth()*(1-zoom),pan));invalidate();return true;}});
    }
    public void onDay(OnDay listener){this.listener=listener;}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();canvas.translate(pan,0);Graphs.draw(canvas,data,Math.max(1,(int)(getWidth()*zoom)),getHeight(),density);canvas.restore();}
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN)scaled=false;
        scale.onTouchEvent(event);if(event.getPointerCount()>1)scaled=true;if(event.getPointerCount()>1)getParent().requestDisallowInterceptTouchEvent(true);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=event.getX();downY=event.getY();dragging=false;return true;
            case MotionEvent.ACTION_MOVE:
                if(zoom>1&&!scale.isInProgress()){
                    float dx=event.getX()-downX;if(Math.abs(dx)>4*density)dragging=true;pan=Math.min(0,Math.max(getWidth()*(1-zoom),pan+dx));downX=event.getX();getParent().requestDisallowInterceptTouchEvent(true);invalidate();
                }return true;
            case MotionEvent.ACTION_UP:
                if(!dragging&&!scaled&&!scale.isInProgress()&&Math.abs(event.getY()-downY)<12*density){
                    LocalDate day=new Graphs.Layout(data.year,Math.max(1,(int)(getWidth()*zoom)),getHeight(),density).hit(event.getX()-pan,event.getY());if(day!=null&&listener!=null)listener.choose(day);performClick();
                }getParent().requestDisallowInterceptTouchEvent(false);return true;
            case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
