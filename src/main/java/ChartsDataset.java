
public class ChartsDataset {

	String[] labels;
	
	class Dataset
	{
		String label;
		String[] backgroundColor;
		//String borderColor;
		//int borderWidth;
		Integer[] data;
	}
	
	Dataset[] datasets;

	public ChartsDataset(String[] labels, Integer[] data, String datasetLabel, String[] backgroundColor) {
		this.labels = labels;

		Dataset set = new Dataset();
		set.label = datasetLabel;
		set.data = data;
		set.backgroundColor = backgroundColor;
		//set.borderColor = borderColor;
		//set.borderWidth = 1;
		
		datasets = new Dataset[1];
		datasets[0] = set;
	}
	
	
}
