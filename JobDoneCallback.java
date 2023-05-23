package com.fivesoft.kioskcore.common.workshop;

public interface JobDoneCallback {
    void onDone(Worker worker, Job job);
    default void onInterrupted(Worker worker, Job job, InterruptedException e){

    }
}
