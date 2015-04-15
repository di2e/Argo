REM 
REM Copyright 2015 Jeff Simpson.
REM
REM Licensed under the MIT License, (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM
REM     http://opensource.org/licenses/MIT
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM

java -cp %ARGO_HOME%/lib/${project.artifactId}-${project.version}.jar -Dnet.java.preferIPv4Stack=true ws.argo.Responder.Responder -pf %ARGO_HOME%/config/responderConfig.prop
