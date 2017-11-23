var xmlHttp;
var tr_selected;
var tcf_selected;
var tsc_selected;
var tst_selected;
var page_num;
var page_sum;
var tset_opt;
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
function LoadTSList(proj){
	var url="TS/ListTS?filter=&proj="+proj+"&page_count=&page_num=";
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				var ts=resp.ts;
				$("#tbody_Tst tr").remove();
				for(var i=0;i<ts.length;i++){
					var line='<tr id="'+ts[i].tsid+'">';
					line=line+'<td width="100px">'+proj+"\\"+ts[i].module+'</td>';
					line=line+'<td width="150px">'+ts[i].name+'</td>';
					line=line+'<td>'+ts[i].owner+'</td>';
					line=line+'</tr>';
					
					$("#tbody_Tst").append(line);
				}
			}
			else alerm(resp.message);
		}
	});
}
// 加载测试集列表
function LoadTsetList(filter){
	var item_ppnum=15;
	if(filter=="")filter="filter=";
	var proj=$("#projlist").attr("value");
	if(proj=="")proj="all";
	var url="TT/ListTestSet?"+filter+"&proj="+proj+"&page_count="+item_ppnum+"&page_num="+page_num;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				var tset=resp.testset;
				var bgcolor='background-color:#F0ECF3;';
				$("#tbody_TsetList tr").remove();
				for(var i=0;i<tset.length;i++){
					if(bgcolor=="")bgcolor='background-color:#F0ECF3;';
					else bgcolor="";
					var line='<tr style="'+bgcolor+'" id="'+tset[i].name+'">';
					line=line+'<td>'+tset[i].name+'</td>';
					line=line+'<td>'+tset[i].creater+'</td>';
					line=line+'<td>'+tset[i].creattime+'</td>';
					line=line+'</tr>';
					
					$("#tbody_TsetList").append(line);
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
				LoadTSList(proj);
			}
			else alerm(resp.message);
		}
	});
}
// 初始化页面
function init(){
	var url="TP/ListProj";
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				$("#projlist option").remove();
				for(var i=0;i<resp.projlist.length;i++){
					var line="<option value='"+resp.projlist[i].tag+"''>"+resp.projlist[i].name+"</option>";
					$("#projlist").append(line);
				}
				if(resp.projlist.length>0)LoadTsetList("");
			}
			else alerm(resp.message);
		}
	});
}
// 加载测试集
function loadTset(tset){
	var url="TT/GetTestSet?tset="+tset;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				tset_opt="modify";
				$("#Tset_name").text(tset);
				$("#des_tset").val(resp.note);
				$("#tbody_tsc_list tr").remove();
				for(var i=0;i<resp.ts.length;i++){
					var line="<tr>";
					line=line+"<td width='30px'><input type='checkbox' class='ts_check'></td>";
					line=line+"<td data-value='"+resp.ts[i].path+"'>"+resp.ts[i].name+"</td>";
					var policy=resp.ts[i].policy;
					var policy_des=$("#policy_continue").next().text();

					var policy_data=parseInt(policy);
					if(policy_data==0)policy_des=$("#policy_end").next().text();
					else if(policy_data==1)policy_des=$("#policy_continue").next().text();
					else if(policy_data>9)policy_des=$("#policy_retrynum").prev().text()+(policy_data/10)+$("#policy_retrynum").next().text();
					else policy_des=$("#policy_retrynum_1").prev().text()+policy_data+$("#policy_retrynum_1").next().text();

					line=line+"<td data-value='"+resp.ts[i].policy+"'>"+policy_des+"</td>";
					line=line+"</tr>";
					$("#tbody_tsc_list").append(line);
				}
				if(resp.ts.length>0){
					$("#policy_retrynum").val("");
					$("#policy_continue").attr("checked",true);
					$("#policy_retry").attr("checked",false);
					$("#policy_end").attr("checked",false);
				}
				else{
					$("#policy_retrynum").val("");
					$("#policy_continue").attr("checked",true);
					$("#policy_retry").attr("checked",false);
					$("#policy_end").attr("checked",false);
				}
			}
			else alerm(resp.message);
		}
	});
}
// 创建测试集
function Add_Tset(){
	var proj=$("#projlist").attr("value");
	if(proj=="")alerm("项目列表为空，请先创建项目!");
	else{
		var tset=$("#Tset_name_new").val();
		if(tset=="")alerm("测试集名称不能为空！");
		else{
			var tobj=$("#"+tset).attr("id");
			if(typeof(tobj)!="undefined")alerm(proj+"项目下的测试集"+tset+"已存在！");
			else{
				$("#Tset_name").text(tset);
				$("#tbody_tsc_list tr").remove();
				$("#policy_retrynum").val("");
				$("#des_tset").val("");
				$("#policy_continue").attr("checked",true);
				$("#policy_retry").attr("checked",false);
				$("#policy_end").attr("checked",false);	
				tset_opt="addnew";
				alerm("新测试集已创建，可以向测试集里添加脚本了！编辑后请注意保存，否则将不会生效！");
				CloseForm('#form_newTset','#overlay');		
			}
		}
	}	
}
// 执行删除测试集的操作
function toDelTset(){
	var tset=tr_selected.children().eq(0).text();
	var url="TT/DelTestSet?tset="+tset;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				CloseForm("#form_confirm_2","#overlay");
				$("#Tset_name").text("");
				$("#tbody_tsc_list tr").remove();
				filt();
			}
			else alerm(resp.message);
		}
	});
}
// 删除测试集
function Del_Tset(){
	if(tr_selected==null)alerm("请先选择要删除的测试集！");
	else open_form("#form_confirm_2","#overlay");
}
// 清除测试集中的脚本队列
function Tset_clear(){
	$("#tbody_tsc_list tr").remove();
}
// 保存测试集
function Tset_save(){
	if($("#Tset_name").text()=="")alerm("请先创建测试集!");
	else if($("#tbody_tsc_list").children().length==0)alerm("还没有为测试集添加测试脚本，请先选择脚本添加！");
	else if($("#des_tset").val()=='')alerm("请补充测试集说明，以便能够识别测试集的用途和创建目的！");
	else{
		var tslist=[];
		var len=$("#tbody_tsc_list").children().length;
		for(var i=0;i<len;i++){
			var ts_line=$("#tbody_tsc_list").children().eq(i);
			var ts={};
			ts.tsid=ts_line.attr("id");
			ts.name=ts_line.children().eq(1).text();
			ts.path=ts_line.children().eq(1).attr("data-value");
			ts.policy=ts_line.children().eq(2).attr("data-value");
			tslist.push(ts);
		}
		var tset={};
		tset.project=$("#projlist").attr("value");
		tset.note=$("#des_tset").val();
		tset.tslist=tslist;
		var body=JSON.stringify(tset);
		var url="TT/AddTestSet?type="+tset_opt+"&tset="+$("#Tset_name").text()+"&usr="+sessionStorage.usrfullname;
		TMS_api(url,"POST",body,function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					alerm("创建成功！");
					LoadTsetList("");
				}
				else alerm(resp.message);
			}
		});
	}
}
// 另存测试集
function Tset_saveas(){
	var proj=$("#projlist").attr("value");
	if(proj=="")alerm("项目列表为空，请先创建项目!");
	else{
		var tset=$("#Tset_name_new_2").val();
		if(tset=="")alerm("测试集名称不能为空！");
		else{
			var tobj=$("#"+tset).attr("id");
			if(typeof(tobj)!="undefined")alerm(proj+"项目下的测试集"+tset+"已存在！");
			else{
				$("#Tset_name").text(tset);
				tset_opt="addnew";
				CloseForm('#form_newTset_2','#overlay');
				Tset_save();
			}
		}
	}
}
// 将测试集添加到测试队列
function addtask(){	
	if($("#Tset_name").text()=="")alerm("请先选择或创建测试集!");
	else{
		var proj=$("#projlist").attr("value");
		var url="TT/AddTestTask?project="+proj+"&name="+$("#Tset_name").text();
		TMS_api(url,"POST",body,function a(){
			if (xmlHttp.readyState==4 && xmlHttp.status==200){
				var resp = JSON.parse(xmlHttp.responseText);
				if(resp.code==200){	
					alerm("添加任务成功！");
				}
				else alerm(resp.message);
			}
		});
	}
}
// 将测试集添加到测试队列前的确认，是否需要编辑环境参数
function save_addtask(ifTCF){
	CloseForm("#form_confirm","#overlay");
	if(ifTCF=='y'){
		var proj=$("#projlist").attr("value");
		var url="TP/ReadTEC";
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
	else addtask();
}
// 查找测试集
function filt(){
	var fts="";
	var filter=$("#filter_value").val();
	var phase=$("#filter_key option:selected").attr("value");
	if(filter!="")fts="filter="+phase+" like '*"+filter+"*'";
	LoadTsetList(fts);
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
// 保存编辑后的测试环境参数
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
				addtask();
			}
			else alerm(resp.message);
		}
	});	
}

/*****************主函数*********************************/
$(document).ready(function(){ 
	tr_selected=null;
	tcf_selected=null;
	tsc_selected=null;
	tst_selected=null;
	var old_bgcolor="";
	var old_bgcolor_tsc="";
	var old_bgcolor_tst="";
	page_sum=0;
	page_num=1;
	// if(typeof(sessionStorage.customerId)=='undefined'){
	// 	var url="login.html";
	// 	window.open(encodeURI(url),'_parent');
	// }
	//页面初始化
	init();

	//选择或取消测试集选择
	$("#tab_TsetList").click(function b(e){
		var tr=$(e.target).parent();
		if(tr_selected!=null)tr_selected.css("background-color",old_bgcolor);
		tr_selected=tr;
		old_bgcolor=tr.css("background-color");
		tr_selected.css("background-color","#E3F1F7");	
		var tset=tr_selected.children().eq(0).text();
		loadTset(tset);
	});
	//选择或取消测试集内的脚本选择
	$("#tab_tsc_list").click(function b(e){
		if($(e.target).attr("class")=='ts_check'){
			var tr=$(e.target).parent().parent();
			// if($(e.target).attr("checked")=="checked")tr=$(e.target).parent().parent();
			if(tsc_selected!=null)tsc_selected.css("background-color",old_bgcolor_tsc);
			tsc_selected=tr;
			old_bgcolor_tsc=tr.css("background-color");
			tsc_selected.css("background-color","#E3F1F7");	
			// 根据脚本的值更新策略
			var policy_id=tr.children().eq(2).attr("data-value");
			$(".policy").attr("checked",false);
			$("#policy_retrynum_1").val("");
			$("#policy_retrynum").val("");
			if($(e.target).attr("checked")=='checked'){
				var policy_data=parseInt(policy_id);
				if(policy_data==1)$("#policy_continue").attr("checked",true);
				else if(policy_data==0)$("#policy_end").attr("checked",true);
				else if(policy_data>9){
					$("#policy_retry_1").attr("checked",true);
					$("#policy_retrynum_1").val(policy_data/10);
				}
				else {
					$("#policy_retry").attr("checked",true);
					$("#policy_retrynum").val(policy_data);
				}
			}			
		}		
	});
	// 从测试集删除脚本
	$("#del_tsc").click(function b(e){
		$(".ts_check:checked").parent().parent().remove();
	});	
	//选择或取消测试脚本选择
	$("#tbody_Tst").click(function b(e){
		var tr=$(e.target).parent();
		if(tst_selected!=null)tst_selected.css("background-color",old_bgcolor_tst);
		tst_selected=tr;
		old_bgcolor_tst=tr.css("background-color");
		tst_selected.css("background-color","#E3F1F7");	
	});
	// 向测试集添加脚本
	$("#add_tsc").click(function b(e){
		if(tst_selected==null)alerm("请先选择要添加的脚本！");
		else{
			var policy_data=1;
			var policy=$(".policy:checked").attr("id");
			var policy_des=$(".policy:checked").next().text();

			var retrynum=$("#policy_retrynum").val();
			if(policy=='policy_retry_1')retrynum=$("#policy_retrynum_1").val();
			
			
			if(policy=='policy_retry'){
				if(retrynum==''){
					policy_data=-1;
					alerm("重复次数不能为空！");
				}
				else if(isNaN(retrynum)){
					policy_data=-1;
					alerm("重复次数必须为1-9的整数！");
				}
				else if(parseInt(retrynum)<1 || parseInt(retrynum)>9){
					policy_data=-1;
					alerm("重复次数必须为1-9的整数！");
				}
				else{
					policy_des=$("#policy_retrynum").prev().text()+retrynum+$("#policy_retrynum").next().text();
					policy_data=parseInt(retrynum);
				}
			}
			else if(policy=='policy_retry_1'){
				if(retrynum==''){
					policy_data=-1;
					alerm("重复次数不能为空！");
				}
				else if(isNaN(retrynum)){
					policy_data=-1;
					alerm("重复次数必须为1-9的整数！");
				}
				else if(parseInt(retrynum)<1 || parseInt(retrynum)>9){
					policy_data=-1;
					alerm("重复次数必须为1-9的整数！");
				}
				else{
					policy_des=$("#policy_retrynum_1").prev().text()+retrynum+$("#policy_retrynum_1").next().text();
					policy_data=parseInt(retrynum)*10;
				}
			}
			else if(policy=='policy_end')policy_data=0;

			if(policy_data>=0){
				var line="<tr id='"+tst_selected.attr("id")+"'>";
				line=line+"<td><input type='checkbox' class='ts_check'></td>";
				line=line+"<td data-value='"+tst_selected.children().eq(0).text()+"'>"+tst_selected.children().eq(1).text()+"</td>";

				line=line+"<td data-value='"+policy_data+"'>"+policy_des+"</td>";
				line=line+"</tr>";
				$("#tbody_tsc_list").append(line);
			}			
		}
	});	
	// 测试策略变更
	$(".policy").click(function b(e){
		$(".policy").attr("checked",false);
		if($(e.target).attr("checked")!="checked"){
			$(e.target).attr("checked",true);
			if($(e.target).attr("id")=="policy_retry")$("#policy_retrynum_1").val("");
			if($(e.target).attr("id")=="policy_retry_1")$("#policy_retrynum").val("");
		}
	});
	// 更新测试集的测试策略
	$("#update_policy").click(function b(e){
		var pocy=$(".policy:checked");
		var policy_des=$("#policy_continue").next().text();
		var policy_data=1;
		if(pocy.attr("id")=='policy_retry'){
			var retrynum=$("#policy_retrynum").val();
			if(retrynum==''){
				policy_data=-1;
				alerm("重复次数不能为空！");
			}
			else if(isNaN(retrynum)){
				policy_data=-1;
				alerm("重复次数必须为1-9的整数！");
			}
			else if(parseInt(retrynum)<1 || parseInt(retrynum)>9){
				policy_data=-1;
				alerm("重复次数必须为1-9的整数！");
			}
			else{
				policy_des=$("#policy_retrynum").prev().text()+retrynum+$("#policy_retrynum").next().text();
				policy_data=parseInt(retrynum);
			}			
		}
		else if(pocy.attr("id")=='policy_retry_1'){
			var retrynum=$("#policy_retrynum_1").val();
			if(retrynum==''){
				policy_data=-1;
				alerm("重复次数不能为空！");
			}
			else if(isNaN(retrynum)){
				policy_data=-1;
				alerm("重复次数必须为1-9的整数！");
			}
			else if(parseInt(retrynum)<1 || parseInt(retrynum)>9){
				policy_data=-1;
				alerm("重复次数必须为1-9的整数！");
			}
			else{
				policy_des=$("#policy_retrynum_1").prev().text()+retrynum+$("#policy_retrynum_1").next().text();
				policy_data=parseInt(retrynum)*10;
			}
		}
		else if(pocy.attr("id")=='policy_end'){
			policy_des=$("#policy_end").next().text();
			policy_data=0;
		}
		if(policy_data>=0){
			var ts_checked=$(".ts_check:checked");
			for(var i=0;i<ts_checked.length;i++){
				var ts_pocy=ts_checked.eq(i).parent().parent().children().eq(2);
				ts_pocy.text(policy_des);
				ts_pocy.attr("data-value",policy_data);
			}
		}		
	});
	//选择或取消测试参数选择
	$("#tbody_tcf").click(function b(e){
		var tr=$(e.target).parent();
		if(tcf_selected!=null)tcf_selected.css("font-weight","100");
		tcf_selected=tr;
		tcf_selected.css("font-weight","600");	
	});
	// 切换项目，变更脚本列表和右侧内容区
	$("#projlist").change(function b(e){
		tr_selected=null;
		tsc_selected=null;
		tst_selected=null;
		$("#Tset_name").text("");
		$("#tbody_tsc_list tr").remove();
		$("#tbody_Tst tr").remove();
		$("#policy_continue").attr("checked",true);
		$("#policy_retry").attr("checked",false);
		$("#policy_end").attr("checked",false);
		$("#policy_retrynum").val("");
		$("#des_tset").val("");

		// 更新测试集列表和脚本库
		filt();	
	});
	//弹层移动
	$('#title_form_newTset').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_newTset').offset().left; 
		var abs_y = event.pageY - $('#form_newTset').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_newTset'); 
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
	$('#title_form_newTset_2').mousedown(function (event) { 
		var isMove = true; 
		var abs_x = event.pageX - $('#form_newTset_2').offset().left; 
		var abs_y = event.pageY - $('#form_newTset_2').offset().top; 
		$(document).mousemove(function (event) { 
			if (isMove) { 
				var obj = $('#form_newTset_2'); 
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