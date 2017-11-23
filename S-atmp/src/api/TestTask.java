/** 类说明：本模块用于实现ATMP服务与测试任务、测试集相关的操作API
 *  作   者：Leo
 *  时   间：2017/10/24
 *  版   本：V2.3.0
 *  方   法：本模块支持的方法包括：
 *  	1. 添加测试任务			Void 		AddTestTask(String usr,String ProjTag, String TSet)
 *  	2. 	删除测试任务			Void 		DelTask(String TaskID)
 *  	3. 	列出所有测试任务	String 	ListTask(String filter, String page_count,String page_num)
 *  	4. 运行测试任务			Void 		RunTask(String taskno)
 *  	5. 停止测试任务			Void 		StopTask()
 *  	6. 添加测试集				Void 		AddTestSet(String opt, String usr, String Tset_name, String Tset_dat) 
 *  	7. 删除测试集				Void 		DelTestSet(String TSet)
 *  	8. 获取指定测试集		String	GetTestSet(String TSet)
 *  	9. 列出测试集列表		String 	ListTestSet(String ProjTag, String filter, String page_count,String page_num)
 */
package api;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.*;
import ATdriver.ATFactory;
import base.DBDriver;

public class TestTask {
	DBDriver dbd = new DBDriver();
//	配置日志属性文件位置
	static String confpath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=confpath+"\\conf\\atmp\\logconf.properties";
	Logger logger = Logger.getLogger(TestTask.class.getName());
	SimpleDateFormat sdf_full = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**[Function] 				API解释模块，根据API检查必要的参数和请求数据的完整性，调用对应API实现模块
	 * @param API			API字符串
	 * @param Param		API请求中URL携带的参数表
	 * @param body		API请求携带的body数据，Json格式字符串
	 * @param token		校验字，API请求携带的header参数，用来保证报文的可靠性和安全性
	 * @return [String]		Json格式字符串，返回API执行的结果
	 */
	public String DoAPI(String API,Map<String, String[]> Param,String body){						
		PropertyConfigurator.configure(logconf);
		logger.info("API: "+API+" "+" [Body]"+body);
		String backvalue="412,http 请求的参数缺失或无效";
		String usr=checkpara(Param,"usr");
		String ProjTag=checkpara(Param,"proj");
		String TSet=checkpara(Param,"tset");
		String TaskID=checkpara(Param,"task");
		String filter=checkpara(Param,"filter");
		String page_count=checkpara(Param,"page_count");
		String page_num=checkpara(Param,"page_num");
		try{
			switch(API){
				case "AddTestTask":   
					if(!ProjTag.equals("") && !TSet.equals("")) {
						logger.info("添加新任务("+ProjTag+"_"+TSet+")");
						AddTestTask(usr,ProjTag,TSet);
						backvalue="200,ok"; 
					}
					break;
				case "DelTask":   
					if(!TaskID.equals("")) {
						logger.info("删除任务"+TaskID);
						DelTask(TaskID);
						backvalue="200,ok"; 
					}
					break;
				case "ListTask":
					logger.info("列出所有用户账户...");
					return ListTask(filter, page_count, page_num);	
				case "RunTask":  
					String taskno=checkpara(Param,"taskno");
					if(taskno.equals("0") | taskno.equals("1")) {
						logger.info("开始执行任务...");
						RunTask(taskno);
						backvalue="200,ok"; 
					}
					break;
				case "StopTask": 
					logger.info("停止测试任务...");
					StopTask();
					backvalue="200,ok"; 
					break;
				case "AddTestSet":  
					String opt=checkpara(Param,"type");
					if(!opt.equals("") && !usr.equals("") && !body.equals("")) {
						if(opt.equals("addnew"))logger.info("添加新测试集"+TSet+"...");
						else logger.info("修改测试集"+TSet+"...");
						AddTestSet(opt, usr, TSet, body);
						backvalue="200,ok"; 
					}
					break;
				case "DelTestSet":  
					if(!TSet.equals("")) {
						DelTestSet(TSet);
						backvalue="200,ok"; 
					}
					break;				
				case "ListTestSet": 
					if(!ProjTag.equals("")) {
						logger.info("列出项目(Tag: "+ProjTag+")下的测试集");
						return ListTestSet(ProjTag, filter, page_count, page_num);	
					}					
					break;
				case "GetTestSet":    
					if(!TSet.equals("")) {
						logger.info("获取测试集"+TSet);
						return GetTestSet(TSet);
					}
					break;					
				default:
					logger.error("无效API: "+API);
					backvalue="400,无效API!";
			}
		}catch (Throwable e) {
			backvalue=e.getMessage();
			int firtag=backvalue.lastIndexOf("[info]");
			if(firtag>-1) backvalue=backvalue.substring(firtag+6);
			else backvalue="500,"+backvalue;
			logger.error(backvalue,e);
		}	
		String code=backvalue.substring(0,backvalue.indexOf(","));
		String message=backvalue.substring(backvalue.indexOf(",")+1);
		backvalue="{\"code\":"+code+",\"message\":\""+message+"\"}";
		return backvalue;
	}
	
	/**[Function] 				添加一个新的测试任务
	 * @param usr			当前发起请求的用户
	 * @param ProjTag	项目标签
	 * @param TSet			测试集名
	 * @throws Exception, 404 测试集不存在  
	 */
	void AddTestTask(String usr,String ProjTag, String TSet) throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
//			判断测试集是否存在
			String DBname="tset_"+TSet;
			int row=dbd.checknum("sys_testsets", "id", "project='"+ProjTag+"' and name='"+TSet+"'");
			if(dbd.check(DBname)==-1 || row==0)throw new Exception("[info]404, 项目(Tag: "+ProjTag+")的测试集"+TSet+"不存在！");
			
//			添加测试任务
			String[] colname={"taskid","tset","proj","projtag","creattime","creater","status","starttime"};
			String[] record={"", TSet, "", ProjTag ,"", usr, "未开始","0001-01-01 00:00:00"};
			record[0]="Task_"+ProjTag+"_"+System.currentTimeMillis();
			record[4]=sdf_full.format(new Date());
			
			String[][] projdat=dbd.readDB("sys_projects", "project", "tag='"+ProjTag+"'");
			record[2]=projdat[0][0];
			dbd.AppendSQl("sys_taskquene", colname, record, 1, 1);
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	
	/**[Function] 				删除测试任务
	 * @param TaskID		要删除的任务
	 * @throws Exception 409, 任务处于不可删除状态
	 */
	void DelTask(String TaskID) throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
//			判断任务是否存在
			String[][] task=dbd.readDB("sys_taskquene", "id,status", "taskid='"+TaskID+"'");
			if(!task[0][0].equals("")) {
//				判断任务状态，处于运行或停止状态
				if(task[0][1].equals("未开始")) {
					int row=Integer.parseInt(task[0][0]);
					dbd.DelSQl("sys_taskquene", row, 1, 1);
				}
				else throw new Exception("[info]409, 任务 "+TaskID+" 处于 '"+task[0][1]+"' 状态，不能删除！");
			}					
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 * [function]	获取任务列表
	 * @param filter				过滤条件
	 * @param page_count	每页要显示的条目数
	 * @param page_num	指定要显示的页码
	 * @return		JSON格式字符串，如{"code":200, "total_num":302, "tasks":[{"taskid":"xxx", "tset":"xxx", "proj":"xxx", "creattime":"2017-09-01 12:00:00", "creater":"", "status":"", "starttime":""},{},...]}
	 * @throws Exception
	 */
	String ListTask(String filter, String page_count,String page_num) throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
			if(filter.equals(""))filter="id>0";	
			else if(filter.indexOf("=")==-1)filter=filter.replace("*", "%");
			String filter1=filter;
			if(!page_count.equals("")&&!page_num.equals("")) {
				int pg_count=Integer.parseInt(page_count);
				int pg_num=Integer.parseInt(page_num);
				int past_itemnum=pg_count*(pg_num-1);
				filter=filter+" limit "+past_itemnum+","+pg_count;
			}
		
			JSONObject tt=new JSONObject();
			JSONArray tasks=new JSONArray();
			String colname="taskid,tset,proj,projtag,creattime,creater,status,starttime";
			String[][] tasklist=dbd.readDB("sys_taskquene", colname, filter);
			int num=dbd.checknum("sys_taskquene", "id", filter1);
			if(!tasklist[0][0].equals("")){
				String[] col=colname.split(",");
				for(int i=0;i<tasklist.length;i++) {
					JSONObject task=new JSONObject();
					tasklist[i][4]=sdf_full.format(sdf_full.parse(tasklist[i][4]));
					tasklist[i][7]=sdf_full.format(sdf_full.parse(tasklist[i][7]));
					if(tasklist[i][7].equals("0001-01-01 00:00:00"))tasklist[i][7]="";
					for(int j=0;j<col.length;j++)task.put(col[j], tasklist[i][j]);
					if(i==0)task.put("process", CheckTP());
					else task.put("process", "0%");
					tasks.put(i, task);
				}
			}					
			tt.put("total_num", num);
			tt.put("tasks", tasks);
			tt.put("code", 200);
			return tt.toString();
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**[Function] 				运行测试任务
	 * @param taskno		任务号，1-继续执行测试任务，0-从头执行测试任务
	 * @throws Exception
	 */
	void RunTask(String taskno) throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
//			1. 进行任务表状态自检
			DBcheck();						
//			2. 开始执行测试任务
			dbd.UpdateSQl("sys_taskquene",1,"status","执行中");
			ATFactory atm=new ATFactory();
			atm.taskno=taskno;
			atm.setPriority(1);
			atm.start();
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}		
	}	

	/**[Function] 	停止测试任务
	 * @throws Exception 404，任务队列为空
	 * @throws Exception 409，当前任务状态不能停止
	 * @throws Exception 423，脚本执行队列正在初始化，不能停止
	 */
	void StopTask() throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
//			1. 检查任务表队列是否为空；
			int row = dbd.check("sys_taskquene");										
			if(row==0)throw new Exception("[info]404, 任务队列为空，没有任务可停止!");
			
//			2. 判断任务表中第一个任务的状态，如果为'执行中'则继续执行，否则其他状态 （未执行\停止\正在停止...）都报状态错误	
			String tasks[][]=dbd.readDB("sys_taskquene", "status", "id='1'");
			if(!tasks[0][0].equals("执行中"))throw new Exception("[info]409, 任务队列当前状态为"+tasks[0][0]+"，不能执行停止操作!");
			
//			3. 检查临时任务表状态
			int TSnum = dbd.check("Temp_Task");					//获取临时执行表的总脚本数
			row= dbd.check("Temp_Task", "status", "running");	//获取临时执行表中正在执行的脚本行号
//			如果临时任务表为空或者刚执行完，则不是停止任务的合适时机，强行停止会造成状态逻辑混乱，此时等待延时，直到可以处理
			int tag=0;
			while(TSnum==0){
				Thread.sleep(10);	
				TSnum = dbd.check("Temp_Task");	
				tag=tag+1;
				if(tag>200)throw new Exception("[info]423, 脚本执行队列正在初始化，不能完成停止操作，请稍后再试!");
			}
			tag=1;
			if(row<TSnum){
				dbd.UpdateSQl("Temp_Task", row+1, "status", "stop");
			}
			else tag=2;		//此处为特殊情况，即任务停止时，当前任务刚好执行到最后一个脚本
			dbd.UpdateSQl("sys_taskquene", tag, "status", "执行中");	
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}	
	}
	
	/**[Function] 	创建一个新的测试集，并在测试集列表中添加对应记录
	 * @param opt				操作类型，addnew表示新增，modify表示编辑
	 * @param Tset_name	测试集名称
	 * @param usr				操作用户
	 * @param Tset_dat		测试集数据，包括一个或多个测试脚本组成的序列，附带脚本执行策略
	 * @throws Exception 409, 存在重名测试集
	 * @throws Exception 412, 测试集数据有错误或缺失
	 */
	void AddTestSet(String opt, String usr, String Tset_name, String Tset_dat) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			String TSet_DBname="tset_"+Tset_name;
//			如果是新建测试集，则需检查名称有效性
			if(opt.equals("addnew")) {
//				检查测试集名称是否包含非法字符
				String[] invchar={"-","?","=","'",",","\""};
				int iclen=invchar.length;
				for(int i=0;i<iclen;i++){
					if(Tset_name.indexOf(invchar[i])>-1)throw new Exception("[info]412, 测试集名包含非法字符["+invchar[i]+"]！");
				}	
//				检查测试集是否有重名
				int row=dbd.check("sys_testsets", "name", Tset_name);
				if(row>0)throw new Exception("[info]409, 已存在重名的测试集！");
			}
//			如果是修改测试集，则需删除原有测试集
			else DelTestSet(TSet_DBname);

//			判断测试集是否包含测试脚本				
			JSONObject testset = new JSONObject(Tset_dat); 			
			JSONArray tslist=testset.getJSONArray("tslist");			
			if(tslist.length()==0)throw new Exception("[info]412, 测试集中没有测试脚本！");
			
//			创建测试集并添加测试脚本
			String[][] phase = {
				{"id", "int(6) NOT NULL auto_increment PRIMARY KEY"},
				{"tsid", "VARCHAR(50) NOT NULL DEFAULT ''"},
				{"name", "VARCHAR(50) NOT NULL DEFAULT ''"},
				{"path", "VARCHAR(50) NOT NULL DEFAULT ''"},
				{"policy", "VARCHAR(10) NOT NULL DEFAULT ''"}
			};
			dbd.CreatTable(TSet_DBname, phase);	

			String[] colname={"name","tsid","path","policy"};
			String[] record=new String[4];
			String[] TScolname={"project","module","name"};	
			String[] TSkey=new String[3];
			for(int i=0;i<tslist.length();i++){
				JSONObject ts=tslist.getJSONObject(i);
				for(int j=0;j<colname.length;j++) {
					record[j]=ts.getString(colname[j]);
					if(record[j].equals(""))throw new Exception("[info]412, 测试集中第"+(i+1)+"条脚本缺少数据["+colname[j]+"]！");
				}
				String[] pp=record[2].split("\\\\");
				if(pp.length<2)throw new Exception("[info]412, 测试集中第"+(i+1)+"条脚本路径不正确！");
				TSkey[0]=pp[0];
				TSkey[1]=pp[1];
				TSkey[2]=record[0];						
				int row=dbd.check("sys_testscripts",TScolname,TSkey);
				if(row==0)throw new Exception("[info]412, 测试集中第"+(i+1)+"条脚本不存在！");
				
//				测试脚本添加到测试集表中，并在测试脚本列表中增加锁定值
				dbd.AppendSQl(TSet_DBname, colname, record, 1, 1);
				String[][] lok=dbd.readDB("sys_testscripts", "lok","id="+row);
				int lk=Integer.parseInt(lok[0][0])+1;
				dbd.UpdateSQl("sys_testscripts",row, "lok",""+lk);	
			}
//			在测试集列表中增加一条记录	
			String[] Tset_colname={"name","creattime","creater","project","note"};
			String[] rec={Tset_name, "", usr, "", ""};
			rec[1]=sdf_full.format(new Date());
			rec[3]=testset.getString("project");
			rec[4]=testset.getString("note");
			dbd.AppendSQl("sys_testsets",Tset_colname, rec,1,1);	
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}	
	}
	
	/**[Function]	删除一个测试集
	 * @param TSet			要删除的测试集
	 * @throws Exception 
	 */
	void DelTestSet(String TSet) throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
			int row=dbd.check("sys_taskquene", "tset", TSet);
			if(row>0)throw new Exception("[info]423, 测试集正在被测试任务调用，不能操作！");
			
			row=dbd.check("sys_testsets","name",TSet);
			if(row>0){
				dbd.DelSQl("sys_testsets", row,1,1);
				String TSet_DBname="tset_"+TSet;
//				如果测试集表存在，则处理下面操作
				if(dbd.check(TSet_DBname)>-1) {
					if(dbd.check(TSet_DBname)>0) {
						String[][] tsids=dbd.readDB(TSet_DBname, "tsid", "id>0");
//						清除与测试脚本之间的关联
						for(int i=0;i<tsids.length;i++) {
							String[][] lok=dbd.readDB("sys_testscripts", "lok,id","tsid='"+tsids[i][0]+"'");
							int lk=0;
							if(!lok[0][0].equals("")) lk=Integer.parseInt(lok[0][0]);
							if(lk>0){
								dbd.UpdateSQl("sys_testscripts",Integer.parseInt(lok[0][1]), "lok",String.valueOf(lk-1));
							}
						}
					}
					dbd.DelTable(TSet_DBname);
				}
			}
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}		
	}
	/**
	 * [Function]	获取一个测试集
	 * @param TSet		测试集名称
	 * @return		JSON格式字符串，如{"code":200, "creattime":"2017-11-01 12:00:00", "creater":"xxx", "project":"xxx", "note":"", "ts":[{"name":"", "path":"", "policy":""},{},...]}
	 * @throws Exception 404, 测试集不存在
	 */
	String GetTestSet(String TSet) throws Exception{
		PropertyConfigurator.configure(logconf);		
		try{	
			String TSet_DBname="tset_"+TSet;
			String[][] tset_dat=dbd.readDB("sys_testsets", "creattime,creater,project,note", "name='"+TSet+"'");
			if(tset_dat[0][0].equals("") || dbd.check(TSet_DBname)==-1)throw new Exception("[info]404, 测试集"+TSet+"不存在！");

			JSONObject tsetjson = new JSONObject();
			tsetjson.put("creattime", sdf_full.format(sdf_full.parse(tset_dat[0][0])));
			tsetjson.put("creater", tset_dat[0][1]);
			tsetjson.put("project", tset_dat[0][2]);
			tsetjson.put("note", tset_dat[0][3]);
			
			JSONArray ts=new JSONArray();
			String[][] tslist=dbd.readDB(TSet_DBname, "name,path,policy", "id>0");
			if(!tslist[0][0].equals("")) {
				for(int i=0;i<tslist.length;i++) {
					JSONObject ts_i=new JSONObject();
					ts_i.put("name", tslist[i][0]);
					ts_i.put("path", tslist[i][1]);
					ts_i.put("policy", tslist[i][2]);
					ts.put(i, ts_i);
				}
			}
			tsetjson.put("ts", ts);
			tsetjson.put("code", 200);	
			return tsetjson.toString();
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
			
	/**
	 * [function]	获取测试集列表
	 * @param ProjTag		项目标签
	 * @param filter				过滤条件
	 * @param page_count	每页要显示的条目数
	 * @param page_num	指定要显示的页码
	 * @return		JSON格式字符串，如{"code":200, "total_num":302, "testset":[{"name":"xxx", "note":"xxx", "project":"xxx", "creattime":"2017-09-01 12:00:00", "creater":"xx"},{},...]}
	 * @throws Exception
	 */		
	String ListTestSet(String ProjTag, String filter, String page_count,String page_num) throws Exception {
		PropertyConfigurator.configure(logconf);		
		try{	
			if(filter.equals(""))filter="project='"+ProjTag+"'";	
			else {
				if(filter.indexOf("=")==-1)filter=filter.replace("*", "%");
				filter=filter+" and project='"+ProjTag+"'";
			}
			String filter1=filter;
			if(!page_count.equals("")&&!page_num.equals("")) {
				int pg_count=Integer.parseInt(page_count);
				int pg_num=Integer.parseInt(page_num);
				int past_itemnum=pg_count*(pg_num-1);
				filter=filter+" limit "+past_itemnum+","+pg_count;
			}
		
			JSONObject tset=new JSONObject();
			JSONArray testsets=new JSONArray();
			String colname="name,creattime,creater,project,note";
			String[][] tset_list=dbd.readDB("sys_testsets", colname, filter);
			int num=dbd.checknum("sys_testsets", "id", filter1);
			if(!tset_list[0][0].equals("")){
				String[] col=colname.split(",");
				for(int i=0;i<tset_list.length;i++) {
					JSONObject tset_item=new JSONObject();
					tset_list[i][1]=sdf_full.format(sdf_full.parse(tset_list[i][1]));
					for(int j=0;j<col.length;j++)tset_item.put(col[j], tset_list[i][j]);
					testsets.put(i, tset_item);
				}
			}					
			tset.put("total_num", num);
			tset.put("testset", testsets);
			tset.put("code", 200);
			return tset.toString();

		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**[Function] 				查询当前测试任务进度
	 * @return [String]		进度百分比
	 * @throws Exception 
	 */
	String CheckTP() throws Exception{
		PropertyConfigurator.configure(logconf);		
		try{			
			int count=dbd.check("Temp_Task");
			int tp=0;
			if(count>0){
				tp=dbd.check("Temp_Task", "status","norun");
				if(tp>0)	tp=tp-1;
				else{
					tp=dbd.check("Temp_Task", "status","stop");
					if(tp>0)tp=count-1;
					else tp=count;
				}
				tp=tp*100/count;
			}
			return ""+tp+"%";
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}			
	}
	/**[Function] 				进行top10排序的算法函数
	 * @param dat 			要进行排序的数组
	 * @return [String]		返回排序的结果，前10个按照大到小排列，不够10个时有几个排几个
	 */
	String[][] resort10(String[][] dat,int len){
		int nu=10;
		if(len<10)nu=len;
		String[][] sort10=new String[nu][2];	
		
		for(int j=0;j<nu;j++){
			int max=Integer.parseInt(dat[j][1]);
			int index=j;
			for(int i=j+1;i<len;i++){
				int n=Integer.parseInt(dat[i][1]);
				if(n>max){
					max=n;
					index=i;
				}
			}
			sort10[j][0]=dat[index][0];
			sort10[j][1]=dat[index][1];
			dat[index][0]=dat[j][0];
			dat[index][1]=dat[j][1];
			dat[j][0]=sort10[j][0];
			dat[j][1]=sort10[j][1];
		}		
		return sort10;
	}

	/**[Function] 		在执行测试任务之前，进行数据库自检和修正函数
	 * @throw			500 - 数据库或系统错误
	 * @throw			409 - 操作请求与任务状态冲突
	 * @throw			404 - 任务为空
	 */
	void DBcheck() throws Exception{
		try{
//			1. 在测试开始之前，第一个任务只能为norun或者stop状态，如果是其他状态则应提示状态冲突
			int Tasknum=dbd.check("sys_taskquene");
			if(Tasknum==0) throw new Exception("[info]404, 没有可以运行的任务");
			
			String[][] taskstat=dbd.readDB("sys_taskquene", "status", "id='1'");
			if(taskstat[0][0].equals("执行中"))throw new Exception("[info]409, 任务正在运行，不能再次运行");
			if(taskstat[0][0].equals("停止中..."))throw new Exception("[info]409, 任务正在停止中，请稍后再操作");
			
//			2. 第一个问题如果不存在，则判断数据库异常，即从第2个任务开始状态都应该为'未开始'，如果不是则应该恢复
			for(int i=2;i<=Tasknum;i++){
				taskstat=dbd.readDB("sys_taskquene", "status", "id='"+i+"'");
				if(!taskstat[0][0].equals("未开始"))dbd.UpdateSQl("sys_taskquene",i, "status","未开始");
			}
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
	}
	
	/**[Function] 				获取http请求报文中的参数值
	 * @author para		请求报文中的参数序列
	 * @author key			预期的参数名
	 * @return [String]		返回参数结果，如果请求的参数序列为空，或者没有要查询的参数，返回“”，否则返回查询到的参数值
	 */
	String checkpara(Map<String,String[]> para,String key){
		PropertyConfigurator.configure(logconf);
		String ba="";		
		if(para.size()>0){
			try{
				String[] val=para.get(key);
				if(null!=val)ba=val[0];
			}catch(NullPointerException e){
				logger.error(e.toString());
			}
		}	
		return ba;
	}
	
	public int[] sort(int[] sdata){
		int count=sdata.length;
		int temp=0;
		if(count>0){
			for(int i=0;i<count-1;i++){
				for(int j=0;j<count-1;j++){
					if(sdata[j]<sdata[j+1]){
						temp=sdata[j];
						sdata[j]=sdata[j+1];
						sdata[j+1]=temp;
					}
				}
			}			
		}
		return sdata;
	}
}