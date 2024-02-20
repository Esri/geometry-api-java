package com.esri.core.geometry;


public class ResultPrinter{

    public static void main(String[] args) {
        BranchCover.resultFileFromName("exportEnvelopeToWKB");
        BranchCover.resultFileFromName("importFromESRIShape");
        BranchCover.resultFileFromName("importFromGeoJsonImpl");
    }

}