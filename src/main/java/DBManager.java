import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

public class DBManager {

	public static Connection dbConnection;
	
	public static void init()
	{
		String dbUrl = System.getenv("JDBC_DATABASE_URL");
		try {
			dbConnection = DriverManager.getConnection(dbUrl);
			
			createTables();

			
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static boolean createTables()
	{
		try {
		Statement smt = dbConnection.createStatement();
		
			smt.executeUpdate("CREATE TABLE IF NOT EXISTS games ("
					+ "gameid char(5) NOT NULL,"
					+ "PRIMARY KEY(gameid)"
					+ ")");
			
			smt.executeUpdate("CREATE TABLE IF NOT EXISTS player_points ("
					+ "nickname varchar(40) NOT NULL,"
					+ "gameid char(5) NOT NULL REFERENCES games,"
					+ "pointstring varchar(20) NOT NULL,"
					+ "PRIMARY KEY(nickname, gameid)"
					+ ")");
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		int[] points = {0, 1, 2, 3, 4};
		int[] points2 = {5, 6, 7, 8, 9};
		int[] points3 = {10, 11, 12, 13, 14};
		
		writeGame("test");
		
		writePoints("A", "test", points);
		writePoints("B", "test", points2);
		writePoints("C", "test", points3);
		
		return true;
	}
	
	public static boolean resetTables()
	{
		try {
			Statement smt = dbConnection.createStatement();
			smt.executeUpdate("DROP TABLE IF EXISTS player_points");
			smt.executeUpdate("DROP TABLE IF EXISTS games");
			
			return createTables();
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean gameExists(String gameId)
	{
		try {
			Statement smt = dbConnection.createStatement();
			String query = "SELECT * "
					+ "FROM games "
					+ "WHERE gameid='"+gameId+"'";
			ResultSet rs = smt.executeQuery(query);
			return rs.next();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean writeGame(String gameId)
	{
		if (gameExists(gameId)) return true;
		else
		{
			try
			{
				Statement smt = dbConnection.createStatement();
				String update;
				StringBuilder rowsb = new StringBuilder();
				rowsb.append("(");
				rowsb.append("'"+gameId+"'");
				rowsb.append(")");
				
				update = "INSERT INTO games VALUES "
							+ rowsb.toString();
				
				
				smt.executeUpdate(update);
				return true;
			}
			catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
		
	}
	
	public static boolean writePoints(String nick, String gameId, int[] points)
	{
		boolean entryExists;
		int[] pointsRead = getPoints(nick, gameId);
		if (pointsRead == null) entryExists = false;
		else entryExists = true;
		
		StringBuilder pointsb = new StringBuilder();
		for (int i = 0; i<points.length; i++)
		{
			pointsb.append(points[i]+"-");
		}
		pointsb.deleteCharAt(pointsb.length()-1);
		
		
		
		try
		{
			Statement smt = dbConnection.createStatement();
			String update;
			if (!entryExists)
			{
				StringBuilder rowsb = new StringBuilder();
				rowsb.append("(");
				rowsb.append("'"+nick+"', ");
				rowsb.append("'"+gameId+"', ");
				rowsb.append("'"+pointsb.toString()+"'");
				rowsb.append(")");
				
				update = "INSERT INTO player_points VALUES "
						+ rowsb.toString();
			}
			else
			{
				update = "UPDATE player_points "
						+ "SET pointstring='"+pointsb.toString()+"' "
						+ "WHERE nickname='"+nick+"'"
						+ " AND gameid='"+gameId+"'";
			}
			smt.executeUpdate(update);
			return true;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static int[] getPoints(String nick, String gameId)
	{
		try {
			Statement smt = dbConnection.createStatement();
			String query = "SELECT t.pointstring "
					+ "FROM player_points t "
					+ "WHERE t.nickname='"+nick+"'"
					+ " AND t.gameid='"+gameId+"'";
			ResultSet rs = smt.executeQuery(query);
			
			
			if (rs.next())
			{
				String qs = rs.getString(1);
				String[] pointstrings = qs.split("-");
				int[] points = new int[pointstrings.length];
				for (int i = 0; i<points.length; i++)
				{
					points[i] = Integer.parseInt(pointstrings[i]);
				}
				return points;
			}

			return null;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static GameStats getStats(String gameId)
	{
		try {
			Statement smt = dbConnection.createStatement();
			String query = "SELECT t.nickname, t.pointstring "
					+ "FROM player_points t "
					+ "WHERE t.gameid='"+gameId+"'";
			ResultSet rs = smt.executeQuery(query);
			
			LinkedList<NamePointPair> npp = new LinkedList<NamePointPair>();
			while (rs.next())
			{
				String name = rs.getString(1);
				String p = rs.getString(2);
				String[] pointstrings = p.split("-");
				int pointSum = 0;
				for (int i = 0; i<pointstrings.length; i++)
				{
					pointSum += Integer.parseInt(pointstrings[i]);
				}
				npp.add(new NamePointPair(name, pointSum));
			}

			return new GameStats(npp.toArray(new NamePointPair[0]));
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
}
