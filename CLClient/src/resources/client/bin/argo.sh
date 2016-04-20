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

java -cp $ARGO_HOME/client/lib/* -Djava.util.logging.config.file="$ARGO_HOME/client/bin/logging.properties" -Dnet.java.preferIPv4Stack=true net.dharwin.common.tools.cli.api.EntryPoint -pf $ARGO_HOME/client/config/clientConfig.xml "$@"
