import java.util.Arrays;

public class GameStats {
	
	public NamePointPair[] namePointPairs;
	
	public GameStats(NamePointPair[] namePointPairs)
	{
		this.namePointPairs = namePointPairs;
		Arrays.sort(this.namePointPairs);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (NamePointPair np : namePointPairs)
		{
			sb.append(np.name);
			sb.append(" : ");
			sb.append(np.points+"; ");
		}
		return sb.toString();
	}

}
