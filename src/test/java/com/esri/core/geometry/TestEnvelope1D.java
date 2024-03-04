package com.esri.core.geometry;

import junit.framework.TestCase;
import org.junit.Test;
public class TestEnvelope1D extends TestCase{
    @Test
    public void testCalculateToleranceFromEnvelopeEmpty() {
        Envelope1D envelope = new Envelope1D();
        envelope.setEmpty();
        double tolerance = envelope._calculateToleranceFromEnvelope();
        assertEquals(100.0 * NumberUtils.doubleEps(), tolerance, 0.0001);
    }

    @Test
    public void testCalculateToleranceFromEnvelopeNonEmpty() {
        Envelope1D envelope = new Envelope1D(2.0, 4.0);
        double tolerance = envelope._calculateToleranceFromEnvelope();
        assertEquals(2.220446049250313e-14, tolerance, 1e-10);
    }


}
