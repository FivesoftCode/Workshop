package com.fivesoft.kioskcore.common.workshop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class Workshop implements Executor {

    private final Worker[] workers;
    private final int mtpw;
    private volatile Worker lbw;

    public Workshop(int wc, int mtpw) {
        this.workers = new Worker[wc];

        JobDoneCallback jdc = (w, j) -> lbw = findLBW();

        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(mtpw, jdc);
        }

        this.mtpw = mtpw;
        lbw = workers[0];
    }

    public boolean newJob(Job job) {
        Worker w = lbw = findLBW();

        if(w != null) {
            return w.newJob(job);
        } else {
            return false;
        }

    }

    public boolean newJob(Runnable job){
        return newJob(new Job(job, 0, null));
    }

    public int getWorkerCount(){
        return workers.length;
    }

    public int getBusyWorkersCount(){
        int sum = 0;
        for(Worker w: workers){
            if(w != null && w.isWorking()){
                sum++;
            }
        }
        return sum;
    }

    public List<Worker> getWorkers(){
        List<Worker> res = new ArrayList<>();
        for(Worker w: workers){
            if(w != null){
                res.add(w);
            }
        }
        return res;
    }

    public List<String> getTaskDebugStrings(){
        List<String> res = new ArrayList<>();
        int c = 0;
        for(Worker w: workers){
            StringBuilder sb = new StringBuilder();
            List<String> t = w.getTaskDebugStrings();

            sb.append("W_").append(c).append(" (").append(t.size()).append(") ");

            for (int i = 0; i < t.size(); i++) {
                sb.append(t.get(i));
                if(i < t.size() - 1){
                    sb.append(", ");
                }
            }

            res.add(sb.toString());

            c++;
        }
        return res;
    }

    public String getTasksDebugString(){
        StringBuilder sb = new StringBuilder();
        List<String> t = getTaskDebugStrings();

        sb.append("WS: workers: ").append(workers.length)
                .append(" mtpw: ").append(mtpw).append("\n");

        for (int i = 0; i < t.size(); i++) {
            sb.append(t.get(i));
            if(i < t.size() - 1){
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public void clearAll(){
        for(Worker w: workers)
            w.clearTasks();
    }

    public void forceStop(){
        for(Worker w: workers)
            if(w != null) {
                w.forceStop();
            }
    }

    private Worker findLBW(){
        int m = Integer.MAX_VALUE;
        Worker mw = lbw;
        for (Worker w : workers) {
            int tc = w.getTaskCount();
            if (m > tc) {
                m = tc;
                mw = w;
                if(tc == 0){
                    //That's all we need - worker with no tasks scheduled
                    break;
                }
            }
        }
        return mw;
    }

    @Override
    public void execute(Runnable command) {
        newJob(command);
    }

    public static void checkInterrupted(){
        if(Thread.interrupted())
            throw new JobInterruptedException();
    }

}
