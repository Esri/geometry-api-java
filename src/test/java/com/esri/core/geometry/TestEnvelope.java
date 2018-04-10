/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.esri.core.geometry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestEnvelope
{
    @Test
    public void testIntersect()
    {
        assertIntersection(new Envelope(0, 0, 5, 5), new Envelope(0, 0, 5, 5), new Envelope(0, 0, 5, 5));
        assertIntersection(new Envelope(0, 0, 5, 5), new Envelope(1, 1, 6, 6), new Envelope(1, 1, 5, 5));
        assertIntersection(new Envelope(1, 2, 3, 4), new Envelope(0, 0, 2, 3), new Envelope(1, 2, 2, 3));

        assertNoIntersection(new Envelope(), new Envelope());
        assertNoIntersection(new Envelope(0, 0, 5, 5), new Envelope());
        assertNoIntersection(new Envelope(), new Envelope(0, 0, 5, 5));
    }

    private static void assertIntersection(Envelope envelope, Envelope other, Envelope intersection)
    {
        boolean intersects = envelope.intersect(other);
        assertTrue(intersects);
        assertEquals(envelope, intersection);
    }

    private static void assertNoIntersection(Envelope envelope, Envelope other)
    {
        boolean intersects = envelope.intersect(other);
        assertFalse(intersects);
        assertTrue(envelope.isEmpty());
    }
}
