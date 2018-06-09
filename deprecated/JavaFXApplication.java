import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.event.ActionEvent;
import javafx.event.Event;

public class JavaFXApplication extends Application {
	
	public static Stage stage;
	
	@Override
	public void start(Stage stage) throws Exception {
		JavaFXApplication.stage = stage;
		
		Server.run();		
		
		//Label label = new Label();
        Button but = new Button("Press me");
        
        but.setOnAction((ActionEvent event) -> {
        	GameStats stats = Server.ResultsHandler.loadGame("jlrnv");
        	stage.fireEvent(new ImageCreationEvent("jlrnv", stats)
        		);});
        
        stage.addEventHandler(ImageCreationEvent.IMAGE_CREATION, new ImageCreationHandler());
        
		//label.setText("Java FX Application");
        
		Scene scene = new Scene(but, 200, 200);
        stage.setTitle("Java FX Application");        
        stage.setScene(scene);
        //stage.show();
	}
		
	public static void main(String[] args) throws Exception {
        launch(args);
    }
}

class ServerThread extends Thread
{
	@Override
	public synchronized void start() {
		try {
			Server.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}


class ImageCreationEvent extends Event
{
	private static final long serialVersionUID = 1L;
	public String gameId;
	public GameStats stats;
	
	public static final EventType<ImageCreationEvent> IMAGE_CREATION = new EventType<ImageCreationEvent>(ANY);
	
	public ImageCreationEvent(String gameId, GameStats stats) {
		super(IMAGE_CREATION);
		this.gameId = gameId;
		this.stats = stats;
	}
}

class ImageCreationHandler implements EventHandler<ImageCreationEvent>
{

	public static void createImages(String gameId, GameStats stats)
	{
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
			place.setName(pair.name);
			place.getData().add(new XYChart.Data<String, Number>(pair.name, pair.points));
			chart.getData().add(place);
		}
		
		WritableImage image = null;
		try {
		Scene scene = new Scene(chart, 800, 600);
		JavaFXApplication.stage.setScene(scene);
		//JavaFXApplication.stage.show();
		
		image = scene.snapshot(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		File file = new File("./images/"+gameId);
		if (!file.isDirectory()) file.mkdir();
		file = new File("./images/"+gameId+"/placement.png");
		try {
	        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		
		} catch (IOException e) {
	    	System.out.println("Couldn't save image.");
	    	e.printStackTrace();
	    }
		
		
		Server.imageCreationMutex = false;
	}
	
	@Override
	public void handle(ImageCreationEvent ev) {
		//createImages(ev.gameId, ev.stats);
		Platform.runLater(new Runnable() {
            @Override public void run() {
            	createImages(ev.gameId, ev.stats);
            }
		});
	}
	
}