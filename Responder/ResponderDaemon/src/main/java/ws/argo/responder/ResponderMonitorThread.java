package ws.argo.responder;

import java.util.concurrent.ThreadPoolExecutor;

public class ResponderMonitorThread implements Runnable {
  private ThreadPoolExecutor executor;

  private int seconds;

  private boolean run = true;

  private Responder responder;

  public ResponderMonitorThread(Responder responder, ThreadPoolExecutor executor, int delay) {
    this.responder = responder;
    this.executor = executor;
    this.seconds = delay;
  }

  public void shutdown() {
    this.run = false;
  }

  @Override
  public void run() {
    while (run) {
      System.out.println(String.format("[monitor] [%d/%d] [%.3f mps] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",
          this.executor.getPoolSize(), this.executor.getCorePoolSize(), this.responder.probesPerSecond(),
          this.executor.getActiveCount(), this.executor.getCompletedTaskCount(), this.executor.getTaskCount(),
          this.executor.isShutdown(), this.executor.isTerminated()));
      try {
        Thread.sleep(seconds * 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }
}
