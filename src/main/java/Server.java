import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.google.gson.Gson;

public class Server {
	
	public static HttpServer server;
	
	public static final String pia_orange = "rgb(242, 132, 55)";
	public static final String grey = "rgb(169,169,169)";
	public static final String red = "rgb(255, 0, 0)";
	public static final String green = "rgb(0, 255, 0)";
	
	public static final int errorCode = 210;
	
	public static final int stations = 5;
	
	// localhost:8000/sendPoints?points=1-2-3-4&nick=maxmuster&gameId=jlrnv
	// localhost:8000/createGame
	// localhost:8000/gameStats?gameId=jlrnv
	public static void main(String[] args) throws Exception {


		//init database
		DBManager.init();
		
		//init server
		int port = Integer.parseInt(System.getenv("PORT"));
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/sendPoints", new PointHandler()).getFilters().add(new ParameterFilter());
        server.createContext("/signUp", new SignUpHandler()).getFilters().add(new ParameterFilter());
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
	
	protected static void sendResponse(int code, String response, HttpExchange t) throws IOException
	{
		t.sendResponseHeaders(code, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
        t.close();
	}
	
	protected static void sendFile(String contentType, HttpExchange he) throws IOException
	{
		Headers headers = he.getResponseHeaders();
        headers.add("Content-Type", contentType);
        
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
	
	protected static String generateHash(String data)
	{
		return DigestUtils.sha1Hex(data);
	}
	
	static class IndexHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			Document doc = Jsoup.parse(new File("html/index.html"), "UTF-8");
			String response = doc.toString();
			Server.sendResponse(200, response, t);
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
			if (!params.containsKey("chart"))
			{
				System.out.println("chart not found");
				sendFail("chart not found", t);
				return;
			}
			String chartStr = (String)params.get("chart");
			
			GameStats stats = loadGame(gameId);
			if (stats == null)
			{
				sendFail("game "+gameId+" does not exist.", t);
				return;
			}
			int stations = stats.opinionStats.length;
			
			Gson gson = new Gson();
			
			LinkedList<String> labels = new LinkedList<>();
			LinkedList<Integer> data = new LinkedList<>();
			
			
			if (chartStr.equals("points"))
			{
				for (int i = 0; i < stats.namePointPairs.length; i++)
				{
					labels.add(stats.namePointPairs[i].name);
					data.add(stats.namePointPairs[i].points);
				}
				String[] colors = new String[stats.namePointPairs.length];
				for (int i=0; i<colors.length; i++) colors[i] = Server.pia_orange;
				ChartsDataset cd = new ChartsDataset(labels.toArray(new String[0]), data.toArray(new Integer[0]), "Punkte", colors);
				String response = gson.toJson(cd);
				Server.sendResponse(200, response, t);
	            return;
			}
			
			if (chartStr.startsWith("opinions"))
			{
				try
				{
					int num = Integer.parseInt(chartStr.charAt(chartStr.length()-1)+"");
					if (num < 0 || num > stations - 1)
					{
						sendFail("unknown chart number: "+num, t);
						return;
					}
					OpinionStats opst = stats.opinionStats[num];
					
					labels.add("pro");
					labels.add("contra");
					labels.add("keine Meinung");
					
					data.add(opst.pro);
					data.add(opst.contra);
					data.add(opst.none);
					
					String[] colors = { Server.green, Server.red, Server.grey };
					ChartsDataset cd = new ChartsDataset(labels.toArray(new String[0]), data.toArray(new Integer[0]), "Meinungen", colors);
					String response = gson.toJson(cd);
					Server.sendResponse(200, response, t);
		            return;

					
				}
				catch (NumberFormatException e)
				{
					sendFail("unknown chart number", t);
					return;
				}
				
			}
			
			sendFail("unknown chart", t);
		}
		
		static void sendFail(String msg, HttpExchange t) throws IOException
        {
        	String response = "Failed requesting game stats: "+msg;
        	Server.sendResponse(Server.errorCode, response, t);
        }

		public static GameStats loadGame(String gameId)
		{
			if (!DBManager.gameExists(gameId))
			{
				return null;
			}
			return DBManager.getStats(gameId);
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
			
        	if (!DBManager.gameExists(gameId))
        	{
        		System.out.println("Couldn't find game "+gameId);
        		sendFail("Couldn't find game "+gameId, t);
        	}
			
        	else
        	{
        		String response = doc.toString();
        		Server.sendResponse(200, response, t);
        	}

		}
		
		
		
		static void sendFail(String msg, HttpExchange t) throws IOException
        {
        	String response = "Failed requesting game stats: "+msg;
        	Server.sendResponse(Server.errorCode, response, t);
        }
		
		
		
	}
	
	static class SignUpHandler implements HttpHandler {
		
	        @Override
	        public void handle(HttpExchange t) throws IOException {
	        	
	        	@SuppressWarnings("unchecked")
				Map<String, Object> params = (Map<String, Object>)t.getAttribute("parameters");
	        	
	        	if (params.get("nick") == null)
	        	{
	        		System.out.println("Nick not found.");
	        		sendFail("nick not found", t);
	        	}
	        	else if (params.get("gameId") == null)
	        	{
	        		System.out.println("GameId not found.");
	        		sendFail("gameId not found", t);
	        	}
	        	else if (params.get("hash") == null)
	        	{
	        		System.out.println("GameId not found.");
	        		sendFail("hash not found", t);
	        	}
	        	else
	        	{
	        		String h = params.get("hash").toString();
	        		String gameId = params.get("gameId").toString();
	        		String nick = params.get("nick").toString();
	        		String full = nick + gameId + "super secret code";
	        		String hashGenerated = Server.generateHash(full);
	        		
	        		if (!h.equals(hashGenerated))
	        		{
	        			System.out.println("Hash read: "+h +"; Hash generated: "+hashGenerated);
	        			sendFail("wrong hash", t);
	        			return;
	        		}
	        		
	        		if (!DBManager.gameExists(gameId))
	        		{
	        			sendFail("game "+gameId+" does not exist", t);
	        			return;
	        		}
	        		
	        		int[] points = new int[Server.stations];
	        		char[] ops = new char[Server.stations];
	        		for (int i = 0; i<ops.length; i++)
	        		{
	        			ops[i] = 'n';
	        		}
	        		
	        		if (DBManager.writePoints(nick, gameId, points, ops))
	        		{
	        			String response = "Sucessfully signed up.";
	        			Server.sendResponse(200, response, t);
	        		}
	        		else
	        		{
	        			sendFail("DB error", t);
	        		}
	        		
	        	}
	        	
	        	
	        }
	        
	        static void sendFail(String msg, HttpExchange t) throws IOException
	        {
	        	String response = "Failed signing up: "+msg;
	        	Server.sendResponse(Server.errorCode, response, t);
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
        	else if (params.get("opinions") == null)
        	{
        		System.out.println("GameId not found.");
        		sendFail("opinions not found", t);
        	}
        	else if (params.get("hash") == null)
        	{
        		System.out.println("GameId not found.");
        		sendFail("hash not found", t);
        	}
        	else
        	{
        		String h = params.get("hash").toString();
        		String ops = params.get("opinions").toString();
        		String pts = params.get("points").toString();
        		String gameId = params.get("gameId").toString();
        		String nick = params.get("nick").toString();
        		String full = nick + gameId + pts + ops + "super secret code";
        		String hashGenerated = Server.generateHash(full);
        		
        		if (!h.equals(hashGenerated))
        		{
        			System.out.println("Hash read: "+h +"; Hash generated: "+hashGenerated);
        			sendFail("wrong hash", t);
        			return;
        		}
        		
        		String[] pointsStr = pts.split("-");
        		String[] opinionsStr = ops.split("-");
				int[] points = new int[pointsStr.length];
				for (int i = 0; i<pointsStr.length; i++)
				{
					points[i] = Integer.parseInt(pointsStr[i]);
				}
				
				char[] opinions = new char[opinionsStr.length];
				for (int i = 0; i<opinionsStr.length; i++)
				{
					if (! (opinionsStr[i].equals("n") || opinionsStr[i].equals("p") || opinionsStr[i].equals("c")))
					{
						sendFail("couldn't save points: opinions not in right format", t);
						return;
					}
					opinions[i] = opinionsStr[i].charAt(0);
				}
				
			
				if(!savePoints(gameId, nick, points, opinions))
				{
					sendFail("couldn't save points", t);
				}
				else
				{
					String response = "Points successfully added.";
					Server.sendResponse(200, response, t);
				}
        	}
        	
        }
        
        static void sendFail(String msg, HttpExchange t) throws IOException
        {
        	String response = "Failed adding points: "+msg;
        	Server.sendResponse(Server.errorCode, response, t);
        }
        
        static boolean savePoints(String gameId, String nickname, int[] points, char[] opinions)
        {
        	return DBManager.writePoints(nickname, gameId, points, opinions);
        }
    }
    
    static class ImageHandler implements HttpHandler{
    	
    	 
        @Override
        public void handle(HttpExchange he) throws IOException {
        	
            Server.sendFile("image/png", he);
        }
    }
    
    static class ScriptHandler implements HttpHandler{
    	
    	@Override
        public void handle(HttpExchange he) throws IOException {
        	
            Server.sendFile("text/javascript", he);
        }
    	
    	
    }
    
    static class StyleHandler implements HttpHandler{
    	
    	@Override
        public void handle(HttpExchange he) throws IOException {
        	Server.sendFile("text/css", he);
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
    		if (DBManager.gameExists(id))
    		{
    			System.out.println("Game "+id+" already exists.");
    			return;
    		}
    		if (!DBManager.writeGame(id))
    		{
    			System.out.println("Couldn't create game "+id);
    			return;
    		}
    		
    		doc.getElementById("gameIDText").text(id);
    		doc.getElementById("gameIDLink").attr("href", "/gameStats?gameId=" + id);
    		
    		Element scr = doc.getElementsByTag("script").first();
    		String oldText = scr.html();
    		scr.text("var gameId = '"+id+"'; \n"+oldText);
    		
    		String response = doc.toString();
    		
            Server.sendResponse(200, response, t);
    	}
    }
}