import java.util.Arrays;

public class GameStats {
	
	public NamePointPair[] namePointPairs;
	
	public GameStats(NamePointPair[] namePointPairs)
	{
		this.namePointPairs = namePointPairs;
		Arrays.sort(this.namePointPairs);
	}

}
