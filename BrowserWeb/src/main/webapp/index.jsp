<html>
<head>
  <meta http-equiv="content-type" content="text/html; charset=utf-8">
  <title>FRuntime Service Discovery Browser</title>

  <link rel="stylesheet" href="http://code.jquery.com/ui/1.9.1/themes/base/jquery-ui.css">
  <script src="//ajax.googleapis.com/ajax/libs/jquery/1/jquery.min.js" type="text/javascript"></script>
  <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1/jquery-ui.min.js" type="text/javascript"></script>
  
  <link href="js/jquery.fancytree/skin-win8/ui.fancytree.css" rel="stylesheet" type="text/css">
  <script src="js/jquery.fancytree/jquery.fancytree.js" type="text/javascript"></script>

<script type="text/javascript">

	var services;
	var byContract;
	var serviceList;

  $(function(){
    initTree();

    $("#refreshButton").button().click(function(event) { refresh(); });
    $("#probeButton").button().click(function(event) { launchProbe(); });

    getServices();
    
  });


  function launchProbe() {
	  $.ajax({
		  url: "/BrowserWeb/api/controller/launchProbe",
		  data: { },
		  success: function( data ) {
			  alert("Response: " + data);
		  },
		  error: function ( data ) { alert("failed"); }
		});


	  }

  function refresh() {

	  getServices();
	  }

  function getContracts() {
	  $.ajax({
		  url: "/AsynchListener/api/responseHandler/contracts",
		  data: { },
		  success: function( data ) {
			  alert("got contracts: " + data);
			  var newSource = [];

			  for (var contract in data.contracts) {
				newSource.push({"title": contract.contractDescription, "key" : contract.contractID});

				  };

			  byContract.source(newSource);
		  }
		});


	  };

  function getServices() {
	  $.ajax({
		  url: "/AsynchListener/api/responseHandler/responses",
		  data: { },
		  success: function( data ) {
			  //alert("got services");
			  
			  var i;
			  var newSource = [];

			  var contractIDMap = {};
			  for(i = 0; i < data.cache.length; i++) {
				  var service = data.cache[i];
				  var contract = contractIDMap[service.serviceContractID];
				  if (!contract) {
					  contract = { "contractID" : service.serviceContractID,
							  "contractDescription" : service.contractDescription, "services" : []};
					  contractIDMap[service.serviceContractID] = contract;
					  }
				  contract.services.push(service);
			  }

			  var s;
			  for (key in contractIDMap) {
				  contract = contractIDMap[key];
				  var contractSource = { "title" : contract.contractDescription, "key" : contract.contractID, "folder" : true, "children" : []};
				  for (s = 0; s < contract.services.length; s++) {
					  var service = contract.services[s];
					  contractSource.children.push({"title": service.serviceName + " : " + service.consumability, "key" : service.id, "data" : service});
					  
					  };
				  newSource.push(contractSource);

				  }

			  $("#byContract").fancytree("option", "source",  newSource );
			  

			  newSource = [];
			  

			  for(i = 0; i < data.cache.length; i++) {
				  var service = data.cache[i];
 				  newSource.push({"title": service.serviceName + " : " + service.consumability, "key" : service.id, "data" : service});
				  };

				  $("#serviceList").fancytree("option", "source",  newSource );
			  
		  }
		});



	  };

  function renderServiceInTable(service, table) {
	  
	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>Service ID</td>"));
	    row.append($("<td>" + service.id + "</td>"));

	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>Service Name</td>"));
	    row.append($("<td>" + service.serviceName + "</td>"));

	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>Service Description</td>"));
	    row.append($("<td>" + service.description + "</td>"));

	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>Consumability</td>"));
	    row.append($("<td>" + service.consumability + "</td>"));

	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>Contract ID</td>"));
	    row.append($("<td>" + service.serviceContractID + "</td>"));

	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>Contract Description</td>"));
	    row.append($("<td>" + service.contractDescription + "</td>"));

	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>IP Address</td>"));
	    row.append($("<td>" + service.ipAddress + "</td>"));

	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>Port</td>"));
	    row.append($("<td>" + service.port + "</td>"));

	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>URL</td>"));
		if (service.consumability == "HUMAN_CONSUMABLE") {
			row.append($("<td><a href='" + service.url + "'>" + service.url + "</td>"));
		} else {
			row.append($("<td>" + service.url + "</td>"));
		}
	  	

	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>TTL</td>"));
	    row.append($("<td>" + service.ttl + "</td>"));
	    
	  	var row = $("<tr />");
	  	table.append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it
	    row.append($("<td align='right' style='padding-right: 25px; font-weight: bold'>Data</td>"));
	    row.append($("<td>" + service.data + "</td>"));
	    
	  }
	  

  function initTree() {
	$("#byContract").fancytree({
	      checkbox: false,
	      selectMode: 2,
	      source: [],
	      activate: function(event, data) {
		      if (data.node.data.serviceName) {
		         //alert("selected " + data.node.data.serviceName);
		         $("#serviceTable1").html("");
		         renderServiceInTable(data.node.data, $("#serviceTable1"));
		      } else {
		    	  $("#serviceTable1").html("");
			      }
	        }
	    });
	 $("#serviceList").fancytree({
	      checkbox: false,
	      selectMode: 3,
	      source: [],
	      activate: function(event, data) {
		      if (data.node.data.serviceName) {
		         //alert("selected " + data.node.data.serviceName);
		         $("#serviceTable2").html("");
		         renderServiceInTable(data.node.data, $("#serviceTable2"));
		      } else {
		    	  $("#serviceTable2").html("");
			      }
	        }
	      
	    });
		  
	  };
</script>

 </head>
<body>

	<div class="intro">
		<h2>Runtime Service Discovery Browser</h2>
		<!-- The name attribute is used by tree.serializeArray()  -->
		<div>
			<button id="refreshButton">Refresh</button><button id="probeButton">Launch Probe</button>
		</div>
		<fieldset>
			<legend>Services By Contract</legend>
			<div id="byContract" name="selNodes"></div>
			<fieldset>
				<legend>Details</legend>
				<div id="serviceDetails1" style='background-color: white'><table id="serviceTable1"></table></div>
			</fieldset>
		</fieldset>
		<fieldset>
			<legend>Plain Service List</legend>
			<div id="serviceList" name="selNodes2"></div>
			<fieldset>
				<legend>Service Details</legend>
				<div id="serviceDetails2" style='background-color: white'><table id="serviceTable2"></table></div>
			</fieldset>
		</fieldset>
	</div>




</body>
</html>
