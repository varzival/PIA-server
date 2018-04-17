import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


public class Server {
	
	static Document baseDoc;

	
	// localhost:8000/sendPoints?points=p
	public static void main(String[] args) throws Exception {
		baseDoc = Jsoup.parse(new File("base.html"),"UTF-8");
		
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        HttpContext context = server.createContext("/sendPoints", new MyHandler());

        File imgDir = new File("./images");
        for (String filename : imgDir.list())
        {
        	File f = new File("./images/"+filename);
        	if (f.isFile())
        	{
        		System.out.println("Found file: "+filename);
        		server.createContext("/"+filename, new ImageHandler(filename));
        	}
        }
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
        	
        	Element respDiv = new Element("div");
        	if (params.get("points") == null)
        	{
        		respDiv.text("Points not found.");
        	}
        	else
        	{
        		respDiv.text(params.get("points").toString());
        	}
        	Document newDoc = baseDoc.clone();
        	respDiv.appendTo(newDoc.body());
        	String response = newDoc.toString();
        	
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
        }
    }
    
    static class ImageHandler implements HttpHandler{
    	
    	String filename;
    	
    	public ImageHandler(String filename) {
    		this.filename = filename;
    	}
    	 
        @Override
        public void handle(HttpExchange he) throws IOException {
        	
            Headers headers = he.getResponseHeaders();
            headers.add("Content-Type", "image/png");
             
            File file = new File ("./images/"+filename);
            byte[] bytes  = new byte [(int)file.length()];
            System.out.println(file.getAbsolutePath());
            System.out.println("length:" + file.length());
             
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            bufferedInputStream.read(bytes, 0, bytes.length);
            bufferedInputStream.close();
 
            he.sendResponseHeaders(200, file.length());
            OutputStream outputStream = he.getResponseBody();
            outputStream.write(bytes, 0, bytes.length);
            outputStream.close();
        }
    }

}
