<g:if test="${fc.userIsLoggedIn()}">
    <div class="btn-group">
        <button class="btn btn-small btn-primary btnProfile" title="profile page">
            <i class="icon-user icon-white"></i><span class="">&nbsp;<fc:currentUserDisplayName /></span>
        </button>
        <button class="btn btn-small btn-primary dropdown-toggle" data-toggle="dropdown">
            <!--<i class="icon-star icon-white"></i>--> My projects&nbsp;&nbsp;<span class="caret"></span>
        </button>
        <div class="dropdown-menu pull-right">
            <fc:userProjectList />
        </div>
    </div>
    <g:if test="${fc.userIsSiteAdmin()}">
        <div class="btn-group">
            <button class="btn btn-warning btn-small btnAdministration"><i class="icon-cog icon-white"></i><span class="">&nbsp;Administration</span></button>
        </div>
    </g:if>
</g:if>
<div class="btn-group">
    <fc:loginLogoutButton logoutUrl="${createLink(controller:'logout', action:'logout')}" cssClass="${loginBtnCss}"/>
</div>