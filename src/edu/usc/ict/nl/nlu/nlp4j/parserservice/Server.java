package edu.usc.ict.nl.nlu.nlp4j.parserservice;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import edu.usc.ict.nl.nlu.nlp4j.DepNLU;


public class Server {
	public static final DepNLU parser = new DepNLU();
	
	public static void main(String[] args) throws Exception{
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
 
        org.eclipse.jetty.server.Server jettyServer = new org.eclipse.jetty.server.Server(8080);
        jettyServer.setHandler(context);
 
        ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);
 
        // Tells the Jersey Servlet which REST service/class to load.
        jerseyServlet.setInitParameter(
           "jersey.config.server.provider.classnames",
           DepParserRestDef.class.getCanonicalName());
 
        try {
            jettyServer.start();
            jettyServer.join();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
            jettyServer.destroy();
        }
        /*
        URI baseUri = UriBuilder.fromUri("http://localhost/").port(9998).build();
		ResourceConfig config = new ResourceConfig(Test.class);
		org.eclipse.jetty.server.Server server = JettyHttpContainerFactory.createServer(baseUri, config);
		*/
	}
}
