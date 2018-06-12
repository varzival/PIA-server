import java.util.Arrays;

public class GameStats {
	
	public NamePointPair[] namePointPairs;
	public OpinionStats[] opinionStats;
	
	
	public GameStats(NamePointPair[] namePointPairs, OpinionStats[] opinionStats)
	{
		this.namePointPairs = namePointPairs;
		Arrays.sort(this.namePointPairs);
		this.opinionStats = opinionStats;
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
