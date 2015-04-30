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
REM Optional Arguments:
REM -nb : This switches the responder in to "no browser" mode.  Meaning that the responder will not
REM       send any responses to a "naked probe" or a probe with no contract IDs or service instances specified.
REM       Such a "naked probe" is the same as a "select all" request.  This type of request is used with probes
REM       from browser or scanning clients.  It's useful when you don't want otherwise correctly formed and
REM       authenticated probes from resulting in providing ALL services in a network
REM -ni <networkInteface Name> : this tells the responder to listen for mulitcast traffic from a particular 
REM       network interface.  Some multi-home systems have specific interfaces that allow outside traffic in.

java -cp %ARGO_HOME%/lib/${project.artifactId}-${project.version}.jar -Djava.util.logging.config.file="logging.properties" -Dnet.java.preferIPv4Stack=true ws.argo.Responder.Responder -pf %ARGO_HOME%/config/responderConfig.prop
