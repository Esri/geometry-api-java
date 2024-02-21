
package com.esri.core.geometry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

/**
 * This class allows to do branch covering measurement more easily
 * Guide on how to use this class :
 * (1) At the begining of your function, create an instance of this class and specify the number of branches
 *      and a unique id for this function (function name would work).
 * (2) Add a "branchCover.add(branchId)" for each new branch (if, else, for ...)
 * (3) Before each exit point, add a "branchCover.saveResults()" so the result from each execution is being saved
 * (4) You can now run all the tests of the project
 * (5) Then add a line with your unique id in ResultPrinter and run its main function. You'll find the results in
 *      "yourUniqueId.log"
 */
public class BranchCover {
    
    private boolean[] ica;
    private String name;
    
    public BranchCover(int branchCount, String name){
        ica = new boolean[branchCount];
        this.name = name;
    }

    public void add(final int index){
        ica[index] = true;
    }

	@Override
    public String toString() {
        String str = "";
        double coverage = 0; 
        for(int i = 0; i < ica.length; i++) {
            str = str + "\n" + i + " " + ica[i];
        } 

        for(int i = 0; i < ica.length; i++) {
            if(ica[i])
                coverage += 1;
        } 

        double ratio = coverage / ica.length;

        return str +  "\n\nBranch Coverage: " + (int)coverage + "/" + ica.length + " |  " + ratio*100 +"%\n" ;
    } 

    public String resultsAsString(){
        StringBuilder builder = new StringBuilder();
        for(boolean b : ica){
            builder.append(" ").append(b);
        }
        return builder.append("\n").toString();
    }

    public boolean saveResults(){
        File file = new File(name +".bc");
        try{
            FileOutputStream outputStream = new FileOutputStream(file, true);
            outputStream.write(resultsAsString().getBytes());
            outputStream.close();
        }catch(Exception e){
            return false;
        }
        return false;

    }

    public static String resultsFromName(String name){
        String results;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(name + ".bc"));
            
            String line = reader.readLine();
            String[] strings = line.split(" ");
            int length = strings.length;

            boolean[] branches = new boolean[length];
            while(line != null){
                strings = line.split(" ");
                for(int i = 0; i < length; i++){
                    branches[i] = branches[i] || Boolean.parseBoolean(strings[i]);
                }

                line = reader.readLine();
                
            }

            StringBuilder resultsBuilder = new StringBuilder("Results : \n");
            int count = 0;
            for(int i = 1; i < length; i++){
                if(branches[i]) count++;
                resultsBuilder.append(i-1).append(" : ").append(branches[i]).append("\n");
            }

            resultsBuilder.append("Coverage : ")
                .append(count)
                .append(" / ")
                .append(length-1)
                .append(", ")
                .append(((float)count*100)/(length-1))
                .append(" %");
            
            
            reader.close();

            results = resultsBuilder.toString();
            
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
        
        return results;
    }

    public static void resultFileFromName(String name){
        File file = new File(name +".log");
        try{
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(resultsFromName(name).getBytes());
            outputStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}



