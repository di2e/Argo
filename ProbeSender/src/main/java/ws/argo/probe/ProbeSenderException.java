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

package ws.argo.probe;

/**
 * 
 * @author jmsimpson
 *
 */
public class ProbeSenderException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -6382572996472308231L;

  public ProbeSenderException() {
  }

  public ProbeSenderException(String message) {
    super(message);    // TODO Auto-generated constructor stub
  }

  public ProbeSenderException(Throwable cause) {
    super(cause);
  }

  public ProbeSenderException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProbeSenderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
