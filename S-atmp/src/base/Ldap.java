package base;
import java.util.Hashtable;

import javax.naming.Context;  
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;  
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;  
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

import org.apache.log4j.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class Ldap {
	static String confpath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=confpath+"\\conf\\atmp\\logconf.properties";
	static String sysconf=confpath+"\\conf\\atmp\\sys_config.xml";
	static Logger logger = Logger.getLogger(Ldap.class.getName());
	private static DirContext ctx;  
	   
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void getCtx(String usrname,String pwd)throws Throwable {  
		PropertyConfigurator.configure(logconf);
        logger.info("连接LDAP");        
        try { 
        	XMLDriver xml=new XMLDriver();
        	String host=xml.GetNode(sysconf, "LDAP_conf/host");
        	String port=xml.GetNode(sysconf, "LDAP_conf/port");
        	String domain=xml.GetNode(sysconf, "LDAP_conf/domain");
        	String root=domain.replace(".", ",DC=");
        	root=root.replace("@", "DC=");
        	
        	Hashtable env = new Hashtable();  
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");  
            env.put(Context.PROVIDER_URL, "ldap://"+host+":"+port+"/" + root);  
            env.put(Context.SECURITY_AUTHENTICATION, "simple");  
            env.put(Context.SECURITY_PRINCIPAL,usrname+domain);
            env.put(Context.SECURITY_CREDENTIALS, pwd);  
            ctx = new InitialDirContext(env);   
        } catch (javax.naming.AuthenticationException e) {  
            logger.error("用户名或密码错误");
            throw e;
        } catch (Throwable e) {  
        	throw e;
        }  
    }  
	
	void closeCtx(){  
		PropertyConfigurator.configure(logconf);
	    try {  
	    	ctx.close();  
	    } catch (NamingException ex) {  
	    	logger.error("关闭LDAP出错", ex);
	    }  
	}  
	
	public boolean authen(String usrname,String pwd) {
		try {
			getCtx(usrname,pwd);
			if(ctx !=null) {
				ctx.close();	
				return true;
			}
		}catch(Throwable e) {
			logger.error("认证失败", e);
		}
		return false;
	}
	
	/**
	 * 函数说明：	获取LDAP中的用户信息
	 * @param baseDN	导出用户的基础路径
	 * @return		一个Json字符串，字段包括（account,fullname,dept,dept2,mail,mobile）;
	 * @throws Throwable
	 */
	public String Getuserlist(String baseDN) throws Exception {
		PropertyConfigurator.configure(logconf);		
		try {
			XMLDriver xml=new XMLDriver();
        	String admin_name=xml.GetNode(sysconf, "LDAP_conf/admin");
        	String admin_pwd=xml.GetNode(sysconf, "LDAP_conf/pwd");
			getCtx(admin_name,admin_pwd);
			
			JSONArray usrl=new JSONArray();	
			int i=0;
			if(ctx != null){
				NamingEnumeration<SearchResult> infos =ctx.search(baseDN, "objectClass=User", null, null);
				while (infos.hasMoreElements()) {
					JSONObject usr=new JSONObject();
			        Attributes attrss=infos.next().getAttributes();
			        usr.put("account", attrss.get("sAMAccountName").get().toString());
			        usr.put("fullname", attrss.get("cn").get().toString());
			        int tag=baseDN.indexOf("OU=");      
			        String dept="";
			        if(tag>-1)dept=baseDN.substring(tag+3, baseDN.indexOf(",", tag+1));
			        usr.put("dept", dept);       
			        usr.put("mail", attrss.get("mail").get().toString());
			        String mob="";
			        if(attrss.get("mobile")!=null)mob=attrss.get("mobile").get().toString();
			        usr.put("mobile", mob);
			        usrl.put(i, usr);
			        i++;
			    }
				ctx.close();				
			}
			return usrl.toString();
		}catch (NamingException e) {
			 logger.error(e.getMessage(),e);
			 throw new Exception(e);
        }catch (Throwable e) {
			 logger.error(e.getMessage(),e);
			 throw new Exception(e);
       }
	}
}
