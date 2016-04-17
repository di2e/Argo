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

java -cp $ARGO_HOME/responder/lib/* -Djava.util.logging.config.file="$ARGO_HOME/responder/bin/logging.properties" -Dnet.java.preferIPv4Stack=true ws.argo.responder.Responder -pf $ARGO_HOME/responder/config/responderConfig.xml "$@"
