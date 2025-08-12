package com.jitendersingh.friendsengineer;

public class PdfModel {
    private String documentId;
    private String pdfUrl;
    private String startDate;
    private String endDate;

    public PdfModel(String documentId, String pdfUrl, String startDate, String endDate) {
        this.documentId = documentId;
        this.pdfUrl = pdfUrl;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }
}
