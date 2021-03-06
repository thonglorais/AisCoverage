function AisJsonClient (serverurl) {
	var self = this;
    this.serverurl = serverurl || "http://localhost:8090";
    this.sources = {};
    this.loading = false;
    this.jsoncoveragerequest = null;
    
    this.addListener = function(listener){
    	alert(listener);
    }

    this.getSources = function(callback){
    	
    	$.get('/coverage/rest/sources/', function(data) {
    		this.sources = data;
    		$.each(this.sources, function(key, val) {
    			val.enabled=true;
    			val.selected=false;
    		});
    		callback(this.sources);
    	});
    }
    
    this.getCoverage = function(dataToBeSent, screenarea, multifactor, start, end, callback){	
    	//aborting any previous requests
    	if(self.jsoncoveragerequest != null){
    		self.jsoncoveragerequest.abort();
    	}
    	//post won't work with the jetty server...
    	self.jsoncoveragerequest = $.get('/coverage/rest/coverage', { sources: dataToBeSent, area: screenarea, multiplicationFactor: multifactor, starttime:start, endtime:end }, function(data) {
    		callback(data);
    	}); 
    }
    
    this.getSatCoverage = function(rectangle, callback){
    	$.get('/coverage/rest/satCoverage', { area: rectangle}, function(data) {
    		callback(data);
    	}); 
    }
    this.getStatus = function(callback){
    	$.get('/coverage/rest/status', function(data) {
    		callback(data);
    	}); 
    }
    this.getShipTrack = function(start, end, ship, callback){
    	$.get('/coverage/rest/shipTrackExport',{startTime:start, endTime:end, shipmmsi:ship}, function(data) {
    		callback(data);
    	}); 
    }
 
}