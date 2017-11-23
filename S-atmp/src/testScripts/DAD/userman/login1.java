package testScripts.DAD.userman;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import ATdriver.AtmpDriver;
public class login1 {  
	//初始化测试参数
	public String TestName="精准广告平台用户登录验证";
	public String Issue="";
	public String TSResult="pass";	
	/**
	 * 测试名称：		登陆精准广告投放系统用户登录验证
	 * 发布日期：		2015-4-22
	 * 作      者：		leo
	 * 测试步骤：
	 * 	1. 打开精准广告投放系统，期望结果：应该打开登录页，标志是存在登录按钮和用户名、密码输入框
	 * 	2. 输入正确的用户名，期望结果：无
	 *  3. 输入正确的密码，期望结果：无
	 *  4. 点击登录按钮，期望结果： 应跳转至登录后页面，标志是
	 */
	
	public void loginin()  throws Exception{		
		AtmpDriver ad=new AtmpDriver();	
		String proj="DAD";
		System.setProperty("webdriver.chrome.driver", ad.GetTEC(proj,"Chrome")); 
		WebDriver wd=new  ChromeDriver();
		String elem="";
		try{			
//			从外部配置文件获取测试环境参数		
			Issue="从外部配置文件获取测试环境地址、登录账号和密码";
			String url = ad.GetTEC(proj,"dad_url");
			String usrname=ad.GetTEC(proj,"dad_usrname");
			String passwd=ad.GetTEC(proj,"password");
			if(usrname.equals(""))Issue="没有从配置文件中获取私有配置项[dad_usrname]";
			else if(passwd.equals(""))Issue="没有从配置文件中获取私有配置项[password]";
			
			//开始测试步骤
			if(Issue.equals("")){
				Issue="在登录页面输入账号密码并登录";
				wd.get(url+"user/view_login");
				elem="[用户文本框]";
		        wd.findElement(By.name("name")).sendKeys(usrname);
		        elem="[密码文本框]";
		        wd.findElement(By.name("password")).sendKeys(passwd);
		        elem="[登录按钮]";
		        wd.findElement(By.cssSelector("a.dad-btn.dad-blue.dad-login-btn")).click();
		        
		        Issue="登录DAD系统成功，页面能够看到‘欢迎您’的文本";
		        wd.manage().timeouts().implicitlyWait(3000, TimeUnit.MILLISECONDS);
		        elem="[欢迎您文本]";
		        wd.findElement(By.partialLinkText("欢迎您"));
		        
		        Issue=Issue+", 登录系统成功";
				System.out.println("测试通过\n");
			}
			else{
				TSResult="error";
				System.out.println("测试不通过,因为"+Issue);
			}
		}catch (Exception e) {
			if(null!=e.getMessage()){
				Issue=e.getMessage(); // 描述失败现象
				if(Issue.indexOf("NullPointerException")>-1)Issue="没有从配置文件中获取私有配置项";
				if(Issue.indexOf("no such element")>-1)Issue="找不到页面元素"+elem;
				if(Issue.length()>200)Issue=Issue.substring(0,200);
			}
			else{
				Issue="null";
			}
			TSResult="fail";
			System.out.println("测试不通过");
		}		
		wd.close();
		wd.quit();
	}
}
