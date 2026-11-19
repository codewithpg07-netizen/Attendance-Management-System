package com.example.studentAttendanceSystemProject.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.studentAttendanceSystemProject.entity.Attendance;
import com.example.studentAttendanceSystemProject.entity.PreVerifiedStudent;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class ReportExportService {

    /**
     * Export attendance list to Excel byte array.
     */
    public byte[] exportAttendanceListToExcel(String title, List<Attendance> attendances) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance Report");

            // Header styling
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);

            // Header Row
            Row headerRow = sheet.createRow(2);
            String[] headers = {"#", "Date", "Roll Number", "Student Name", "Department", "Subject", "Teacher", "Status", "Marked Via"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            int counter = 1;
            for (Attendance att : attendances) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(counter++);
                row.createCell(1).setCellValue(att.getDate() != null ? att.getDate().toString() : "");
                row.createCell(2).setCellValue(att.getPstudent() != null ? att.getPstudent().getRollNumber() : "");
                row.createCell(3).setCellValue(att.getPstudent() != null ? att.getPstudent().getName() : "");
                row.createCell(4).setCellValue(att.getPstudent() != null && att.getPstudent().getDepartment() != null
                        ? att.getPstudent().getDepartment().getName() : "");
                row.createCell(5).setCellValue(att.getSubject() != null ? att.getSubject().getSubjectName() : "");
                row.createCell(6).setCellValue(att.getTeacher() != null ? att.getTeacher().getName() : "");
                row.createCell(7).setCellValue(att.isPresent() ? "Present" : "Absent");
                row.createCell(8).setCellValue(att.getMarkedVia() != null ? att.getMarkedVia() : "MANUAL");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Export Defaulter list to Excel byte array.
     */
    public byte[] exportDefaultersToExcel(String title, List<Map<String, Object>> defaulters) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Defaulter List");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"#", "Roll Number", "Student Name", "Department", "Year", "Mobile", "Parent Mobile", "Attended", "Total", "Attendance %"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            int counter = 1;
            for (Map<String, Object> map : defaulters) {
                PreVerifiedStudent st = (PreVerifiedStudent) map.get("student");
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(counter++);
                row.createCell(1).setCellValue(st != null ? st.getRollNumber() : "");
                row.createCell(2).setCellValue(st != null ? st.getName() : "");
                row.createCell(3).setCellValue(st != null && st.getDepartment() != null ? st.getDepartment().getName() : "");
                row.createCell(4).setCellValue(st != null ? st.getYear() : "");
                row.createCell(5).setCellValue(st != null ? st.getMobileNumber() : "");
                row.createCell(6).setCellValue(st != null ? st.getParentMobileNumber() : "");
                row.createCell(7).setCellValue((long) map.get("attendedClasses"));
                row.createCell(8).setCellValue((long) map.get("totalClasses"));
                row.createCell(9).setCellValue((String) map.get("percentageFormatted"));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Export attendance report to PDF using iText.
     */
    public byte[] exportAttendanceListToPdf(String title, List<Attendance> attendances) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(15);
        document.add(titlePara);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);

        String[] headers = {"#", "Date", "Roll No", "Student Name", "Subject", "Status", "Via"};
        com.itextpdf.text.Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new BaseColor(13, 110, 253));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        com.itextpdf.text.Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        int counter = 1;
        for (Attendance att : attendances) {
            table.addCell(new PdfPCell(new Phrase(String.valueOf(counter++), bodyFont)));
            table.addCell(new PdfPCell(new Phrase(att.getDate() != null ? att.getDate().toString() : "", bodyFont)));
            table.addCell(new PdfPCell(new Phrase(att.getPstudent() != null ? att.getPstudent().getRollNumber() : "", bodyFont)));
            table.addCell(new PdfPCell(new Phrase(att.getPstudent() != null ? att.getPstudent().getName() : "", bodyFont)));
            table.addCell(new PdfPCell(new Phrase(att.getSubject() != null ? att.getSubject().getSubjectName() : "", bodyFont)));

            PdfPCell statusCell = new PdfPCell(new Phrase(att.isPresent() ? "Present" : "Absent", bodyFont));
            if (att.isPresent()) {
                statusCell.setBackgroundColor(new BaseColor(220, 245, 220));
            } else {
                statusCell.setBackgroundColor(new BaseColor(255, 220, 220));
            }
            table.addCell(statusCell);

            table.addCell(new PdfPCell(new Phrase(att.getMarkedVia() != null ? att.getMarkedVia() : "MANUAL", bodyFont)));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    /**
     * Export Defaulter list to PDF using iText.
     */
    public byte[] exportDefaultersToPdf(String title, List<Map<String, Object>> defaulters) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new BaseColor(180, 40, 40));
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(15);
        document.add(titlePara);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);

        String[] headers = {"#", "Roll No", "Student Name", "Parent Mobile", "Attended / Total", "Percentage"};
        com.itextpdf.text.Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new BaseColor(220, 53, 69));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        com.itextpdf.text.Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        int counter = 1;
        for (Map<String, Object> map : defaulters) {
            PreVerifiedStudent st = (PreVerifiedStudent) map.get("student");
            table.addCell(new PdfPCell(new Phrase(String.valueOf(counter++), bodyFont)));
            table.addCell(new PdfPCell(new Phrase(st != null ? st.getRollNumber() : "", bodyFont)));
            table.addCell(new PdfPCell(new Phrase(st != null ? st.getName() : "", bodyFont)));
            table.addCell(new PdfPCell(new Phrase(st != null && st.getParentMobileNumber() != null ? st.getParentMobileNumber() : "N/A", bodyFont)));
            table.addCell(new PdfPCell(new Phrase(map.get("attendedClasses") + " / " + map.get("totalClasses"), bodyFont)));

            PdfPCell pctCell = new PdfPCell(new Phrase((String) map.get("percentageFormatted"), bodyFont));
            pctCell.setBackgroundColor(new BaseColor(255, 230, 230));
            pctCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(pctCell);
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }
}
