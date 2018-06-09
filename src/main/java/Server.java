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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.google.gson.Gson;

public class Server {
	
	public static HttpServer server;
	
	public static final String pia_orange = "rgb(242, 132, 55)";
	
	public static final int errorCode = 210;
	
	// localhost:8000/sendPoints?points=1,2,3,4&nick=maxmuster&gameId=jlrnv
	// localhost:8000/createGame
	// localhost:8000/gameStats?gameId=jlrnv
	public static void main(String[] args) throws Exception {
		//Server.createImages("jlrnv");
		
		//JavaFXApplication.launch(args);
		
        server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/sendPoints", new PointHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/gameInfo", new GameInfoHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/images/", new ImageHandler());
        server.createContext("/createGame", new GameCreateHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/gameStats", new ResultsHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/scripts/", new ScriptHandler());
        server.createContext("/", new IndexHandler());
        server.createContext("/styles/", new StyleHandler());
        
        server.setExecutor(null);
        System.out.println("Server Running.");
        server.start();
    }
	
	static class IndexHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			Document doc = Jsoup.parse(new File("html/index.html"), "UTF-8");
			String response = doc.toString();
			t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
		}
	}
	
	public static class GameInfoHandler implements HttpHandler {
		
		@Override
		public void handle(HttpExchange t) throws IOException {
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
			
			Gson gson = new Gson();
			
			LinkedList<String> labels = new LinkedList<>();
			LinkedList<Integer> data = new LinkedList<>();
			for (int i = 0; i < stats.namePointPairs.length; i++)
			{
				labels.add(stats.namePointPairs[i].name);
				data.add(stats.namePointPairs[i].points);
			}
			ChartsDataset cd = new ChartsDataset(labels.toArray(new String[0]), data.toArray(new Integer[0]), "Punkte", Server.pia_orange, Server.pia_orange);
			
			String response = gson.toJson(cd);
			t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
		}
		
		static void sendFail(String msg, HttpExchange t) throws IOException
        {
        	String response = "Failed requesting game stats: "+msg;
            t.sendResponseHeaders(errorCode, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
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
	
	
	public static class ResultsHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			Document doc = Jsoup.parse(new File("html/results_base.html"), "UTF-8");
			
			@SuppressWarnings("unchecked")
			Map<String, Object> params = (Map<String, Object>)t.getAttribute("parameters");
			if (!params.containsKey("gameId"))
			{
				System.out.println("gameId not found");
				sendFail("gameId not found", t);
				return;
			}
			String gameId = (String)params.get("gameId");
			
			File gameDir = new File("./games/"+gameId);
        	if (!gameDir.isDirectory())
        	{
        		System.out.println("Couldn't find game "+gameId);
        		sendFail("Couldn't find game "+gameId, t);
        	}
			
        	else
        	{
        		String response = doc.toString();
    			t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                t.close();
        	}

		}
		
		
		
		static void sendFail(String msg, HttpExchange t) throws IOException
        {
        	String response = "Failed requesting game stats: "+msg;
            t.sendResponseHeaders(errorCode, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
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
            t.sendResponseHeaders(errorCode, response.length());
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
    
    static class ScriptHandler implements HttpHandler{
    	
    	@Override
        public void handle(HttpExchange he) throws IOException {
        	
            Headers headers = he.getResponseHeaders();
            headers.add("Content-Type", "text/javascript");
            
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
    
    static class StyleHandler implements HttpHandler{
    	
    	@Override
        public void handle(HttpExchange he) throws IOException {
        	
            Headers headers = he.getResponseHeaders();
            headers.add("Content-Type", "text/css");
            
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
        	Document doc = Jsoup.parse(new File("html/gameCreated.html"), "UTF-8");
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
    		
    		doc.getElementById("gameIDText").text(id);
    		String link = server.getAddress().getHostString()+ "/gameStats?gameId=" + id;
    		doc.getElementById("gameIDLink").text(link);
    		doc.getElementById("gameIDLink").attr("href", "/gameStats?gameId=" + id);
    		
    		String response = doc.toString();
    		
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
    	}
    }
}