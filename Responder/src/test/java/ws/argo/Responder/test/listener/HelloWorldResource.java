
package ws.argo.Responder.test.listener;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;


@Path("/helloworld")
public class HelloWorldResource {

	public HelloWorldResource() {
		System.out.println("Instantiating HelloWorld");
	}
	
	
    // The Java method will process HTTP GET requests
    @GET 
    // The Java method will produce content identified by the MIME Media
    // type "text/plain"
    @Produces("text/plain")
    public String getIt() {
        return "Hello World";
    }
}
