package ATdriver;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import base.*;

public class ATFactory extends Thread{
	DBDriver dbd = new DBDriver();
	AtmpDriver ad=new AtmpDriver();
//	配置日志属性文件位置
	static String currentProjectPath=System.getProperty("user.dir").replace("\\bin", "");
	static String logconf=currentProjectPath+"\\conf\\atmp\\logconf.properties";
	Logger logger = Logger.getLogger(ATFactory.class.getName());
	public String taskno;
	public void run(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		int StartRow=1;
		try {
			PropertyConfigurator.configure(logconf);
			int Tasknum=dbd.check("sys_taskquene");
//			获取任务表第一条记录，返回id, taskid, tset, proj, projtag, creattime, creater, status, starttime
			while(Tasknum>0) {
				String[][] task=dbd.readDB("sys_taskquene", "*", "id='1'");
//				如果当前任务状态为stop说明之前执行了stop操作，则跳出循环，停止执行脚本
				if(task[0][7].equals("停止")) {
					logger.info("测试被停止...");
					break;
				}
				if(taskno.equals("1")) {
//					获取临时任务表中的stop脚本序号，如果找不到则说明脚本已被执行完，或者异常，需要从新初始化
					StartRow=dbd.check("Temp_Task", "status","stop"); 
					if(StartRow>0)dbd.UpdateSQl("Temp_Task", StartRow, "status", "norun");
					else StartRow=1;
					
				}
//				如果StartRow=1说明需要进行初始化操作
				if(StartRow==1){
					logger.info("初始化测试库...");
					ad.init_temptask();	
					dbd.UpdateSQl("sys_taskquene", 1, "starttime",sdf.format(new Date()));
				}
				logger.info("开始执行测试任务"+task[0][1]);
				ad.ATexec(task[0][1], task[0][2], StartRow,task[0][6]);
				StartRow=1;
				Tasknum=dbd.check("sys_taskquene");
			}
		}catch(Throwable e) {		
			try {
				int row=dbd.check("sys_taskquene", "status", "执行中");
				dbd.UpdateSQl("sys_taskquene", row, "status", "停止");
			}catch(Throwable e1) {		
				logger.error(e.getMessage(),e1);
			}			
			logger.error(e.getMessage(),e);
		}	
	}
}
