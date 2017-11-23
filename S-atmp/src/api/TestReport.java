/** 类说明：本模块用于实现ATMP服务与测试报告相关的API
 *  作   者：Leo
 *  时   间：2017/10/30
 *  版   本：V2.2.1
 *  方   法：本模块支持的方法包括：
 *  	1. 列出测试报告				String 	ListTR(String filter, String page_count, String page_num)
 *  	2. 获取指定测试报告			String 	GetTR(String trname)
 *  	3. 删除指定测试报告			Void 		DelTR(String trname) 
 */
package api;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.*;
import base.*;

public class TestReport {
	DBDriver dbd = new DBDriver();
//	配置日志属性文件位置
	static String confpath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=confpath+"\\conf\\atmp\\logconf.properties";
	static String sysconf=confpath+"\\conf\\atmp\\Sys_config.xml";
	Logger logger = Logger.getLogger(TestReport.class.getName());
	SimpleDateFormat sdf_full = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public String DoAPI(String API,Map<String, String[]> Param,String body){						
		PropertyConfigurator.configure(logconf);
		String backvalue="412,http 请求的参数缺失或无效";
		String filter=checkpara(Param,"filter");
		String page_count=checkpara(Param,"page_count");
		String page_num=checkpara(Param,"page_num");
		String TR_name=checkpara(Param,"name");
		try {
			switch(API){
			case "ListTR":   				
				logger.info("获取测试报告列表");
				return ListTR(filter, page_count, page_num);				
			case "GetTR":   
				if(!TR_name.equals("")) {
					logger.info("请求测试报告"+TR_name);
					return GetTR(TR_name);
				}
				break;
			case "DelTR":   
				if(!TR_name.equals("")) {
					logger.info("删除测试报告"+TR_name);
					DelTR(TR_name);
					backvalue="200,ok";
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
	
	/**
	 * [function]	列出测试报告列表
	 * @param filter				报告过滤条件
	 * @param page_count	每页条目数
	 * @param page_num	页码
	 * @return	 JSON字符串，例如{"TR_list":[{"id":"13", "trname":"xxx", "project":"ccc", "testset":"tom", "owner":"leo", "result":"通过", "creattime":"2017-12-21 12:00:00"}, ...], 
	 * "total_num":23, "code":200}
	 * @throws Exception
	 */
	String ListTR(String filter, String page_count, String page_num) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			JSONObject trs=new JSONObject();
			JSONArray trlist=new JSONArray();
			if(filter.equals(""))filter="id>0";	
			else if(filter.indexOf("=")==-1)filter=filter.replace("*", "%");
			filter=filter+" order by creattime desc";	
			String filter1=filter;
			if(!page_count.equals("")&&!page_num.equals("")) {
				int pg_count=Integer.parseInt(page_count);
				int pg_num=Integer.parseInt(page_num);
				int past_itemnum=pg_count*(pg_num-1);
				filter=filter+" limit "+past_itemnum+","+pg_count;
			}
			String[][] testreports=dbd.readDB("sys_testreports", "id,trname,project,testset,owner,result,creattime", filter);
			int num=dbd.checknum("sys_testreports", "id", filter1);
			if(!testreports[0][0].equals("")){
				String[] tr_colname={"id","trname","project","testset","owner","result","creattime"};
				for(int i=0;i<testreports.length;i++) {
					JSONObject tr_dat=new JSONObject();	
					testreports[i][6]=sdf_full.format(sdf_full.parse(testreports[i][6]));
					for(int k=0;k<tr_colname.length;k++)tr_dat.put(tr_colname[k], testreports[i][k]);
					trlist.put(i, tr_dat);
				}
			}		
			trs.put("total_num", num);
			trs.put("TR_list", trlist);
			trs.put("code", 200);
			return trs.toString();
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e);
		}
	}
	/**
	 * [function]	获取一个测试报告
	 * @param trname	测试报告名称
	 * @return		返回JSON格式字符串，如{"code":", "":""}
	 * @throws Exception
	 */
	String GetTR(String trname) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			JSONObject tr=new JSONObject();
//			获取基本信息
			String Colname= "project,testset,owner,result,starttime,creattime";
			String[] cols_ts=Colname.split(",");
			String[][] tr_info=dbd.readDB("sys_testreports", Colname, "trname='"+trname+"'");
			if(!tr_info[0][0].equals("")) {
				Date time1=sdf_full.parse(tr_info[0][4]);
				Date time2=sdf_full.parse(tr_info[0][5]);
				tr_info[0][4]=sdf_full.format(time1);
				tr_info[0][5]=sdf_full.format(time2);
				for(int i=0;i<cols_ts.length;i++) tr.put(cols_ts[i], tr_info[0][i]);
				String taskid=trname.replace("TR_", "Task_");
				tr.put("taskid", taskid);
//				计算测试周期
				
				int interval =(int) (time2.getTime() - time1.getTime())/1000;
				int hour=interval/3600;
				int min=(interval-hour*3600)/60;
				int sec=interval-hour*3600-min*60;
				String timeresume=""+hour+"小时"+min+"分"+sec+"秒";	
				if(hour==0) timeresume=""+min+"分"+sec+"秒";	
				if(hour==0 && min==0 ) timeresume=""+sec+"秒";	
				tr.put("timeresume",timeresume );			
			}
//			获取测试报告数据			
			int fail_num=0;   //测试不通过的脚本数量
			int error_num=0;   //测试异常的脚本数量
			JSONArray ts=new JSONArray();
			int ts_num=dbd.check(trname);	//测试脚本数
			if(ts_num>0) {
				String[] cols_dat= {"id","tsname","testname","issue","testresult","starttime"};
				String[][] trdat=dbd.readDB(trname, "id,tsname,testname,issue,testresult,starttime", "id>0");
				SimpleDateFormat sdf_hms = new SimpleDateFormat("HH:mm:ss");
				for(int i=0;i<ts_num;i++) {
					JSONObject dat=new JSONObject();
					trdat[i][5]=sdf_hms.format(sdf_full.parse(trdat[i][5]));
					for(int j=0;j<cols_dat.length;j++)dat.put(cols_dat[j], trdat[i][j]);
					if(trdat[i][4].equals("fail"))fail_num++;
					else if(trdat[i][4].equals("error"))error_num++;
					ts.put(i, dat);
				}
			}
			if(ts_num==-1)ts_num=0;
			tr.put("num_ts", ""+ts_num);
			tr.put("num_ts_fail", ""+fail_num);
			tr.put("num_ts_error", ""+error_num);
			int pass_rate=0;
			if(ts_num>0) {
				pass_rate=(ts_num-fail_num-error_num)*100/ts_num;
			}
			tr.put("rate_ts_pass", ""+pass_rate+"%");
			tr.put("ts", ts);
			tr.put("code", 200);
			return tr.toString();
		}catch(Throwable e) {		
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**[Function] 				删除测试报告
	 * @param trname		报告名称
	 * @throws Exception
	 */
	void DelTR(String trname) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			int row=dbd.check("sys_testreports", "trname", trname);
			if(row>0) dbd.DelSQl("sys_testreports", row, 1, 1);
			if(dbd.check(trname)>-1)dbd.DelTable(trname);
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e);
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
}