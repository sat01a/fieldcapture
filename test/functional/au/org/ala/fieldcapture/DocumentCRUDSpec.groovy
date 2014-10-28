package au.org.ala.fieldcapture

import geb.Browser
import geb.spock.GebReportingSpec
import org.openqa.selenium.Cookie
import pages.*
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
public class DocumentCRUDSpec extends GebReportingSpec {


    @Shared def testConfig
    def setupSpec() {
        def filePath = new File('grails-app/conf/Config.groovy').toURI().toURL()
        testConfig = new ConfigSlurper(System.properties.get('grails.env')).parse(filePath)

        logout(browser)
        login(browser,  testConfig.test.user.admin.email , testConfig.test.user.admin.password)

    }
    def projectId = "cb5497a9-0f36-4fef-9f6a-9ea832c5b68c"

    def "Add a document"() {

        when: "go to the admin tab of the project index page"

        to ProjectIndex, projectId
        waitFor 5, {adminTab.click()} // Some page initialisation occurs with a blocking spinner
        admin.documentsTab.click()
        admin.documents.attachDocumentButton.click()

        then:
        waitFor 2, {admin.documents.attachDocumentDialog.isDisplayed()}  // This dialog is animated into position so we have to wait.

    }

    def "Enter details of the new document"() {

        when: "find the new activity on the project page"
        admin.documents.attachDocumentDialog.title = 'Test'
        admin.documents.attachDocumentDialog.attribution = 'Test attribution'
        admin.documents.attachDocumentDialog.type = 'Information'
        admin.documents.attachDocumentDialog.license = 'Creative Commons'
        admin.documents.attachDocumentDialog.publiclyViewable = true
        def file = new File('test/functional/resources/testImage.png')
        assert file.exists()
        admin.documents.attachDocumentDialog.file = file.getAbsolutePath()
        admin.documents.attachDocumentDialog.saveButton.click()

        then:
        waitFor 10, {!admin.documents.attachDocumentDialog.isDisplayed()}
        def newDocument = admin.documents.documents.find {it.name == 'Test' && it.attribution == 'Test attribution'}
        newDocument != null
        newDocument.icon.attr('src').endsWith('testImage.png')

    }

    def "edit the new document"() {
        when:
        def newDocument = admin.documents.documents.find {it.name == 'Test' && it.attribution == 'Test attribution'}
        newDocument.editButton.click()

        then:
        waitFor 2, {admin.documents.attachDocumentDialog.isDisplayed()}  // This dialog is animated into position so we have to wait.

        when:
        admin.documents.attachDocumentDialog.title = 'Test [edited]'
        admin.documents.attachDocumentDialog.attribution = 'Test attribution [edited]'
        admin.documents.attachDocumentDialog.type = 'Information'
        admin.documents.attachDocumentDialog.license = 'Creative Commons [edited]'
        admin.documents.attachDocumentDialog.publiclyViewable = false
        admin.documents.attachDocumentDialog.saveButton.click()

        then:
        waitFor 10, {!admin.documents.attachDocumentDialog.isDisplayed()}
        def editedDocument = admin.documents.documents.find {it.name == 'Test [edited]' && it.attribution == 'Test attribution [edited]'}
        editedDocument != null
        editedDocument.icon.attr('src').endsWith('testImage.png')

    }

    def "delete the new document"() {
        when:
        def docs = admin.documents.documents
        def docCount = docs.size()
        def newDocument = admin.documents.documents.find {it.name == 'Test [edited]' && it.attribution == 'Test attribution [edited]'}
        newDocument.deleteButton.click()

        then:
        waitFor 5, {
            admin.documents.documents.size() == (docCount -1)
        }
    }

    def login(Browser browser, username, password) {

        browser.to LoginPage, service: getConfig().baseUrl+EntryPage.url
        browser.page.username = username
        browser.page.password = password
        browser.page.submit()

        browser.at EntryPage

        browser.driver.manage().addCookie(new Cookie("hide-intro", "1"))
    }

    def logout(Browser browser) {
        browser.go 'https://auth.ala.org.au/cas/logout'

    }

}

