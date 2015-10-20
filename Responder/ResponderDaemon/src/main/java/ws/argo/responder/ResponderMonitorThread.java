/*
 * Copyright 2015 Jeff Simpson.
 *
 * Licensed under the MIT License, (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
          .format("[monitor] [%d/%d] [%.3f mps] Active: %d, Completed: %d, Task: %d", this.executor.getPoolSize(), this.executor.getCorePoolSize(), this.responder
              .probesPerSecond(), this.executor.getActiveCount(), this.executor.getCompletedTaskCount(), this.executor.getTaskCount()));
      try {
        Thread.sleep(seconds * 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }
}
