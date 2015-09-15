package ws.argo.responder;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * The ResponderMonitorThread provides a basic way to check on the state of the
 * running responder thread system and also to check on how much load is being
 * put on the responder in Probes per Second.
 * 
 * @author jmsimpson
 *
 */
public class ResponderMonitorThread implements Runnable {
  private ThreadPoolExecutor executor;

  private int seconds;

  private boolean run = true;

  private Responder responder;

  /**
   * Creates a new ResponderMonitor thread.
   * 
   * @param responder - the responder to monitor
   * @param executor - the executor to check
   * @param delay - how long to wait between checks
   */
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
      //TODO: This looks like it should be logged rather then just pushed to Stdout.
      System.out.println(String
          .format("[monitor] [%d/%d] [%.3f mps] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s", this.executor.getPoolSize(), this.executor.getCorePoolSize(), this.responder
              .probesPerSecond(), this.executor.getActiveCount(), this.executor.getCompletedTaskCount(), this.executor.getTaskCount(), this.executor.isShutdown(), this.executor.isTerminated()));
      try {
        Thread.sleep(seconds * 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }
}
