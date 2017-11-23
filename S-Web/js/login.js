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
	$("body").css({"height":getWindowInnerHeight,"width":getWindowInnerWidth});
	$("#butt_login").click(function(){	
		var user=$("#loginuser").val();
		var pwd= $("#loginpwd").val();	
		if(user==""){
			alert("用户名不能为空！");
			$("#loginuser").focus();
		}
		else if(pwd==""){
			alert("密码不能为空！");
			$("#loginpwd").focus();
		}
		else{	 
			url="User/Authen?user="+user+"&pwd="+encypt(pwd);
			TMS_api(url,"GET","",function()
			{if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){
					sessionStorage.currpage="TestLab.html";
					sessionStorage.customerId=user;
					sessionStorage.customerPwd=pwd;
					sessionStorage.usrfullname=resp.fullname;
					window.open("home.html",'_self');
				}
				else alerm(resp.message);
			}
			});
		}
	});
});

$(document).keyup(function(event){
	if(event.keyCode ==13) $("#butt_login").trigger("click");
});