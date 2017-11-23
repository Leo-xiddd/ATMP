/** 类说明：本模块用于实现XML配置文件的创建、读、写等操作，准备扩展为通用模块，所以不涉及本项目相关配置
 *  作   者：Leo
 *  时   间：2016/10/30
 *  方   法：本模块支持的方法包括：
 *  	1. Create			创建一个XML文件
 *  	2. Remove			删除一个XML文件
 *  	3. Add				添加一个节点（proj）
 *  	4. Del				删除一个节点（含下面所有内容）（proj）
 *  	5. Update			更新一个节点的内容（proj）
 *  	6. GetNode		获取一个节点的内容（comm）
 *  	7. GetList			获取一个节点下的所有子节点（comm）
 *  	8. NodeExist		判断节点是否存在
 */
package base;
import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLDriver {	
	/**
	 * @函数说明					使用模板创建一个XML配置文件
	 * @param ftemp			模板文件路径和名称
	 * @param filename		要创建的文件路径和名称
	 * @throws Throwable 	404，文件模板不存在
	 * @throws Throwable	409，目标文件已存在
	 * @throws Throwable 	412，参数错误
	 * @throws Throwable	500，系统错误
	 */
	public void Create(String ftemp,String filename) throws Throwable{
//		判断参数是否正确
		if (ftemp.equals("")) throw new Exception("[info]412,模板名不能为空！");
		if (filename.equals("")) throw new Exception("[info]412,新文件名不能为空！");
		
//		判断模板文件是否存在，如果不存在返回404
		File tempfile = new File(ftemp);
		if (!tempfile.exists()) throw new Exception("[info]404,文件模板"+ftemp+"不存在！");
		
//		判断要创建的文件是否已存在，如果存在返回409
		File pconf = new File(filename);
		if (pconf.exists()) throw new Exception("[info]409,已存在文件"+filename);
		
//		创建新文件
		try{	
			BufferedReader br = new BufferedReader (new InputStreamReader (new FileInputStream (tempfile), "UTF-8"));
			BufferedWriter bw = new BufferedWriter (new OutputStreamWriter (new FileOutputStream (pconf), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {    
				bw.write(line);
				bw.newLine();
			} 
			br.close();
			bw.flush();
			bw.close();
		} catch (Exception e) {
			if (pconf.exists())pconf.delete();
			throw new Exception("[info]500,"+e.toString());
		}
	}
	
	/**
	 * @函数说明					删除一个XML文件
	 * @param filename		文件名（含路径）
	 * @throws Throwable 	404，文件不存在
	 * @throws Throwable 	412，参数错误
	 * @throws Throwable 	500，系统错误
	 */
	public void Remove(String filename) throws Throwable{
		if (filename.equals("")) throw new Exception("[info]412,文件名不能为空！");
		File f = new File(filename);
		if (!f.exists()) throw new Exception("[info]404,文件"+filename+"不存在！");
		try {
			f.delete();
		}catch(Exception e) {
			throw new Exception("[info]500,"+e.toString());
		}
	}
	
	/**
	 * @函数说明					为当前节点添加一个子节点，本方法使用时要求节点树上同一目录下的节点名都是唯一的
	 * @param filename		XML文件名（含路径）
	 * @param Path				要添加的节点位置，如果位置节点名是唯一的可以直接填节点名，否则需要给出能够标明唯一位置的路径，父节点与节点间用‘/’分隔
	 * @param key				要添加的节点名
	 * @param Value			要添加的节点值
	 * @throws Throwable 	404，文件不存在或者找不到节点
	 * @throws Throwable 	412，参数错误
	 * @throws Throwable 	500，系统错误
	 */
	public void Add(String filename,String Path,String key,String Value) throws Throwable{
		File f = new File(filename);
		if (!f.exists()) throw new Exception("[info]404,配置文件"+filename+"不存在！");
		if (Path.equals("")) throw new Exception("[info]412,Path参数不能为空！");
		if (key.equals("")) throw new Exception("[info]412,节点名不能为空！");
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try{
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(f);
			Node nd=CheckNode(doc,Path);
			if(nd==null)throw new Exception("[info]404,节点路径未找到！");
			Element emc=doc.createElement(key);
			nd.appendChild(emc);
			emc.setTextContent(Value);
			
//			将结果写入xml文件
            TransformerFactory tFactory = TransformerFactory.newInstance();  
            Transformer transformer = tFactory.newTransformer();  
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");  
            DOMSource source = new DOMSource(doc);  
            StreamResult result = new StreamResult(f);  
            transformer.transform(source, result); 
		}catch (Throwable e) {
			throw new Exception("[info]500,"+e.toString());
		}
	}
	/**
	 * @函数说明					删除XML文件的一个节点
	 * @param filename		XML文件名（含路径）
	 * @param Path				要删除的节点位置，如果位置节点名是唯一的可以直接填节点名，否则需要给出能够标明唯一位置的路径，父节点与节点间用‘/’分隔
	 * @throws Throwable 	404，文件不存在或者找不到节点
	 * @throws Throwable 	412，参数错误
	 * @throws Throwable 	500，系统错误
	 */
	public void Del(String filename,String Path) throws Throwable{
		File f = new File(filename);
		if (!f.exists()) throw new Exception("[info]404,文件"+filename+"不存在！");
		if (Path.equals("")) throw new Exception("[info]412,Path参数不能为空！");
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try{
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(f);
			Node nd=CheckNode(doc,Path);
			if(nd==null)throw new Exception("[info]404,节点路径未找到！");
			Node catParent = nd.getParentNode(); 
			catParent.removeChild(nd);
			
//			将结果写入xml文件
            TransformerFactory tFactory = TransformerFactory.newInstance();  
            Transformer transformer = tFactory.newTransformer();  
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");  
            DOMSource source = new DOMSource(doc);  
            StreamResult result = new StreamResult(f);  
            transformer.transform(source, result); 
		}catch (Throwable e) {
			throw new Exception("[info]500,"+e.toString());
		}
	}

	/**
	 * @方法说明					修改一个节点的内容
	 * @param filename		XML文件名（含路径）
	 * @param Path				要修改的节点位置
	 * @param value			要修改的值
	 * @throws Throwable 	404，文件不存在或者找不到节点
	 * @throws Throwable 	412，参数错误
	 * @throws Throwable 	500，系统错误
	 */
	public void Update(String filename,String Path, String value) throws Throwable{
		File f = new File(filename);
		if (!f.exists()) throw new Exception("[info]404,文件"+filename+"不存在！");
		if (Path.equals("")) throw new Exception("[info]412,Path参数不能为空！");
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try{
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(f);
			Node nd=CheckNode(doc,Path);
			if(nd==null)throw new Exception("[info]404,节点路径未找到！");
			NodeList nl=nd.getChildNodes();
			int num=nl.getLength();
			for(int i=0;i<nl.getLength();i++) {
//				子节点有三种类型，类型3的不应包括在里面
				if(nl.item(i).getNodeType()==3)num--;
			}
			if(num>0)throw new Exception("[info]409,指定节点下面有子节点，不能修改值！");
			nd.setTextContent(value);
			
//			将结果写入xml文件
            TransformerFactory tFactory = TransformerFactory.newInstance();  
            Transformer transformer = tFactory.newTransformer();  
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");  
            DOMSource source = new DOMSource(doc);  
            StreamResult result = new StreamResult(f);  
            transformer.transform(source, result); 
		}catch (Throwable e) {
			throw new Exception("[info]500,"+e.toString());
		}
	}
	
	/**
	 * @函数说明					读取指定节点的值
	 * @param filename		XML文件名（含路径）
	 * @param Path  			要读取的节点路径
	 * @return 					节点的值
	 * @throws Throwable 	404，文件不存在或者找不到节点
	 * @throws Throwable 	412，参数错误
	 * @throws Throwable 	500，系统错误
	 */
	public String GetNode(String filename, String Path) throws Throwable{
		File f = new File(filename);
		if (!f.exists()) throw new Exception("[info]404,文件"+filename+"不存在！");
		if (Path.equals("")) throw new Exception("[info]412,Path参数不能为空！");
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		String ba="";
		try{
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(f);
			Node nd=CheckNode(doc,Path);
			if(nd==null)throw new Exception("[info]404,节点路径未找到！");
			NodeList nl=nd.getChildNodes();
			for(int i=0;i<nl.getLength();i++) {
				if(nl.item(i).getNodeType()==3) {
					ba=nl.item(i).getTextContent();
					break;
				}
			}			
		}catch (Throwable e) {
			e.printStackTrace();
			throw new Exception("[info]500,"+e.toString());
		}	
		return ba;
	}
	
	/**
	 * @函数说明					读取文件中指定节点下所有的子节点按数组返回，只适用于三级数组节点的情况
	 * @param filename		XML文件名（含路径）
	 * @param Path				要读取的节点路径，格式为：A/B/C/或者A
	 * @return 					Json数组格式字符串，返回所有子节点和内容，示例如下：
	 * 									[{"abc":"123"},{"d":"123"}]
	 * @throws Throwable 	404，文件不存在或者找不到节点
	 * @throws Throwable 	412，参数错误
	 * @throws Throwable 	500，系统错误
	 */
	public String GetList(String filename,String Path) throws Throwable{
		File f = new File(filename);
		if (!f.exists()) throw new Exception("[info]404,文件"+filename+"不存在！");
		if (Path.equals("")) throw new Exception("[info]412,Path参数不能为空！");
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		String ba="";
		try{
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(f);
			Node nd=CheckNode(doc,Path);
			if(nd==null)throw new Exception("[info]404,节点路径未找到！");						
			JSONArray trs = new JSONArray();
			NodeList nl=nd.getChildNodes();
			int num=nl.getLength();
			Node cnd;
			for(int i=0;i<num;i++){				
				JSONObject tr = new JSONObject(); 
//				判断当前节点类型是node（1）还是文本节点（3）（xml中会将空格、tab、回车当成文本节点）
				cnd=nl.item(i);
				if(cnd.getNodeType()==1) {
					NodeList cnl=cnd.getChildNodes();
					for(int j=0;j<cnl.getLength();j++) {
						if(cnl.item(j).getNodeType()==1)	tr.put(cnl.item(j).getNodeName(),cnl.item(j).getTextContent());	
					}
					trs.put(tr);
					tr=null;
				}
			}
			ba=trs.toString();
		}catch (Throwable e) {
			e.printStackTrace();
			throw new Exception("[info]500,"+e.toString());
		}
		return ba;
	}	
	/**
	 * @函数说明					查找符合条件的节点的父节点名
	 * @param filename		XML文件名（含路径）
	 * @param key				作为查找条件的节点名
	 * @param value			作为查找条件的节点内容
	 * @return 					父节点名，未找到则返回""
	 * @throws Throwable 	404，文件不存在或者找不到节点
	 * @throws Throwable 	412，参数错误
	 * @throws Throwable 	500，系统错误
	 */
	public String GetParentNode(String filename, String key, String value)throws Throwable{
		File f = new File(filename);
		if (!f.exists()) throw new Exception("[info]404,文件"+filename+"不存在！");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		String ba="";
		try{
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(f);
			NodeList nl=doc.getElementsByTagName(key);
			for(int i=0;i<nl.getLength();i++) {
				if(nl.item(i).getTextContent().equals(value)) {
					Node nd=nl.item(i).getParentNode();
					return nd.getNodeName();
				}
			}
		}catch (Throwable e) {
			e.printStackTrace();
			throw new Exception("[info]500,"+e.toString());
		}
		return ba;
	}
	/**
	 * @函数说明					读取文件中指定节点下所有的子节点(不含3级节点)
	 * @param filename		XML文件名（含路径）
	 * @param Path				要读取的节点路径，格式为：A/B/C/或者A
	 * @return 					Json格式字符串，返回所有子节点和内容，示例如下：
	 * 									{"abc":"123","d":"123","key1":"123","key2":"123"}
	 * @throws Throwable 	404，文件不存在或者找不到节点
	 * @throws Throwable 	412，参数错误
	 * @throws Throwable 	500，系统错误
	 */
	public String GetListA(String filename,String Path) throws Throwable{
		File f = new File(filename);
		if (!f.exists()) throw new Exception("[info]404,文件"+filename+"不存在！");
		if (Path.equals("")) throw new Exception("[info]412,Path参数不能为空！");
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		String ba="";
		try{
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(f);
			Node nd=CheckNode(doc,Path);
			if(nd==null)throw new Exception("[info]404,节点路径未找到！");			
			NodeList nl=nd.getChildNodes();
			int num=nl.getLength();
			Node cnd;
			String value="";
			JSONObject tr = new JSONObject(); 
			for(int i=0;i<num;i++){								
//				判断当前节点类型是node（1）还是文本节点（3）（xml中会将空格、tab、回车当成文本节点）
				cnd=nl.item(i);
				if(cnd.getNodeType()==3)continue;
				
				NodeList cnl=cnd.getChildNodes();
				for(int j=0;j<cnl.getLength();j++) {
					if(cnl.item(j).getNodeType()==3) {
						value=cnl.item(j).getTextContent();
						break;
					}
				}
				tr.put(cnd.getNodeName(),value);
			}
			ba=tr.toString();
		}catch (Throwable e) {
			e.printStackTrace();
			throw new Exception("[info]500,"+e.toString());
		}
		return ba;
	}
	
	/**
	 * @函数说明					检查指定节点是否存在
	 * @param filename		XML文件名（含路径）
	 * @param Path  			要读取的节点路径
	 * @return 					如果该节点存在则返回True，否则为False
	 * @throws Throwable 	500，系统错误
	 */
	public  boolean NodeExist(String filename, String Path) throws Throwable{
		File f = new File(filename);
		if (!f.exists()) throw new Exception("[info]404,文件"+filename+"不存在！");
		if (Path.equals("")) throw new Exception("[info]412,Path参数不能为空！");
		
		boolean ra=true;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(f);
			Node nd=CheckNode(doc,Path);
			if(null==nd)ra=false;
		}catch (Throwable e) {
			throw new Exception("[info]500,"+e.toString());
		}
		return ra;
	}
	
	Node CheckNode(Document doc, String Path) {
		Node nd=null;
		try {
			String[] nds=Path.split("/");
//			在文件根目录查找路径的根节点
			NodeList nl = doc.getElementsByTagName(nds[0]);		
			System.out.println(nl.getLength());
			if(nl.getLength()>0) {
				nd=nl.item(0);
				int num=nds.length;
				int numb;
				int tag=0;
				String nm;
//				如果根节点存在，且路径未查完，则进入循环
				for(int i=1;i<num;i++) {
					nl=nd.getChildNodes();
					numb=nl.getLength();
					for(int j=0;j<numb;j++) {
						nd=nl.item(j);
//						只检查元素节点
						if(nd.getNodeType()==1) {
							nm=nd.getNodeName();
							if(nm.equals(nds[i])) {
								tag=1;						
								break;
							}
						}				
					}
					if(tag==0) {
						nd=null;
						break;
					}
					tag=0;
				}
			}		
		}catch (Throwable e) {
			nd=null;
		}
		return nd;
	}
}
