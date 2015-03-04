#!/bin/sh

java -cp $ARGO_HOME/lib/Responder-0.2.1-SNAPSHOT.jar -Dnet.java.preferIPv4Stack=true ws.argo.Responder.Responder -pf $ARGO_HOME/config/responderConfig.prop
