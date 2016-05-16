This file provides details on how to run an Argo Responder for PlugFest 2016
There are additional details on the Px Wiki at: 
https://confluence.di2e.net/display/PLUGFEST/Service+Discovery

Before the responder can be run the following should be done:
 1. Set the ARGO_HOME evironment variable to the directory that the dodcio-argo-responder.zip
    file was unzipped into.  So you should have an $ARGO_HPME/responder/bin directory

 2. Update the $ARGO_HPME/responder/config/fileServer.prop (instructions in file)
 3. Update the $ARGO_HPME/responder/web/organization.xml (instructions in file)
 4. Update the $ARGO_HPME/responder/web/software-components.xml (instructions in file)
 5. Update the $ARGO_HPME/responder/web/artifacts.xml file with the correct IP Address for 
    your server and add/remove any other artifacts
    
Start the Argo Responder by running the $ARGO_HPME/responder/bin/responder.bat or responder.sh command

Verify you can navigate to http://[server]:8282/artifacts.xml and it prints out the location of the other
artifacts you are exposing to the DoD CIO mashup