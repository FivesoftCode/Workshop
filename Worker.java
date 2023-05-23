package com.fivesoft.kioskcore.common.workshop;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Worker {

    private final Queue<Job> tasks;
    final JobDoneCallback gc;
    private final int maxTasks;
    private volatile Thread tt;

    private volatile Job currentJob;

    public Worker(int maxTasks, JobDoneCallback cb) {
        this.tasks = new ConcurrentLinkedQueue<>();
        this.maxTasks = maxTasks;
        gc = cb;
    }

    public boolean newJob(Job job){
        return putJob(job);
    }

    /**
     * Removes all waiting tasks, but doesn't stop currently running one.
     */

    public void clearTasks(){
        synchronized (tasks){
            tasks.clear();
        }
    }

    public void forceStop(){
        synchronized (tasks){
            tasks.clear();
        }

        try {
            if(tt != null) {
                tt.interrupt();
            }
        } catch (Throwable e){
            e.printStackTrace();
        }

    }

    /**
     * Gets count of pending tasks.
     * @return task count.
     */

    public int getTaskCount(){
        return tasks.size();
    }

    public List<Job> getTasks(){
        List<Job> res;
        synchronized (tasks){
            res = new ArrayList<>(tasks);
        }
        return res;
    }

    public List<String> getTaskDebugStrings(){
        List<String> res = new ArrayList<>();
        synchronized (tasks){
            for(Job job: tasks){
                res.add("J_" + job.id);
            }
        }
        return res;
    }

    /**
     * Checks if this worker is currently doing some job or is waiting for a new one.
     * @return true if does.
     */

    public boolean isWorking(){
        return tt != null && tt.isAlive();
    }

    private boolean putJob(Job job){
        if(tasks.size() >= maxTasks){
            //Job queue is full
            //Do not add next one
            return false;
        } else {
            //Add to job queue
            synchronized (tasks){

                //Add job
                tasks.add(job);

                //Ensure worker is running
                runQueue();

                return true;
            }
        }
    }

    private boolean runQueue(){
        //Check if worker is already active
        if(tt == null || !tt.isAlive()){

            //Worker was sleeping, wake up him

            tt = new Thread(() -> {

                int ii = 0;

                //Worker goes to sleep after ~ 1 second of inactivity
                while (ii < 1000 && !Thread.interrupted()) {

                    //Try to get the next job
                    Job todo;
                    synchronized (tasks) {
                        todo = tasks.peek();
                    }

                    //Save current job
                    currentJob = todo;

                    //Check if there is some job to do
                    if(todo == null) {

                        //There is nothing to do for now
                        ii++; //Increase idle iterations
                        SystemClock.sleep(1);

                        continue;
                    }

                    //There is some job to do
                    //Stop break, go to work
                    ii = 0;

                    //Do the job
                    todo.run(this);

                    //Mark job as done by removing it from the queue
                    synchronized (tasks) {
                        tasks.poll();
                    }

                }

            });

            tt.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
            tt.start();

            return true;

        } else {
            //Worker is active now
            return false;
        }
    }

    private static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
            (t, e) -> {
        throw new RuntimeException(e);
    };

}
