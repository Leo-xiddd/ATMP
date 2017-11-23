/** 类说明：本模块用于实现ATMP服务器脚本相关的API
 *  作   者：Leo
 *  时   间：2017/10/30
 *  版   本：V3.0.1
 *  方   法：本模块支持的方法包括：
 *  	1. 上传测试脚本				String 	UploadTS(String usr,String proj,String mod,HttpServletRequest req)
 *  	2. 删除测试脚本				String 	DelTS(String usr,String TS)
 *  	3. 列出脚本						String	ListTS(String filter, String page_count, String page_num) 
 */
package api;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.*;
import base.*;

public class TestScript {
	DBDriver dbd = new DBDriver();
//	配置日志属性文件位置
	static String confpath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=confpath+"\\conf\\atmp\\logconf.properties";
	Logger logger = Logger.getLogger(TestScript.class.getName());
	SimpleDateFormat sdf_full = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public String DoAPI(String API,Map<String, String[]> Param,String body,HttpServletRequest req){						
		PropertyConfigurator.configure(logconf);		
		logger.info("API: "+API+" "+" [Body]"+body);
		String backvalue="412,http 请求的参数缺失或无效";
		String ProjTag=checkpara(Param,"proj");
		String ModuleTag=checkpara(Param,"module");
		String ts_id=checkpara(Param,"tsid");
		String usr=checkpara(Param,"usr");
//		开始处理API
		try {
			switch(API){
			case "UploadTS": 
				if(!ProjTag.equals("")&&!ModuleTag.equals("")) {
					logger.info("上传脚本...");
					UploadTS(usr, ProjTag, ModuleTag, req);
					backvalue="200,ok"; 
				}
				break;	
			case "DelTS":  					
				if(!ts_id.equals("")) {
					logger.info("删除测试脚本["+ts_id+"]...");
					DelTS(ts_id);
					backvalue="200,ok"; 
				}
				break;			
			case "ListTS":  
				logger.info("列出测试脚本...");
				String filter=checkpara(Param,"filter");
				String page_count=checkpara(Param,"page_count");
				String page_num=checkpara(Param,"page_num");
				return ListTS(ProjTag,filter, page_count, page_num);
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
	
	/**[Function] 	上传测试脚本，并在数据库中添加对应记录
	 * @param ProjTag			脚本所归属的项目
	 * @param ModuleTag		脚本所归属的模块
	 * @param usr					当前发起请求的用户
	 * @return	 JSON格式字符串，message字段会给出上传结果
	 * @throws Exception 201, 文件上传成功，但存在重名文件
	 * @throws Exception 402, 上传文件格式不正确或上传失败
	 * @throws Exception 403, 上传文件容量过大
	 * @throws Exception 404, 项目或模块不存在 
	 */
	void UploadTS(String usr, String ProjTag, String ModuleTag, HttpServletRequest req) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
//			判断项目和模块是否存在
			int row=dbd.check("sys_projects", "tag", ProjTag);
			if(row==0)throw new Exception("[info]404, 项目(Tag: "+ProjTag+")不存在，请先创建项目和目录再上传脚本！");
			row=dbd.checknum("sys_modules", "id", "project='"+ProjTag+"' and tag='"+ModuleTag+"'");
			if(row==0)throw new Exception("[info]404, 模块 "+ProjTag+"/"+ModuleTag+" 不存在，请先创建项目和目录再上传脚本！");
			
//			判断文件格式是否正确
			if(!ServletFileUpload.isMultipartContent(req))throw new Exception("[info]402, 上传的文件格式不正确！");		
//			开始上传文件
			String tempPath = confpath+"\\webapps\\S-atmp\\WEB-INF\\classes\\testScripts\\temp\\tempTS";
			String filepath=confpath+"\\webapps\\S-atmp\\WEB-INF\\classes\\testScripts\\"+ProjTag+"\\"+ModuleTag+"\\";

//			1、创建一个DiskFileItemFactory工厂
			DiskFileItemFactory factory = new DiskFileItemFactory();                     									
			factory.setRepository(new File(tempPath));
			factory.setSizeThreshold(1024*100);	
			
//			2、创建一个文件上传解析器
			ServletFileUpload upload = new ServletFileUpload(factory);
			upload.setHeaderEncoding("UTF-8");
			upload.setFileSizeMax(1024*1024*10);
			upload.setSizeMax(1024*1024*100);	      
			logger.info("上传路径确认，开始上传脚本文件...");	
			
//			3、使用ServletFileUpload解析器解析上传数据，解析结果返回的是一个List<FileItem>集合，每一个FileItem对应一个Form表单的输入项
            List<FileItem> list = upload.parseRequest(req);
            String[] colname={"tsid","name","project","module","owner","upload_time","lok","tcf"};
            String[] record=new String[colname.length];
            record[2]=ProjTag;
            record[3]=ModuleTag;
            record[4]=usr;
            record[5]=sdf_full.format(new Date());
            record[6]="0";
            record[7]="无";
//        	初始化重复文件列表
            String dup_file="";
            for(FileItem item : list){
            	String filename = item.getName();
                if(filename==null || filename.trim().equals("")) continue;
                filename = filename.substring(filename.lastIndexOf("\\")+1);		              
                
                String fileExtName = filename.substring(filename.lastIndexOf(".")+1);  //得到上传文件的扩展名
                String fileMainName=filename.substring(0,filename.lastIndexOf(".")+1);
                
               	InputStream in = item.getInputStream();		// 获取item中的上传文件的输入流
               	FileOutputStream outa = new FileOutputStream(filepath + "\\" + filename);			//创建一个文件输出流
               	byte buffer[] = new byte[1024];
               	int len = 0;
               	while((len=in.read(buffer))>0){
                   outa.write(buffer, 0, len);
                } 
               	in.close();  
 	            outa.close();  
// 	           	如果是脚本则追加脚本记录
 	            String[][] ids=dbd.readDB("sys_testscripts", "id", "name like '"+fileMainName+"%' and project='"+ProjTag+"' and module='"+ModuleTag+"'");	          
 	            if(fileExtName.equals("class")) {
 	            	if(ids[0][0].equals("")) {
 	            		record[0]=ProjTag+"_"+System.currentTimeMillis();
 	 	            	record[1]=filename;
 	 	  	           	dbd.AppendSQl("sys_testscripts",colname, record,1,1);	
 	            	}
 	            	else {
 	            		row=Integer.parseInt(ids[0][0]);
 	            		dbd.UpdateSQl("sys_testscripts", row, "upload_time", record[5]);
 	            		dbd.UpdateSQl("sys_testscripts", row, "owner", usr);
 	            	}
 	            } 	
// 	            如果是数据文件则更新对应脚本tcf属性
 	            else {
 	            	if(!ids[0][0].equals("")) {
 	            		row=Integer.parseInt(ids[0][0]);
 	            		dbd.UpdateSQl("sys_testscripts", row, "tcf", "有");
 	            	}	            	
 	            }
            }
            if(!dup_file.equals("")) {
            	dup_file=dup_file.substring(0, dup_file.length()-1)+"]";
            	throw new Exception("[info]201, 文件上传成功，但存在重名文件["+dup_file);
            }
		}catch (FileUploadBase.FileSizeLimitExceededException e) {
			logger.error(e.getMessage(), e);
			throw new Exception("[info]403, 单个文件超出最大值！");
        }catch (FileUploadBase.SizeLimitExceededException e) {
        	logger.error(e.getMessage(), e);
			throw new Exception("[info]403, 上传文件的总的大小超出限制的最大值！");
        }catch (SQLException e) {	
        	logger.error(e.getMessage(), e);
			throw new Exception(e);
        }catch (Throwable e) {
        	logger.error(e.getMessage(), e);
			throw new Exception("[info]402, 文件上传失败！");
        }
	}

	/**[Function] 				删除脚本
	 * @param tsid			要删除的测试脚本编号
	 * @throws Exceeption 412, 要删除的脚本被锁定
	 * @throws Exceeption 500, 脚本删除文件失败
	 */
	void DelTS(String tsid) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			String[][] ts=dbd.readDB("sys_testscripts", "lok,id,project,module,name", "tsid='"+tsid+"'");
			if(!ts[0][0].equals("")) {
				if(!ts[0][0].equals("0"))throw new Exception("[info]412, 脚本["+tsid+"]被测试集调用，不能删除!");
				int row=Integer.parseInt(ts[0][1]);
				dbd.DelSQl("sys_testscripts", row, 1, 1);
//				删除脚本文件
				String TSpath=confpath+"\\webapps\\S-atmp\\WEB-INF\\classes\\testScripts\\";
				String TSfilepath=TSpath+ts[0][2]+"\\"+ts[0][3]+"\\"+ts[0][4];
				File file = new File(TSfilepath);  								
				if(file.exists()) {
					if(!file.delete())throw new Exception("[info]500, 脚本["+tsid+"]删除文件失败，请登录服务器手动删除!"); 	
					TSfilepath=TSfilepath.replace(".class", ".txt");
					file = new File(TSfilepath);  	
					if(file.exists()) {
						if(!file.delete())throw new Exception("[info]500, 脚本["+tsid+"]删除数据配置文件失败，请登录服务器手动删除!"); 	
					}
				}
			}
		}catch(Throwable e) {
			logger.error(e.getMessage(),e);
			throw new Exception(e);
		}
	}
	/**
	 * [Function]	列出指定项目的测试脚本
	 * @param ProjTag		项目标签				
	 * @param filter				脚本过滤条件，只支持name、uploader、upload_time、lok、tcf
	 * @param page_count	每页条目数
	 * @param page_num	页码
	 * @return	 JSON字符串，例如{"testscripts":[{"tsid":"13", "name":"xxx", "project":"abc", "module":"wer", "owner":"leo", "upload_time":"2017-12-21 12:00:00", "lok":"0"}, ...],
	 * "total_num":23, "code":200}
	 * @throws Exception
	 */
	String ListTS(String ProjTag, String filter, String page_count, String page_num) throws Exception{
		PropertyConfigurator.configure(logconf);
		try {
			JSONObject ts=new JSONObject();
			JSONArray tslist=new JSONArray();
			if(filter.equals(""))filter="project='"+ProjTag+"'";	
			else {
				if(filter.indexOf("=")==-1)filter=filter.replace("*", "%");			
				filter=filter+" and project='"+ProjTag+"'";	
			}
			filter=filter+" order by module,upload_time desc";	
			if(!page_count.equals("")&&!page_num.equals("")) {
				int pg_count=Integer.parseInt(page_count);
				int pg_num=Integer.parseInt(page_num);
				int past_itemnum=pg_count*(pg_num-1);
				filter=filter+" limit "+past_itemnum+","+pg_count;
			}
			String[][] ats=dbd.readDB("sys_testscripts", "tsid,name,module,owner,upload_time,lok,tcf", filter);
			int num=dbd.checknum("sys_testscripts", "id", filter);
			if(!ats[0][0].equals("")){
				String[] ats_colname={"tsid","name","module","owner","upload_time","lok","tcf"};
				for(int i=0;i<ats.length;i++) {
					JSONObject ats_dat=new JSONObject();	
					ats[i][4]=sdf_full.format(sdf_full.parse(ats[i][4]));
					for(int k=0;k<ats_colname.length;k++)ats_dat.put(ats_colname[k], ats[i][k]);
					tslist.put(i, ats_dat);
				}
			}		
			ts.put("total_num", num);
			ts.put("ts", tslist);
			ts.put("code", 200);
			return ts.toString();
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
	
	/**[Function] 				验证Token参数是否正确，以及用户名和密码是否匹配
	 * @return [int]			返回执行结果代码，200对应操作成功，412对应校验不通过，500为数据库或系统错误
	 */
	int TokenVerify(String token){
		int code=0;
		return code;
	}
}