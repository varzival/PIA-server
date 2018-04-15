import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


public class Server {

	public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        HttpContext context = server.createContext("/sendPoints", new MyHandler());
        server.setExecutor(null);
        context.getFilters().add(new ParameterFilter());
        System.out.println("Server Running.");
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
        	@SuppressWarnings("unchecked")
			Map<String, Object> params = (Map<String, Object>)t.getAttribute("parameters");
        	
        	String response = "";
        	if (params.get("points") == null)
        	{
        		response = "Points not found.";
        	}
        	else
        	{
        		System.out.println(params.get("points").toString());
        		response = "Points received.";
        	}
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
        }
    }

}
