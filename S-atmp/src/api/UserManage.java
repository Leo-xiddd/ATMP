/**
 * 本模块提供的API包括：
 * 1. Authen	--	User.Authority(String name, String pwd)
 * 2. Add		--	User.Add(String name, String pwd, String fname, String role, String email, String mobile)
 * 3. Delete	--	User.Del(String name) 
 * 4. List			--	User.List(String filter)
 * 5. Getinfo	--	User.Get(String name)
 */
package api;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.*;

import base.*;

public class UserManage {
	XMLDriver xml= new XMLDriver();
	Ldap ldap =new Ldap();
	DBDriver dbd = new DBDriver();
//	配置日志属性文件位置
	static String confpath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=confpath+"\\conf\\atmp\\logconf.properties";
	static String sysconf=confpath+"\\conf\\atmp\\sys_config.xml";
	Logger logger = Logger.getLogger(UserManage.class.getName());
	
	SimpleDateFormat sdf_full = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public String DoAPI(String API,Map<String, String[]> Param,String body){						
		PropertyConfigurator.configure(logconf);		
		String message="";
		logger.info("API: "+API+" "+" [Body]"+body);
		String backvalue="412,http 请求的参数缺失或无效";
		String usr=checkpara(Param,"user");
		try {
			switch(API){
			case "Authen":							
				String p=checkpara(Param,"pwd");
				if((!usr.equals(""))&&(!p.equals(""))) {
					logger.info("对用户"+usr+"进行鉴权...");	
					return Authority(usr,p);
				}
				break;
			case "AddLdap":   				
				if(!body.equals("")) {
					logger.info("添加LDAP...");
					ConfigLDAP(body);
					backvalue="200,ok"; 
				}
				break;
			case "GetLdap":   	
				logger.info("获取LDAP配置信息...");
				return GetLDAP();
			case "syncLdap":   	
				logger.info("从LDAP服务器同步用户信息...");
				return syncLDAP();
			case "Add":   					
				if(!body.equals("")) {
					logger.info("创建新用户账户...");
					AddUser(body);
					backvalue="200,ok";
				}			
				break;
			case "Update":   				
				if(!body.equals("")) {
					logger.info("更新用户信息...");
					UpdateUser(body);
					backvalue="200,ok";
				}			
				break;
			case "Delete":  
				if(!usr.equals("")) {
					logger.info("删除用户账户"+usr+"...");
					DelUser(usr);
					backvalue="200,ok";
				}			
				break;
			case "List":   
				logger.info("列出所有用户账户...");
				String filter=checkpara(Param,"filter");
				String page_count=checkpara(Param,"page_count");
				String page_num=checkpara(Param,"page_num");
				return ListUser(filter, page_count, page_num);
			case "Getinfo":   			
				if(!usr.equals("")) {
					logger.info("获取用户"+usr+"的信息...");
					return GetUser(usr);				
				}
				break;
			case "GetUsersinfo":   	
				String phase=checkpara(Param,"phase");
				if(!usr.equals("") && !phase.equals("")) {
					logger.info("获取一组用户的"+phase+"信息...");
					return GetUsersinfo(usr,phase);				
				}
			case "CheckUser":   	
				filter=checkpara(Param,"filter");
				if(filter.indexOf("=")==-1)filter=filter.replace("*", "%");
				if(!filter.equals("")) {
					logger.info("根据条件"+filter+"查询用户...");
					return CheckUser(filter);				
				}
			case "LoadPurview":				
				if(!usr.equals("")) {
					logger.info("获取指定用户的权限表...");
					return LoadPurview(usr);				
				}
			case "UpdatePurview":				
				if(!usr.equals("") && !body.equals("")) {
					logger.info("更新指定用户的权限表...");
					UpdatePurview(usr,body);	
					backvalue="200,ok";
				}	
				break;
			case "InitPurview":				
				logger.info("初始化权限表...");
				InitPurview();	
				backvalue="200,ok";		
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
		message=backvalue.substring(backvalue.indexOf(",")+1);
		backvalue="{\"code\":"+code+",\"message\":\""+message+"\"}";
		return backvalue;
	}
	
	/**[Function] 					进行用户密码校验，多用于用户登录
	 * @param  name - 		用户名 
	 * @param  pwd - 			用户密码，经过加密处理
	 * @return		返回JSON格式字符串，包括校验结果和用户全名
	 * @throws Throwable	404 - 没有找到用户
	 * @throws Throwable	412 - 密码校验失败
	 * @throws Throwable	500 - 数据库操作失败
	 */
	String Authority(String name,String pwd) throws Throwable{	
		PropertyConfigurator.configure(logconf);		
		try{
//			判断用户是否存在
			String[][] adm=dbd.readDB("sys_usrdb", "type,passwd,fullname","usrname='"+name+"'");
			if(adm[0][0].equals(""))throw new Exception("[info]404,用户不存在");
			
//			对密码进行解密
			pwd=decrypt(pwd);
//			进行鉴权认证
			if(adm[0][0].equals("local")) {
				if(!auth(pwd,adm[0][1]))  throw new Exception("[info]412,用户名或密码错误");
			}
			else {
				if(!ldap.authen(name, pwd)) throw new Exception("[info]412,用户名或密码错误");
			}	
			JSONObject auth=new JSONObject();
			auth.put("fullname", adm[0][2]);
			auth.put("code", 200);
			return auth.toString();
		}catch (Throwable e){
			logger.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 * [function]	配置LDAP连接器属性
	 * @param body	LDAP配置信息，包括LDAP服务器地址、端口、基础域、登录用户账号、密码、导入路径（可以支持多个）
	 * @throws Exception 404，配置文件不存在或者文件中内容错误
	 * @throws Exception 412，xml调用参数错误
	 * @throws Exception 500，系统错误
	 */
	void ConfigLDAP(String body) throws Exception {
		PropertyConfigurator.configure(logconf);		
		try {
			JSONObject ldap=new JSONObject(body);
			xml.Update(sysconf, "LDAP_conf/host", ldap.getString("host"));
			xml.Update(sysconf, "LDAP_conf/port", ldap.getString("port"));
			xml.Update(sysconf, "LDAP_conf/domain", ldap.getString("domain"));
			xml.Update(sysconf, "LDAP_conf/admin", ldap.getString("admin"));
			String pwd=ldap.getString("pwd");
			if(!pwd.equals(""))	xml.Update(sysconf, "LDAP_conf/pwd",pwd );
			
//			更新baseDN
			xml.Del(sysconf, "LDAP_conf/BaseDN");
			xml.Add(sysconf, "LDAP_conf", "BaseDN", "");
			JSONArray bds=ldap.getJSONArray("BaseDN");
			for(int i=0;i<bds.length();i++) {
				xml.Add(sysconf, "LDAP_conf/BaseDN", "BD"+i, bds.getString(i));
			}
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e);
		}
	}
	/**
	 * [function]	获取LDAP配置信息
	 * @return		JSON格式字符串，如{"host":"192.168.1.1","port":"389","domain":"@bac.com","admin":"admin","BaseDN":["xxxxxx","xxxxxxxx","xxx",....],"code":200}
	 * @throws Exception 404，配置文件不存在或者文件中内容错误
	 * @throws Exception 412，xml调用参数错误
	 * @throws Exception 500，系统错误
	 */
	String GetLDAP() throws Exception {
		PropertyConfigurator.configure(logconf);		
		try {
			JSONObject ldap=new JSONObject();
			JSONArray bds=new JSONArray();
			ldap.put("host", xml.GetNode(sysconf, "LDAP_conf/host"));
			ldap.put("port", xml.GetNode(sysconf, "LDAP_conf/port"));
			ldap.put("domain", xml.GetNode(sysconf, "LDAP_conf/domain"));
			ldap.put("admin", xml.GetNode(sysconf, "LDAP_conf/admin"));
			JSONObject BD=new JSONObject(xml.GetListA(sysconf, "LDAP_conf/BaseDN"));
			for(int i=0;i<BD.length();i++) {
				bds.put(i, xml.GetNode(sysconf, "LDAP_conf/BaseDN/BD"+i));
			}
			
			ldap.put("BaseDN", bds);
			ldap.put("code",200);
			return ldap.toString();
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e);
		}
	}
	/**
	 * [function]	与LDAP服务器同步用户信息
	 * @return		返回同步结果，包括新增用户数new_member和更新用户数update_member
	 * @throws Exception 404，配置文件不存在或者文件中内容错误
	 * @throws Exception 412，xml调用参数错误
	 * @throws Exception 500，系统错误
	 */
	String syncLDAP() throws Exception {
		PropertyConfigurator.configure(logconf);		
		try {
//			1.判断配置文件中是否有完整的LDAP配置信息
			String host=xml.GetNode(sysconf, "LDAP_conf/host");
			String port=xml.GetNode(sysconf, "LDAP_conf/port");
			String domain=xml.GetNode(sysconf, "LDAP_conf/domain");
			String admin=xml.GetNode(sysconf, "LDAP_conf/admin");
			if(host.equals("") || port.equals("") || domain.equals("") || admin.equals(""))throw new Exception("[info]412,请先配置LDAP。");
			
//			2.判断BaseDN是否存在
			JSONObject BD=new JSONObject(xml.GetListA(sysconf, "LDAP_conf/BaseDN"));
			if(BD.length()==0)throw new Exception("[info]412,缺少导入用户的目录路径（BaseDN），请先配置。");
			String[] bds=new String[BD.length()];
			for(int i=0;i<BD.length();i++) {
				bds[i]=xml.GetNode(sysconf, "LDAP_conf/BaseDN/BD"+i);
			}
			
//			3.从LDAP中导入用户信息
			String[] phase= {"account","fullname","dept","mail","mobile"};
			String[] colname= {"usrname","fullname","dept","email","mobile","role","type"};
			String[] record=new String[colname.length];
			String[] check_col= {"usrname","type"};
			String[] check_dat={"","ldap"};
			int new_count=0;
			int update_count=0;
			for(int i=0;i<BD.length();i++) {
				JSONArray ulist=new JSONArray(ldap.Getuserlist(bds[i]));
				for(int j=0;j<ulist.length();j++) {
					JSONObject a=ulist.getJSONObject(j);
					for(int k=0;k<phase.length;k++) record[k]=a.getString(phase[k]);
					record[phase.length]="用户";
					record[phase.length+1]="ldap";
					check_dat[0]=record[0];
					int row=dbd.check("sys_usrdb", check_col, check_dat);
					if(row>0) {
						for(int k=1;k<5;k++)dbd.UpdateSQl("sys_usrdb", row, colname[k],record[k]);
						update_count++;
					}
					else {
						dbd.AppendSQl("sys_usrdb", colname, record, 1, 1);
						new_count++;
					}
				}
			}
//			更新同步时间
			String sync_time=sdf_full.format(new Date());
			xml.Update(sysconf, "LDAP_conf/last_sync_time", sync_time);
			
			JSONObject syncr=new JSONObject();
			syncr.put("new_member", new_count);
			syncr.put("update_member", update_count);
			syncr.put("code", 200);
			return syncr.toString();
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**
	 * [function]	人工添加一个用户
	 * @param user_info	用户基本信息
	 * @throws Exception	 409，用户名已存在
	 * @throws Exception	 412，用户名或密码为空
	 */
	void AddUser(String user_info)throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			JSONObject jsb=new JSONObject(user_info);	
			String[] colname= {"usrname","passwd","fullname","dept","role","email","mobile","type"};
			String[] record=new String[colname.length];
			for(int i=0;i<colname.length;i++)record[i]=jsb.getString(colname[i]); 
//			检查用户名
			if(record[0].equals(""))throw new Exception("[info]412,用户名为空！");
			int row=dbd.check("sys_usrdb", "usrname", record[0]);
			if(row>0)throw new Exception("[info]409,用户账号["+record[0]+"]已存在！");
			
//			检查和加密密码
			if(record[1].equals(""))throw new Exception("[info]412,密码参数没有赋值,不支持空密码！");
			record[1]=encrypt(record[1]);			//对密码进行加密处理
			if(record[7].equals("域账户"))record[7]="ldap";
			else record[7]="local";
			dbd.AppendSQl("sys_usrdb", colname, record, 1, 1);
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**
	 * [function]	更新用户信息
	 * @param user_info	用户基本信息
	 * @throws Exception	 404，用户不存在
	 */
	void UpdateUser(String user_info)throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			JSONObject jsb=new JSONObject(user_info);	
			String[] colname= {"usrname","passwd","fullname","dept","role","email","mobile","type"};
			String[] record=new String[colname.length];
			for(int i=0;i<colname.length;i++)record[i]=jsb.getString(colname[i]); 
//			检查用户是否存在
			int row=dbd.check("sys_usrdb", "usrname", record[0]);
			if(row==0)throw new Exception("[info]404,用户账号["+record[0]+"]不存在！");
			
			record[1]=encrypt(record[1]);	
			int index=1;
			if(record[colname.length-1].equals("域账户")) {
				index=2;
				record[colname.length-1]="ldap";
			}
			else record[colname.length-1]="local";
			for(int i=index;i<colname.length;i++)dbd.UpdateSQl("sys_usrdb", row, colname[i], record[i]);
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**[Function] 					删除指定的用户，如果系统中无此用户则不作任何操作，也不报错
	 * @param	name - 		用户名 
	 * @throws	Exception	404，没有找到数据表
	 * @throws	Exception	409，用户不可被删除
	 * @throws	Exception	500，数据库操作失败
	 */
	void DelUser(String name) throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
			int row=dbd.check("sys_usrdb", "usrname", name);
			if(row>0){
				if(name.equals("admin"))throw new Exception("[info]409,系统管理员用户不可删除");
				dbd.DelSQl("sys_usrdb",row,1,1);
			}
		}catch(Throwable e){
			throw new Exception(e);
		}	
	}
	/**
	 * 函数说明：用于获取用户的单一信息，包括全名、邮箱、手机号等
	 * @param account		要获取信息的用户名
	 * @return		JSON格式字符串，包括字段usrname,fullname,dept,role,email,mobile,type
	 * @throws Exception 404，用户账号不存在
	 * @throws Exception 500，系统错误
	 */
	String GetUser(String account) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			String Colname="usrname,fullname,dept,role,email,mobile,type";
			String[] cols=Colname.split(",");
			
			JSONObject Userinfo=new JSONObject();
			String[][] user_info=dbd.readDB("sys_usrdb", Colname, "usrname='"+account+"'");			
			if(user_info[0][0].equals(""))throw new Exception("[info]404,用户"+account+"不存在");
			if(user_info[0][6].equals("ldap"))user_info[0][6]="域账户";
			else user_info[0][6]="本地账户";
			for(int i=0;i<cols.length;i++)Userinfo.put(cols[i], user_info[0][i]);
			Userinfo.put("code", 200);
			return Userinfo.toString();	
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e);
		}
	}
	
	/**[Function] 				列出指定用户的指定属性
	 * @param  usrlist		用户账号，可以是一个或一组，用逗号分隔 
	 * @param  phase		要列出的用户属性, 包括fullname,dept,role,email,mobile,type
	 * @return	  JSON格式字符串，如{"info":"a,b,c,...","code":200}
	 * @throws Throwable	404 - 没有找到用户
	 * @throws Throwable	500 - 数据库操作失败
	 */
	String GetUsersinfo(String usrlist,String phase) throws Throwable{
		PropertyConfigurator.configure(logconf);
		try{
			String info_list="";
			String filter="usrname='"+usrlist+"'";
			if(usrlist.indexOf(",")>-1) {
				String[] usrs=usrlist.split(",");
				filter="";
				for(int i=0;i<usrs.length;i++) filter=filter+"usrname='"+usrs[i]+"' or ";
				filter=filter.substring(0, filter.lastIndexOf(" or"));
			}
			String[][] info=dbd.readDB("sys_usrdb", phase, filter);
			if(info[0][0].equals("")) {
				filter=filter.replace("usrname", "fullname");
				info=dbd.readDB("sys_usrdb", phase, filter);
			}			
			if(info[0][0].equals("")) throw new Exception("[info]404,用户"+usrlist+"不存在");
			
			for(int i=0;i<info.length;i++) {
				if(phase.equals("type")) {
					if(info[i][0].equals("ldap"))info[i][0]="域账户";
					else info[i][0]="本地账户";
				}
				info_list=info_list+info[i][0]+",";
			}
			info_list=info_list.substring(0, info_list.length()-1);
			JSONObject inf=new JSONObject();
			inf.put("info", info_list);
			inf.put("code",200);
			return inf.toString();
		}catch(Throwable e){
			throw e;
		}
	}
	
	/**
	 * [function]	获取用户列表
	 * @param filter				用户过滤条件
	 * @param page_count	每页要显示的条目数
	 * @param page_num	指定要显示的页码
	 * @return		JSON格式字符串，如{}
	 * @throws Exception
	 */
	String ListUser(String filter, String page_count,String page_num) throws Exception {
		PropertyConfigurator.configure(logconf);		
		try {		
			if(filter.equals(""))filter="id>0";	
			else if(filter.indexOf("=")==-1)filter=filter.replace("*", "%");
			filter=filter+" order by dept, usrname";	
			String filter1=filter;
			if(!page_count.equals("")&&!page_num.equals("")) {
				int pg_count=Integer.parseInt(page_count);
				int pg_num=Integer.parseInt(page_num);
				int past_itemnum=pg_count*(pg_num-1);
				filter=filter+" limit "+past_itemnum+","+pg_count;
			}
			
			JSONObject UserTab=new JSONObject();
			JSONArray Userlist= new JSONArray();
			String colname="id,usrname,fullname,dept,role,email,mobile,type";
			String[][] usr_list=dbd.readDB("sys_usrdb", colname, filter);
			int num=dbd.checknum("sys_usrdb", "id", filter1);
			if(!usr_list[0][0].equals("")){
				String[] col=colname.split(",");
				for(int i=0;i<usr_list.length;i++) {
					JSONObject userinfo=new JSONObject();
					if(usr_list[i][7].equals("ldap"))usr_list[i][7]="域账户";
					else usr_list[i][7]="本地账户";
					for(int j=0;j<col.length;j++)userinfo.put(col[j], usr_list[i][j]);
					Userlist.put(i, userinfo);
				}
			}					
			UserTab.put("last_sync_time", xml.GetNode(sysconf, "LDAP_conf/last_sync_time"));
			UserTab.put("total_num", num);
			UserTab.put("userlist", Userlist);
			UserTab.put("code",200);
			return UserTab.toString();
		}catch(Throwable e) {
			logger.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}		
	}
	/**
	 * 函数说明：查询满足条件的用户账号
	 * @param filter	查询条件
	 * @return		JSON格式字符串，如：{"usrname":"xxxxx","code":200}
	 * @throws Exception
	 */
	String CheckUser(String filter) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			JSONObject bku=new JSONObject();
			String[][] usrinfo=dbd.readDB("sys_usrdb", "usrname", filter);
			bku.put("usrname", usrinfo[0][0]);
			bku.put("code", 200);
			return bku.toString();
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e);
		}
	}
	/**
	 * 函数说明：获取指定用户的权限表
	 * @param usrname	用户账号
	 * @return		JSON格式字符串，如{"purview":[{"module":"运营报告","list":[{"key":"pur_amoeba_account","value":"y","text":"阿米巴报表"},{},...]},{},...],"code":200}
	 * @throws Exception
	 */
	String LoadPurview(String usrname) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			JSONObject bku=new JSONObject();
			int row=dbd.check("sys_usr_purview", "usrname", usrname);
			if(row==0)throw new Exception("[info]404,用户"+usrname+"不存在！");
					
			String[][] pruview=dbd.readDB("sys_usr_purview", "*", "id="+row);
			String[] keys=dbd.GetDBColname("sys_usr_purview").split(",");
			String[][] modules={{"测试实验室","2","3"},{"测试报告","4","5"},{"测试集","6","8"},{"测试脚本","9","10"},{"用户管理","11","15"}};
			String[][] text=dbd.readDB("sys_usr_purview", "*", "id=1");
			JSONArray purv=new JSONArray();
			for(int i=0;i<modules.length;i++) {
				JSONObject module=new JSONObject();
				module.put("module", modules[i][0]);
				int tag1=Integer.parseInt(modules[i][1]);
				int tag2=Integer.parseInt(modules[i][2]);
				JSONArray list=new JSONArray();
				for(int j=tag1;j<=tag2;j++) {
					JSONObject item=new JSONObject();
					item.put("key", keys[j]);
					item.put("value", pruview[0][j]);
					item.put("text", text[0][j]);
					list.put(item);
				}
				module.put("list", list);
				purv.put(i, module);
			}
			bku.put("purview", purv);
			bku.put("code", 200);
			return bku.toString();
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e);
		}
	}
	/**
	 * 函数说明：更新指定用户的权限表
	 * @param usrname	要更新权限的用户
	 * @param body	权限数据
	 * @throws Exception
	 */
	void UpdatePurview(String usrname, String body) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			JSONArray purview=new JSONArray(body);
			int row=dbd.check("sys_usr_purview", "usrname", usrname);
//			如果用户不存在，则在权限表新增一个用户
			if(row==0) {
				String[] colname=new String [purview.length()+1];
				String[] record=new String [purview.length()+1];
				for(int i=0;i<purview.length();i++) {
					JSONObject item=purview.getJSONObject(i);
					colname[i]=item.getString("key");
					record[i]=item.getString("value");
				}			
				colname[purview.length()]="usrname";
				record[purview.length()]=usrname;
				dbd.AppendSQl("sys_usr_purview", colname, record, 1, 1);
			}
			else {
				for(int i=0;i<purview.length();i++) {
					JSONObject item=purview.getJSONObject(i);
					dbd.UpdateSQl("sys_usr_purview", row, item.getString("key"), item.getString("value"));
				}
			}			
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e);
		}
	}
	/**
	 *  函数说明：初始化权限表
	 * @throws Exception
	 */
	void InitPurview() throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			int count=dbd.checknum("sys_usr_purview", "id", "id>0");
			for(int i=count;i>1;i--)dbd.DelSQl("sys_usr_purview", i, 1, 1);
//			初始化字段
			String[] colname= {"usrname"};		
			String[] record= {""};
//			同步用户表
			String[][] usrlist=dbd.readDB("sys_usrdb", "usrname", "id>0");
			if(!usrlist[0][0].equals("")) {
				for(int i=0;i<usrlist.length;i++) {
					record[0]=usrlist[i][0];
					dbd.AppendSQl("sys_usr_purview", colname, record, 1, 1);
				}
			}
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
				logger.error(e.getMessage());
			}
		}	
		return ba;
	}
	/**[Function] 				对数据加密并返回加密后的结果，用于用户密码保存
	 * @return [String]		加密后的用户密码
	 */
	public String encrypt(String data){
		String ency="";
		char[] tcr;
		String pi="31415926535897932384626";
		char[] key=pi.toCharArray();
		int i=0;
		tcr=data.toCharArray();
		for(char a : tcr){
			tcr[i]=(char) (a+key[i]);			
			i=i+1;
		}
		ency=String.valueOf(tcr);
		return ency;
	}
	
	/**[Function] 				对加密数据进行解密并返回解密后的结果，用于用户密码传输
	 * @return [String]		解密后的用户密码
	 */
	public String decrypt(String data){
		String ency="";
		String pi="31415926535897932384626";
		char[] key=pi.toCharArray();	
		String[] num_dat=data.split(":");
		for(int i=0;i<num_dat.length;i++) {
			int asc_data=Integer.parseInt(num_dat[i]);
			int asc_pi=key[i];
			char tt=(char) (asc_data-asc_pi);
			ency=ency+tt;
		}
		return ency;
	}
	/**[Function] 				校验密码正确性
	 * @param Dpass		用来校验的用户密码
	 * @param Spass		数据库中用户的密文密码
	 * @return [Boolean]	返回校验结果，校验通过为True
	 */
	public Boolean auth(String Dpass, String Spass){
		boolean res=false;
		Dpass=encrypt(Dpass);
		if(Dpass.equals(Spass))res=true;
		return res;
	}
}