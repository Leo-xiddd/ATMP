package api;

import java.text.SimpleDateFormat;
import java.util.Date;
import ATdriver.AtmpDriver;
import base.XMLDriver;
import testScripts.DAD.userman.AddAdvertiser;

@SuppressWarnings("unused")
public class TSDebug {
	static String currentProjectPath=System.getProperty("user.dir").replace("\\bin", "");
	public static void main(String[] args) {
		AtmpDriver ad = new AtmpDriver();
		try {
			String b="qw\\w";
			String a=b.replace("\\",".");
			System.out.println(a);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

}
