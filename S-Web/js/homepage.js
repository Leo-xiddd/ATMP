var xmlHttp;
function TMS_api(url,med,dats,cfunc){
	var hostpath=getHostUrl();		
	try{
		url=hostpath+url;
		xmlHttp = new XMLHttpRequest();
		xmlHttp.onreadystatechange=cfunc;		
		xmlHttp.open(med,url,true);
		if(med=="GET")xmlHttp.send();
		else xmlHttp.send(dats);	
	}catch(e){
		alert(e);
	}	
}
$(document).ready(function(){ 
	// if(typeof(sessionStorage.customerId)=='undefined'){
	// 	var url="login.html";
	// 	window.open(encodeURI(url),'_self');
	// }
	$("body").css('height',($(document).height()));
	$("#main").css('height',($(document).height()-70));
	$("#user").text(sessionStorage.usrfullname);
	//避免页面刷新调回主页面，默认登录进入工作空间页面，对应按钮置亮
	var homepage="TestLab.html";
	if(typeof(sessionStorage.currpage)=='undefined'){
		sessionStorage.currpage=homepage;
		sessionStorage.module="TestLab";
	}
	$("#main").attr("src",sessionStorage.currpage);	
	$("#"+sessionStorage.module).css("background-color", "#2B83AB");	

	//点击退出按钮
	$("#butt_exit").click(function b(){	
		sessionStorage.currpage="login.html";
		window.open(sessionStorage.currpage,'_self');
	});

	//鼠标滑过菜单按钮的变色效果
	$(".main_menu").mouseenter(function(e) { 
		var clas=$(e.target).attr("class");	
		if(clas=='bb')$(e.target).parent().parent().css("background-color", "#2B83AB");
		else if(clas=='aa')$(e.target).parent().css("background-color", "#2B83AB");
		else $(e.target).css("background-color", "#2B83AB");		
	});
	$(".main_menu").mouseleave(function (e) { 
		var clas=$(e.target).attr("class");
		var  page_id="";
		if(clas=='bb')page_id=$(e.target).parent().parent().attr("id");
		else if(clas=='aa')page_id=$(e.target).parent().attr("id");
		else page_id=$(e.target).attr("id");
		if(page_id!=sessionStorage.module)$("#"+page_id).css("background-color", "#295669");
	});

	//选择模块跳转
	$(".main_menu").click(function b(e){
		var clas=$(e.target).attr("class");
		var  page_id="";
		if(clas=='bb')page_id=$(e.target).parent().parent().attr("id");
		else if(clas=='aa')page_id=$(e.target).parent().attr("id");
		else page_id=$(e.target).attr("id");

		$(".main_menu").css("background-color", "#295669");
		$("#"+page_id).css("background-color", "#2B83AB");		

		sessionStorage.currpage=page_id+".html";
		sessionStorage.module=page_id;
		$("#main").attr("src",sessionStorage.currpage);
	});
});