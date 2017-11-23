/**
 * 说明：本类主要实现告警和邮件报告类的功能，包括如下接口
 * void infom(String Context)
 * void toemail(String Receivers,String message)
 * void tosm(String Receivers,String message)
 */
package base;

import java.util.Calendar;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Inform{
	XMLDriver xml=new XMLDriver();
//	配置日志属性文件位置
	static String confpath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=confpath+"\\conf\\atmp\\logconf.properties";
	static String sysconf=confpath+"\\conf\\atmp\\sys_config.xml";	
	Logger logger = Logger.getLogger(Inform.class.getName());
	
	public void toemail(String Receivers,String Senders,String Subject,String Context,String Texttype,String Atta)throws Exception {
		Thread t = new Thread(new Runnable(){  
			public void run(){
				PropertyConfigurator.configure(logconf);		
				 try {
					 logger.info("Receivers:"+Receivers+"  Subject:"+Subject);
					SDemail(Receivers,Senders,Subject,Context,Texttype,Atta);
				} catch (Exception e) {
					logger.error(e.getMessage(),e);
					try {
						throw new Exception(e.getMessage());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		});  
		t.start();
	}
	/**
	 * 函数说明：	本函数用于实现通用的邮件发送功能，支持附件，可以独立使用
	 * @param Receivers		接收人邮件地址，支持多人，用逗号分隔
	 * @param Senders		邮件发送人
	 * @param Subject			邮件主题
	 * @param Context		邮件正文
	 * @param Texttype		邮件正文字符编码
	 * @param Atta				附件地址，为""表示没有附件
	 * @throws Exception		412,邮件地址参数错误
	 * @throws Exception		500,邮件发送故障
	 */
	public void SDemail(String Receivers,String Senders,String Subject,String Context,String Texttype,String Atta)throws Exception {
//		String smtpHost="mail.hd.cig.com.cn";				
//		String mail_account="asws@hd.cig.com.cn";
//		String mail_passwd="NDU886mckJ";		
		PropertyConfigurator.configure(logconf);		
		try {
			 // 第一步：配置邮件服务器对象 
			String smtpHost=xml.GetNode(sysconf, "Mail_conf/smtpHost");
			String mail_account=xml.GetNode(sysconf, "Mail_conf/account");
			String mail_passwd=xml.GetNode(sysconf, "Mail_conf/passwd");
			Senders=mail_account;
			String tls_enable=xml.GetNode(sysconf, "Mail_conf/tls");
			String ssl_enable=xml.GetNode(sysconf, "Mail_conf/ssl");
			String port=xml.GetNode(sysconf, "Mail_conf/port");
			String auth=xml.GetNode(sysconf, "Mail_conf/auth");
			
			Properties props = new Properties();
			props.put("mail.smtp.host", smtpHost);  
			props.put("mail.smtp.starttls.enable",tls_enable);//使用 STARTTLS安全连接  
			props.put("mail.smtp.ssl.enable", ssl_enable);
			
			props.put("mail.smtp.port", port);              
			props.put("mail.smtp.auth", auth);        // 使用验证  ;  
			Session mailSession = Session.getInstance(props,new MyAuthenticator(mail_account,mail_passwd));  
			
//			进行邮件地址去重
			if(!Receivers.equals("")) {
				String rec="";
				String[] b=Receivers.split(",");
				Receivers=Receivers+",";
				for(int i=0;i<b.length;i++) {
					if(!b[i].equals("")&&Receivers.indexOf(b[i])>-1) {
						rec=rec+b[i]+",";
						Receivers=Receivers.replace((b[i]+","), "");	
					}
				}
				Receivers=rec.substring(0,rec.length()-1); 
				String[] tomails=Receivers.split(",");
//				 第二步：设置邮件地址			
				InternetAddress fromAddress = new InternetAddress(Senders);  
				InternetAddress[] toAddress = new InternetAddress[tomails.length];
				for(int j=0;j<tomails.length;j++)toAddress[j]=new InternetAddress(tomails[j]);
				MimeMessage message = new MimeMessage(mailSession);  
				
				message.setFrom(fromAddress);  
				message.setRecipients(RecipientType.TO, toAddress);  
				  
				message.setSentDate(Calendar.getInstance().getTime());  
				message.setSubject(Subject);  
				
				Multipart mp = new MimeMultipart();
				MimeBodyPart mbp_cont = new MimeBodyPart();
				mbp_cont.setContent(Context,Texttype);
				mp.addBodyPart(mbp_cont);
				
//				判断是否有附件并添加
				if(!Atta.equals("")) {
					if(Atta.indexOf(";")>-1) {
						String[] Attapath=Atta.split(";");
						for(int k=0;k<Attapath.length;k++) {
							MimeBodyPart mbp_attach = new MimeBodyPart();       
							FileDataSource fds=new FileDataSource(Attapath[k]); //得到数据源  
							mbp_attach.setDataHandler(new DataHandler(fds)); //得到附件本身并至入BodyPart  
							mbp_attach.setFileName(fds.getName()); 
							mp.addBodyPart(mbp_attach);
						}					
					}
					else {
						MimeBodyPart mbp_attach = new MimeBodyPart();       
						FileDataSource fds=new FileDataSource(Atta); //得到数据源  
						mbp_attach.setDataHandler(new DataHandler(fds)); //得到附件本身并至入BodyPart  
						mbp_attach.setFileName(fds.getName()); 
						mp.addBodyPart(mbp_attach);
					}
				}
				message.setContent(mp);  
				// 第三步：发送消息  
				Transport transport = mailSession.getTransport("smtp");  
				transport.connect(smtpHost,mail_account, mail_passwd);  
				Transport.send(message, message.getRecipients(RecipientType.TO));
			}			  
		}catch(AddressException e) {
			throw new Exception("[info]412,发送人邮件地址不正确，"+e.toString());
		}catch(MessagingException e) {
			e.printStackTrace();			
			throw new Exception("[info]500,邮件发送错误："+e.getCause().toString());			
		}catch(Throwable e) {
			logger.error("XML err: "+e.toString(), e);
        	throw new Exception(e);
		}
	}
	
	public void tosm(String Receivers,String message) {
		
	}
}
class MyAuthenticator extends Authenticator{  
    String userName="";  
    String password="";  
    public MyAuthenticator(){  
          
    }  
    public MyAuthenticator(String userName,String password){  
        this.userName=userName;  
        this.password=password;  
    }  
    protected PasswordAuthentication getPasswordAuthentication(){     
       return new PasswordAuthentication(userName, password);     
     }   
  }  
