package com.jitendersingh.friendsengineer;

import android.content.Context;
import android.util.Log;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.File;
import java.io.InputStream;

public class SalarySlipPdfGenerator {

    private static final String TAG = "SalarySlipPdf";

    private static final DeviceRgb BLACK = new DeviceRgb(0, 0, 0);
    private static final DeviceRgb RED   = new DeviceRgb(220, 0, 0);

    private PdfFont fontRegular;
    private PdfFont fontBold;

    private final Context context;

    public SalarySlipPdfGenerator(Context context) {
        this.context = context;
    }

    // =========================================================
    // DATA MODEL
    // =========================================================
    public static class SalaryData {
        public String mon1 = "", mon2 = "", year = "";
        public String name = "", fatherName = "", designation = "", department = "", doj = "";
        public String punchingNo = "", pfNo = "", esiNo = "", uanNo = "";
        public String tpd = "", nodw = "", wO = "", holiday = "", otH = "";
        public String basic = "", hra = "", convenience = "", cl = "", pl = "", bonus = "", gross = "";
        public String basicE = "", hraE = "", convenieceE = "", otE = "", clE = "", plE = "", bonusE = "", totalEarning = "";
        public String pfD = "", esiD = "", oteD = "", advanceD = "", tea = "", canteen = "", totalDeduction = "";
        public String netSalary = "", netSalaryWords = "";
        public String bankName = "", accountNo = "";
    }

    // =========================================================
    // MAIN GENERATE
    // =========================================================
    public void generate(File outputFile, SalaryData d) throws Exception {
        PdfWriter writer = new PdfWriter(outputFile);
        PdfDocument pdf  = new PdfDocument(writer);
        Document   doc   = new Document(pdf, PageSize.A4);
        doc.setMargins(20, 30, 20, 30);

        fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        addHeader(doc, d);
        addEmployeeInfo(doc, d);
        addSalaryRatesAndAttendance(doc, d);
        addEarningsAndDeductions(doc, d);
        addTotalsRow(doc, d);
        addNetSalary(doc, d);
        addSignatureArea(doc, d);

        doc.close();
        Log.d(TAG, "PDF generated: " + outputFile.getAbsolutePath());
    }

    // =========================================================
    // HEADER
    // Fix #1 (new): No border between logo and centre column at all.
    // =========================================================
    private void addHeader(Document doc, SalaryData d) throws Exception {

        float[] colWidths = {55f, 295f, 190f};
        Table header = new Table(UnitValue.createPointArray(colWidths));
        header.setWidth(UnitValue.createPercentValue(100));
        header.setBorder(new SolidBorder(BLACK, 1));

        // Col 1: Logo spans 2 rows — NO right border (fix #1 new: remove line between logo & centre)
        Cell logoCell = new Cell(2, 1)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(3);

        try {
            InputStream logoStream = context.getAssets().open("logo.png");
            byte[] b = new byte[logoStream.available()];
            logoStream.read(b);
            logoStream.close();
            Image logo = new Image(ImageDataFactory.create(b));
            logo.setWidth(48).setAutoScaleHeight(true);
            logo.setHorizontalAlignment(HorizontalAlignment.CENTER);
            logoCell.add(logo);
        } catch (Exception e) {
            logoCell.add(new Paragraph("Friends\nEngineers")
                    .setFont(fontBold).setFontSize(7).setTextAlignment(TextAlignment.CENTER));
            Log.w(TAG, "logo.png not found in assets");
        }
        header.addCell(logoCell);

        // Col 2 Row 1: Company name — no left border (merged look with logo)
        header.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderRight(new SolidBorder(BLACK, 1))
                .setBorderBottom(Border.NO_BORDER)
                .setPadding(3)
                .add(new Paragraph("FRIENDS ENGINEERS")
                        .setFont(fontBold).setFontSize(13)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(0)));

        // Col 3 Row 1: FORM XIX
        header.addCell(new Cell()
                .setBorder(new SolidBorder(BLACK, 1))
                .setPadding(3)
                .add(new Paragraph("FORM XIX\nSEE RULE 78(1)(b)")
                        .setFont(fontBold).setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER)));

        // Col 2 Row 2: Address
        header.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderRight(new SolidBorder(BLACK, 1))
                .setBorderBottom(Border.NO_BORDER)
                .setPadding(3)
                .add(new Paragraph("4-G-6 R.H.B BHIWADI, ALWAR RAJASTHAN")
                        .setFont(fontRegular).setFontSize(7.5f)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1))
                .add(new Paragraph("MOB.: 9680584205  E-mail: friendseng@ymail.com")
                        .setFont(fontRegular).setFontSize(7.5f)
                        .setTextAlignment(TextAlignment.CENTER)));

        // Col 3 Row 2: Wage slip — one liner
        String monthLine = "WAGE SLIP FOR THE MONTH OF- " + d.mon1 + "-" + d.mon2 + " " + d.year;
        header.addCell(new Cell()
                .setBorder(new SolidBorder(BLACK, 1))
                .setPadding(3)
                .add(new Paragraph(monthLine)
                        .setFont(fontBold).setFontSize(7)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFixedLeading(10)));

        doc.add(header);
    }

    // =========================================================
    // HELPER: build a two-column inner box where label and value
    // are SEPARATED by a small transparent gap between them.
    // Each column (label block / value block) gets its own border box.
    // No row dividers inside each block.
    // Fix #2 (new): gap between label box and value box.
    // Fix #3 (new): box stretches full height.
    // =========================================================

    /**
     * Creates a 3-column layout cell to hold one label+value pair section:
     *   [label box] [gap spacer] [value box]
     *
     * labelWidth  – width of the label inner box
     * gapWidth    – transparent spacer between boxes (e.g. 4f)
     * valueWidth  – width of the value inner box
     * labels / values – parallel arrays; empty strings fill remaining rows
     * stretchRows – if true, adds empty rows so box fills parent height visually
     */
    private Table buildLabelValueTable(
            float labelWidth, float gapWidth, float valueWidth,
            String[] labels, String[] values) {

        // Outer wrapper: 3 cols — label | gap | value
        float[] cols = {labelWidth, gapWidth, valueWidth};
        Table wrapper = new Table(UnitValue.createPointArray(cols));
        wrapper.setWidth(UnitValue.createPercentValue(100));
        wrapper.setBorder(Border.NO_BORDER);

        int rows = labels.length;

        // ---- Label column (inner box) ----
        Table labelBox = new Table(UnitValue.createPointArray(new float[]{labelWidth - 2}));
        labelBox.setWidth(UnitValue.createPercentValue(100));
        labelBox.setBorder(new SolidBorder(BLACK, 0.8f));

        for (int i = 0; i < rows; i++) {
            labelBox.addCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(2)
                    .add(new Paragraph(labels[i]).setFont(fontRegular).setFontSize(7.5f)));
        }

        // ---- Value column (inner box) ----
        Table valueBox = new Table(UnitValue.createPointArray(new float[]{valueWidth - 2}));
        valueBox.setWidth(UnitValue.createPercentValue(100));
        valueBox.setBorder(new SolidBorder(BLACK, 0.8f));

        for (int i = 0; i < rows; i++) {
            String v = (i < values.length) ? values[i] : "";
            valueBox.addCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(2)
                    .add(new Paragraph(v).setFont(fontRegular).setFontSize(7.5f)));
        }

        // Add label box cell
        wrapper.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .add(labelBox));

        // Transparent gap
        wrapper.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .add(new Paragraph(" ").setFontSize(1)));

        // Add value box cell
        wrapper.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .add(valueBox));

        return wrapper;
    }

    // =========================================================
    // EMPLOYEE INFO
    // =========================================================
    private void addEmployeeInfo(Document doc, SalaryData d) {

        float[] outerCols = {315f, 225f};
        Table outer = new Table(UnitValue.createPointArray(outerCols));
        outer.setWidth(UnitValue.createPercentValue(100));
        outer.setBorder(new SolidBorder(BLACK, 1));

        String[] leftLabels = {"EMPLOYEE NAME:", "FATHER'S NAME:", "DESIGNATION:", "DEPARTMENT:", "DATE OF JOINING:"};
        String[] leftValues = {d.name, d.fatherName, d.designation, d.department, d.doj};

        // Left: label box(105) | gap(5) | value box(195)
        Table leftSection = buildLabelValueTable(105f, 5f, 195f, leftLabels, leftValues);

        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER)
                .setBorderRight(new SolidBorder(BLACK, 1))
                .setPadding(3)
                .add(leftSection));

        // Right: 4 rows; pad with empty strings to match left height (fix #3)
        String[] rightLabels = {"PUNCHING NO:", "PF NO.:", "ESI NO:", "UAN NO:", "", ""};
        String[] rightValues = {d.punchingNo, d.pfNo, d.esiNo, d.uanNo, "", ""};

        // Right: label box(88) | gap(4) | value box(118)
        Table rightSection = buildLabelValueTable(88f, 4f, 118f, rightLabels, rightValues);

        outer.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(3)
                .add(rightSection));

        doc.add(outer);
    }

    // =========================================================
    // SALARY RATES + ATTENDANCE
    // =========================================================
    private void addSalaryRatesAndAttendance(Document doc, SalaryData d) {

        float[] outerCols = {245f, 295f};
        Table outer = new Table(UnitValue.createPointArray(outerCols));
        outer.setWidth(UnitValue.createPercentValue(100));
        outer.setBorder(new SolidBorder(BLACK, 1));

        // Left: Basic→Gross (7 rows)
        String[] rateLabels = {"BASIC", "HRA", "CONVENIENCE", "CL", "PL", "BONUS", "GROSS"};
        String[] rateValues = {d.basic, d.hra, d.convenience, d.cl, d.pl, d.bonus, d.gross};

        // We need GROSS bold — build manually for that row
        float[] lCols = {100f, 5f, 130f};
        Table leftSection = new Table(UnitValue.createPointArray(lCols));
        leftSection.setWidth(UnitValue.createPercentValue(100));
        leftSection.setBorder(Border.NO_BORDER);

        Table rateLabelBox = new Table(UnitValue.createPointArray(new float[]{98f}));
        rateLabelBox.setWidth(UnitValue.createPercentValue(100));
        rateLabelBox.setBorder(new SolidBorder(BLACK, 0.8f));

        Table rateValueBox = new Table(UnitValue.createPointArray(new float[]{128f}));
        rateValueBox.setWidth(UnitValue.createPercentValue(100));
        rateValueBox.setBorder(new SolidBorder(BLACK, 0.8f));

        for (int i = 0; i < rateLabels.length; i++) {
            boolean gross = rateLabels[i].equals("GROSS");
            rateLabelBox.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(2)
                    .add(new Paragraph(rateLabels[i])
                            .setFont(gross ? fontBold : fontRegular).setFontSize(7.5f)));
            rateValueBox.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(2)
                    .add(new Paragraph(rateValues[i])
                            .setFont(gross ? fontBold : fontRegular).setFontSize(7.5f)));
        }

        leftSection.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(rateLabelBox));
        leftSection.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0)
                .add(new Paragraph(" ").setFontSize(1)));
        leftSection.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(rateValueBox));

        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER).setBorderTop(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER).setBorderRight(new SolidBorder(BLACK, 1))
                .setPadding(3).add(leftSection));

        // Right: attendance 5 rows, pad to 7 rows to match left height (fix #3)
        String[] attLabels = {"TOTAL PAYABLE DAYS:", "NO. OF DAYS WORKED:", "WEEKLY OFF:", "HOLIDAY:", "OT HOURS:", "", ""};
        String[] attValues = {d.tpd, d.nodw, d.wO, d.holiday, d.otH, "", ""};

        Table rightSection = buildLabelValueTable(148f, 4f, 132f, attLabels, attValues);

        outer.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(3).add(rightSection));

        doc.add(outer);
    }

    // =========================================================
    // EARNINGS + DEDUCTIONS
    // Fix #4 (new): No column border between EARNING and DEDUCTIONS sides.
    // =========================================================
    private void addEarningsAndDeductions(Document doc, SalaryData d) {

        String[] earnLabels = {"BASIC", "HRA", "CONVENIENCE", "OT", "CL", "PL", "BONUS"};
        String[] earnValues = {d.basicE, d.hraE, d.convenieceE, d.otE, d.clE, d.plE, d.bonusE};
        String[] dedLabels  = {"PF(BASIC+CONV.)@12%", "ESI (GROSS)@0.75%", "OT ESI @ 0.75%", "ADVANCE", "TEA", "CANTEEN", ""};
        String[] dedValues  = {d.pfD, d.esiD, d.oteD, d.advanceD, d.tea, d.canteen, ""};

        // Single outer table, two halves
        float[] outerCols = {270f, 270f};
        Table outer = new Table(UnitValue.createPointArray(outerCols));
        outer.setWidth(UnitValue.createPercentValue(100));
        outer.setBorder(new SolidBorder(BLACK, 1));

        // Header row: EARNING | DEDUCTIONS — NO inner border between them (fix #4)
        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER).setBorderTop(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)           // <-- no divider
                .setBorderBottom(new SolidBorder(BLACK, 0.8f))
                .setPadding(3)
                .add(new Paragraph("EARNING")
                        .setFont(fontBold).setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER)));

        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER).setBorderTop(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BLACK, 0.8f))
                .setPadding(3)
                .add(new Paragraph("DEDUCTIONS")
                        .setFont(fontBold).setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER)));

        // EARNING data: label(110) gap(5) value(145)
        Table earnSection = buildLabelValueTable(110f, 5f, 145f, earnLabels, earnValues);

        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER).setBorderTop(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER).setBorderRight(Border.NO_BORDER) // no divider
                .setPadding(3).add(earnSection));

        // DEDUCTIONS data: label(145) gap(5) value(110)
        Table dedSection = buildLabelValueTable(145f, 5f, 110f, dedLabels, dedValues);

        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER).setBorderTop(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(3).add(dedSection));

        doc.add(outer);
    }

    // =========================================================
    // TOTALS ROW
    // Fix #5 (new): No column border between the two halves.
    //               Each label and value get their own separate box with gap.
    // =========================================================
    private void addTotalsRow(Document doc, SalaryData d) {

        float[] outerCols = {270f, 270f};
        Table outer = new Table(UnitValue.createPointArray(outerCols));
        outer.setWidth(UnitValue.createPercentValue(100));
        outer.setBorder(new SolidBorder(BLACK, 1));

        // Left: TOTAL EARNING label box | gap | value box
        Table leftSection = buildBoldLabelValueSingle(
                "TOTAL EARNING:", d.totalEarning, 115f, 5f, 140f);

        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER).setBorderTop(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER).setBorderRight(Border.NO_BORDER) // no divider (fix #5)
                .setPadding(3).add(leftSection));

        // Right: TOTAL DEDUCTION label box | gap | value box
        Table rightSection = buildBoldLabelValueSingle(
                "TOTAL DEDUCTION:", d.totalDeduction, 140f, 5f, 115f);

        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER).setBorderTop(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(3).add(rightSection));

        doc.add(outer);
    }

    /** Single-row label+value pair with their own boxes separated by a gap, bold text. */
    private Table buildBoldLabelValueSingle(
            String label, String value,
            float labelWidth, float gapWidth, float valueWidth) {

        float[] cols = {labelWidth, gapWidth, valueWidth};
        Table t = new Table(UnitValue.createPointArray(cols));
        t.setWidth(UnitValue.createPercentValue(100));
        t.setBorder(Border.NO_BORDER);

        // Label box
        Table lBox = new Table(UnitValue.createPointArray(new float[]{labelWidth - 2}));
        lBox.setWidth(UnitValue.createPercentValue(100));
        lBox.setBorder(new SolidBorder(BLACK, 0.8f));
        lBox.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(3)
                .add(new Paragraph(label).setFont(fontBold).setFontSize(8)));

        // Value box
        Table vBox = new Table(UnitValue.createPointArray(new float[]{valueWidth - 2}));
        vBox.setWidth(UnitValue.createPercentValue(100));
        vBox.setBorder(new SolidBorder(BLACK, 0.8f));
        vBox.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(3)
                .add(new Paragraph(value).setFont(fontBold).setFontSize(8)));

        t.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(lBox));
        t.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0)
                .add(new Paragraph(" ").setFontSize(1)));
        t.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(vBox));

        return t;
    }

    // =========================================================
    // NET SALARY
    // One outer box. One inner box containing both rows.
    // No row or column dividers inside. Values in RED.
    // =========================================================
    private void addNetSalary(Document doc, SalaryData d) {

        Table outer = new Table(UnitValue.createPointArray(new float[]{540f}));
        outer.setWidth(UnitValue.createPercentValue(100));
        outer.setBorder(new SolidBorder(BLACK, 1));

        // Single inner box — 2 columns (label | value), 2 rows, zero borders inside
        float[] innerCols = {200f, 330f};
        Table inner = new Table(UnitValue.createPointArray(innerCols));
        inner.setWidth(UnitValue.createPercentValue(100));
        inner.setBorder(new SolidBorder(BLACK, 0.8f)); // the one visible inner box border

        // Row 1
        inner.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(4)
                .add(new Paragraph("NET SALARY IN AMOUNT:")
                        .setFont(fontBold).setFontSize(8)));
        inner.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(4)
                .add(new Paragraph(d.netSalary)
                        .setFont(fontBold).setFontSize(8).setFontColor(RED)));

        // Row 2
        inner.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(4)
                .add(new Paragraph("NET SALARY IN FIGURE:")
                        .setFont(fontBold).setFontSize(8)));
        inner.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(4)
                .add(new Paragraph(d.netSalaryWords)
                        .setFont(fontBold).setFontSize(8).setFontColor(RED)));

        outer.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(5)
                .add(inner));

        doc.add(outer);
    }

    // =========================================================
    // SIGNATURE AREA
    // Fix #7 (new): Signature image smaller — change sig.setWidth(60) to resize.
    // Fix #8 (new): No column border between left and right halves.
    //               Right side = one single box (bank + account + Signature label).
    //               Empty sub-box removed.
    // =========================================================
    private void addSignatureArea(Document doc, SalaryData d) {

        float[] outerCols = {270f, 270f};
        Table outer = new Table(UnitValue.createPointArray(outerCols));
        outer.setWidth(UnitValue.createPercentValue(100));
        outer.setBorder(new SolidBorder(BLACK, 1));

        // ---- LEFT: signature image + prepared-by in inner box ----
        Table leftInner = new Table(UnitValue.createPointArray(new float[]{260f}));
        leftInner.setWidth(UnitValue.createPercentValue(100));
        leftInner.setBorder(new SolidBorder(BLACK, 0.8f));

        // Mirror bankCell's padding structure so both sides align at the same vertical position.
        // setPaddingTop here should equal bankCell's setPaddingTop minus (sig height + marginTop).
        // With sig height=60 and marginTop=4, paddingTop = 55 - 60 - 4 = -9 → clamp to 0, so use 0.
        // Effectively: content starts at top, image+text together occupy the same bottom region as bank text.
        Cell sigContent = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(20)    // <-- CHANGE: match this to bankCell's setPaddingTop minus sig height (~60) minus 4
                .setPaddingBottom(6)
                .setPaddingLeft(6)
                .setPaddingRight(6)
                .setVerticalAlignment(VerticalAlignment.BOTTOM);

        try {
            InputStream sigStream = context.getAssets().open("signature.png");
            byte[] b = new byte[sigStream.available()];
            sigStream.read(b);
            sigStream.close();
            Image sig = new Image(ImageDataFactory.create(b));
            sig.setWidth(155);   // <-- CHANGE: width in points (155 ≈ width of "Prepared by..." text)
            sig.setHeight(60);   // <-- CHANGE: height in points
            sigContent.add(sig);
        } catch (Exception e) {
            sigContent.add(new Paragraph("\n").setFontSize(8));
            Log.w(TAG, "signature.png not found in assets");
        }

        sigContent.add(new Paragraph("Prepared by..... Jitendra Singh Rajawat")
                .setFont(fontRegular).setFontSize(7.5f).setMarginTop(4));

        leftInner.addCell(sigContent);

        // No right border between left and right halves (fix #8)
        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER).setBorderTop(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(3).add(leftInner));

        // ---- RIGHT: one single box with bank + account + Signature label ----
        // Uses large top padding so the text sits low, matching the left cell's height.
        Table rightInner = new Table(UnitValue.createPointArray(new float[]{260f}));
        rightInner.setWidth(UnitValue.createPercentValue(100));
        rightInner.setBorder(new SolidBorder(BLACK, 0.8f));

        Cell bankCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(55)   // <-- CHANGE 55 to adjust vertical position of right-side text
                .setPaddingBottom(6)
                .setPaddingLeft(6)
                .setPaddingRight(6)
                .setVerticalAlignment(VerticalAlignment.BOTTOM)
                .setTextAlignment(TextAlignment.CENTER);

        bankCell.add(new Paragraph(d.bankName)
                .setFont(fontRegular).setFontSize(7.5f).setTextAlignment(TextAlignment.CENTER));
        bankCell.add(new Paragraph(d.accountNo)
                .setFont(fontRegular).setFontSize(7.5f).setTextAlignment(TextAlignment.CENTER));
        bankCell.add(new Paragraph("Signature")
                .setFont(fontRegular).setFontSize(7.5f)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(8));

        rightInner.addCell(bankCell);

        outer.addCell(new Cell()
                .setBorderLeft(Border.NO_BORDER).setBorderTop(Border.NO_BORDER)
                .setBorderBottom(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(3).add(rightInner));

        doc.add(outer);
    }
}