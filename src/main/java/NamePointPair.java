public class NamePointPair implements Comparable<NamePointPair>
{
	public String name;
	public int points;
	
	
	
	public NamePointPair(String name, int points) {
		super();
		this.name = name;
		this.points = points;
	}



	@Override
	public int compareTo(NamePointPair o) {
		return (int) (o.points - this.points);
	}
}
