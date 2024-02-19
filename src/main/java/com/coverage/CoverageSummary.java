package com.coverage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CoverageSummary{
    
    private void checkCoverageFromFile(String filePath, int numberOfBranches, String methodName) {
        System.out.println("--------Checking coverage for " + methodName + "--------");
        boolean[] booleanArray = new boolean[numberOfBranches]; // MAX_INDEX is the maximum index number
        File tempFile = new File(filePath);
        
        try (BufferedReader br = new BufferedReader(new FileReader(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                int index = Integer.parseInt(line.trim()); // Parse the index number from the file
                booleanArray[index] = true; // Set the corresponding index in the boolean array to true
            }
        } catch (IOException e) {
            System.err.println("Error reading from file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in file: " + e.getMessage());
        }
        
        // Now the booleanArray contains true values at the indices read from the file
        int uncheckedBranches = 0;
        for (int i = 0; i < numberOfBranches; i++) {
            if (!booleanArray[i]) {
                uncheckedBranches++;
                System.out.println("Branch number " + String.valueOf(i) + " is not covered.");
            }
        }
        if (uncheckedBranches == 0) {
            System.out.println("All branches are covered! :)");
        } else {
            System.out.println("Not all branches are checked");
            System.out.println("Total unchecked branches: " + String.valueOf(uncheckedBranches));
            System.out.println("Total checked branches: " + String.valueOf(numberOfBranches - uncheckedBranches));
        }
        tempFile.delete();
    }
    public static void main(String[] args) {
        CoverageSummary cs = new CoverageSummary();
        cs.checkCoverageFromFile("target/temp/coverage_geodesic_distance_ngs.txt", 77, "geodesic_distance_ngs");
    }
        
}
