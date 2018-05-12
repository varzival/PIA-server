import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

public class JavaFXApplication extends Application {

	@Override
	public void start(Stage stage) throws Exception {
		
		String gameId = "jlrnv";
		Document doc = Jsoup.parse("results_base.html", "UTF-8");
		GameStats stats = loadGame(gameId);
        
        CategoryAxis xAxis = new CategoryAxis();
		NumberAxis yAxis = new NumberAxis();
		BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
		chart.setTitle("Siegertreppchen");
		xAxis.setLabel("Nickname");
		yAxis.setLabel("Punkte");
		
		for (int i = 1; i <= 3; i++)
		{
			if (i-1 >= stats.namePointPairs.length) break;
			NamePointPair pair = stats.namePointPairs[i-1];
			if (pair == null) break;
			
			XYChart.Series<String, Number> place = new XYChart.Series<>();
			place.setName("Platz "+i);
			place.getData().add(new XYChart.Data<String, Number>(pair.name, pair.points));
			chart.getData().add(place);
		}
		
		Scene scene = new Scene(chart, 800, 600);
        stage.setTitle("Stats View");        
        stage.setScene(scene);
		//stage.show();
		
		WritableImage image = scene.snapshot(null);
		File file = new File("./images/"+gameId);
		if (!file.isDirectory()) file.mkdir();
		file = new File("./images/"+gameId+"/placement.png");
		try {
	        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
	        //Server.server.createContext("/"+gameId+"/placement.png", new ImageHandler("./images/"+gameId+"/placement.png"));
	    
	        Element imageElement = new Element("img");
	        imageElement.attr("src", "/"+gameId+"/placement.png");
	        imageElement.attr("alt", "Platzierung");
	        imageElement.appendTo(doc.body());
		
		} catch (IOException e) {
	    	System.out.println("Couldn't save image.");
	    	e.printStackTrace();
	    }
		
		doc.title("Spielstatistiken von "+gameId);
    }
	
	static GameStats loadGame(String gameId)
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

	public void createImages(String gameId, GameStats stats, Document doc)
	{
		
	}
	
	public static void main(String[] args) {
        launch(args);
    }
}
