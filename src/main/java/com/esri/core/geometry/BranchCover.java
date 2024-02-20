
package com.esri.core.geometry;

public class BranchCover {
    
    private Boolean[] ica;
    private int length;
    private static BranchCover instance;

    public static BranchCover getInstance() {
        if (instance == null) {
                if (instance == null) {
                    instance = new BranchCover();
            }
        }
        return instance;
    }

    private BranchCover() {
        ica = new Boolean[1000];
        for(int i = 0; i < 1000; i++) {
            ica[i] = false;
        }
    }

    public void add(final int index){
        ica[index] = true;
    }

	public void setLength(int length) {
		this.length = length;
	}

	@Override
    public String toString() {
        // TODO Auto-generated method stub
        String str = "";
        double coverage = 0; 
        for(int i = 0; i < this.length; i++) {
            str = str + "\n" + i + " " + ica[i].toString();
        } 

        for(int i = 0; i < this.length; i++) {
            if(ica[i])
                coverage += 1;
        } 

        double ratio = coverage / this.length;

        return str +  "\n\nBranch Coverage: " + (int)coverage + "/" + this.length + " |  " + ratio*100 +"%\n" ;
    } 
}



