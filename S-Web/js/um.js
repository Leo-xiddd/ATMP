var xmlHttp;
var page_num;
var page_sum;
var tr_selected;
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
		alerm(e);
	}	
}
// 获取指定用户的权限表
function loadpurview(){
	var user=tr_selected.children().eq(1).text();
	var url="User/LoadPurview?user="+user;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				var purview=resp.purview;
				$("#tbody_purviewlist tr").remove();
				for(var i=0;i<purview.length;i++){
					var line='<tr><td colspan="2"><div class="module_title">'+purview[i].module+'</div></td></tr>';
					var pv_list=purview[i].list;
					var ll="";
					for(var j=0;j<pv_list.length;j++){
						ll=ll+'<td><input type="checkbox" id="'+pv_list[j].key+'">'+pv_list[j].text+'</td>';
						if((j+1)%2==0){
							line=line+"<tr>"+ll+"</tr>";
							ll="";
						}
					}
					if(ll!="")line=line+"<tr>"+ll+"</tr>";
					$("#tbody_purviewlist").append(line);
					for(var j=0;j<pv_list.length;j++){
						if(pv_list[j].value=="x")$("#"+pv_list[j].key).attr("checked",false);
						else $("#"+pv_list[j].key).attr("checked",true);	
					}
				}
			}
			else if(resp.code==404){
				// 如果返回404则认为权限表不存在该用户
				$("#tbody_purviewlist input").attr("checked",false);		
			}
			else alerm(resp.message);
		}
	});	
}
function LoadUserList(filter){
	var item_ppnum=18;
	var url="User/List?"+filter+"&page_count="+item_ppnum+"&page_num="+page_num;;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				var usrlist=resp.userlist;
				var bgcolor='background-color:#F0ECF3;';
				$("#tbody_usrlist tr").remove();
				for(var i=0;i<usrlist.length;i++){
					if(bgcolor=="")bgcolor='background-color:#F0ECF3;';
					else bgcolor="";
					var line='<tr style="'+bgcolor+'">';
					line=line+'<td align="center">'+usrlist[i].id+'</td>';
					line=line+'<td>'+usrlist[i].usrname+'</td>';
					line=line+'<td>'+usrlist[i].fullname+'</td>';
					line=line+'<td>'+usrlist[i].role+'</td>';
					line=line+'<td>'+usrlist[i].dept+'</td>';
					line=line+'<td>'+usrlist[i].email+'</td>';
					line=line+'<td>'+usrlist[i].mobile+'</td>';
					line=line+'<td style="border-right:1px solid #868A8D;">'+usrlist[i].type+'</td>';
					line=line+'</tr>';
					
					$("#tbody_usrlist").append(line);
				}
				var item_sum=parseInt(resp.total_num);
				page_sum=Math.ceil(item_sum/item_ppnum);
				$("#page_num").text(page_sum);
				$("#curr_page").text(page_num);
				if(page_num==1){
					$("#Fir_page").attr("disabled",true);
					$("#Pre_page").attr("disabled",true);
				}
				else{
					$("#Fir_page").attr("disabled",false);
					$("#Pre_page").attr("disabled",false);
				}
				if(page_num==page_sum){
					$("#Next_page").attr("disabled",true);
					$("#Las_page").attr("disabled",true);
				}
				else{
					$("#Next_page").attr("disabled",false);
					$("#Las_page").attr("disabled",false);
				}

				$("#last_sync_time").text(resp.last_sync_time);
			}
			else alerm(resp.message);
		}
	});
}
// 保存用户信息
function save_userInfo(url){
	var usrinfo={};
	usrinfo.usrname=$("#usr_account").val();
	usrinfo.fullname=$("#usr_fullname").val();
	usrinfo.passwd=$("#usr_pwd").val();
	
	if(usrinfo.usrname==""){
		alerm("用户账号不能为空");
		$("#usr_account").focus();
	}
	else if(usrinfo.fullname==""){
		alerm("用户姓名不能为空");
		$("#usr_fullname").focus();
	}
	else if(usrinfo.passwd==""){
		alerm("用户密码不能为空");
		$("#usr_passwd").focus();
	}
	else{
		usrinfo.type=$("#usr_type").val();
		usrinfo.dept=$("#usr_dept").val();
		usrinfo.role=$("#usr_role").val();
		usrinfo.email=$("#usr_mail").val();
		usrinfo.mobile=$("#usr_mobile").val();
		var body=JSON.stringify(usrinfo);
		TMS_api(url,"POST",body,function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){
					if(url.indexOf("Update")>-1)alerm("用户更新成功！");
					else alerm("用户添加成功！");
					CloseForm('#form_UserInfo','#overlay');
					LoadUserList("filter=");
				}
				else alerm(resp.message);
			}
		});
	}		
}
function ldap_save(){
	var ldapconf={};
	ldapconf.host=$("#ldap_host").val();
	ldapconf.port=$("#ldap_port").val();
	ldapconf.domain=$("#ldap_domain").val();
	ldapconf.admin=$("#ldap_admin").val();
	ldapconf.pwd=$("#ldap_pwd").val();
	if(ldapconf.host==""){
		alerm("请输入LDAP服务器的IP地址");
		$("#ldap_host").focus();
	}
	else if(ldapconf.port==""){
		alerm("请输入LDAP服务器端口");
		$("#ldap_port").focus();
	}
	else if(ldapconf.domain==""){
		alerm("请输入基础域信息");
		$("#ldap_domain").focus();
	}
	else if(ldapconf.admin==""){
		alerm("请输入管理员账号");
		$("#ldap_admin").focus();
	}
	else{
		var BaseDN=[];
		var bd_list=$("#list_ldap_BaseDN").children();
		for(var i=0;i<bd_list.length;i++)BaseDN.push(bd_list.eq(i).text());
		ldapconf.BaseDN=BaseDN;
		var body=JSON.stringify(ldapconf);
		var url="User/AddLdap";
		TMS_api(url,"POST",body,function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200)alerm("LDAP配置保存成功");
				else alerm(resp.message);
			}
		});
	}		
}
// 添加BaseDN
function BaseDN_Save(){
	var BaseDN=$("#BaseDN").val();
	if(BaseDN=="")alerm("请输入BaseDN信息！");
	else{
		$("#list_ldap_BaseDN").append("<option>"+BaseDN+"</option>");
		CloseForm('#form_input_BaseDN','#overlay2');
	}	
}
// 与ldap同步用户信息
function ldap_sync(){
	var url="User/syncLdap";
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				alerm("同步完成，新增用户"+resp.new_member+"名,更新信息"+resp.update_member+"条。");
				LoadUserList("filter=");
			}
			else alerm(resp.message);
		}
	});	
}
// 选择所有配置项
function SelectAll(){
	$("#tab_purviewlist input").attr("checked",true);
}
// 取消所有配置项
function ClearAll(){
	$("#tab_purviewlist input").attr("checked",false);
}
// 初始化权限表
function InitConf(){
	CloseForm('#form_confirm','#overlay');
	var url="User/InitPurview";
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				alerm("权限表初始化完成，请尽快进行特殊用户权限的配置！");
				$("#tab_purviewlist input").attr("checked",false);
				$("#tbody_usrlist tr").css("background-color","#FFFFFF");
			}
			else alerm(resp.message);
		}
	});		
}
// 保存配置
function SaveConf(){
	if(tr_selected!=null){
		var purview=[];
		var confs=$("#tab_purviewlist").find("input");
		for(var i=0;i<confs.length;i++){
			var item={
				key:"",
				value:""
			}
			item.key=confs.eq(i).attr("id");
			item.value="x";
			if(confs.eq(i).attr('checked'))item.value="y";
			purview.push(item);
		}
		var usr=tr_selected.children().eq(1).text();
		var url="User/UpdatePurview?user="+usr;
		var body=JSON.stringify(purview);
		TMS_api(url,"POST",body,function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){
					alerm("修改完成！");
					loadpurview();
				}
				else alerm(resp.message);
			}
		});	
	}			
}
// 查找用户
function user_filt(){
	var fts="filter=";
	var filter=$("#user_filter").val();
	var phase=$("#user_phase option:selected").attr("value");
	if(filter!="")fts="filter="+phase+" like '*"+filter+"*'";
	LoadUserList(fts);
}

// 翻页
function Topage(num){
	if(page_num!=num){
		if(num==0)page_num=page_sum;
		else page_num=num;
		user_filt();
	}
}
function Nextpage(tag){
	if(tag=="+" && page_num!=page_sum) page_num++;
	else if(tag=="-" && page_num!=1) page_num--;		
	user_filt();	
}
$(document).ready(function(){ 
	tr_selected=null;
	var old_bgcolor="";
	var usr_opt="";
	page_sum=0;
	page_num=1
	if(typeof(sessionStorage.customerId)=='undefined'){
		var url="login.html";
		window.open(encodeURI(url),'_parent');
	}
	//页面初始化
	LoadUserList("filter=");

	// 打开ldap配置弹层
	$("#ldap_conf").click(function b(){
		var url="User/GetLdap";
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					$("#ldap_host").val(resp.host);
					$("#ldap_port").val(resp.port);
					$("#ldap_domain").val(resp.domain);
					$("#ldap_admin").val(resp.admin);
					$("#ldap_pwd").val("");
					$("#list_ldap_BaseDN option").remove();
					var baseDN=resp.BaseDN;
					for(var i=0;i<baseDN.length;i++){
						$("#list_ldap_BaseDN").append("<option>"+baseDN[i]+"</option>");
					}
					open_form("#form_ldap_conf","#overlay");
				}
				else alerm(resp.message);
			}
		});	
	});

	// 弹层按钮-打开添加BaseDN的小弹层
	$("#butt_add_ldap_BaseDN").click(function b(){
		$("#BaseDN").val("");
		open_form("#form_input_BaseDN","#overlay2");
		$("#BaseDN").focus();
	});

	// 弹层按钮-删除BaseDN条目
	$("#butt_del_ldap_BaseDN").click(function b(){
		$("#list_ldap_BaseDN option:selected").remove();
	});

	//添加用户
	$("#Usr_add").click(function b(){
		$("#title_UserInfo").text("添加用户");
		$("#usr_pwd").attr("disabled",false);
		$("#usr_account").attr("disabled",false);
		$("#form_UserInfo input").val("");
		open_form("#form_UserInfo","#overlay");
		$("#usr_account").focus();
		usr_opt="Add";
	});
	
	//双击表格 - 打开用户编辑弹层
	$("#tab_usrlist").dblclick(function b(e){
		var tr=$(e.target).parent();
		var username=tr.children().eq(1).text();
		var url="User/Getinfo?user="+username;
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					var ty=tr.children().eq(7).text();
					$("#usr_role").val(resp.role);
					$("#usr_account").val(username);						
					$("#usr_fullname").val(tr.children().eq(2).text());
					$("#usr_dept").val(tr.children().eq(3).text());
					$("#usr_mail").val(tr.children().eq(5).text());
					$("#usr_mobile").val(tr.children().eq(6).text());
					$("#usr_type").val(ty);
					if(ty=="ldap")$("#usr_pwd").attr("disabled","disabled");
					$("#usr_account").attr("disabled","disabled");
					$("#title_UserInfo").text("编辑用户");
					open_form("#form_UserInfo","#overlay");
					usr_opt="Update";
				}
				else alerm(resp.message);
			}
		});				
	});
	//弹层按钮 - 保存用户信息
	$("#UserInfo_save").click(function b(){	
		var url="User/"+usr_opt;
		save_userInfo(url);
	});
	//删除用户
	$("#Usr_del").click(function b(){
		if(tr_selected==null)alerm("请先选择要删除的用户");
		else {
			var username=tr_selected.children().eq(1).text();
			var url="User/Delete?user="+username;
			TMS_api(url,"GET","",function a(){
				if (xmlHttp.readyState==4 && xmlHttp.status==200){
					var resp = JSON.parse(xmlHttp.responseText);
					if(resp.code==200){	
						LoadUserList("filter=");
					}
					else alerm(resp.message);
				}
			});
		}
	});
	
	// 点击用户，切换权限
	$("#tab_usrlist").click(function b(e){
		var tr=$(e.target).parent();
		if(tr_selected!=null)tr_selected.css("background-color",old_bgcolor);
		old_bgcolor=tr.css("background-color");
		tr_selected=tr;
		tr_selected.css("background-color","#E3F1F7");	
		loadpurview();
	});
	// 弹层移动
	$('#form_alert_title').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_alert').offset().left; 
		var abs_y = event.pageY - $('#form_alert').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_alert'); 
				var rel_left=event.pageX - abs_x;
				if(rel_left<0)rel_left=0;
				if(rel_left>1080)rel_left=1080;
				var rel_top=event.pageY - abs_y;
				if(rel_top<0)rel_top=0;
				if(rel_top>740)rel_top=740;
				obj.css({'left':rel_left, 'top':rel_top}); 
			} 
		}).mouseup( function () { 
			isMove = false; 
		}); 
	});
	//弹层移动
	$('#title_LDAPconf').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_ldap_conf').offset().left; 
		var abs_y = event.pageY - $('#form_ldap_conf').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_ldap_conf'); 
				var rel_left=event.pageX - abs_x;
				if(rel_left<0)rel_left=0;
				if(rel_left>1080)rel_left=1080;
				var rel_top=event.pageY - abs_y;
				if(rel_top<0)rel_top=0;
				if(rel_top>740)rel_top=740;
				obj.css({'left':rel_left, 'top':rel_top}); 
			} 
		}).mouseup( function () { 
			isMove = false; 
		}); 
	});	
	$('#title_input_BaseDN').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_input_BaseDN').offset().left; 
		var abs_y = event.pageY - $('#form_input_BaseDN').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_input_BaseDN'); 
				var rel_left=event.pageX - abs_x;
				if(rel_left<0)rel_left=0;
				if(rel_left>1080)rel_left=1080;
				var rel_top=event.pageY - abs_y;
				if(rel_top<0)rel_top=0;
				if(rel_top>740)rel_top=740;
				obj.css({'left':rel_left, 'top':rel_top}); 
			} 
		}).mouseup( function () { 
			isMove = false; 
		}); 
	});
	$('#title_UserInfo').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_UserInfo').offset().left; 
		var abs_y = event.pageY - $('#form_UserInfo').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_UserInfo'); 
				var rel_left=event.pageX - abs_x;
				if(rel_left<0)rel_left=0;
				if(rel_left>1080)rel_left=1080;
				var rel_top=event.pageY - abs_y;
				if(rel_top<0)rel_top=0;
				if(rel_top>740)rel_top=740;
				obj.css({'left':rel_left, 'top':rel_top}); 
			} 
		}).mouseup( function () { 
			isMove = false; 
		}); 
	});
	$('#form_alert_title').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_alert').offset().left; 
		var abs_y = event.pageY - $('#form_alert').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_alert'); 
				var rel_left=event.pageX - abs_x;
				if(rel_left<0)rel_left=0;
				if(rel_left>1080)rel_left=1080;
				var rel_top=event.pageY - abs_y;
				if(rel_top<0)rel_top=0;
				if(rel_top>740)rel_top=740;
				obj.css({'left':rel_left, 'top':rel_top}); 
			} 
		}).mouseup( function () { 
			isMove = false; 
		}); 
	});
});