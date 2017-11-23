var xmlHttp;
var tcf_selected;
var tr_selected;
var page_num;
var page_sum;
var Task_runTag;
function TMS_api(url,med,dats,cfunc){
	var hostpath=getHostUrl();
	try{
		url=encodeURI(hostpath+url);
		xmlHttp = new XMLHttpRequest();
		xmlHttp.onreadystatechange=cfunc;		
		xmlHttp.open(med,url,true);
		if(med=="GET")xmlHttp.send();
		else xmlHttp.send(dats);	
	}catch(e){
		alerm(e);
	}	
}
// 加载测试任务
function Loadtask(filter){
	var item_ppnum=13;
	if(filter=="")filter="filter=";
	var url="TT/ListTask?"+filter+"&page_count="+item_ppnum+"&page_num="+page_num;;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				var tt=resp.tasks;
				var bgcolor='b';
				var state_1st="";
				$("#tbody_TaskList tr").remove();
				for(var i=0;i<tt.length;i++){
					if(bgcolor=="")bgcolor='background-color:#F0ECF3;';
					else bgcolor="";
					var line='<tr style="'+bgcolor+'" id="'+tt[i].taskid+'">';
					if(i==0)state_1st=tt[i].status;
					line=line+'<td>'+tt[i].taskid+'</td>';
					line=line+'<td>'+tt[i].tset+'</td>';
					line=line+'<td data-value="'+tt[i].projtag+'">'+tt[i].proj+'</td>';
					line=line+'<td>'+tt[i].creattime+'</td>';
					line=line+'<td>'+tt[i].creater+'</td>';
					line=line+'<td>'+tt[i].status+'</td>';
					line=line+'<td>'+tt[i].starttime+'</td>';
					line=line+'<td>'+tt[i].process+'</td>';
					line=line+'</tr>';
					
					$("#tbody_TaskList").append(line);
				}
				// 变更页码
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
				// 判断首任务状态，修改按钮颜色与状态
				if(state_1st==""){
					// '启动'\'暂停'按钮被禁止
					$("#butt_start_task").attr("disabled",true);
					$("#butt_start_task").css("background-color","#C9C9C9");
					$("#butt_stop_task").attr("disabled",true);
					$("#butt_stop_task").css("background-color","#C9C9C9");
					if(Task_runTag=="1"){
						alerm("任务已经全部执行完毕，请前往测试报告模块查看！");
						Task_runTag="";
					}
				}
				else if(state_1st=="执行中" || state_1st=="停止中..."){
					// '启动'按钮被禁止
					$("#butt_start_task").attr("disabled",true);
					$("#butt_start_task").css("background-color","#C9C9C9");
					// '暂停'按钮使能
					$("#butt_stop_task").attr("disabled",false);
					$("#butt_stop_task").css("background-color","#C95D26");
				}
				else{
					// '启动'按钮使能
					$("#butt_start_task").attr("disabled",false);
					$("#butt_start_task").css("background-color","#2A83AC");
					// '暂停'按钮被禁止
					$("#butt_stop_task").attr("disabled",true);
					$("#butt_stop_task").css("background-color","#C9C9C9");
				}
				$("#testpolicy").text("");
				$("#Des_testset").val("");
			}
			else alerm(resp.message);
		}
	});
}
// 初始化页面
function init(){
	Loadtask("");
}
// 打开弹层-添加新任务
function OPform_newtask(){	
	var url="TP/ListProj";
	$("#projs_newTaskForm option").remove();
	$("#tsets_newTaskForm option").remove();
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				var projs=resp.projlist;
				for(var i=0;i<projs.length;i++){
					$("#projs_newTaskForm").append("<option value='"+projs[i].tag+"'>"+projs[i].name+"</option>");
				}
				if(projs.length>0){
					url="TT/ListTestSet?proj="+projs[0].tag;
					TMS_api(url,"GET","",function a(){
						if (xmlHttp.readyState==4 && xmlHttp.status==200){
							var resp = JSON.parse(xmlHttp.responseText);
							if(resp.code==200){
								for(var i=0;i<resp.testset.length;i++){
									$("#tsets_newTaskForm").append("<option>"+resp.testset.name+"</option>");
								}
								open_form("#form_task","#overlay");
							}
							else alerm(resp.message);
						}
					});		
				}
				else alerm("系统中还没有项目，请先创建项目和测试集！");
			}
			else alerm(resp.message);
		}
	});		
}
// 添加新任务
function add_task(){
	var projtag=$("#projs_newTaskForm option:selected").attr("value");
	if($("#tsets_newTaskForm option:selected").length==0)alerm("请先选择要添加任务的测试集！");
	else{
		var tset=$("#tsets_newTaskForm option:selected").val();
		var url="TT/AddTestTask?proj="+projtag+"&tset="+tset+"&usr="+sessionStorage.usrfullname;
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){				
					CloseForm('#form_task','#overlay');
					alerm("新任务添加成功！");
					Loadtask("");
				}
				else alerm(resp.message);
			}
		});
	}			
}
// 查找任务
function filt(){
	var fts="";
	var filter=$("#filter_value").val();
	var phase=$("#filter_key option:selected").attr("value");
	if(filter!="")fts="filter="+phase+" like '*"+filter+"*'";
	Loadtask(fts);
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
// 执行删除测试任务的操作
function toDelTT(){
	CloseForm('#form_confirm_2','#overlay');
	var task=tr_selected.children().eq(0).text();
	var url="TT/DelTask?task="+task;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				alerm("删除完毕！");
				filt();
			}
			else alerm(resp.message);
		}
	});		
}
// 删除任务
function Del_task(){
	if(tr_selected==null)alerm("请先选择要操作的任务！");
	else{
		var task_status=tr_selected.children().eq(5).text();
		if(task_status=="执行中" || task_status=="停止中...")alerm("状态为'"+task_status+"'的任务不能删除，请先停止执行。");
		else{			
			$("#warn_mess").text("删除测试任务为不可逆操作，确认是否删除?");
			open_form("#form_confirm_2","#overlay");
		}
	}
}
// 打开弹层-编辑测试环境配置文件
function Edit_tcf(){
	if(tr_selected==null)alerm("请先选择要操作的任务！");
	else{
		var task_status=tr_selected.children().eq(5).text();
		if(task_status=="执行中" || task_status=="停止中...")alerm("状态为'"+task_status+"'的任务不能修改环境配置参数，请先停止执行。");
		else{
			var ProjTag=tr_selected.children().eq(2).attr("data-value");
			var proj=tr_selected.children().eq(2).text();
			var url="TP/ReadTEC?proj="+ProjTag;
			TMS_api(url,"GET","",function a(){
				if (xmlHttp.readyState==4 && xmlHttp.status==200){
					var resp = JSON.parse(xmlHttp.responseText);
					if(resp.code==200){
						$("#tbody_tcf tr").remove();
						$("#proj_TCFForm").text(proj);
						for(var i=0;i<resp.tec.length;i++){
							var line="<tr id='"+resp.tec[i].key+"'><td class='tcf_key'>"
							+resp.tec[i].key+"</td><td>"+resp.tec[i].value+"</td></tr>";
							$("#tbody_tcf").append(line);
						}
						open_form("#form_TCF","#overlay");
					}
					else alerm(resp.message);
				}
			});					
		}
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
	var proj=tr_selected.children().eq(2).attr("data-value");
	var tcf_len=$("#tbody_tcf").children().length;
	var tcf=[];
	for(var i=0;i<tcf_len;i++){
		var tcf_line=$("#tbody_tcf").children().eq(i);
		var kv={};
		kv.key=tcf_line.children().eq(0).text();
		kv.value=tcf_line.children().eq(1).text();
		tcf.push(kv);
	}
	var url="TP/WriteTEC?proj="+proj;
	var body=JSON.stringify(tcf);
	TMS_api(url,"POST",body,function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){				
				CloseForm('#form_TCF','#overlay');
				alerm("保存成功！");
			}
			else alerm(resp.message);
		}
	});	
}
// 启动任务
function st_task(taskno){
	var url="TT/RunTask?taskno="+taskno;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				CloseForm('#form_confirm','#overlay');
				alerm("任务已继续执行!");
				Task_runTag="1";
				Loadtask("");
			}
			else alerm(resp.message);
		}
	});		
}
// 启动任务前判断
function Start_task(){
	var task_status=$("#tbody_TaskList").children().eq(0).children().eq(5).text();
	if(task_status=="未开始")st_task(0);
	else open_form("#form_confirm","#overlay");
}
// 停止任务
function Stop_task(){
	var url="TT/StopTask";
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				alerm("已向后台发送任务停止请求，请稍等!");
				Loadtask("");
			}
			else alerm(resp.message);
		}
	});
}

/***************主函数******************/
$(document).ready(function(){ 
	tr_selected=null;
	tcf_selected=null;
	var old_bgcolor="";
	Task_runTag="";
	page_sum=0;
	page_num=1
	if(typeof(sessionStorage.customerId)=='undefined'){
		var url="login.html";
		window.open(encodeURI(url),'_parent');
	}
	// 页面初始化
	init();
	
	//选择或取消任务选择
	$("#tab_TaskList").click(function b(e){
		var tr=$(e.target).parent();
		var v_class=tr.parent().attr('id');

		if(tr_selected!=null)tr_selected.css("background-color",old_bgcolor);
		tr_selected=tr;
		old_bgcolor=tr.css("background-color");
		tr_selected.css("background-color","#E3F1F7");	
		// 获取测试任务相关测试集的信息
		var tset_name=tr_selected.children().eq(1).text();
		var url="TT/GetTestSet?tset="+tset_name;
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){
					$("#Des_testset").text(resp.note);
				}
				else alerm(resp.message);
			}
		});
	});
	//选择或取消测试参数选择
	$("#tbody_tcf").click(function b(e){
		var tr=$(e.target).parent();
		var v_class=tr.parent().attr('id');
		if(tcf_selected!=null)tcf_selected.children().css("font-weight","100");
		tcf_selected=tr;
		tcf_selected.children().css("font-weight","600");	
	});
	//添加新任务弹层-切换项目，更新测试集
	$("#projs_newTaskForm").change(function b(e){
		var proj=$("#projs_newTaskForm option:selected").attr("value");
		var url="TT/ListTestSet?proj="+proj;
		TMS_api(url,"GET","",function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					var tset=resp.testset;
					$("#tsets_newTaskForm tr").remove();
					for(var i=0;i<tset.length;i++){
						var line='<option>'+tset[i].name+'</option>';
						$("#tsets_newTaskForm").append(line);
					}
				}
				else alerm(resp.message);
			}
		});
	});
	//弹层移动
	$('#title_form_task').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_task').offset().left; 
		var abs_y = event.pageY - $('#form_task').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_task'); 
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
	$('#form_confirm_title').mousedown(function (event) { 
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