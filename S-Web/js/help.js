var xmlHttp;

function open_charpter(file_id){
	$(".sub_charpter").hide();
	$("#"+file_id).show();
}
/***************主函数******************/
$(document).ready(function(){ 
	// if(typeof(sessionStorage.customerId)=='undefined'){
	// 	var url="login.html";
	// 	window.open(encodeURI(url),'_parent');
	// }
	$(".Charpter_body").css('height',($(document).height()-52));
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