package com.fivesoft.kioskcore.common.workshop;

public class Job {

    public final Runnable task;
    public final int id;
    public final JobDoneCallback callback;
    public final long maxExecutionTime;

    private volatile Throwable err;
    private volatile long start;
    private volatile long end;

    public Job(Runnable task, int id, JobDoneCallback callback, long maxExecutionTime) {
        this.task = task;
        this.id = id;
        this.callback = callback;
        this.maxExecutionTime = maxExecutionTime;
    }

    public Job(Runnable task, int id, JobDoneCallback callback) {
        this(task, id, callback, Long.MAX_VALUE);
    }

    public boolean run(Worker worker){

        start = System.currentTimeMillis();

        try {

            task.run();

            err = null;
            end = System.currentTimeMillis();

            worker.gc.onDone(worker, this);

            if(callback != null)
                callback.onDone(worker, this);

            return true;
        } catch (Throwable e){
            err = e;
            end = System.currentTimeMillis();

            worker.gc.onDone(worker, this);

            if(callback != null)
                callback.onDone(worker, this);

            return false;
        }
    }

    public Throwable getError(){
        return err;
    }

    public boolean wasRun(){
        return start != 0;
    }

    public boolean wasSuccessful(){
        return err == null && start != 0;
    }

    public long getStart(){
        return start;
    }

    public long getEnd(){
        return end;
    }

    public long getTookTime(){
        return end - start;
    }

}
