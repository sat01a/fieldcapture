package au.org.ala.merit

import au.org.ala.fieldcapture.DateUtils
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.grails.plugins.excelimport.ExcelImportService
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

/**
 * Specification for the AnnouncementsMapper
 */
class AnnouncementsMapperSpec extends Specification {

    def excelImportService
    def announcementsMapper
    def response

    def setup() {
        excelImportService = new ExcelImportService()
        announcementsMapper = new AnnouncementsMapper(excelImportService)
        response = new MockHttpServletResponse()
    }

    def cleanup() {
        DateTimeUtils.setCurrentMillisSystem()
    }

    void "A list of announcements can be downloaded as a spreadsheet"() {
        given:
        def announcements = buildAnnouncements(10)

        when:
        announcementsMapper.announcementsToExcel(response, announcements)

        then:
        byte[] content = response.getContentAsByteArray()

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))
        Sheet sheet = workbook.getSheet(AnnouncementsMapper.DEFAULT_SHEET)
        sheet.getLastRowNum() == 10

        sheet.getRow(1).getCell(1).stringCellValue == "Project 0"
        sheet.getRow(2).getCell(0).stringCellValue == "Grant 1"
        sheet.getRow(3).getCell(2).stringCellValue == "Event 2"
        sheet.getRow(4).getCell(3).stringCellValue == '2015-06-03'
        sheet.getRow(5).getCell(4).stringCellValue == 'Other'
        sheet.getRow(6).getCell(5).stringCellValue == 'Description 5'
        sheet.getRow(7).getCell(6).numericCellValue == 6d


    }

    void "The downloaded spreadsheet should have a sensible name"() {
        DateTime pretendToday = DateUtils.parse('2015-07-01')
        DateTimeUtils.setCurrentMillisFixed(pretendToday.getMillis())

        given:
        def announcements = buildAnnouncements(10)

        when:
        announcementsMapper.announcementsToExcel(response, announcements)

        then:
        response.getHeader('Content-Disposition') == 'attachment; filename=announcements_01-07-2015.xlsx;'

    }



    void "Announcements can be parsed from an uploaded spreadsheet"() {

        given:
        InputStream announcementsSpreadSheet = getClass().getResourceAsStream('/resources/announcements.xlsx')


        when:
        def announcements = announcementsMapper.excelToAnnouncements(announcementsSpreadSheet)

        then:
        announcements.size() == 10
        announcements[0].grantId == 'Grant 0'
        announcements[1].name == "Project 1"
        announcements[2].eventName == "Event 2"
        announcements[3].eventDate == '2015-06-03'
        announcements[4].eventType == 'Other'
        announcements[5].eventDescription == 'Description 5'
        announcements[6].funding == 6d
    }

    def buildAnnouncements(howMany) {
        def announcements = []
        for (int i=0; i<howMany; i++) {
            announcements << [projectId:'project'+i, grantId:'Grant '+i, name:'Project '+i, eventName:'Event '+i, eventDescription:'Description '+i, eventDate:'2015-06-0'+i, funding:i, eventType:'Other']
        }
        announcements
    }
}