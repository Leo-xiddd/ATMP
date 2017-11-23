/** 类说明：本模块用于实现ATMP服务与测试项目和测试环境配置数据相关的操作
 *  作   者：Leo
 *  时   间：2017/10/24
 *  版   本：V1.0.0
 *  方   法：本模块支持的方法包括：
 *  	1. 添加项目					Void 		AddProject(String ProjName, String ProjTag)
 *  	2. 	删除项目				Void 		DelProject(String ProjTag)
 *  	3. 	列出所有项目			String 	ListProject()
 *  	4. 添加模块					Void 		AddModule(String ProjTag, String ModuleName, String ModuleTag)
 *  	5. 删除模块					Void 		DelModule(String ProjTag, String ModuleTag)
 *  	6. 列出所有模块			String 	ListModule(String ProjTag)
 *  	7. 读取测试配置数据		String 	ReadTEC(String ProjTag)
 *  	8. 更新测试配置数据		Void		WriteTEC(String ProjTag, String TECData)
 */
package api;
import java.io.File;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.*;
import base.*;

public class TestProject {
	XMLDriver xml= new XMLDriver();
	Ldap ldap =new Ldap();
	DBDriver dbd = new DBDriver();
//	配置日志属性文件位置
	static String confpath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=confpath+"\\conf\\atmp\\logconf.properties";
	Logger logger = Logger.getLogger(TestProject.class.getName());
	
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
		String name=checkpara(Param,"name");
		String tag=checkpara(Param,"tag");
		String proj=checkpara(Param,"proj");
		try {
			switch(API){
			case "AddProj":  			
				if(!name.equals("") && !tag.equals("")){
					logger.info("添加新项目"+name+": "+tag);
					AddProject(name,tag);
					backvalue="200,ok"; 
				}
				break;
			case "DelProj":    
				if(!tag.equals("")){
					logger.info("删除项目"+tag);
					DelProject(tag);
					backvalue="200,ok"; 
				}
				break;
			case "ListProj":  
				logger.info("列出所有项目...");
				return ListProject();
			case "AddModule":    
				if(!proj.equals("") && !name.equals("") && !tag.equals("")){
					logger.info("添加项目"+proj+"下的新模块: "+name+"："+tag);
					AddModule(proj,name,tag);
					backvalue="200,ok"; 
				}
				break;
			case "DelModule":    
				if(!proj.equals("") && !tag.equals("")) {
					logger.info("删除项目"+proj+"下的模块: "+tag);
					DelModule(proj,tag);
					backvalue="200,ok"; 
				}
				break;		
			case "ListModule":   
				if(!proj.equals("")){
					if(!proj.equals("all"))logger.info("列出项目"+proj+"的所有模块...");
					else logger.info("列出所有模块...");
					return ListModule(proj);
				}
				break;
			case "ReadTEC":		
				if(!proj.equals("")){
					logger.info("读取项目(Tag："+proj+")测试环境配置文件");
					return ReadTEC(proj);	
				}
				break;	
			case "WriteTEC":
				if(!proj.equals("") && !body.equals("")) {
					logger.info("更新项目(Tag："+proj+")测试环境配置文件");
					WriteTEC(proj,body);		
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
	
	/**[Function] 					添加一个新项目
	 * @param ProjName		项目名称
	 * @param ProjTag		项目标签
	 * @throws Exception 409,已存在重名项目
	 */
	void AddProject(String ProjName, String ProjTag) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
//			判断项目是否存在
			int row=dbd.check("sys_projects", "project", ProjName);
			if(row>0)throw new Exception("[info]409, 项目"+ProjName+"已存在！");
			row=dbd.check("sys_projects", "tag", ProjTag);
			if(row>0)throw new Exception("[info]409, 项目标签"+ProjTag+"已被使用！");
//			创建新项目
			String[] colname= {"project","tag"};
			String[] record= {ProjName,ProjTag};
			dbd.AppendSQl("sys_projects", colname, record, 1, 1);
//			创建项目目录
			String TSpath=confpath+"\\webapps\\S-atmp\\WEB-INF\\classes\\testScripts\\";
			String TSfilepath=TSpath+ProjTag;
			File file = new File(TSfilepath);  	
			file.mkdirs();		
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**[Function] 				删除项目及其下属的所有模块，并删除对应的文件夹，只有没有下属脚本和测试集的项目才能被删除，一次操作只能删除一个项目
	 * @param ProjTag	项目标签
	 * @throws Exception 404,要删除的项目不存在
	 * @throws Exception 409,要删除的项目下还存在脚本或测试集
	 */
	void DelProject(String ProjTag) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {		
//			判断项目是否存在
			String[][] proj=dbd.readDB("sys_projects", "project,id", "tag='"+ProjTag+"'");
			if(proj[0][0].equals(""))throw new Exception("[info]404, 项目(tag: "+ProjTag+")不存在！");
			
//			判断项目下是否还有脚本或测试集
			int row=dbd.check("sys_testscripts", "project", ProjTag);
			if(row>0)throw new Exception("[info]409, 项目"+proj[0][0]+"下还存在测试脚本，不能删除！");
			row=dbd.check("sys_testsets", "project", ProjTag);
			if(row>0)throw new Exception("[info]409, 项目"+proj[0][0]+"存在关联测试集，不能删除！");

//			删除项目和下属模块
			row=Integer.parseInt(proj[0][1]);
			dbd.DelSQl("sys_projects", row, 1, 1);
			String[][] rows=dbd.readDB("sys_modules", "id", "project='"+ProjTag+"' order by id desc");
			if(!rows[0][0].equals("")) {
				for(int i=0;i<rows.length;i++) {
					row=Integer.parseInt(rows[i][0]);
					dbd.DelSQl("sys_modules", row, 1, 1);
				}
			}
//			删除项目目录
			String TSpath=confpath+"\\webapps\\S-atmp\\WEB-INF\\classes\\testScripts\\";
			String TSfilepath=TSpath+ProjTag;
			File file = new File(TSfilepath);  	
			if(!deleteDir(file))throw new Exception("[info]404, 项目"+proj[0][0]+"目录删除失败，请访问服务器目录直接删除！");
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**[Function] 	返回项目列表
	 * @return  Json格式字符串，如{"projlist":[{"name":"abc项目", "tag":"abc"}, {"name":"erd项目", "tag":"erd"}, ...], "code":200}
	 * @throws Exception
	 */
	String ListProject() throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			JSONObject projlist=new JSONObject();
			JSONArray projs=new JSONArray();
			String[][] pros=dbd.readDB("sys_projects", "project,tag", "id>0 order by tag");
			if(!pros[0][0].equals("")) {
				for(int i=0;i<pros.length;i++) {
					JSONObject temp=new JSONObject();
					temp.put("name", pros[i][0]);
					temp.put("tag", pros[i][1]);
					projs.put(i, temp);
				}
			}
			projlist.put("projlist", projs);
			projlist.put("code", 200);
			return projlist.toString();
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**[Function] 	添加一个新模块
	 * @param ProjTag			新模块所属项目Tag
	 * @param ModuleName	新模块名称
	 * @param ModuleTag		新模块说明
	 * @throws Exception 409,已存在重名模块
	 */
	void AddModule(String ProjTag, String ModuleName, String ModuleTag) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
//			判断模块是否存在
			int row=dbd.checknum("sys_modules", "id", "project='"+ProjTag+"' and tag='"+ModuleTag+"'" );
			if(row>0)throw new Exception("[info]409, 模块标签"+ModuleTag+"已被使用！");
			row=dbd.checknum("sys_modules", "id", "project='"+ProjTag+"' and module='"+ModuleName+"'" );
			if(row>0)throw new Exception("[info]409, 项目(Tag: "+ProjTag+")下已存在模块"+ModuleName);
			
//			判断模块所属项目是否存在
			row=dbd.check("sys_projects", "tag", ProjTag);
			if(row==0)throw new Exception("[info]404, 模块所属项目(tag："+ProjTag+")不存在！");
			
//			创建新模块
			String[] colname= {"proj_id","project","module","tag"};
			String[] record= {""+row,ProjTag,ModuleName,ModuleTag};
			dbd.AppendSQl("sys_modules", colname, record, 1, 1);
//			创建模块目录
			String TSpath=confpath+"\\webapps\\S-atmp\\WEB-INF\\classes\\testScripts\\";
			String TSfilepath=TSpath+ProjTag+"\\"+ModuleTag;
			File file = new File(TSfilepath);  	
			file.mkdirs();		
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	
	/**[Function] 	删除一个项目下的模块，并删除对应的文件夹，只有没有下属脚本的模块才能被删除，一次操作只能删除一个模块
	 * @param ProjTag		模块所属项目标签
	 * @param ModuleTag	模块标签
	 * @throws Exception 404,要删除的模块不存在
	 * @throws Exception 409,要删除的模块下还存在脚本
	 */
	void DelModule(String ProjTag, String ModuleTag) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
//			判断模块是否存在
			int row=dbd.checknum("sys_modules", "id", "project='"+ProjTag+"' and tag='"+ModuleTag+"'");
			if(row==0)throw new Exception("[info]404, 项目(Tag："+ProjTag+")下的模块(Tag： "+ModuleTag+")不存在！");
			
//			判断模块下是否还有脚本
			int nu=dbd.checknum("sys_testscripts", "id", "project='"+ProjTag+"' and module='"+ModuleTag+"'");
			if(nu>0)throw new Exception("[info]409, 项目(Tag："+ProjTag+")的模块(Tag： "+ModuleTag+")下还存在测试脚本，不能删除！");

//			删除模块
			dbd.DelSQl("sys_modules", row, 1, 1);
//			删除模块目录
			String TSpath=confpath+"\\webapps\\S-atmp\\WEB-INF\\classes\\testScripts\\";
			String TSfilepath=TSpath+ProjTag+"\\"+ModuleTag;
			File file = new File(TSfilepath);  	
			if(!deleteDir(file))throw new Exception("[info]404, 项目 "+ProjTag+" 下的模块 "+ModuleTag+" 目录删除失败，请访问服务器目录直接删除！");
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**[Function] 	返回模块列表
	 * @param  ProjTag	项目标签，如果为all，则返回整个项目树
	 * @return  Json格式字符串，如{"modules":[{"project":"abc", "submodules":[{"name":"cdb", "note":"xxxx"}, {"name":"cdb", "note":"xxxx"}, ...]}, ...], "code":200}
	 * @throws Exception
	 */
	String ListModule(String ProjTag) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			String filter="id>0";
			if(!ProjTag.equals("all")) filter="tag='"+ProjTag+"'";
			JSONObject modlist=new JSONObject();
			JSONArray mods=new JSONArray();
			
			String[][] pros=dbd.readDB("sys_projects", "tag,project", filter+" order by tag");
			if(!pros[0][0].equals("")) {
				for(int i=0;i<pros.length;i++) {
					JSONObject projs=new JSONObject();
					JSONArray modules=new JSONArray();
					filter="project='"+pros[i][0]+"' order by tag";
					String[][] module=dbd.readDB("sys_modules", "module,tag", filter);
					if(!module[0][0].equals("")) {
						for(int j=0;j<module.length;j++) {
							JSONObject temp=new JSONObject();
							temp.put("name", module[j][0]);
							temp.put("tag", module[j][1]);
							modules.put(j, temp);
						}
					}
					projs.put("tag", pros[i][0]);
					projs.put("name", pros[i][1]);
					projs.put("modules", modules);
					mods.put(i, projs);
				}
			}
			modlist.put("projtree", mods);
			modlist.put("code", 200);
			return modlist.toString();
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}
	}
	/**
	 * @函数说明			读取项目测试环境配置文件
	 * @param ProjTag		项目标签	 
	 * @return 			返回读取的值
	 * @throws Exception 500,xml文件操作异常
	 */
	String ReadTEC(String ProjTag) throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
			String DBname="tec_"+ProjTag;
			JSONObject bku=new JSONObject();
			JSONArray tec=new JSONArray();
			if(dbd.check(DBname)>0) {
				String[][] tec_record=dbd.readDB(DBname, "tec_key,value", "id>0");
				for(int i=0;i<tec_record.length;i++) {
					JSONObject kv=new JSONObject();
					kv.put("key", tec_record[i][0]);
					kv.put("value", tec_record[i][1]);
					tec.put(i,kv);
				}
			}
			bku.put("tec", tec);
			bku.put("code", 200);
			return bku.toString();
		}catch (Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e.getMessage());
		}		
	}
	/**
	 * @函数说明				更新项目测试环境配置文件
	 * @param ProjTag	项目标签
	 * @param TECData	要写入的配置数据
	 */
	void WriteTEC(String ProjTag, String TECData)throws Exception{
		PropertyConfigurator.configure(logconf);
		try{
			String DBname="tec_"+ProjTag;
//			如果项目之前没有配置过TEC，则创建新表
			String[] colname= {"tec_key","value"};
			String[] record=new String[2];
			if(dbd.check(DBname)==-1) {
				String[][] phase= {
					{"id", "int(6)"},
					{"tec_key", "VARCHAR(100) NOT NULL DEFAULT ''"},
					{"value", "VARCHAR(200) NOT NULL DEFAULT ''"}
				};
				dbd.CreatTable(DBname, phase);
			}
			
			JSONArray tec=new JSONArray(TECData);
			for(int i=0;i<tec.length();i++) {
				JSONObject tdat=tec.getJSONObject(i);
				record[0]=tdat.getString("key");
				record[1]=tdat.getString("value");
				int row=dbd.check(DBname, "tec_key", record[0]);
				if(row==0)dbd.AppendSQl(DBname, colname, record, 1, 1);
				else {
					dbd.UpdateSQl(DBname, row, "value", record[1]);
				}
			}
		}catch (Throwable e) {
			logger.error(e.getMessage(),e);
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
				logger.error(e.getMessage());
			}
		}	
		return ba;
	}
	
	/**[Function] 				删除目录及其下面的所有内容
	 * @param dir			要删除的文件夹路径
	 * @return [boolean]	返回删除的结果
	 */
	private static boolean deleteDir(File dir) {
		boolean res=true;
		if (dir.isDirectory()) {
			String[] children = dir.list();
			System.out.println("cou="+children.length);
//　		递归删除目录中的子目录下
			for (int i=0; i<children.length; i++) {
				System.out.println("i="+i);
				System.out.println(children[i]);
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success)  return false;
			}
	        // 目录此时为空，可以删除
			res= dir.delete();
		}
		return res;
    }
}