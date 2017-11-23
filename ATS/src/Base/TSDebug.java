package Base;

import ATdriver.AtmpDriver;

public class TSDebug {
	public static void main(String[] args) {
		AtmpDriver ad = new AtmpDriver();
		try {
			ad.Run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
