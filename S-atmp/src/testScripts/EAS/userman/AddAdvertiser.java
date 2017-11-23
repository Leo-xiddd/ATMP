package testScripts.EAS.userman;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import ATdriver.AtmpDriver;
public class AddAdvertiser {
	//初始化测试参数
	public String TestName="登录系统";
	public String Issue="";
	public String TSResult="pass";
	
	/**
	 * 测试名称：		创建新广告主验证
	 * 发布日期：		2015-5-8
	 * 作      者：		leo
	 * 测试步骤：
	 * 	1. 在已经打开的精准广告投放系统中点击添加广告主，期望结果：打开添加广告主页
	 * 	2. 输入正确的广告主名，期望结果：无
	 *  3. 输入备注信息，期望结果：无
	 *  4. 点击确定按钮，期望结果： 应返回广告主列表，列表中有新广告主ea
	 * @throws Exception 
	 */
	public void AddAdrtiser() throws Exception{
		AtmpDriver ad=new AtmpDriver();		
		String proj="DAD";
		System.setProperty("webdriver.chrome.driver", ad.GetTEC(proj,"Chrome"));
		WebDriver wd=new  ChromeDriver();
		String elem="";
		try{
			//从外部配置文件获取测试环境参数
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
		}catch (NullPointerException e) {
			Issue="没有从配置文件中获取私有配置项";
		}catch (Exception e) {
			if(null!=e.getMessage()){
				Issue=e.getMessage(); // 描述失败现象
				if(Issue.indexOf("no such element")>-1)Issue="找不到页面元素"+elem;
				else if(Issue.length()>200)Issue=Issue.substring(0,200);
			}else{
				Issue="null";
			}
			e.printStackTrace();
			TSResult="fail";
			System.out.println("测试不通过");
		}		
		wd.close();
		wd.quit();
	}			        
}