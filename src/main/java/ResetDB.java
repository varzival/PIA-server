
public class ResetDB {

	public static void main(String[] args) {
		
		DBManager.init();
		if (DBManager.resetTables())
		{
			System.out.println("DB reset.");
		}
		
		else
		{
			System.out.println("Error while resetting DB");
		}

	}

}
