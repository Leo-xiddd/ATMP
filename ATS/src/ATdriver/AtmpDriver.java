/** 类说明：本模块用于实现ATMP服务器测试管理功能，返回API约定的Json数据
 *  作   者：Leo
 *  时   间：2016/9/30
 *  版   本：V1.0
 *  方   法：本模块支持的方法包括：
 *  	void		ATexec(String Testset,String Type,int Tasktag,String tester)throws Exception
 *  	String 	RunTS(int tag,String tester) throws Exception
 *  	String[] 	runTestScript(String pth) throws Exception
 *  	String	GetTEC(String ProjTag, String key)
 */
package ATdriver;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.log4j.*;

public class AtmpDriver{
	static String confpath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=confpath+"\\conf\\atmp\\logconf.properties";
	static String testconf=confpath+"\\conf\\atmp\\testconfile.txt";
	Logger logger = Logger.getLogger(AtmpDriver.class.getName());
	DBDriver dbd=new DBDriver();
	XMLDriver xml=new XMLDriver();

	/**
	 * @函数说明				根据本地文本配置文件执行测试，不支持暂停和继续测试
	 * @param Testset		本地文本测试配置文件的名称，不带格式后缀
	 * @return					测试结果返回pass,fail
	 * @throws				404 - 测试序列空
	 */
	public void Run(){
		PropertyConfigurator.configure(logconf);
//		1. 读取本地配置文件
		try{
			List<String> list = readConfigFile();
			int TRn=list.size();
//			2. 循环读取测试配置文件中的每一行,格式为：TS_name,policy,description
			for (int i = 1; i < TRn; i++) {
				String line = list.get(i).toString();
				String[] arr = line.split(",");  
				String testScriptName = arr[0]; 
				logger.info("开始执行第" + i + "个脚本：" + testScriptName);
				
				int policy = Integer.parseInt(arr[1]);
				String testResult = runTestScript(testScriptName);				
//				3. 根据测试结果判断失败处理
				if (!testResult.equals("pass")) {						
//					policy=0表示脚本失败后终止执行后续的测试脚本
					if (policy == 0 || testResult.equals("error")){
						logger.info("测试结束.");
						break;
					}
					// 1表示脚本失败后继续执行后续的测试脚本
					else if(policy==1)continue;
					else {  							
						int nt=policy-1;							// policy：1<n<10，表示脚本失败后,最多重复执行该脚本n次，失败后继续执行
						if(policy>9)  nt=policy/10-1;
						for (int num = 0; num < nt; num++) {			
							testResult = runTestScript(testScriptName);
							if (testResult.equals("pass")){
								logger.info("脚本测试通过，共重复执行 "+(num+1)+"次.");
								break;
							}
							else if(testResult.equals("error"))break;
						}
						if(testResult.equals("error")) {
							logger.info("测试结束.");
							break;
						}
						if (!testResult.equals("pass")){	
							logger.info("脚本重复测试 "+nt+"  次，全部测试不通过。");
							//n0表示脚本重复执行n-1仍然失败后终止执行后续的脚本;
							if(policy>9){
								logger.info("测试结束.");
								break;	
							}
						}
					}									
				}
			}
		}catch (Exception e){
			logger.error("执行脚本出现错误,"+e.getMessage(),e);
		}
	}

	/**
	 * @函数说明		执行指定的测试脚本，脚本名称通过全局变量传递
	 * @param 		pth  	[String]脚本存放的文件夹名称
	 * @return 		返回脚本的测试结果，数组格式，包括脚本名称、验证内容、问题、测试结果、测试日期
	 */
	String runTestScript(String className) throws Exception{			
		PropertyConfigurator.configure(logconf);
		String TSResult="";
		try {		
			Class<?> clazz = Class.forName(className);				// 加载一个类
			Object obj = clazz.newInstance();									// 创建一个类的实例				
			Method[] method = clazz.getMethods();						// 获取类中的所有方法
			method[0].invoke(obj);												// 调用类中的方法, new Object[] {}							
			Field[] fields = obj.getClass().getDeclaredFields();			 // 调用类中的变量
			String TestName=fields[0].get(obj).toString();
			String Issue=fields[1].get(obj).toString();
			TSResult=fields[2].get(obj).toString();		
			if(TSResult.equals("fail"))logger.info("测试["+TestName+"]执行失败，问题描述："+Issue);
			if(TSResult.equals("pass"))logger.info("测试["+TestName+"]执行成功。");
		}catch (Throwable e){
			String errinfo=e.getMessage();
			if(e instanceof ClassNotFoundException) {
				errinfo="脚本类未找到，请检查本地测试配置文件中的脚本路径(项目、模块、脚本名)是否正确，错误原因："+errinfo;
			}
			logger.info("测试脚本'"+className+"'执行异常，问题描述："+errinfo,e);
			TSResult="error";
		}
		return TSResult;				// 返回测试脚本的测试结果
	}
	/**
	 * @函数说明	从本地文本文件读取测试序列内容，仅用于本地脚本开发和调试
	 * @return		测试配置文件的所有内容，用list类型返回，每行一个元素
	 */
	List<String> readConfigFile()throws Exception {
		List<String> list = new ArrayList<String>();
		try {
			FileInputStream is = new FileInputStream(testconf);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				if (line.equals("")) continue;
				list.add(line); // 将读取的每一行数据保存到List集合
			}
			br.close();
			isr.close();
			is.close();
			if(list.size()==0)throw new Exception("读取本地测试序列文件错误！原因：文件内容为空!");
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("读取本地测试序列文件错误！原因："+e.getMessage());
		}		
	}
	/**
	 * 函数说明：获取项目的测试配置数据
	 * @param ProjTag	项目标签
	 * @param key			参数名
	 * @return	  参数值
	 * @throws Exception
	 */
	public String GetTEC(String ProjTag, String key) throws Exception {
		PropertyConfigurator.configure(logconf);
		try {
			String DBname="tec_"+ProjTag;
			String[][] vau=dbd.readDB(DBname, "value", "tec_key='"+key+"'");
			return vau[0][0];
		}catch(Throwable e){
			logger.error(e.toString(),e);
			throw new Exception(e.getMessage());
		}		
	}
	/**
	 * 函数说明：获取项目的测试配置数据
	 * @param TCFfile		脚本测试数据文件，含路径，如"proj/module/filename"
	 * @return	  字符串列表格式的测试数据
	 * @throws Exception
	 */
	public String GetTCF(String TCFfile, String key) throws Exception {
		PropertyConfigurator.configure(logconf);
		try {
			String path=confpath+"\\bin\\testScripts\\";
			String TCFfilepath=path+TCFfile;
			FileInputStream is = new FileInputStream(TCFfilepath);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line="";
			String value="";
			while ((line = br.readLine()) != null) {
				if (line.equals("")) continue;
				String temp_key=line.substring(0, line.indexOf(","));
				if(temp_key.equals(key)) {
					value=line.substring(line.indexOf(",")+1);
					break;
				}
			}
			br.close();
			isr.close();
			is.close();
			return value;
		}catch(Throwable e){
			logger.error(e.toString(),e);
			throw new Exception(e.getMessage());
		}		
	}
	public String GetBrowseDriver(String type) {
		String filename=confpath+"\\conf\\atmp\\sys_config.xml";
		String a="";
		try {
			a = xml.GetNode(filename, "Browser/"+type);
		} catch (Throwable e) {
			logger.error(e.getMessage(),e);
		}
		return a;
	}
	/**
	 * @函数说明				查找指定端口的进程
	 * @param port			要查找的进程端口号
	 * @return					boolean值，如果存在该进程，则返回真，否则假
	 */
	public boolean findProcess(String port) {
		BufferedReader bufferedreader = null;
		PropertyConfigurator.configure(logconf);
		try {
			Process proc = Runtime.getRuntime().exec("netstat -nao");
			bufferedreader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			while ((line = bufferedreader.readLine()) != null) {
				if (line.contains(port)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			logger.error(e.toString());
			return false;
		} finally {
			if (bufferedreader != null) {
				try {
					bufferedreader.close();
				} catch (Exception e) {
					logger.error(e.toString());
				}
			}
		}
	}
}