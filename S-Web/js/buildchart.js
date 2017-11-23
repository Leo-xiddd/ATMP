function BarLine_chart(target,picname,xAxis_dats,dats){
	var myChart = echarts.init(document.getElementById(target)); 
	var option = {
    	title : {
        	text: '柱形图和线性图实例',
            textStyle:{fontWeight:'bolder',fontFamily:'微软雅黑',fontSize:14},
            left:'40%',
            right:'auto'
    	},
    	tooltip : {
        	trigger: 'axis'
    	},
    	legend: {
            show:false,
        	data:[]
    	},
    	grid:{ x:40, y:10,x2:30,y2:20},
    	toolbox: {
        	show : false,
        	feature : {
            	dataView : {show: true, readOnly: false},
            	magicType : {show: true, type: ['line', 'bar']},
            	restore : {show: true},
            	saveAsImage : {show: true}
        	}
    	},
    	calculable : true,
    	xAxis : [
        	{
            	type : 'category',
            	data : []
        	}
    	],
    	yAxis : [
        	{
            	type : 'value',
                max:'auto',
                min:0,
                splitNumber:8
        	}
    	],
    	series : []
	};
	if(picname==""){
		option.title.show=false;
	}
	else option.title.text=picname;
	for(var i=0;i<xAxis_dats.length;i++)option.xAxis[0].data[i]=xAxis_dats[i];
	for(var i=0;i<dats.length;i++){
		option.legend.data[i]=dats[i].seriers;
		var item={
			name:dats[i].seriers,
			type:dats[i].type,
			itemStyle:{
				normal:{
					color:dats[i].color1
				},
				emphasis:{
					color:dats[i].color2
				}
			},
			data:[],
			markPoint:'', 
			markLine:''	
		};
		if(dats[i].markPoint!="")item.markPoint=dats[i].markPoint;
		if(dats[i].markLine!="")item.markLine=dats[i].markLine;
		option.series.push(item);
		var dd=dats[i].dat.split(",");		
		for(var j=0;j<dd.length;j++)option.series[i].data[j]=dd[j];	
	}
	myChart.setOption(option); 
}
function ToPiechart(target,picname,seriers,legend){
    var myChart = echarts.init(document.getElementById(target)); 
    var option = {
        tooltip: {
            trigger: 'item',
            formatter: "{a} <br/>{b}: {c} ({d}%)"
        },
        title:{
            show:true,
            text:'',
            textStyle:{fontWeight:'bolder',fontFamily:'微软雅黑',fontSize:14},
            x:'center'
        },
        grid:{ x:3, y:5,x2:5,y2:3},
        legend: {
            orient: 'vertical',
            data: []
        },  
        series: []
    };
    // 配置图片标题
    option.title.text=picname;
    if(picname=='')option.title.show=false;
    // 配置图片图例
    if(legend.align!=null){
        if(legend.align=='left'){
            option.legend.left='left';
            option.legend.left=30;
            option.legend.top=legend.top;
        }
        else if(legend.align=='right'){
            option.legend.right='right';
            option.legend.right=20;
            option.legend.top=legend.top;
        }
        else if(legend.align=='bottom'){
            option.legend.bottom='bottom';
            option.legend.bottom=10;
        }
        option.legend.data=legend.data;       
        option.legend.orient=legend.orient;
    } 
    for(var j=0;j<seriers.length;j++){
        var item={
            name:seriers[j].name,
            type:'pie',
            radius: seriers[j].radius,     //饼图半径
            label:{
                normal: {
                    position: 'inner',
                    show:false
                }
            },
            data:seriers[j].data 
        };
        option.series.push(item);
    }
    myChart.setOption(option); 
}
function Stack_chart(target,picname,xAxis_dats,dats){
	var myChart = echarts.init(document.getElementById(target)); 
	var option = {
    	tooltip: {
        	trigger: 'axis',
        	position: function (pt) {
            	return [pt[0], '10%'];
       	 	}
    	},
    	title: {
        	left: 'center',
        	text: '大数据量面积图',
            textStyle:{fontSize:12}
    	},
    	toolbox: {
    		show:false,
        	feature: {
            	dataZoom: {
                	yAxisIndex: 'none'
            	},
            	restore: {},
            	saveAsImage: {}
        	}
    	},
    	grid:{ x:40, y:40,x2:30,y2:20},
    	xAxis: {
        	type: 'category',
        	boundaryGap: false,
        	data: []
    	},
    	yAxis: {
        	type: 'value',
        	boundaryGap: [0, '100%']
    	},
    	dataZoom: {
        	type: 'inside',
        	start: 0,
        	end: 100
    	},
    	series: []
	};
	if(picname==""){
		option.title.show=false;
		option.grid.y=20;
	}
	else option.title.text=picname;
	for(var i=0;i<xAxis_dats.length;i++)option.xAxis.data.push(xAxis_dats[i]);
	for(var i=0;i<dats.length;i++){
		var item={
			name:dats[i].seriers,
			type:'line',
            smooth:true,
            symbol: 'none',
            sampling: 'average',
            itemStyle: {
               	normal: {
                   	color: dats[i].color_line
               	}
            },
            areaStyle: {
               	normal: {color: dats[i].color_area}
            },
			data:[]			
		};
		option.series.push(item);
		var dd=dats[i].dat.split(",");		
		for(var j=0;j<dd.length;j++)option.series[i].data.push(dd[j]);	
	}
	myChart.setOption(option); 
}
function HeatMap_chart(target,picname,dats,ruler_min,ruler_max,calendar_range,color_range){
	var myChart = echarts.init(document.getElementById(target));
	var option = {
		title: {
        	left: 'center',
        	text: '连续热力图实例',
        	show:false
    	},
    	tooltip: {
        	position: 'top',
        	show:true
    	},
    	visualMap: {
        	min: ruler_min,
        	max: ruler_max,
        	calculable: true,
        	orient: 'vertical',
        	left: 'right',
        	top: 0,
        	inRange:{color:['#D8D9D4','#1B8D07']},
        	itemHeight:100
    	},
    	calendar: {
    		range: [],	
    		cellSize: [17.6, 15],
    		left:58,
    		top:20
    	},
    	series: [
    		{
        		type: 'heatmap',
        		coordinateSystem: 'calendar',
        		data: []       		
    		}
    	]
	};
	for(var i=0;i<calendar_range.length;i++)option.calendar.range.push(calendar_range[i]);
	for(var i=0;i<dats.length;i++)option.series[0].data.push(dats[i]);
	if(color_range!="")option.visualMap.inRange.color=color_range;
	myChart.setOption(option);
}
function HeatMap_chart_pieces(target,picname,dats,pieces,categories,calendar_range){
    var myChart = echarts.init(document.getElementById(target));
    var option = {
        title: {
            left: 'center',
            text: '离散热力图实例',
            show:false
        },
        tooltip: {
            position: 'top',
            show:true
        },
        visualMap: {
            type: 'piecewise',
            orient: 'vertical',
            left: 'right',
            top: 10,
            padding: [5, 0,5,10],
            itemHeight:20,
            textStyle:{
                fontSize:12
            }
        },
        calendar: {
            range: [],  
            cellSize: [17.6, 15],
            left:58,
            top:20
        },
        series: [
            {
                type: 'heatmap',
                coordinateSystem: 'calendar',
                data: []            
            }
        ]
    };
    for(var i=0;i<calendar_range.length;i++)option.calendar.range.push(calendar_range[i]);
    for(var i=0;i<dats.length;i++)option.series[0].data.push(dats[i]);
    option.visualMap.pieces=pieces;
    option.visualMap.categories=categories;
    myChart.setOption(option);
}