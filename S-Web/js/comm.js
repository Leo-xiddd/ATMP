// 公共变量
function getHostUrl(){
    return "http://S-ATMP.tech.bitauto.com:8080/S-atmp/";
}
// 浏览器兼容 取得浏览器可视区高度 
function getWindowInnerHeight() {    
    var winHeight = window.innerHeight    
          || (document.documentElement && document.documentElement.clientHeight)    
          || (document.body && document.body.clientHeight);    
	return winHeight;          
}    
// 浏览器兼容 取得浏览器可视区宽度    
function getWindowInnerWidth() {    
    var winWidth = window.innerWidth    
		|| (document.documentElement && document.documentElement.clientWidth)    
        || (document.body && document.body.clientWidth);    
    return winWidth;            
} 
// 显示遮罩层     
function showOverlay(item_ID) {    
  // 遮罩层宽高分别为页面内容的宽高 
	$(item_ID).css("display","block");  
  $(item_ID).css({'height':$(document).height(),'width':$(document).width()});    
  $(item_ID).show();    
}  
//从URL中获取参数数值
function getvalue(str,name){ 
    if (str.indexOf(name)>-1){           
        var pos_start=str.indexOf(name)+name.length+1;
        var pos_end=str.indexOf("&",pos_start);
        if (pos_end==-1){
            return str.substring(pos_start);
        }else{
            return str.substring(pos_start,pos_end);      
        }
    }else return "";
}
//获取日期，返回YYYY-MM-DD HH:mm:ss格式的日期
function getDate() {
    var date = new Date();
    var seperator1 = "-";
    var seperator2 = ":";
    var month = date.getMonth() + 1;
    var strDate = date.getDate();
    if (month >= 1 && month <= 9) {
        month = "0" + month;
    }
    if (strDate >= 0 && strDate <= 9) {
        strDate = "0" + strDate;
    }
  var strhours=date.getHours();
  if (strhours >= 0 && strhours <= 9) {
        strhours = "0" + strhours;
    }
  var strmins=date.getMinutes();
  if (strmins >= 0 && strmins <= 9) {
        strmins = "0" + strmins;
    }
  var strsecs=date.getSeconds();
  if (strsecs >= 0 && strsecs <= 9) {
        strsecs = "0" + strsecs;
    }
    var currentdate = date.getFullYear() + seperator1 + month + seperator1 + strDate
            + " " + strhours+ seperator2 + strmins + seperator2 + strsecs;
    return currentdate;
}
// 判断字符串是否符合日期格式要求
function isDate(dateString){
  if(dateString.trim()=="")return true;
  var r=dateString.match(/^(\d{1,4})(-|\/)(\d{1,2})\2(\d{1,2})$/); 
  if(r==null){
   alert("请输入格式正确的日期\n\r日期格式：yyyy-mm-dd\n\r例  如：2008-08-08\n\r");
  return false;
  }
  var d=new Date(r[1],r[3]-1,r[4]);  
  var num = (d.getFullYear()==r[1]&&(d.getMonth()+1)==r[3]&&d.getDate()==r[4]);
  if(num==0){
   alert("请输入格式正确的日期\n\r日期格式：yyyy-mm-dd\n\r例  如：2008-08-08\n\r");
  }
  return (num!=0);
 }
 // 返回两个日期的差，type为返回单位，支持日、月、年
function getTime_diff(pretime,lasttime,type){
    var oDate1,oDate2,iDays,iMonths,iYears;  
    var aDate=pretime.split("-")
    var oDate1=new Date(aDate[1]+'-'+aDate[2]+'-'+aDate[0]);    //转换为12-18-2002格式  
    aDate=lasttime.split("-");
    var oDate2=new Date(aDate[1]+'-'+aDate[2]+'-'+aDate[0]);  
    var iDays=parseFloat(Math.abs(oDate1-oDate2)/1000/60/60/24);    //把相差的毫秒数转换为天数 
    var iMonths=iDays/30.0;    //把相差的毫秒数转换为月数 
    var iYears=iDays/365.0;    //把相差的毫秒数转换为年数 
    if(type=="year") return  iYears.toFixed(1);
    else if(type=="month")return iMonths.toFixed(1);
    else return iDays.toFixed(0);  
}
//收缩或展开窗体
function toggle_box(child_formID){
    $(child_formID).parent().slideToggle("fast");
}
// 检查字符串是否符合货币数字格式，支持为空
function checkfee(fee){
  if(fee!=""){
    if(isNaN(fee))return false;
    if(fee.indexOf(".")>-1){
      if(fee.length-fee.indexOf(".")>3) return false;
    }
  }
  return true;
}
// 检查字符串是否符合百分率格式，必须是小于100的数字，支持两位小数
function checkrate(rate){
  if(rate=="") return false;
  if(isNaN(rate))return false;
  if(rate.indexOf(".")>-1){
    if(rate.length-rate.indexOf(".")>3) return false;
  }
  var temp=parseFloat(rate);
  if(temp>100)return false;
  return true;
}
// 将数字字符串转化为货币格式字符串
function rmb(fee){
  if(fee.indexOf(".")==-1)fee=fee+".00";
  var fs=fee.split(".");
  if(fs[1].length==1)fs[1]=fs[1]+"0";
  else if(fs[1].length>2)fs[1]=fs[1].substr(0,2);
  var fas=fs[0].split("");
  var temp="";
  var k=0;
  for(var i=fas.length-1;i>=0;i--){
    if(k==3){
      temp=","+temp;
      k=0;
    }
    temp=fas[i]+temp;
    k++;
  }
  return temp+"."+fs[1];
}
// 打开弹层
function open_form(formID,overlayID){
  $(formID).css("display","block");
  showOverlay(overlayID);
  $(formID).show(); 
}
// 关闭弹层
function CloseForm(formID,overlayID){
  $(formID).hide();
  $(overlayID).hide();
}
// 告警提示
function alerm(mess){
  $("#alert_mess").text(mess);
  open_form("#form_alert","#overlay");
}