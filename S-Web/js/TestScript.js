var xmlHttp;
var tcf_selected;
var pm_selected;
var tr_selected;
var file_selected;
var UpdateTag_pm;
var formData;
var page_num;
var page_sum;
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
// 加载脚本列表
function LoadTSList(filter){
	var item_ppnum=18;
	if(filter=="")filter="filter=";
	var url="TS/ListTS?"+filter+"&proj="+pm_selected+"&page_count="+item_ppnum+"&page_num="+page_num;;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				var ts=resp.ts;
				var bgcolor='b';
				$("#tbody_tslist tr").remove();
				for(var i=0;i<ts.length;i++){
					if(bgcolor=="")bgcolor='background-color:#F0ECF3;';
					else bgcolor="";
					var line='<tr style="'+bgcolor+'" id="'+ts[i].tsid+'">';
					line=line+'<td>'+(i+1)+'</td>';
					line=line+'<td>'+ts[i].tsid+'</td>';
					line=line+'<td>'+ts[i].module+'</td>';
					line=line+'<td>'+ts[i].name+'</td>';
					line=line+'<td>'+ts[i].owner+'</td>';
					line=line+'<td>'+ts[i].upload_time+'</td>';
					var lok='闲置';
					if(ts[i].lok!='0')lok='被调用';
					line=line+'<td>'+lok+'</td>';
					line=line+'<td>'+ts[i].tcf+'</td>';
					line=line+'</tr>';
					
					$("#tbody_tslist").append(line);
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
			}
			else alerm(resp.message);
		}
	});
}
// 加载项目树
function load_projs_tr(){
	//清除当前项目树和项目信息
	$("#treelist").remove();
	pm_selected="";
	//获取项目树
	var url="TP/ListModule?proj=all";
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				$("#treebox").append('<ul id="treelist" class="filetree"></ul>');	
				var tao1='';
				var tag=0;
				var mid='';
				var projs=resp.projtree;
				for(var i=0;i<projs.length;i++){
					tao1=tao1+'<li><span class="folder" id="'+projs[i].tag+'">'+projs[i].name+'</span>';
					
					var modules=projs[i].modules;
					var tao2='';
					for(var j=0;j<modules.length;j++){
						tao2=tao2+'<li><span class="file" id="'+modules[j].tag+'">'+modules[j].name+'</span></li>';
					}
					if(tao2!="")tao2='<ul>'+tao2+'</ul>';
					tao1=tao1+tao2+'</li>';
				}
				if(projs.length>0)pm_selected=projs[0].tag;
				$("#treelist").append(tao1);
				$("#treelist").treeview();				
				if(pm_selected!="")LoadTSList("");
			}
			else alert(resp.message);
		}		
	});
}
// 查找脚本
function filt(){
	var fts="";
	var filter=$("#filter_value").val();
	var phase=$("#filter_key option:selected").attr("value");
	if(filter!="")fts="filter="+phase+" like '*"+filter+"*'";
	LoadTSList(fts);
}
// 打开上传脚本弹层
function uploadTS(){
	formData = new FormData(); 
	open_form("#form_uploadTS","#overlay");
	$("#projlist_upl option").remove();
	$("#modulelist_upl option").remove();
	$("#tbody_filelist tr").remove();
	file_selected=null;
	var url="TP/ListProj";
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				var projs=resp.projlist;
				for(var i=0;i<projs.length;i++){
					var line="<option value='"+projs[i].tag+"'>"+projs[i].name+"</option>";
					$("#projlist_upl").append(line);
				}
				if(projs.length>0){
					var pp=projs[0].tag;
					if(pm_selected!=""){
						$("#projlist_upl").attr("value",pm_selected);
						pp=pm_selected;
					}
					url="TP/ListModule?proj="+pp;
					TMS_api(url,"GET","",function a(){
						if (xmlHttp.readyState==4 && xmlHttp.status==200){
							var resp = JSON.parse(xmlHttp.responseText);
							if(resp.code==200){	
								var modules=resp.projtree[0].modules;
								for(var i=0;i<modules.length;i++){
									var line="<option value='"+modules[i].tag+"'>"+modules[i].name+"</option>";
									$("#modulelist_upl").append(line);
								}
								open_form("#form_uploadTS","#overlay");
							}
							else alerm(resp.message);
						}
					});	
				}							
			}
			else alerm(resp.message);
		}
	});	
}
// 打开文件选择器
function chosefile(){
	$("#file_selector").click();
}
// 上传脚本和测试文件
function upload(){
	//post方式  
	var u=0;
	for(var i of formData.values())u++;
	if(u==0)alerm("请先选择要上传的脚本或数据文件！");
	else{
		var body=formData;
		var proj=$("#projlist_upl option:selected").attr("value");
		var module=$("#modulelist_upl option:selected").attr("value");
		var url="TS/UploadTS?proj="+proj+"&module="+module+"&usr="+sessionStorage.usrfullname;
		TMS_api(url,"POST",body,function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					alerm("上传完毕");
					CloseForm("#form_uploadTS","#overlay");
					LoadTSList("");
				}
				else alerm(resp.message);
			}
		});	
	}  
}  
// 选择文件并添加到文件列表
function addFile(){
	var file = document.getElementById('file_selector').files
	var filenum=$("#tbody_filelist tr").length;
	for(i=0;i<file.length;i++){    
		$("#tbody_filelist").append("<tr><td>"+(filenum+i+1)+"</td><td>"+file[i].name+"</td></tr>");
        formData.append("file_"+i+"", file[i]); 
    } 	
}
// 从待上传列表中移除脚本
function Del_file(){
	if(file_selected==null)alerm("请先选择一个文件！");
	else{
		formData.delete("file_"+file_selected.children().eq(0).text());
		file_selected.remove();
		file_selected=null;
		
	}	
}
// 打开项目管理弹层
function proj_manage(){
	$("#projs option").remove();
	var url="TP/ListProj";
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				var projs=resp.projlist;
				for(var i=0;i<projs.length;i++){
					var line="<option value='"+projs[i].tag+"'>"+projs[i].name+"</option>";
					$("#projs").append(line);
				}
				open_form("#form_pm","#overlay");
			}
			else alerm(resp.message);
		}
	});	
}
// 添加新项目
function Add_proj(){
	$(".obj").text("项目");
	$("#butt_confirm").attr("data-value","proj");
	$("#name").val("");
	$("#tag").val("");
	open_form("#form_confirm","#overlay2");
}
// 删除项目
function Del_proj(){
	var proj=$("#projs option:selected").attr("value");
	if(typeof(proj)=='undefined')alerm("请选择要删除的项目!");
	else{
		var url="TP/DelProj?tag="+proj;
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					alerm("删除成功！");
					UpdateTag_pm=1;
					$("#projs option:selected").remove();
				}
				else alerm(resp.message);
			}
		});	
	}
}
// 打开模块管理弹层
function module_manage(){
	$("#projs2 option").remove();
	$("#modules option").remove();
	var url="TP/ListProj";
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				var projs=resp.projlist;
				for(var i=0;i<projs.length;i++){
					var line="<option value='"+projs[i].tag+"'>"+projs[i].name+"</option>";
					$("#projs2").append(line);
				}

				if(projs.length>0){
					var pp=projs[0].tag;
					if(pm_selected!=""){
						$("#projs2").attr("value",pm_selected);
						pp=pm_selected;
					}
					url="TP/ListModule?proj="+pp;
					TMS_api(url,"GET","",function a(){
						if (xmlHttp.readyState==4 && xmlHttp.status==200){
							var resp = JSON.parse(xmlHttp.responseText);
							if(resp.code==200){	
								var modules=resp.projtree[0].modules;
								for(var i=0;i<modules.length;i++){
									var line="<option value='"+modules[i].tag+"'>"+modules[i].name+"</option>";
									$("#modules").append(line);
								}
								open_form("#form_mm","#overlay");
							}
							else alerm(resp.message);
						}
					});	
				}							
			}
			else alerm(resp.message);
		}
	});	
}
// 添加新的模块
function Add_module(){
	$(".obj").text("模块");
	$("#butt_confirm").attr("data-value","module");
	$("#name").val("");
	$("#tag").val("");
	open_form("#form_confirm","#overlay2");
}
// 删除模块
function Del_module(){
	var proj=$("#projs2 option:selected").attr("value");
	var module=$("#modules option:selected").attr("value");
	if(typeof(module)=='undefined')alerm("请选择要删除的模块!");
	else{
		var url="TP/DelModule?tag="+module+"&proj="+proj;
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					alerm("删除成功！");
					UpdateTag_pm=1;
					$("#modules option:selected").remove();
				}
				else alerm(resp.message);
			}
		});	
	}
}
// 关闭项目管理或模块管理弹层，如果有变化，则更新项目树
function SavePM(mod){
	CloseForm('#form_'+mod,'#overlay');
	if(UpdateTag_pm==1){
		UpdateTag_pm=0;
		load_projs_tr();
	}	
}
// 点击确认按钮 - 保存新的项目或模块名
function confirm(){
	var obj=$("#butt_confirm").attr("data-value");
	var name=$("#obj_name").val();
	var tag=$("#tag").val();
	if(name==""){
		var obj_text=$("#obj_name").prev().text();
		obj_text=obj_text.replace("：","");
		alerm(obj_text+"不能为空！");
		$("#obj_name").focus();
	}
	else if(tag==""){
		alerm("英文标签不能为空！");
		$("#tag").focus();
	}
	else{
		var para="AddProj?name="+name+"&tag="+tag;
		if(obj=='module'){
			para="AddModule?name="+name+"&tag="+tag+"&proj="+$("#projs2 option:selected").attr("value");
		}
		var url="TP/"+para;
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					alerm("添加成功！");
					$("#"+obj+"s").append("<option data-value='"+tag+"'>"+name+"</option>");
					UpdateTag_pm=1;
					CloseForm('#form_confirm','#overlay2');
				}
				else alerm(resp.message);
			}
		});			
	}	
}
// 打开编辑TCF测试数据的弹层
function editTCF(){
	if(pm_selected=="")alerm("请先选择一个项目！");
	else{
		open_form("#form_TCF","#overlay");
		$("#proj_TCF").text($("#"+pm_selected).text());
		$("#tbody_tcf tr").remove();
		var url="TP/ReadTEC?proj="+proj_obj.attr("id");
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){
					for(var i=0;i<resp.tec.length;i++){
						var line="<tr id='"+resp.tec[i].key+"'><td class='tcf_key'>";
						line=line+resp.tec[i].key+"</td><td>"+resp.tec[i].value+"</td></tr>";
						$("#tbody_tcf").append(line);
					}
					open_form("#form_TCF","#overlay");
				}
				else alerm(resp.message);
			}
		});				
	}	
}
// 弹层按钮-编辑或新增测试环境参数
function save_TCF_para(){
	if($("#tcf_key").val()=="")alerm("参数名是不能为空！");
	else if($("#tcf_value").val()=="")alerm("参数值是不能为空！");
	else{
		var obj=$("#butt_saveTCF").attr("data-value");
		if(obj==""){
			$("#tbody_tcf").children().css("font-weight","100");
			// 判断是否存在同名参数
			var obj_key=$("#"+$("#tcf_key").val());
			if(typeof(obj_key.attr("id"))!='undefined')alerm("已存在同名参数！");
			else{
				var item='<tr id="'+$("#tcf_key").val()+'" style="font-weight:600;"><td class="tcf_key">'+
				$("#tcf_key").val()+'</td><td>'+$("#tcf_value").val()+'</td></tr>';
				$("#tbody_tcf").append(item);
			}			
		}
		else{
			tcf_selected.children().eq(0).text($("#tcf_key").val());
			tcf_selected.children().eq(1).text($("#tcf_value").val());
		}
		CloseForm("#form_TCF_add","#overlay2");
	}
}
// 打开二次弹层-添加新测试环境参数
function TCF_add(){
	open_form('#form_TCF_add','#overlay2');
	$("#butt_saveTCF").attr("data-value","");
}
// 打开二次弹层-修改测试环境参数
function TCF_edit(){
	if(tcf_selected==null)alerm("请先选择要操作的参数！");
	else{
		$("#tcf_key").val(tcf_selected.children().eq(0).text());
		$("#tcf_value").val(tcf_selected.children().eq(1).text());
		$("#butt_saveTCF").attr("data-value",tcf_selected.attr("id"));
		open_form('#form_TCF_add','#overlay2');		
	}
}
// 删除测试环境参数
function TCF_del(){
	if(tcf_selected==null)alerm("请先选择要操作的参数！");
	else{
		tcf_selected.remove();
		tcf_selected=null;
	}
}
// 保存测试环境参数
function TCF_save(){
	var proj=$("#proj_TCFForm").text();
	var tcf_len=$("#tbody_tcf").children().length;
	var tcf=[];
	for(var i=0;i<tcf_len;i++){
		var tcf_line=$("#tbody_tcf").children().eq(i);
		var kv={};
		kv.key=tcf_line.children().eq(0).text();
		kv.value=tcf_line.children().eq(1).text();
		tcf.push(kv);
	}
	var url="TP/WriteTEC?project="+proj;
	var body=JSON.stringify(tcf);
	TMS_api(url,"POST",body,function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				alerm("保存成功！");
			}
			else alerm(resp.message);
		}
	});	
}
// 执行脚本删除
function Del_TS(){
	var url="TS/DelTS?tsid="+tr_selected.children().eq(1).text();
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				CloseForm("#form_confirm_2","#overlay")
				if(pm_selected!="")LoadTSList("");
			}
			else alerm(resp.message);
		}
	});	
}
// 确认是否删除脚本
function ToDel_TS(){
	if(tr_selected==null)alerm("请先选择要删除的脚本！");
	else{
		var lok=tr_selected.children().eq(6).text();
		if(lok=='被引用')alerm("该脚本被测试集引用，不能删除，请先从该测试集中解除关联！");
		else open_form("#form_confirm_2","#overlay");
	}
}
// 翻页
function Topage(num){
	if(page_num!=num){
		if(num==0)page_num=page_sum;
		else page_num=num;
		filt();
	}
}
function Nextpage(tag){
	if(tag=="+" && page_num!=page_sum) page_num++;
	else if(tag=="-" && page_num!=1) page_num--;		
	filt();	
}
$(document).ready(function(){ 
	tr_selected=null;
	tcf_selected=null;
	file_selected=null;
	pm_selected="";
	UpdateTag_pm=0;
	var old_bgcolor="";
	page_sum=0;
	page_num=1
	if(typeof(sessionStorage.customerId)=='undefined'){
		var url="login.html";
		window.open(encodeURI(url),'_parent');
	}
	//页面初始化
	load_projs_tr();

	//在项目树选择模块
	$("#treebox").click(function b(e){
		var pid=$(e.target).attr("class");
		if(pm_selected!="")$("#"+pm_selected).css("background-color","#FFFFFF");
		if(pid=='folder hover' || pid=='folder'){
			pm_selected=$(e.target).attr("id");
		}
		else if(pid=='file')pm_selected=$(e.target).parent().parent().prev().attr("id");
		else pm_selected="";
		if(pm_selected!=""){
			$("#"+pm_selected).css("background-color","#E3F1F7");
			LoadTSList("");
		}		
	});
	//在模块管理弹层切换项目
	$("#projs2").change(function b(){
		var proj=$("#projs2 option:selected").attr("value");
		$("#modules option").remove();
		var url="TP/ListModule?proj="+proj;
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					var modules=resp.projtree[0].modules;
					for(var i=0;i<modules.length;i++){
						var line="<option value='"+modules[i].tag+"'>"+modules[i].name+"</option>";
						$("#modules").append(line);
					}
				}
				else alerm(resp.message);
			}
		});	
	});
	//在上传脚本弹层切换项目
	$("#projlist_upl").change(function b(){
		var proj=$("#projlist_upl option:selected").attr("value");
		$("#modulelist_upl option").remove();
		url="TP/ListModule?proj="+proj;
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					var modules=resp.projtree[0].modules;
					for(var i=0;i<modules.length;i++){
						var line="<option value='"+modules[i].tag+"'>"+modules[i].name+"</option>";
						$("#modulelist_upl").append(line);
					}
				}
				else alerm(resp.message);
			}
		});	
	});
	//选择脚本
	$("#tbody_tslist").click(function b(e){
		var tr=$(e.target).parent();
		if(tr_selected!=null)tr_selected.css("background-color",old_bgcolor);
		tr_selected=tr;
		old_bgcolor=tr.css("background-color");
		tr_selected.css("background-color","#E3F1F7");	
	});
	//选择测试参数
	$("#tbody_tcf").click(function b(e){
		var tr=$(e.target).parent();
		if(tcf_selected!=null)tcf_selected.css("font-weight","100");
		tcf_selected=tr;
		tcf_selected.css("font-weight","600");	
	});
	//在待上传文件框中选择脚本文件
	$("#tbody_filelist").click(function b(e){
		var tr=$(e.target).parent();
		if(file_selected!=null)file_selected.css("background-color","#FFFFFF");
		file_selected=tr;
		file_selected.css("background-color","#E3F1F7");	
	});
	//弹层移动
	$('#title_form_pm').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_pm').offset().left; 
		var abs_y = event.pageY - $('#form_pm').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_pm'); 
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
	$('#title_form_mm').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_mm').offset().left; 
		var abs_y = event.pageY - $('#form_mm').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_mm'); 
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
	$('#title_form_confirm').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_confirm').offset().left; 
		var abs_y = event.pageY - $('#form_confirm').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_confirm'); 
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
	$('#title_form_uploadTS').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_uploadTS').offset().left; 
		var abs_y = event.pageY - $('#form_uploadTS').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_uploadTS'); 
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
	$('#title_form_TCF').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_TCF').offset().left; 
		var abs_y = event.pageY - $('#form_TCF').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_TCF'); 
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
	$('#title_form_TCF_add').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_TCF_add').offset().left; 
		var abs_y = event.pageY - $('#form_TCF_add').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_TCF_add'); 
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
	$('#form_confirm_2_title').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_confirm_2').offset().left; 
		var abs_y = event.pageY - $('#form_confirm_2').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_confirm_2'); 
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