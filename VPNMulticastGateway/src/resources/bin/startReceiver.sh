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
# Expected arguments on the command line include:
# -debug -> turns on output of debugging level log messages
# -dnr -> if this switch is set then "do not repeat" the traffic onto the multicast group
# -ma -> (required) The multicast group to listen to join.  The standard Argo group is 230.0.0.1 
# -mp -> (required) The multicast port associated with the group.  The standard Argo port is 4003
# -up -> (required) The unicast port of the gateway receiver on the other side of the VPN 
# -ni -> (required) The name of the network interface to listen and send on.  e.g. eth0, en0, utun1 or wherever network NIC you like (see ifconfig in unix and ipconfig in Windows)#
# to change the logging level please edit logging.properties


java -cp $GW_HOME/lib/${project.artifactId}-${project.version}.jar -Djava.util.logging.config.file="$GW_HOME/bin/logging.properties" -Dnet.java.preferIPv4Stack=true ws.argo.MCGateway.GatewayReceiver "$@"
