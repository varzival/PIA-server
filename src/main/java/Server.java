import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javafx.application.Application;

public class Server {
	
	static Document baseDoc;
	public static HttpServer server;
	public static Application javaFxApp;
	
	public static boolean imageCreationMutex;
	
	// localhost:8000/sendPoints?points=1,2,3,4&nick=maxmuster&gameId=jlrnv
	// localhost:8000/createGame
	// localhost:8000/gameStats?gameId=jlrnv
	public static void run() throws Exception {
		//Server.createImages("jlrnv");
		baseDoc = Jsoup.parse(new File("base.html"),"UTF-8");
		//JavaFXApplication.launch(args);
		
        server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/sendPoints", new PointHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/images/", new ImageHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/createGame", new GameCreateHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/gameStats", new ResultsHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/test", new TestHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/", new DefaultHandler());
        
        server.setExecutor(null);
        System.out.println("Server Running.");
        server.start();
    }
	
	static void createImages(String gameId)
	{
		GameStats stats = Server.ResultsHandler.loadGame("jlrnv");
    	JavaFXApplication.stage.fireEvent(new ImageCreationEvent("jlrnv", stats));
	}
	
	static class DefaultHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			String response = "Server running.";
			t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
		}
	}
	
	static class TestHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			Document doc = Jsoup.parse(new File("imgtest.html"), "UTF-8");
			String response = doc.toString();
			t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
		}
	}
	
	
	public static class ResultsHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			Document doc = Jsoup.parse(new File("results_base.html"), "UTF-8");
			
			@SuppressWarnings("unchecked")
			Map<String, Object> params = (Map<String, Object>)t.getAttribute("parameters");
			if (!params.containsKey("gameId"))
			{
				System.out.println("gameId not found");
				sendFail("gameId not found", t);
				return;
			}
			String gameId = (String)params.get("gameId");
			
			GameStats stats = loadGame(gameId);
			System.out.println("Stats: "+stats.toString());
			
			Server.imageCreationMutex = true;
	    	JavaFXApplication.stage.fireEvent(new ImageCreationEvent(gameId, stats));
			
	    	int maxWaitTime = 2000;
	    	while (imageCreationMutex)
	    	{
	    		try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	    		maxWaitTime -= 100;
	    		if (maxWaitTime < 0)
	    		{
	    			System.out.println("Image Creation timed out.");
	    			return;
	    		}
	    	}
	    	
			addImagesToDoc("jlrnv", doc);
			String response = doc.toString();
			t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
			
			/*
			if (params.get("gameId") == null)
        	{
        		System.out.println("GameId not found.");
        		sendFail("gameId not found", t);
        	}

			else
			{
			
				String gameId = (String)params.get("gameId");
				GameStats stats = loadGame(gameId);
				createImages(gameId, stats, doc);
				String response = doc.toString();
				t.sendResponseHeaders(200, response.length());
	            OutputStream os = t.getResponseBody();
	            os.write(response.getBytes());
	            os.close();
	            t.close();
				System.out.println("Not implemented.");
        		sendFail("not implemented", t);
			}
			*/
			
		}
		
		static void sendFail(String msg, HttpExchange t) throws IOException
        {
        	String response = "Failed requesting game stats: "+msg;
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
        }
		
		
		static void addImagesToDoc(String gameId, Document doc)
		{
			Element imageElement = doc.body().getElementById("platzierung");
	        imageElement.attr("src", "/images/"+gameId+"/placement.png");
	        imageElement.appendTo(doc.body());

			doc.title("Spielstatistiken von "+gameId);
		}
		
		public static GameStats loadGame(String gameId)
		{
			File gameDir = new File("./games/"+gameId);
	    	if (!gameDir.isDirectory())
	    	{
	    		System.out.println("Couldn't find game "+gameId);
	    		return null;
	    	}

			LinkedList<NamePointPair> allPoints = new LinkedList<>();
	    	for (File personFile : gameDir.listFiles())
	    	{
	    		try {
					BufferedReader fr = new BufferedReader(new FileReader(personFile));
					String name = fr.readLine();
					String contents = fr.readLine();
					fr.close();
					
					String[] pointsStr = contents.split(",");
					int points = 0;
					for (int i = 0; i<pointsStr.length; i++)
					{
						points += Integer.parseInt(pointsStr[i]);
					}
					allPoints.add(new NamePointPair(name, points));
	    		} catch (FileNotFoundException e) {
					System.out.println("File reading went wrong:");
					e.printStackTrace();
				} catch (IOException e) {
					System.out.println("File reading went wrong:");
					e.printStackTrace();
				} catch (NumberFormatException e)
	    		{
					System.out.println("File corrupted:");
					e.printStackTrace();
	    		}
	    		
	    	}
	    	
	    	GameStats stats = new GameStats(allPoints.toArray(new NamePointPair[0]));
			return stats;
		}
	}
	
    static class PointHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
        	@SuppressWarnings("unchecked")
			Map<String, Object> params = (Map<String, Object>)t.getAttribute("parameters");
        	
        	if (params.get("points") == null)
        	{
        		System.out.println("Points not found.");
        		sendFail("points not found", t);
        	}
        	else if (params.get("nick") == null)
        	{
        		System.out.println("Nick not found.");
        		sendFail("nick not found", t);
        	}
        	else if (params.get("gameId") == null)
        	{
        		System.out.println("GameId not found.");
        		sendFail("gameId not found", t);
        	}
        	else
        	{
        		String[] pointsStr = params.get("points").toString().split(",");
				int[] points = new int[pointsStr.length];
				for (int i = 0; i<pointsStr.length; i++)
				{
					points[i] = Integer.parseInt(pointsStr[i]);
				}
				if(!savePoints(params.get("gameId").toString(), params.get("nick").toString(), points))
				{
					sendFail("couldn't save points", t);
				}
				else
				{
					String response = "Points successfully added.";
		            t.sendResponseHeaders(200, response.length());
		            OutputStream os = t.getResponseBody();
		            os.write(response.getBytes());
		            os.close();
		            t.close();
				}
        	}
        	
        	/*
        	Document newDoc = baseDoc.clone();
        	respDiv.appendTo(newDoc.body());
        	String response = newDoc.toString();
        	*/
        	
        }
        
        static void sendFail(String msg, HttpExchange t) throws IOException
        {
        	String response = "Failed adding points: "+msg;
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
        }
        
        static boolean savePoints(String gameId, String nickname, int[] points)
        {
        	File gameDir = new File("./games/"+gameId);
        	if (!gameDir.isDirectory())
        	{
        		System.out.println("Couldn't find game "+gameId);
        		return false;
        	}
        	File personFile = new File("./games/"+gameId+"/"+nickname+".txt");
        	try {
				FileWriter fw = new FileWriter(personFile);
				StringBuilder sb = new StringBuilder();
				sb.append(nickname+"\n");
				for (int p : points)
				{
					sb.append(p+",");
				}
				sb.deleteCharAt(sb.length()-1);
				fw.write(sb.toString());
				fw.close();
			} catch (IOException e) {
				System.out.println("IO Exception while creating "+"./games/"+gameId+"/"+nickname);
				return false;
			}
        	return true;
        }
    }
    
    static class ImageHandler implements HttpHandler{
    	
    	/*
    	String filename;
    	
    	public ImageHandler(String filename) {
    		this.filename = filename;
    	}
    	*/
    	 
        @Override
        public void handle(HttpExchange he) throws IOException {
        	
            Headers headers = he.getResponseHeaders();
            headers.add("Content-Type", "image/png");
            
            String uriPath = he.getRequestURI().getPath();
            String filename = "."+uriPath;
            
            File file = new File (filename);
            byte[] bytes  = new byte [(int)file.length()];
             
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            bufferedInputStream.read(bytes, 0, bytes.length);
            bufferedInputStream.close();
 
            he.sendResponseHeaders(200, file.length());
            OutputStream outputStream = he.getResponseBody();
            outputStream.write(bytes, 0, bytes.length);
            outputStream.close();
        }
    }
    
    static class GameCreateHandler implements HttpHandler
    {
    	
        @Override
        public void handle(HttpExchange he) throws IOException {
        	//@SuppressWarnings("unchecked")
    		//Map<String, Object> params = (Map<String, Object>)he.getAttribute("parameters");
        	
        	//random String
        	String s = "";
        	Random random = new Random();
        	while (s.equals(""))
        	{
        		int leftLimit = 97; // letter 'a'
        	    int rightLimit = 122; // letter 'z'
        	    int targetStringLength = 5;
        	    
        	    StringBuilder buffer = new StringBuilder(targetStringLength);
        	    for (int i = 0; i < targetStringLength; i++) {
        	        int randomLimitedInt = leftLimit + (int) 
        	          (random.nextFloat() * (rightLimit - leftLimit + 1));
        	        buffer.append((char) randomLimitedInt);
        	    }
        	    s = buffer.toString();
        	    
        	    if (new File("./games/"+s).isDirectory())
        	    	s = "";
        	}
        	
        	createGame(s, he);
        	
        	
        }
        
        static void createGame(String id, HttpExchange t) throws IOException
    	{
    		File gameDir = new File("./games/"+id);
    		if (gameDir.isDirectory())
    		{
    			System.out.println("Game "+id+" already exists.");
    			return;
    		}
    		if (!gameDir.mkdir())
    		{
    			System.out.println("Couldn't create game "+id);
    			return;
    		}
    		String response = "Game created: "+id;
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
    	}
    }
}