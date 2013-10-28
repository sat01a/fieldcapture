package au.org.ala.fieldcapture

import grails.converters.JSON

class ProjectController {

    def projectService, metadataService, commonService, activityService, userService, webService, authService
    static defaultAction = "index"
    static ignore = ['action','controller','id']

    def index(String id) {
        def project = projectService.get(id, 'brief')
        def roles = metadataService.getAccessLevels().collect { it.name }

        if (!project || project.error) {
            forward(action: 'list', model: [error: project.error])
        } else {
            project.sites?.sort {it.name}
            def user = userService.getUser()
            def members = projectService.getMembersForProjectId(id)
            def admins = members.findAll{ it.role == "admin" }.collect{ it.userName }.join(",") // comma separated list of user email addresses

            if (user && projectService.isUserAdminForProject(user.userId, id)) {
                // add admin tab to page
                user.metaClass.isAdmin = true
                user.metaClass.isEditor = true
            } else if (user && projectService.canUserEditProject(user.userId, id)) {
                //user["isEditor"] = true // use this for KO to allow editing of activities, etc ??
                user.metaClass.isEditor = true // use this for KO to allow editing of activities, etc ??
            } else if (user) {
                user.metaClass.isAdmin = false
                user.metaClass.isEditor = false
            }
            //log.debug activityService.activitiesForProject(id)
            //todo: ensure there are no control chars (\r\n etc) in the json as
            //todo:     this will break the client-side parser
            [project: project,
             activities: activityService.activitiesForProject(id),
             mapFeatures: commonService.getMapFeatures(project),
             // TODO The test data we have loaded has an organisationName but no corresponding collectory entry.
             // Remove the organisationName default after this is tidied up.
             organisationName: metadataService.getInstitutionName(project.organisation) ?: project.organisationName,
             isProjectStarredByUser: userService.isProjectStarredByUser(user?.userId?:"0", project.projectId)?.isProjectStarredByUser,
             user: user,
             roles: roles,
             admins: admins,
             activityTypes: metadataService.activityTypesList(),
             metrics: projectService.summary(id),
             activityScores: metadataService.getOutputTargetsByActivity()]
        }
    }

    @PreAuthorise
    def edit(String id) {
        def project = projectService.get(id)
        if (project) {
            [project: project,
             institutions: metadataService.institutionList(),
             programs: metadataService.programsModel()]
        } else {
            forward(action: 'list', model: [error: 'no such id'])
        }
    }

    @PreAuthorise(accessLevel = 'admin')
    def create() {
        render view: 'edit', model: [create:true,
                institutions: metadataService.institutionList(),
                programs: metadataService.programsModel()
        ]
    }

    /**
     * Updates existing or creates new output.
     *
     * If id is blank, a new project will be created
     *
     * @param id projectId
     * @return
     */
    @PreAuthorise
    def ajaxUpdate(String id) {
        def postBody = request.JSON
        if (!id) { id = ''}
        println "Body: " + postBody
        println "Params:"
        params.each { println it }
        def values = [:]
        // filter params to remove keys in the ignore list
        postBody.each { k, v ->
            if (!(k in ignore)) {
                values[k] = v
            }
        }
        log.debug "json=" + (values as JSON).toString()
        log.debug "id=${id} class=${id.getClass()}"
        def result = projectService.update(id, values)
        log.debug "result is " + result
        if (result.error) {
            render result as JSON
        } else {
            //println "json result is " + (result as JSON)
            render result.resp as JSON
        }
    }

    @PreAuthorise
    def update(String id) {
        //params.each { println it }
        projectService.update(id, params)
        chain action: 'index', id: id
    }

    @PreAuthorise(accessLevel = 'admin')
    def delete(String id) {
        projectService.delete(id)
        forward(controller: 'home')
    }

    def list() {
        // will show a list of projects
        // but for now just go home
        forward(controller: 'home')
    }

    def species(String id) {
        def project = projectService.get(id, 'brief')
        def activityTypes = metadataService.activityTypesList();
        render view:'/species/select', model: [project:project, activityTypes:activityTypes]
    }

    /**
     * Star or unstar a project for a user
     * Action is determined by the URI endpoint, either: /add | /remove
     *
     * @return
     */
    def starProject() {
        String act = params.id?.toLowerCase() // rest path starProject/add or starProject/remove
        String userId = params.userId
        String projectId = params.projectId

        if (act && userId && projectId) {
            if (act == "add") {
                render userService.addStarProjectForUser(userId, projectId) as JSON
            } else if (act == "remove") {
                render userService.removeStarProjectForUser(userId, projectId) as JSON
            } else {
                render status:400, text: 'Required endpoint (path) must be one of: add | remove'
            }
        } else {
            render status:400, text: 'Required params not provided: userId, projectId'
        }
    }

    def getMembersForProjectId() {
        String projectId = params.id

        if (projectId) {
            render projectService.getMembersForProjectId(projectId) as JSON
        } else {
            render status:400, text: 'Required params not provided:  projectId'
        }
    }
}
