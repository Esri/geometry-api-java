
package com.esri.core.geometry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;


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

    
}



