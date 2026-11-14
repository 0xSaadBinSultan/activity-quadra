package dev.saad.activitytrio;

import android.app.job.*;
import android.content.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SyncJob extends JobService {
    public static final String UPDATED="dev.saad.activitytrio.UPDATED";
    private static final int PERIODIC=31061,ONCE=31062;
    private static final AtomicBoolean RUNNING=new AtomicBoolean(false);
    private ExecutorService executor;
    private volatile boolean stopped;
    private volatile int activeId=-1;
    public static void schedule(Context context,boolean immediate){
        JobScheduler scheduler=context.getSystemService(JobScheduler.class);if(scheduler==null)return;
        JobInfo.Builder builder=new JobInfo.Builder(immediate?ONCE:PERIODIC,new ComponentName(context,SyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        if(immediate)builder.setOverrideDeadline(0);else builder.setPeriodic(6*60*60*1000L).setPersisted(true);
        scheduler.schedule(builder.build());
    }
    public static void maintain(Context context){
        JobScheduler scheduler=context.getSystemService(JobScheduler.class);if(scheduler==null)return;
        if(Widgets.hasWidgets(context)){if(scheduler.getPendingJob(PERIODIC)==null)schedule(context,false);}else scheduler.cancel(PERIODIC);
    }
    @Override public boolean onStartJob(JobParameters parameters){
        if(!RUNNING.compareAndSet(false,true))return false;
        stopped=false;activeId=parameters.getJobId();executor=Executors.newSingleThreadExecutor();
        executor.execute(()->{
            String result="Cached activity retained";
            try{result=new SyncEngine(this).refresh();}catch(RuntimeException ignored){result="Refresh interrupted · cached activity retained";}finally{
                try{
                Widgets.updateAll(this);Intent update=new Intent(UPDATED).setPackage(getPackageName()).putExtra("message",result);sendBroadcast(update);
                if(!stopped)jobFinished(parameters,false);
                }finally{activeId=-1;executor.shutdown();RUNNING.set(false);}
            }
        });return true;
    }
    @Override public boolean onStopJob(JobParameters parameters){if(parameters.getJobId()!=activeId)return false;stopped=true;if(executor!=null)executor.shutdownNow();return true;}
}
