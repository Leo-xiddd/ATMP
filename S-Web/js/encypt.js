function encypt(dat){
	var ency="";
	var pi="31415926535897932384626";
	var key=pi.split("");
	var tcr=dat.split("");
	for(var i=0;i<tcr.length;i++){
		tcr[i]=""+(key[i].charCodeAt()+tcr[i].charCodeAt())+":";
		ency=ency+tcr[i];	
	}
	return ency;
}