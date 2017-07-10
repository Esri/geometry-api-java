/*
 Copyright 1995-2017 Esri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts Dept
 380 New York Street
 Redlands, California, USA 92373

 email: contracts@esri.com
 */

package com.esri.core.geometry;

import java.io.IOException;
import java.nio.ByteBuffer;
import junit.framework.TestCase;
import org.junit.Test;

//import com.vividsolutions.jts.io.WKBReader;

public class TestWKBSupport extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testWKB() {
		// JSON -> GEOM -> WKB
	
		String strPolygon1 = "{\"xmin\":-1.1663479012889031E7,\"ymin\":4919777.494405342,\"xmax\":-1.1658587043078788E7,\"ymax\":4924669.464215587,\"spatialReference\":{\"wkid\":102100}}";
		// String strPolygon1 =
		// "{\"rings\":[[[-119.152450421001,38.4118009590513],[-119.318825070203,38.5271086243914],[-119.575687062955,38.7029101298904],[-119.889341639399,38.9222515603984],[-119.995254694357,38.9941061536377],[-119.995150114198,39.0634913594691],[-119.994541258334,39.1061318056708],[-119.995527335641,39.1587132866355],[-119.995304181493,39.3115454332125],[-119.996011479298,39.4435009764511],[-119.996165311172,39.7206108077274],[-119.996324660047,41.1775662656441],[-119.993459369715,41.9892049531992],[-119.351692186077,41.9888529749781],[-119.3109421304,41.9891353872811],[-118.185316829038,41.9966370981387],[-117.018864363596,41.9947941808341],[-116.992313337997,41.9947945094663],[-115.947544658193,41.9945994628997],[-115.024862911148,41.996506455953],[-114.269471632824,41.9959242345073],[-114.039072662345,41.9953908974688],[-114.038151248682,40.9976868405942],[-114.038108189376,40.1110466529553],[-114.039844684228,39.9087788600023],[-114.040105338584,39.5386849268845],[-114.044267501155,38.6789958815881],[-114.045090206153,38.5710950539539],[-114.047272999176,38.1376524399918],[-114.047260595159,37.5984784866001],[-114.043939384154,36.9965379371421],[-114.043716435713,36.8418489458647],[-114.037392074194,36.2160228969702],[-114.045105557286,36.1939778840226],[-114.107775185788,36.1210907070504],[-114.12902308363,36.041730493896],[-114.206768869568,36.0172554164834],[-114.233472615347,36.0183310595897],[-114.307587598189,36.0622330993643],[-114.303857056018,36.0871084040611],[-114.316095374696,36.1114380366653],[-114.344233941709,36.1374802520568],[-114.380803116644,36.1509912717765],[-114.443945697733,36.1210532841897],[-114.466613475422,36.1247112590539],[-114.530573568745,36.1550902046725],[-114.598935242024,36.1383354528834],[-114.621610747198,36.1419666834504],[-114.712761724737,36.1051810523675],[-114.728150311069,36.0859627711604],[-114.728966012834,36.0587530361083],[-114.717673567756,36.0367580437018],[-114.736212493583,35.9876483502758],[-114.699275906446,35.9116119537412],[-114.661600122152,35.8804735854242],[-114.662462095522,35.8709599070091],[-114.689867343369,35.8474424944766],[-114.682739704595,35.7647034175617],[-114.688820027649,35.7325957399896],[-114.665091345861,35.6930994107107],[-114.668486064922,35.6563989882404],[-114.654065925137,35.6465840800053],[-114.6398667219,35.6113485698329],[-114.653134321223,35.5848331056108],[-114.649792053474,35.5466373866597],[-114.672215155693,35.5157541647721],[-114.645396168451,35.4507608261463],[-114.589584275424,35.3583787306827],[-114.587889840369,35.30476812919],[-114.559583045727,35.2201828714608],[-114.561039964054,35.1743461616313],[-114.572255261053,35.1400677445931],[-114.582616239058,35.1325604694085],[-114.626440825485,35.1339067529872],[-114.6359090842,35.1186557767895],[-114.595631971944,35.0760579746697],[-114.633779872695,35.0418633504303],[-114.621068606189,34.9989144286133],[-115.626197382816,35.7956983148418],[-115.88576934392,36.0012259572723],[-117.160423771838,36.9595941441767],[-117.838686423167,37.457298239715],[-118.417419755966,37.8866767486211],[-119.152450421001,38.4118009590513]]], \"spatialReference\":{\"wkid\":4326}}";
	
		MapGeometry mapGeom = GeometryEngine.jsonToGeometry(strPolygon1);
		Geometry geom = mapGeom.getGeometry();
		OperatorExportToWkb operatorExport = (OperatorExportToWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToWkb);
		ByteBuffer byteBuffer = operatorExport.execute(0, geom, null);
		byte[] wkb = byteBuffer.array();
	
		// WKB -> GEOM -> JSON
		OperatorImportFromWkb operatorImport = (OperatorImportFromWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ImportFromWkb);
		geom = operatorImport.execute(0, Geometry.Type.Polygon,
				ByteBuffer.wrap(wkb), null);
		// geom = operatorImport.execute(0, Geometry.Type.Polygon,
		// byteBuffer);
		String outputPolygon1 = GeometryEngine.geometryToJson(-1, geom);
	}

	@Test
	public void testWKB2() throws Exception {
		// JSON -> GEOM -> WKB

		// String strPolygon1 =
		// "{\"xmin\":-1.16605115291E7,\"ymin\":4925189.941699997,\"xmax\":-1.16567772126E7,\"ymax\":4928658.771399997,\"spatialReference\":{\"wkid\":102100}}";
		String strPolygon1 = "{\"rings\" : [ [ [-1.16605115291E7,4925189.941699997], [-1.16567772126E7,4925189.941699997], [-1.16567772126E7,4928658.771399997], [-1.16605115291E7,4928658.771399997], [-1.16605115291E7,4925189.941699997] ] ], \"spatialReference\" : {\"wkid\" : 102100}}";

		MapGeometry mapGeom = GeometryEngine.jsonToGeometry(strPolygon1);
		Geometry geom = mapGeom.getGeometry();

		// simplifying geom
		OperatorSimplify operatorSimplify = (OperatorSimplify) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.Simplify);
		SpatialReference sr = SpatialReference.create(102100);
		geom = operatorSimplify.execute(geom, sr, true, null);

		OperatorExportToWkb operatorExport = (OperatorExportToWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ExportToWkb);
		ByteBuffer byteBuffer = operatorExport.execute(0, geom, null);
		byte[] wkb = byteBuffer.array();

		// // checking WKB correctness
		// WKBReader jtsReader = new WKBReader();
		// com.vividsolutions.jts.geom.Geometry jtsGeom = jtsReader.read(wkb);
		// System.out.println("jtsGeom = " + jtsGeom);

		// WKB -> GEOM -> JSON
		OperatorImportFromWkb operatorImport = (OperatorImportFromWkb) OperatorFactoryLocal
				.getInstance().getOperator(Operator.Type.ImportFromWkb);
		geom = operatorImport.execute(0, Geometry.Type.Polygon,
				ByteBuffer.wrap(wkb), null);
		assertTrue(!geom.isEmpty());
		// geom = operatorImport.execute(0, Geometry.Type.Polygon, byteBuffer);
		// String outputPolygon1 = GeometryEngine.geometryToJson(-1, geom);
		// System.out.println(strPolygon1);
		// System.out.println(outputPolygon1);

	}

}
