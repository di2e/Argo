#!/bin/sh

java -cp Responser-0.2.1-SNAPSHOT.jar -Dnet.java.preferIPv4Stack=true ws.argo.Responder.Responder -pf /opt/argo/responderConfig.prop
