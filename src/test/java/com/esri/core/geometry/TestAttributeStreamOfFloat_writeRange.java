package com.esri.core.geometry;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
/**
 * WriteRange has complexity of 17
 * @author joele
 */
public class TestAttributeStreamOfFloat_writeRange {

	AttributeStreamOfFloat lc;
	@Before
	public void setUp() throws Exception {
		lc = new AttributeStreamOfFloat(5,1);
	}
	
	private float[] readBuffer(int size, int offset){
		float[] f = new float[size-offset];
		for(int i = offset,j=0; i < size; i++,j++){
			f[j] = lc.read(i);
		}
		return f;
	}
	
	@Test(expected = IllegalArgumentException.class)  
	public void testIllegalArgs1(){
		lc.writeRange(-2, 1, null, 0, true, 0);	//startelement < 0 should throw illegalargumentexception
	}
	
	@Test(expected = IllegalArgumentException.class)  
	public void testIllegalArgs2(){
		lc.writeRange(5, 3, null, 0, false, 2);	// !bForward and count%stride != 0, so should throw illegalargumentexception
	}
	
	/**
	 * Write forwards
	 */
	@Test
	public void testWrite1(){
		AttributeStreamOfFloat tmp = new AttributeStreamOfFloat(3,2);
		float[] fl = {(float) 1.0,(float) 1.0,(float) 1.0,(float) 1.0,(float) 1.0};
		assertArrayEquals(readBuffer(5,0), fl, (float)0.1);
		lc.writeRange(2, 2, tmp, 0, true, 1);					//should change index 3 and 4 to 2 instead of 1
		float[] fl2 = {(float) 1.0,(float) 1.0,(float) 2.0,(float) 2.0,(float) 1.0};
		assertArrayEquals(readBuffer(5,0), fl2, (float)0.01);
	}
	
	/**
	 * Write backwards
	 */
	@Test
	public void testWrite2(){
		AttributeStreamOfFloat tmp = new AttributeStreamOfFloat(3,1);
		tmp.write(0, (float)0);
		tmp.write(1, (float)1);
		tmp.write(2, (float)2);
		float[] fl = {(float)2,(float)1,(float)0};
		lc = new AttributeStreamOfFloat(3);
		lc.writeRange(0, 3, tmp, 0, false, 1);
		assertArrayEquals(fl, readBuffer(3, 0), (float)0.01);
	}

	

}
