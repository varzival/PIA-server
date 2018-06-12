public class OpinionStats
	{
		public int pro;
		public int contra;
		public int none;
		
		public OpinionStats(int pro, int contra, int none) {
			this.pro = pro;
			this.contra = contra;
			this.none = none;
		}
		
		public int sum()
		{
			return pro + contra + none;
		}
	}
