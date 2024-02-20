
package com.esri.core.geometry;

public class BranchCover {
    
    private Boolean[] ica;
    private int length;
    private static BranchCover instance1;
    private static BranchCover instance2;
    private static BranchCover instance3;
    private static BranchCover instance4;
    private static BranchCover instance5;

    public static BranchCover getInstance1() {        
        if (instance1 == null) {
            instance1 = new BranchCover();
        }
        return instance1;
    }

    public static BranchCover getInstance2() {        
        if (instance2 == null) {
            instance2 = new BranchCover();
        }
        return instance2;
    }

    public static BranchCover getInstance3() {        
        if (instance3 == null) {
            instance3 = new BranchCover();
        }
        return instance3;
    }

    public static BranchCover getInstance4() {        
        if (instance4 == null) {
            instance4 = new BranchCover();
        }
        return instance4;
    }

    public static BranchCover getInstance5() {        
        if (instance5 == null) {
            instance5 = new BranchCover();
        }
        return instance5;
    }

    private BranchCover() {
        ica = new Boolean[1000];
        for(int i = 0; i < 1000; i++) {
            ica[i] = false;
        }
    }

    public void add(final int index){
        ica[index] = true;
        this.setLength(index + 1)
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



