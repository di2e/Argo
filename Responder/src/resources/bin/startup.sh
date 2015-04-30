#!/bin/sh
# 
#  Copyright 2015 Jeff Simpson.
#
#  Licensed under the MIT License, (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#      http://opensource.org/licenses/MIT
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
# 
# Optional Arguments:
# -nb : This switches the responder in to "no browser" mode.  Meaning that the responder will not
#       send any responses to a "naked probe" or a probe with no contract IDs or service instances specified.
#       Such a "naked probe" is the same as a "select all" request.  This type of request is used with probes
#       from browser or scanning clients.  It's useful when you don't want otherwise correctly formed and
#       authenticated probes from resulting in providing ALL services in a network
# -ni <networkInteface Name> : this tells the responder to listen for mulitcast traffic from a particular 
#       network interface.  Some multi-home systems have specific interfaces that allow outside traffic in.

java -cp $ARGO_HOME/lib/${project.artifactId}-${project.version}.jar -Djava.util.logging.config.file="logging.properties" -Dnet.java.preferIPv4Stack=true ws.argo.Responder.Responder -pf $ARGO_HOME/config/responderConfig.prop
