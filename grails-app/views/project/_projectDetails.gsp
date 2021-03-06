<div data-bind="ifnot: details.status() == 'active'">
	<h4>MERI Plan not available.</h4>
</div>

<div data-bind="if: details.status() == 'active'">
	<span style="float:right;" data-bind="if:detailsLastUpdated">Last update date : <span data-bind="text:detailsLastUpdated.formattedDate"></span></span>
		<h3>MERI Plan Information</h3>
		<div class="row-fluid space-after">
			    <div class="span6">
			        <div id="project-objectives" class="well well-small">
			 			<label><b>Project Outcomes:</b></label>
						<table style="width: 100%;">
					        <thead>
					            <tr>
					            	<th></th>
					                <th>Outcomes</th>
					                <th>Asset(s) addressed</th>
					            </tr>
					        </thead>
						<tbody data-bind="foreach : details.objectives.rows1">
							<tr>
				            	<td><span data-bind="text: $index()+1"></span></td>
				            	<td><span data-bind="text:description"></span></td>
				            	<td><label data-bind="text:assets"></label></td>
				            </tr>
						</tbody>		
						</table>	
						
						<table style="width: 100%;">
					        <thead>
					            <tr>
					            	<th></th>
					                <th>Monitoring indicator</th>
					                <th>Monitoring approach</th>
					            </tr>
					        </thead>
						<tbody data-bind="foreach : details.objectives.rows">
							<tr>
				            	<td><span data-bind="text: $index()+1"></span></td>
				            	<td><span data-bind="text:data1"></span></td>
				            	<td><label data-bind="text:data2"></label></td>
				            </tr>
						</tbody>		
						</table>			 			
			        </div>
			    </div>
			    <div class="span6">
		        <div id="project-partnership" class="well well-small">
		 			<label><b>Project partnership:</b></label> 
		 			<table style="width: 100%;">
					        <thead>
					            <tr>
					            	<th></th>
					                <th>Partner name</th>
					                <th>Nature of partnership</th>
					                <th>Type of organisation</th>
					            </tr>
					        </thead>
						<tbody data-bind="foreach : details.partnership.rows">
							<tr>
				            	<td><span data-bind="text: $index()+1"></span></td>
				            	<td><span data-bind="text:data1"></span></td>
				            	<td><label data-bind="text:data2"></label></td>
				            	<td><label data-bind="text:data3"></label></td>
				            </tr>
						</tbody>		
					</table>			
		        </div>
	        </div>
		</div>
		
		<div class="row-fluid space-after">
		    <div class="span6">
		        <div id="project-implementation" class="well well-small">
		 			<label><b>Project implementation / delivery mechanism:</b></label>
		 			<span style="white-space: pre-wrap;" data-bind="text: details.implementation.description"> </span>
		        </div>
		    </div>
		    
		    <div class="span6">

		    </div>
		</div>



		<div class="row-fluid space-after">
			<div class="well well-small">
				<div id="project-keq" class="well well-small">
					<label><b>Projects Announcements</b></label>
					<table style="width: 100%;">
						<thead>
						<tr>
							<th></th>
							<th>Proposed event/announcement</th>
							<th>Proposed Date of event/<br/>announcement (if known)</th>
							<th>Description of the event</th>
							<th>Will there be, or do you <br/>intend there to be, media <br/> involvement in this event?</th>
							<th></th>
						</tr>
						</thead>
						<tbody data-bind="foreach : details.events">
						<tr>
							<td><span data-bind="text: $index()+1"></span></td>
							<td><span data-bind="text:name"></span></td>
							<td><label data-bind="text:scheduledDate.formattedDate"></label></td>
							<td><span data-bind="text: description"></span></td>
							<td><span data-bind="text: media"></span></td>
						</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<div class="row-fluid space-after">
			<div class="well well-small">
 				<label><b>Key evaluation question</b></label>
	 			<table style="width: 100%;">
			        <thead>
			            <tr>
			            	<th></th>
			                <th>Project Key evaluation question (KEQ)</th>
			                <th>How will KEQ be monitored </th>
			            </tr>
			        </thead>
					<tbody data-bind="foreach : details.keq.rows">
						<tr>
			            	<td><span data-bind="text: $index()+1"></span></td>
			            	<td><span data-bind="text:data1"></span></td>
			            	<td><label data-bind="text:data2"></label></td>
			            </tr>
					</tbody>		
				</table>
			</div>
		</div>		
		
		<div class="row-fluid space-after">
			<div id="national-priorities" class="well well-small">
	 			<label><b>National and regional priorities:</b></label>
	 			<table style="width: 100%;">
			        <thead>
			            <tr>
			            	<th></th>
			                <th>Document name</th>
			                <th>Relevant section</th>
			                <th>Explanation of strategic alignment</th>
			            </tr>
			        </thead>
				<tbody data-bind="foreach : details.priorities.rows">
					<tr>
		            	<td><span data-bind="text: $index()+1"></span></td>
		            	<td><span data-bind="text: data1"></span></td>
		            	<td><label data-bind="text: data2"></label></td>
		            	<td><label data-bind="text: data3"></label></td>
		            </tr>
				</tbody>		
				</table>
			</div>
		</div>		
		
		<div class="row-fluid space-after">
			<div class="required">
	        <div id="keq" class="well well-small">
	 			<label><b>Project Budget</b></label>
			    <table style="width: 100%;">
			        <thead>
			            <tr>
			            	<th width="2%"></th>
			                <th width="12%">Investment/Priority Area</th>
			                <th width="12%">Description</th>
			                <!-- ko foreach: details.budget.headers -->
			                	<th style="text-align: center;" width="10%" ><div style="text-align: center;" data-bind="text:data"></div>$</th>
			                <!-- /ko -->
							<th  style="text-align: center;" width="10%">Total</th>
							
			            </tr>
			        </thead>
			        <tbody data-bind="foreach : details.budget.rows">
			                <tr>
			                	<td><span data-bind="text:$index()+1"></span></td>
			                    <td><span style="width: 97%;" data-bind="text:shortLabel"> </span></td>
			                   	<td><div style="text-align: left;"><span style="width: 90%;" data-bind="text: description"></span></div></td>
							
								<!-- ko foreach: costs -->
		                    	<td><div style="text-align: center;"><span style="width: 90%;" data-bind="text: dollar.formattedCurrency"></span></div></td>
		                    	<!-- /ko -->
			                    
			                    <td style="text-align: center;" ><span style="width: 90%;" data-bind="text: rowTotal.formattedCurrency"></span></td>
			                    
			                </tr>
					 </tbody>
 					<tfoot>
           				<tr>
           					<td></td>
           					<td></td>
							<td style="text-align: right;" ><b>Total </b></td>
							 <!-- ko foreach: details.budget.columnTotal -->
								<td style="text-align: center;" width="10%"><span data-bind="text:data.formattedCurrency"></span></td>
							<!-- /ko -->
							<td style="text-align: center;"><b><span data-bind="text:details.budget.overallTotal.formattedCurrency"></span></b></td>
                   		</tr>
					</tfoot>
			    </table>
	        </div>
	    	</div>
		</div>
		
		<div class="row-fluid space-after">
			<div class="required">
			        <div id="project-risks-threats" class="well well-small">
					<label><b>Project risks & threats</b></label> 
					<div align="right">
				  		<b> Overall project risk profile : <span data-bind="text: risks.overallRisk, css: overAllRiskHighlight" ></span></b>
					</div>
					<table>
				    <thead style="width:100%;">
			          <tr>
			            <th>Type of threat / risk</th>
			            <th>Description</th>
						<th>Likelihood</th>			                
						<th>Consequence</th>							
						<th>Risk rating</th>
						<th>Current control / Contingency strategy</th>														
						<th>Residual risk</th>	
			          </tr>
				    </thead>
					<tbody data-bind="foreach : risks.rows" >
					             <tr>
					                 <td>
					                 	<label data-bind="text: threat" ></label>
					                 </td>
					                 <td>
					                 	<label data-bind="text: description" ></label>
					                 </td>
					                 <td>
					                 	<label data-bind="text: likelihood" ></label>
					                 </td>
					                 <td>
					                 	<label data-bind="text: consequence" ></label>
					                 </td>
					                 <td>
					                 <label data-bind="text: riskRating" ></label> 
					                 </td>
					                 <td>
					                 	<label data-bind="text: currentControl" ></label>
					                  </td>
					                 <td>
					                 	<label data-bind="text: residualRisk" ></label>
					                  </td>
					              </tr>
					      </tbody>
					  </table>
		        </div>
			    </div>
		</div>
		
				
		<div class="row-fluid space-after">
			<div class="span6">
	        	<div class="well well-small">
		        	<label><b>Workplace Health and Safety</b></label>
		 			<div>1. Are you aware of, and compliant with, your workplace health and safety legislation and obligations: <b><span data-bind="text: details.obligations"></span></b></div>
		 			<div>2. Do you have appropriate policies and procedures in place that are commensurate with your project activities: <b><span data-bind="text: details.policies"></span></b></div>
	        	</div>
	        </div>
	        <div class="span6">
	        	<div class="well well-small">
		        	<span><b>&nbsp;Are you willing for your project to be used as a case study by the Department?</b></span>
		        	<input class="pull-left" type="checkbox"  data-bind="checked: details.caseStudy, disable: true" />
	        	</div>
	        </div>	
		</div>
		
	
	
</div>
