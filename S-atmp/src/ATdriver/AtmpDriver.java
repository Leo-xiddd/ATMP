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
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.*;

import base.*;

public class AtmpDriver{
	static String confpath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=confpath+"\\conf\\atmp\\logconf.properties";
	Logger logger = Logger.getLogger(AtmpDriver.class.getName());

	String testScriptName;		//测试脚本名称
	String TestReport;				//用来存储测试报告名称
	DBDriver dbd = new DBDriver();	
	XMLDriver xml=new XMLDriver();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * @函数说明：执行一个测试配置文件
	 * @param taskid		测试任务编号
	 * @param Testset		测试序列名称	
	 * @param Tasktag	测试开始的行号，如果是新任务，则默认为1
	 * @param tester		测试者
	 * @throws Exception	 500，集成数据库操作错误
	 * @throws Exception	 501，执行脚本出现错误
	 */
	public void ATexec(String taskid, String Testset, int Tasktag,String tester) throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
			TestReport=taskid.replace("Task_", "TR_");			
			String TestResult=RunTS(Tasktag,tester);	
//			如果当前脚本执行结果为stop，则修改任务的状态为stop
			if(TestResult.indexOf("stop")>-1) {
				dbd.UpdateSQl("sys_taskquene",1,"status","停止");
				logger.info("测试任务被中止");
			}
			else{
//				获取测试任务的状态，如果出现error，则为error，依次为fail和pass
				int status=dbd.check(TestReport, "testresult", "error");
				if(status>0)TestResult="异常";
				else {
					status=dbd.check(TestReport, "testresult", "fail");
					if(status>0)TestResult="失败";
					else TestResult="通过";
				}
//				2. 清理临时任务表
				dbd.DelSQl("Temp_Task", 0,0,0);
				
//				3. 任务结束，添加测试报告信息				
				String[][] task=dbd.readDB("sys_taskquene", "proj,starttime", "id=1");
				task[0][1]=sdf.format(sdf.parse(task[0][1]));
				String[] TRlist={"trname","project","testset","owner","result","starttime","creattime"};	
				String[] record={TestReport, task[0][0],Testset, tester,TestResult, task[0][1], sdf.format(new Date())};	
				dbd.AppendSQl("sys_testreports", TRlist,record,1,1);
					
//				4. 在测试任务表中删除执行结束的任务						
				dbd.DelSQl("sys_taskquene", 1,1,1);
					
//				5. 判断是否存在下一个任务，如果存在则标识为'停止'或者'执行中' 
//				特殊情况：当测试任务结束，但是下一个任务的状态为'停止中...'，说明上一个任务暂停的时候刚好执行到最后一个脚本
				int Tasknum=dbd.check("sys_taskquene");
				if(Tasknum>0){
					String a[][]=dbd.readDB("sys_taskquene","status","id='1'");
					if(a[0][0].equals("停止中...")){
						dbd.UpdateSQl("sys_taskquene",1,"status","停止");
						logger.info("测试任务被中止");
					}
					else if(a[0][0].equals("未开始"))dbd.UpdateSQl("sys_taskquene",1,"status","执行中");
				}
			}
		}catch(Throwable e){
			logger.error(e.toString(),e);
			throw new Exception(e.getMessage());
		}
	}
	
	/**[Function] 	读取任务队列的第一个测试集来对temp_task表进行初始化，填写该序列中的所有测试脚本
	 * @throws Exception 404，找不到资源，如测试任务表为空、测试集内容为空、测试集不存在
	 * @throws Exception	 500，数据库故障或者系统错误
	 */
	void init_temptask() throws Exception{
		PropertyConfigurator.configure(logconf);
		logger.info("初始化测试脚本执行容器...");
		try{
//			检查任务队列是否为空
			dbd.DelSQl("Temp_Task", 0,0,0);
			int row=dbd.check("sys_taskquene");
			if(row==0)throw new Exception("[info]404,测试任务表为空，无法初始化脚本队列");
//			检查当前任务对应测试集是否为空
			String[][] tt=dbd.readDB("sys_taskquene", "tset","id=1");	
			String TestsetName="tset_"+tt[0][0];
			int TSnum=dbd.check(TestsetName);
			if(TSnum==0)throw new Exception("[info]404,测试集"+tt[0][0]+"内容为空，无法初始化脚本队列");
			else if(TSnum==-1)throw new Exception("[info]404,测试集"+tt[0][0]+"不存在，无法初始化脚本队列");
			
			String[] pattern={"name","path","policy","status"};			
			String[] record={"","","","norun"};
			String[][] ts;
			for(int i=1;i<=TSnum;i++){
				ts=dbd.readDB(TestsetName, "name,path,policy","id="+i);
				record[0]=ts[0][0];
				record[1]=ts[0][1];
				record[2]=ts[0][2];
				dbd.AppendSQl("Temp_Task", pattern,record,1,1);
			}
		}catch(Throwable e){
			logger.error(e.toString(),e);
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 * @函数说明				执行测试，支持暂停和继续测试
	 * @param tag			测试执行标记，如果是新任务，则默认为1
	 * @param tester		测试执行人员
	 * @return	[String]		测试结果返回pass,fail,stop[n],error
	 */
	String RunTS(int tag,String tester)throws Exception{						   
		PropertyConfigurator.configure(logconf);
		logger.info("开始从数据库读取脚本进行自动化测试...");
		String backpara="pass";					//函数返回值
		try{
			int TScount=dbd.check("Temp_Task");
			if(TScount<1)throw new Exception("[info]500, 系统异常，脚本执行队列无法初始化，请联系管理员！");
			
//			tag=1表示是新任务，创建新报告表
			if(tag==1) {
				if(dbd.check(TestReport)>-1)dbd.DelTable(TestReport);
				String[][] phase= {
					{"id", "int(6) NOT NULL auto_increment PRIMARY KEY"},
					{"tsname", "VARCHAR(100) DEFAULT ''"},
					{"testname", "VARCHAR(100) DEFAULT ''"},
					{"issue", "VARCHAR(400) DEFAULT ''"},
					{"testresult", "VARCHAR(40) DEFAULT ''"},
					{"starttime", "datetime  NOT NULL DEFAULT '0001-01-01'"}
				};
				dbd.CreatTable(TestReport, phase);
			}
//			2. 开始从临时任务表中读取脚本并执行,执行结果写入测试报告			
			int policy=0;									//测试策略值
			String TS_path="";							//存放测试脚本的文件夹名称，路径从\TestScripts之后开始
			String[] TR={"tsname","testname","issue","testresult","starttime"}; 
			for (int i = tag; i <= TScount; i++) {
				String[][] TS=dbd.readDB("Temp_Task", "*","id='"+i+"'");// 循环读取测试配置文件中的每一行,[id,name,path,policy,status]
				String status=TS[0][4];									//获取当前行的状态
				if(status.equals("stop")){
					backpara ="stop["+i+"]";						
					break;
				}
				testScriptName=TS[0][1];					//获取测试脚本名称
				TS_path=TS[0][2];								//获取测试脚本路径
				policy=Integer.parseInt(TS[0][3]);			//获取测试策略
				dbd.UpdateSQl("Temp_Task", i, "status", "running");
				logger.info("开始执行第 " + i + " 个脚本：" + testScriptName);
				String[] testResult = runTestScript(TS_path);	
				dbd.AppendSQl(TestReport, TR, testResult, 1, 1);
				dbd.UpdateSQl("Temp_Task", i, "status", testResult[3]);
				backpara=testResult[3];
//				3. 根据测试策略决定测试是否结束
				if (!backpara.equals("pass")) {					
//					policy=0表示脚本失败后终止执行后续的测试脚本												
					if (policy == 0){						
						logger.info("第 " + i + " 个脚本测试失败，测试任务结束.");
						TScount=i;
						break;
					}
//					policy > 1则表示需要重复执行当前脚本,包括n和n0两种情况判断
					if (policy > 1){							 	
						int nt=policy-1;							// policy：1<n<10，表示脚本失败后,最多重复执行该脚本n-1次，如果失败则继续执行
						if(policy>9)  nt=policy/10-1;		// policy：n>9，表示脚本失败后,最多重复执行该脚本n/10-1次，如果失败则停止任务
						logger.info("第 " + i + " 个脚本测试失败，准备重复测试.");
						for (int num = 0; num < nt; num++) {
							testResult = runTestScript(TS_path);	
							backpara=testResult[3];
							if (backpara.equals("pass")) {									
								logger.info("第 " + i + " 个脚本重复测试第 "+(num+1)+"  次时测试通过.");
								break;
							}
						}
						dbd.UpdateSQl(TestReport, i, "issue", testResult[2]);
						dbd.UpdateSQl(TestReport, i, "testresult", backpara);
						dbd.UpdateSQl("Temp_Task", i, "status", backpara);	
						
//						4. 根据重复测试的结果判断测试是否结束
						if (!backpara.equals("pass")){
							logger.info("第 " + i + " 个脚本重复测试 "+nt+"  次，全部测试不通过.");
							if(policy>10){
								logger.info("测试任务结束.");
								TScount=i;
//								n0表示脚本重复执行n-1仍然失败后终止执行后续的脚本，否则继续执行;
								break;
							}
							logger.info("继续执行后续脚本.");
						}						
					}					
//					policy=1表示脚本失败后继续执行后续的测试脚本，所以这里不做处理，						
				}				
			}			
		}catch (Throwable e){
			try {
				int row=dbd.check("Temp_Task", "status", "running");
				dbd.UpdateSQl("Temp_Task", row, "status", "stop");
			}catch (Throwable e1){
				logger.error(e1.getMessage(),e1);
				throw new Exception(e1.getMessage());
			}
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
		return backpara;
	}
	/**
	 * @函数说明		执行指定的测试脚本，脚本名称通过全局变量传递
	 * @param 		pth  	[String]脚本存放的文件夹名称
	 * @return 		返回脚本的测试结果，数组格式，包括脚本名称、验证内容、问题、测试结果、测试日期
	 */
	String[] runTestScript(String pth) throws Exception{			
		PropertyConfigurator.configure(logconf);
		String[] TSResult={"","","","",""};				//存储测试结果的数组，包括脚本名称、验证内容、问题、测试结果、测试日期
		try {		
			if(pth.equals("")) throw new Exception("[info]500, 脚本路径错误，测试终止");
			pth=pth.replace("\\",".");
			String packageName = "testScripts."+pth+".";
			
			String tScriptName = testScriptName.substring(0,testScriptName.lastIndexOf("."));		
			String className = packageName + tScriptName;		// 获取包名.类名,如:testscript.testset1.T01_Login
			TSResult[0]=className.replace("testScripts.","");		
			TSResult[4]=sdf.format(new Date());
			
			Class<?> clazz = Class.forName(className);				// 加载一个类
			Object obj = clazz.newInstance();									// 创建一个类的实例				
			Method[] method = clazz.getMethods();						// 获取类中的所有方法
			method[0].invoke(obj);												// 调用类中的方法, new Object[] {}							
			Field[] fields = obj.getClass().getDeclaredFields();			 // 调用类中的变量
			TSResult[1]=fields[0].get(obj).toString();
			TSResult[2]=fields[1].get(obj).toString();
			TSResult[3]=fields[2].get(obj).toString();			
		}catch(ClassFormatError e){
			String errinfo="脚本文件格式错误";
			logger.error(errinfo);
			TSResult[2]=errinfo;
			TSResult[3]="error";						
		}catch (Throwable e){
			String errinfo=e.toString();
			if(e instanceof ClassNotFoundException) {
				errinfo="脚本类未找到，请检查脚本代码中的package路径是否与服务器上项目、模块等一致，错误原因："+errinfo;
			}
			logger.error(errinfo,e);
			TSResult[2]=errinfo;
			TSResult[3]="error";
		}
		return TSResult;				// 返回测试脚本的测试结果
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
	 * @return	  字符串格式的测试数据
	 * @throws Exception
	 */
	public String GetTCF(String TCFfile, String key) throws Exception {
		PropertyConfigurator.configure(logconf);
		try {
			String path=confpath+"\\webapps\\S-atmp\\WEB-INF\\classes\\testScripts\\";
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