package au.org.ala.merit

import au.org.ala.fieldcapture.DateUtils
import grails.converters.JSON
import org.apache.commons.lang.CharUtils
import org.joda.time.Days
import org.joda.time.Interval
import org.joda.time.Period

import java.text.SimpleDateFormat

class ProjectService extends au.org.ala.fieldcapture.ProjectService {

    static dateWithTime = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")
    static dateWithTimeFormat2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    static convertTo = new SimpleDateFormat("dd MMM yyyy")

    static final String PLAN_APPROVED = 'approved'
    static final String PLAN_NOT_APPROVED = 'not approved'
    static final String PLAN_SUBMITTED = 'submitted'

    def update(id, body) {
        TimeZone.setDefault(TimeZone.getTimeZone('UTC'))
        body?.custom?.details?.lastUpdated = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")

        def datesChanged = false
        if (body['plannedStartDate'] || body['plannedEndDate']) {
            body.timeline = null // Force a reset of the project timeline.
            datesChanged = true
        }

        def resp = super.update(id, body)
//        if (datesChanged && resp && !resp.error) {
//            createReportingActivitiesForProject(id)
//        }

        return resp
    }

    /**
     * Does the current user have permission to edit the requested projectId?
     * Checks for the ADMIN role in CAS and then checks the UserPermission
     * lookup in ecodata.
     *
     * @param userId
     * @param projectId
     * @return boolean
     */
    def canUserEditProject(userId, projectId) {
        def userCanEdit
        if (userService.userIsSiteAdmin()) {
            userCanEdit = true
        } else {
            def url = grailsApplication.config.ecodata.baseUrl + "permissions/canUserEditProject?projectId=${projectId}&userId=${userId}"
            userCanEdit = webService.getJson(url)?.userIsEditor?:false
        }

        userCanEdit
    }

    def submitPlan(String projectId) {
        def project = get(projectId)

        if (!project.planStatus || project.planStatus == PLAN_NOT_APPROVED) {
            def resp = update(projectId, [planStatus:PLAN_SUBMITTED])
            if (resp.resp && !resp.resp.error) {
                emailService.sendPlanSubmittedEmail(projectId, [project:project])
                return [message:'success']
            }
            else {
                return [error:"Update failed: ${resp?.resp?.error}"]
            }
        }
        return [error:'Invalid plan status']
    }

    def approvePlan(String projectId) {
        def project = get(projectId)
        if (project.planStatus == PLAN_SUBMITTED) {
            def resp = update(projectId, [planStatus:PLAN_APPROVED])
            if (resp.resp && !resp.resp.error) {
                emailService.sendPlanApprovedEmail(projectId, [project:project])
                return [message:'success']
            }
            else {
                return [error:"Update failed: ${resp?.resp?.error}"]
            }
        }
        return [error:'Invalid plan status']

    }

    def rejectPlan(String projectId) {
        def project = get(projectId)
        if (project.planStatus in [PLAN_SUBMITTED, PLAN_APPROVED]) {
            def resp = update(projectId, [planStatus:PLAN_NOT_APPROVED])
            if (resp.resp && !resp.resp.error) {
                emailService.sendPlanRejectedEmail(projectId, [project:project])
                return [message:'success']
            }
            else {
                return [error:"Update failed: ${resp?.resp?.error}"]
            }
        }
        return [error:'Invalid plan status']
    }

    /**
     * Submits a report of the activities performed during a specific time period (a project stage).
     * @param projectId the project the performing the activities.
     * @param stageDetails details of the activities, specifically a list of activity ids.
     */
    def submitStageReport(projectId, stageDetails) {

        def activities = activityService.activitiesForProject(projectId);

        def allowedStates = ['finished', 'deferred', 'cancelled']
        def readyForSubmit = true
        stageDetails.activityIds.each { activityId ->
            def activity = activities.find {it.activityId == activityId}
            if (!allowedStates.contains(activity?.progress)) {
                readyForSubmit = false
            }
        }
        if (!readyForSubmit) {
            return [error:'All activities must be finished, deferred or cancelled']
        }
		
		//generate stage report and attach to the project
		def projectAll = get(projectId, 'all')
		readyForSubmit = false;
		projectAll?.timeline?.each{
			if(it.name.equals(stageDetails.stage)){
				readyForSubmit = true;
			}
		}
		if (!readyForSubmit) {
			return [error:'Invalid stage']
		}
		
		def stageName = stageDetails.stage;
		def param  = [project: projectAll, activities:activities, stageName:stageName, status:"Report submitted"]
		def htmlTxt = createHTMLStageReport(param)
		def dateWithTime = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss")
		def name = projectAll?.grantId + '_' + stageName + '_' + dateWithTime.format(new Date()) + ".pdf"
		def doc = [name:name, projectId:projectId, saveAs:'pdf', type:'pdf', role:'stageReport',filename:name, readOnly:true, public:false]
		documentService.createTextDocument(doc, htmlTxt)
        def result = activityService.submitActivitiesForPublication(stageDetails.activityIds)
        def project = get(projectId)
        stageDetails.project = project
        if (!result.resp.error) {
            emailService.sendReportSubmittedEmail(projectId, stageDetails)
        }
        result
    }

    /**
     * Approves a submitted stage report.
     * @param projectId the project the performing the activities.
     * @param stageDetails details of the activities, specifically a list of activity ids.
     */
    def approveStageReport(projectId, stageDetails) {
        def result = activityService.approveActivitiesForPublication(stageDetails.activityIds)

        // TODO Send a message to GMS.
        def project = get(projectId, 'all')
        def readableId = project.grantId + (project.externalId?'-'+project.externalId:'')
        def name = "${readableId} ${stageDetails.stage} approval"
        def doc = [name:name, projectId:projectId, type:'text', role:'approval',filename:name, readOnly:true, public:false]
        documentService.createTextDocument(doc, (project as JSON).toString())
        stageDetails.project = project
        if (!result.resp.error) {
            emailService.sendReportApprovedEmail(projectId, stageDetails)
        }

        //Update project status to completed
        int published = 0;
        int validActivities = 0
        def activities = activityService.activitiesForProject(projectId);;
        project.timeline?.each {timeline->
            activities.each{act->
                def endDate = act.plannedEndDate ? act.plannedEndDate : act.endDate
                if(dateInSlot(timeline.fromDate,timeline.toDate,endDate)){
                    validActivities++;
                    if(act.publicationStatus.equals("published")){
                        published++
                    }
                }
            }
        }

        if(validActivities == published){
            def values = [:]
            values["status"] = "completed"
            update(projectId, values)
        }

        result
    }

    /**
     * Rejects a submitted stage report.
     * @param projectId the project the performing the activities.
     * @param stageDetails details of the activities, specifically a list of activity ids.
     */
    def rejectStageReport(projectId, stageDetails) {
        def result = activityService.rejectActivitiesForPublication(stageDetails.activityIds)

        // TODO Send a message to GMS.  Delete previous approval document (only an issue for withdrawal of approval)?
        def project = get(projectId)
        stageDetails.project = project

        if (!result.resp.error) {
            emailService.sendReportRejectedEmail(projectId, stageDetails)
        }

        result
    }


    def changeProjectStartDate(projectId, plannedStartDate) {
        def project = get(projectId)
        if (!project.planStatus || project.planStatus == PLAN_NOT_APPROVED) {

            def previousStartDate = DateUtils.parse(project.plannedStartDate)
            def newStartDate = DateUtils.parse(plannedStartDate)

            def daysChanged = Days.daysBetween(previousStartDate, newStartDate).days

            log.info("Updating start date for project ${projectId} from ${project.plannedStartDate} to ${plannedStartDate}, ${daysChanged} days difference")

            def newEndDate = DateUtils.format(DateUtils.parse(project.plannedEndDate).plusDays(daysChanged))

            def resp = update(projectId, [plannedStartDate:plannedStartDate, plannedEndDate:newEndDate])
            if (resp.resp && !resp.resp.error) {

                def activities = activityService.activitiesForProject(projectId)
                activities.each { activity ->
                    if (!activityService.isReport(activity)) {
                        def newActivityStartDate = DateUtils.format(DateUtils.parse(activity.plannedStartDate).plusDays(daysChanged))
                        def newActivityEndDate = DateUtils.format(DateUtils.parse(activity.plannedEndDate).plusDays(daysChanged))
                        activityService.update(activity.activityId, [activityId:activity.activityId, plannedStartDate:newActivityStartDate, plannedEndDate:newActivityEndDate])
                    }

                }
                createReportingActivitiesForProject(project.projectId, [[period: Period.months(1), type:'Green Army - Monthly project status report']])

                return [message:'success']
            }
            else {
                return [error:"Update failed: ${resp?.resp?.error}"]
            }
        }
        return [error:'Invalid plan status']
    }

    def changeProjectDates(projectId, plannedStartDate, plannedEndDate) {
        def project = get(projectId)
        if (!project.planStatus || project.planStatus == PLAN_NOT_APPROVED) {

            def previousStartDate = DateUtils.parse(project.plannedStartDate)
            def newStartDate = DateUtils.parse(plannedStartDate)

            def previousEndDate = DateUtils.parse(project.plannedEndDate)
            def newEndDate = DateUtils.parse(plannedEndDate)

            def daysStartChanged = Days.daysBetween(previousStartDate, newStartDate).days

            def previousDuration = Days.daysBetween(previousStartDate, previousEndDate).days
            def newDuration = Days.daysBetween(newStartDate, newEndDate).days

            if (newDuration <= 0 || previousDuration <= 0) {
                return [error:"Invalid project dates"]
            }

            def scale = (double)newDuration / (double)previousDuration

            log.info("Updating start date for project ${projectId} from ${project.plannedStartDate} to ${newStartDate}, ${daysStartChanged} days difference")
            log.info("Updating end date for project ${projectId} from ${project.plannedEndDate} to ${newEndDate}")
            log.info("Project duration changing by a factor of ${scale}")

            def resp = update(projectId, [plannedStartDate:plannedStartDate, plannedEndDate:plannedEndDate])
            if (resp.resp && !resp.resp.error) {

                def activities = activityService.activitiesForProject(projectId)
                activities.each { activity ->
                    if (!activityService.isReport(activity)) {
                        def newActivityStartDate = DateUtils.format(DateUtils.parse(activity.plannedStartDate).plusDays(daysStartChanged))
                        def daysToChangeEndDate = (int)Math.round(daysStartChanged * scale)
                        def newActivityEndDate = DateUtils.format(DateUtils.parse(activity.plannedEndDate).plusDays(daysToChangeEndDate))

                        // Account for any rounding errors that would result in the activity falling outside the project date range.
                        if (newActivityStartDate < plannedStartDate) {
                            newActivityStartDate = plannedStartDate
                        }
                        if (newActivityEndDate > plannedEndDate) {
                            newActivityEndDate = plannedEndDate
                        }
                        if (newActivityStartDate > newActivityEndDate) {
                            newActivityStartDate = newActivityEndDate
                        }

                        activityService.update(activity.activityId, [activityId:activity.activityId, plannedStartDate:newActivityStartDate, plannedEndDate:newActivityEndDate])
                    }

                }
                createReportingActivitiesForProject(project.projectId, [[period: Period.months(1), type:'Green Army - Monthly project status report']])

                return [message:'success']
            }
            else {
                return [error:"Update failed: ${resp?.resp?.error}"]
            }
        }
        return [error:'Invalid plan status']
    }

    def createReportingActivitiesForProject(projectId, config) {
        def result = regenerateReportingActivitiesForProject(projectId, config)
        result.create.each { activity ->
            activityService.create(activity)
        }

        result.delete.each { activity ->
            if (activity.progress != 'planned') {
                log.warn("Attempt to delete non - planned activity ${activity.activityId}, progress: ${activity.progress}")
            }
            else {
                activityService.delete(activity.activityId)
            }
        }
    }

    /**
     * This method supports automatically creating reporting activities for a project that re-occur at defined intervals.
     * e.g. a stage report once every 6 months or a green army monthly report once per month.
     * Activities will only be created when no reporting activity of the correct type exists within each period.
     * @param projectId identifies the project.
     * @param config List of [type:<activity type>, period:<period that must have a reporting activity>
     * @return
     */
    def regenerateReportingActivitiesForProject(projectId, config) {

        def project = get(projectId, 'all')

        def startDate = DateUtils.parse(project.plannedStartDate)
        def endDate = DateUtils.parse(project.plannedEndDate)


        def toCreate = []
        def toDelete = []
        config.each {

            def periodStartDate = startDate
            def periodEndDate = endDate
            def activitiesOfType = project.activities.findAll {activity -> activity.type == it.type}
            if (activitiesOfType) {
                def firstActivityEndDate = DateUtils.parse(activitiesOfType.min{it.plannedEndDate}.plannedEndDate)
                periodStartDate = startDate < firstActivityEndDate ? startDate : firstActivityEndDate

                def lastActivityEndDate = DateUtils.parse(activitiesOfType.max{it.plannedEndDate}.plannedEndDate)
                periodEndDate = endDate > lastActivityEndDate ? endDate : lastActivityEndDate

            }

            periodStartDate = DateUtils.alignToPeriod(periodStartDate, it.period)

            def existingActivitiesByPeriod = DateUtils.groupByDateRange(activitiesOfType, {it.plannedEndDate}, it.period, periodStartDate, periodEndDate)

            def gaps = []
            existingActivitiesByPeriod.each { interval, activities ->
                if (interval.isBefore(startDate) || interval.isAfter(endDate)) {
                    toDelete += activities
                }
                else if (!activities) {
                    gaps << interval;
                }
            }

            gaps.each {Interval period ->
                // Subtract a day from the end date so the activity is displayed as 01/01/2014-31/01/2014 etc
                // If the period end date is after the project end date, use the project end date.
                def end = period.end.isBefore(endDate) ? period.end.minusDays(1) : endDate
                def activity = [type:it.type, plannedStartDate:DateUtils.format(period.start), plannedEndDate:DateUtils.format(end), projectId:projectId]
                activity.description = activityService.defaultDescription(activity)
                toCreate << activity
            }
        }
        return [create:toCreate, delete:toDelete]

    }

    def createHTMLStageReport(param) {

        def project = param.project
        def activities = param.activities
        def stageName = param.stageName
        def status = param.status

        def stage = ''
        def planned = 0
        def started = 0
        def finished = 0
        def deferred = 0
        def cancelled = 0
        def stageStartDate = ''
        def stageEndDate = ''

        org.codehaus.groovy.runtime.NullObject.metaClass.toString = {return ''}
        project.timeline?.each {
            if(it.name.equals(stageName)){
                stage = "${it.name} : "+convertDate(it.fromDate) +" - " +convertDate(it.toDate)
                stageStartDate = it.fromDate
                stageEndDate =  it.toDate
            }
        }
        activities.each{
            if(dateInSlot(stageStartDate,stageEndDate,it.plannedEndDate)){
                if(it.progress.equals('planned'))
                    planned++
                else if (it.progress.equals('started'))
                    started++
                else if (it.progress.equals('finished'))
                    finished++
                else if (it.progress.equals('deferred'))
                    deferred++
                else if (it.progress.equals('cancelled'))
                    cancelled++
            }
        }

        StringBuilder html = new StringBuilder();
        append(html,"<html lang=\"en-AU\">")
        append(html,"<head>")
        append(html,"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />")
        append(html,"</head>")

        append(html,'<body>')
        append(html,'<font face="Arial">')
        append(html,'<h1 align="center"><font color="#008080">MERIT STAGE SUMMARY</font></h1>')
        append(html,'<br>')
        append(html,'<h2><font color="">'+stage+'</font></h2></hr>')

        append(html,'<table cellpadding="3" border="0">')
        append(html,'<tr><td>Project Name</td><td>'+project.name+'</td></tr>')
        append(html,'<tr><td>Recipient</td><td>'+project.organisationName+'</td></tr>')
        append(html,'<tr><td>Service Provider</td><td></td></tr>')
        append(html,'<tr><td>Funded by</td><td>'+project.associatedProgram+'</td></tr>')
        append(html,'<tr><td>Funding</td><td>'+project.fundingSource+'</td></tr>')
        append(html,'<tr><td>Project Start</td><td>'+convertDate(project.plannedStartDate)+'</td></tr>')
        append(html,'<tr><td>Project finish</td><td>'+convertDate(project.plannedEndDate)+'</td></tr>')
        append(html,'<tr><td>Grant ID</td><td>'+project.grantId+'</td></tr>')
        append(html,'<tr><td>External ID</td><td>'+project.externalId+'</td></tr>')
        append(html,'</table>')

        append(html,'<br>')
        append(html,'<p align="left">_________________________________________________________________________________________________________</p>')
        append(html,'<br>')
        append(html,'<h2><font color="">Summary</font></h2>')
        append(html,'<h4><font color="">Number of activities:</font></h4>')
        append(html,'<table cellpadding="3" border="0">')
        append(html,'<tr><td>Planned</td><td>'+planned+'</td></tr>')
        append(html,'<tr><td>Started</td><td>'+started+'</td></tr>')
        append(html,'<tr><td>Finished</td><td>'+finished+'</td></tr>')
        append(html,'<tr><td>Deferred</td><td>'+deferred+'</td></tr>')
        append(html,'<tr><td>Cancelled</td><td>'+cancelled+'</td></tr>')
        append(html,'</table>')

        append(html,'<br>')
        append(html,'<p align="left">_________________________________________________________________________________________________________</p>')
        append(html,'<br>')
        append(html,'<h2><font color="">Supporting Documents Attached During This Stage</font></h2>')
        append(html,'<table cellpadding="3" border="0">')
        append(html,'<tr><th>Document name</th></tr>')
        project.documents?.each{
			String name = "Stage ${it.stage}";	
            if("active".equals(it.status) && name.equals(stageName)){
                append(html,"<tr><td>${it.name}</td></tr>")
            }
        }
        append(html,'</table>')
		append(html,'<br>')
		
		// use existing project dashboard calculation to display metrics data.
		append(html,'<p align="left">_________________________________________________________________________________________________________</p>')
		append(html,'<br>')
		append(html,'<h2><font color="">Outputs: Targets Vs Achieved</font></h2>')
		append(html,'<table cellpadding="3" border="0">')
		append(html,'<tr><th>Output type</th><th>Output Target Measure</th><th>Output Achieved (project to date)</th><th>Output Target (whole project)</th></tr>')
		
		def metrics = summary(project.projectId); 			
		metrics?.targets?.each{ k, v->
			v?.each{ data ->
				String units = data.score?.units ? data.score.units : '';
				double total = 0.0;
				data.results?.each { result ->
					total = total + result.result;
				}
				append(html,"<tr><td>${data.score?.outputName}</td><td>${data.score?.label}</td><td>${total}</td><td>${data.target} ${units}</td></tr>")
			}
		}
		append(html,'</table>')
        append(html,'<br>')
        append(html,'<p align="left">_________________________________________________________________________________________________________</p>')
        append(html,'<br>')
        append(html,'<h2><font>Project Outcomes</font></h2>')
        append(html,'<table cellpadding="3" border="0">')
        append(html,'<tr><td>Outcomes</td><td>Project Goals</td></tr>');
        project?.custom?.details?.objectives?.rows1?.each {
            append(html,'<tr><td>'+it.description+'</td>');
            append(html,'<td>'+it.assets?.join(", ")+'</td></tr>');
        }
        append(html,'</table>')

        append(html,'<br>')
        append(html,'<p align="left">_________________________________________________________________________________________________________</p>')
        append(html,'<br>')
        append(html,'<h2><font>Summary of Project Progress and Issues</font></h2>')
        
		project?.activities?.each {
			if(it.type.equals('Progress, Outcomes and Learning - stage report') &&
                    dateInSlot(stageStartDate,stageEndDate,it.plannedEndDate)){
				it.outputs?.each{
					def type = metadataService.annotatedOutputDataModel("$it.name")
					append(html,"<b> $it.name: </b> <br>");
					it.data?.each{ k, v ->
						def label = "Result"
						type.each{ view ->
							if(view.name.equals(k)){
								label = view.label;
							}
						}
						append(html,"${label}:- ${v}<br>");
					}
					append(html,"<br>");
				}
			}
        }
				
        append(html,'<br>')
        append(html,'<p align="left">_________________________________________________________________________________________________________</p>')
        append(html,'<br>')
        append(html,'<h2><font color="">Project Risk</font></h2>')
        append(html,'<p>To help anticipate and determine management and mitigation strategies for the risks associated with delivering and '+
                'reporting the outcomes of this Regional Delivery project, complete the table below. Risks identified should be those that the '+
                'project team consider to be within the reasonable influence of the project team to anticipate and manage.</p>')
        append(html,'<table cellpadding="3" border="0">')
        append(html,'<tr><th>Risk/Threat Description</th><th>Likelihood</th><th>Consequence</th><th>Rating</th><th>Current Controls/Contingency </th><th>Residual Risk</th></tr>')
        project?.risks?.rows?.each{
            append(html,'<tr><td>'+it.description+'</td><td>'+it.likelihood+'</td><td>'+it.consequence+'</td><td>'+
                    it.riskRating+'</td><td>'+it.currentControl+'</td><td>'+it.residualRisk+'</td></tr>')
        }
        append(html,'</table>')

        append(html,'<br>')
        append(html,'<p align="left">_________________________________________________________________________________________________________</p>')
        append(html,'<br>')
        append(html,'<h2><font color="">Project Against Each Activity</font></h2>')

        int i=0;
        project?.activities?.each{
            if(dateInSlot(stageStartDate,stageEndDate,it.plannedEndDate)){
                i++;
                append(html,'<p>')
                append(html,'<table cellpadding="3" border="0">')
                append(html,'<tr><td><b>'+i+'. Activity Type</b></td><td><b>'+it.type+'</b></td></tr>')
                append(html,'<tr><td>Status</td><td>'+it.progress+'</td></tr>')
                append(html,'<tr><td>Activity Description</td><td>'+it.description+'</td></tr>')
                append(html,'<tr><td>Major Theme</td><td>'+it.mainTheme+'</td></tr>')

                def temp = it.siteId;
                project.sites?.each{
                    if(it.siteId.equals(temp)){
                        append(html,'<tr><td>Site</td><td>'+it.name+'</td></tr>')
                    }
                }
                append(html,"<tr><td>Start Date</td><td>${convertDate(it.startDate)}</td></tr>")
                append(html,"<tr><td>End Date</td><td>${convertDate(it.endDate)}</td></tr>")

                def reason = ''
                if(it.progress.equals('deferred') || it.progress.equals('cancelled')){
                    it.documents.each{
                        reason = "${it.notes}${it.data?.eventNotes}${it.data?.debrisNotes}${it.data?.erosionNotes}${it.data?.pestObservationNotes}${it.data?.weedInspectionNotes}${it.data?.fenceNotes}"
                    }
                    append(html,'<tr><td>Reason '+it.name+'</td><td>'+reason+'</td></tr>')
                    it.outputs?.each{
                        def outputNotes = "${it?.data?.notes}${it?.data?.eventNotes}${it?.data?.debrisNotes}${it?.data?.erosionNotes}${it?.data?.pestObservationNotes}${it?.data?.weedInspectionNotes}${it?.data?.fenceNotes}";
                        append(html,"<tr><td>Comments for ${it.name} </td><td> ${outputNotes} </td></tr>")
                    }
                }
                append(html,'</table>')
                append(html,'</p>')
            }
        }
        append(html,'<br>')
        append(html,'<p align="left">_________________________________________________________________________________________________________</p>')
        append(html,'<br>')
        append(html,'<table cellpadding="3" border="0">')
        append(html,"<tr><td><b>Summary generated by: </b></td><td><b>${userService.getUser().displayName}(${userService.getUser().userName})</b></td></tr>")
        append(html,"<tr><td>Position/Role</td><td>MERIT Project Administrator and authorised representative of ${project.organisationName}</td></tr>")
        append(html,"<tr><td>Date</td><td>${dateWithTimeFormat2.format(new Date())}</td></tr>")
        append(html,"<tr><td>Report status</td><td>${status}</td></tr>")
        append(html,'</table>')

        append(html,"</font></body>")
        append(html,"</html>")
        org.codehaus.groovy.runtime.NullObject.metaClass.toString = {return 'null'}

        return html.toString();
    }

    private append(StringBuilder str, String data){
        str.append(data).append(CharUtils.CR).append(CharUtils.LF)
    }

    private convertDate(date) {
        if(date)
            convertTo.format(dateWithTime.parse(date))
        else
            '-';
    }

    private dateInSlot(d1,d2,range){
        if(d1 && d2 && range){
            d1 = dateWithTime.parse(d1)
            d2 = dateWithTime.parse(d2)
            range = dateWithTime.parse(range)
            def slot = d1..d2
            return slot.containsWithinBounds(range)
        }
        return false;
    }




}
