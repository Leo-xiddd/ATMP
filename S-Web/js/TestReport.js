var xmlHttp;
var tr_selected;
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
// 获取测试报告
function LoadTestReport(trname){
	var url="TR/GetTR?name="+trname;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				$("#proj").text(resp.project);
				$("#testset").text(resp.testset);
				$("#Ttask_name").text(resp.taskid);
				$("#owner").text(resp.owner);
				$("#result").text(resp.result);
				$("#starttime").text(resp.starttime);
				$("#endtime").text(resp.creattime);
				$("#timeresume").text(resp.timeresume);
				$("#num_ts").text(resp.num_ts);
				$("#num_ts_fail").text(resp.num_ts_fail);
				$("#num_ts_error").text(resp.num_ts_error);
				$("#rate_ts_pass").text(resp.rate_ts_pass);

				var step=resp.ts;
				$("#tbody_testrecord tr").remove();
				for(var i=0;i<step.length;i++){
					var bgcolor="";
					if(step[i].testresult=='失败')bgcolor=" style='background-color:#F9C8EB;'";
					else if(step[i].testresult=='异常')bgcolor=" style='background-color:#CC6CE0;'";
					var line="<tr"+bgcolor+">";
					line=line+"<td>"+step[i].id+"</td>";
					line=line+"<td style='text-align:left'>"+step[i].tsname+"</td>";
					line=line+"<td style='text-align:left'>"+step[i].testname+"</td>";
					line=line+"<td style='text-align:left'>"+step[i].issue+"</td>";
					line=line+"<td>"+step[i].testresult+"</td>";
					line=line+"<td>"+step[i].starttime+"</td>";
					line=line+"</tr>";
					$("#tbody_testrecord").append(line);
				}
			}
			else alerm(resp.message);
		}
	});
}
// 获取测试报告列表
function LoadTRs(filter){
	var item_ppnum=15;
	if(filter=="")filter="filter=";
	var url="TR/ListTR?"+filter+"&page_count="+item_ppnum+"&page_num="+page_num;;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){	
				var TRs=resp.TR_list;
				var bgcolor='b';
				$("#tbody_TRList tr").remove();
				for(var i=0;i<TRs.length;i++){
					if(bgcolor=="")bgcolor='background-color:#F0ECF3;';
					else bgcolor="";
					var line='<tr style="'+bgcolor+'" id="'+TRs[i].trname+'">';
					line=line+'<td>'+TRs[i].creattime+'</td>';
					line=line+'<td>'+TRs[i].trname+'</td>';
					line=line+'<td>'+TRs[i].owner+'</td>';
					line=line+'<td>'+TRs[i].result+'</td>';
					line=line+'</tr>';
					
					$("#tbody_TRList").append(line);
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
				// 刷新右侧报告内容
				if(TRs.length>0){
					tr_selected=$("#tbody_TRList").children().eq(0);
					tr_selected.css("background-color","#E3F1F7");
					LoadTestReport(TRs[0].trname);
				}
			}
			else alerm(resp.message);
		}
	});
}
// 查找用户
function filt(){
	var fts="";
	var filter=$("#filter_value").val();
	var phase=$("#filter_key option:selected").attr("value");
	if(filter!="")fts="filter="+phase+" like '*"+filter+"*'";
	LoadTRs(fts);
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
// 执行删除测试报告
function To_Del_TR(){
	var tr=tr_selected.children().eq(1).text();
	var url="TR/DelTR?name="+tr;
	TMS_api(url,"GET","",function a(){
		if (xmlHttp.readyState==4 && xmlHttp.status==200){
			var resp = JSON.parse(xmlHttp.responseText);
			if(resp.code==200){
				alerm("删除完毕！");
				CloseForm('#form_confirm','#overlay');
				filt();
			}
			else alerm(resp.message);
		}
	});		
}
// 删除测试报告前确认
function Del_TR(){
	if(tr_selected==null)alerm("请先选择要删除的测试报告！");
	else{
		open_form("#form_confirm","#overlay");
	}
}
/*********************主函数*********************************/
$(document).ready(function(){ 
	tr_selected=null;
	var testrcd_selected=null;
	var old_bgcolor="";
	var old_bgcolor_trcd="";
	page_sum=0;
	page_num=1
	if(typeof(sessionStorage.customerId)=='undefined'){
		var url="login.html";
		window.open(encodeURI(url),'_parent');
	}
	//页面初始化
	LoadTRs("");

	//选择或取消测试报告
	$("#tbody_TRList").click(function b(e){
		var tr=$(e.target).parent();
		if(tr_selected!=null)tr_selected.css("background-color",old_bgcolor);
		tr_selected=tr;
		old_bgcolor=tr.css("background-color");
		tr_selected.css("background-color","#E3F1F7");	
		var trname=tr.children().eq(1).text();
		LoadTestReport(trname);
	});
	//选择或取消测试报告中的记录
	$("#tbody_testrecord").click(function b(e){
		var tr=$(e.target).parent();
		if(testrcd_selected!=null)testrcd_selected.css("background-color",old_bgcolor_trcd);
		testrcd_selected=tr;
		old_bgcolor_trcd=tr.css("background-color");
		testrcd_selected.css("background-color","#E3F1F7");	
	});

	//弹层移动
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
});