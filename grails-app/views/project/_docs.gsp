<div class="well" >
    <div class="row-fluid">
        <div class="span2 text-left">
            <b>Project documents:</b>
        </div>
        <div class="span10 text-left">
            <!-- <div id="documents" data-bind="css: { span3: primaryImages() != null, span7: primaryImages() == null }"> -->
            <div id="documents">
                <div data-bind="visible:documents().length == 0">
                    No documents are currently attached to this project.
                    <g:if test="${user?.isAdmin}">To add a document use the Documents section of the Admin tab.</g:if>
                </div>
                <g:render plugin="fieldcapture-plugin" template="/shared/listDocuments"
                          model="[useExistingModel: true,editable:false, filterBy: 'all', ignore: 'programmeLogic', imageUrl:resource(dir:'/images/filetypes'),containerId:'overviewDocumentList']"/>
            </div>
        </div>

    </div>
</div>