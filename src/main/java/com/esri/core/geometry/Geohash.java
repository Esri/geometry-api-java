package com.esri.core.geometry;

import java.security.InvalidParameterException;

/**
 * Helper class to work with geohash
 */
public class Geohash {

	private static final String base32 = "0123456789bcdefghjkmnpqrstuvwxyz";

	private static final String INVALID_CHARACTER_MESSAGE =
		"Invalid character in geohash: ";
	private static final String GEOHASH_EXCEED_MAX_PRECISION_MESSAGE =
		"Precision to high in geohash (max 24)";

	/**
     * Create an evelope from a given geohash
     * @param geoHash
     * @return The envelope that corresponds to the geohash
     * @throws InvalidParameterException if the precision of geoHash is greater than 24 characters
     */
    public static Envelope2D geohashToEnvelope(String geoHash) {
        if (geoHash.length() > 24) {
        	throw new InvalidParameterException(GEOHASH_EXCEED_MAX_PRECISION_MESSAGE);
        }

        long latBits = 0;
        long lonBits = 0;
        for (int i = 0; i < geoHash.length(); i++) {
        	int pos = base32.indexOf(geoHash.charAt(i));
        	if (pos == -1) {
        	    throw new InvalidParameterException(
        	    new StringBuilder(INVALID_CHARACTER_MESSAGE)
        	        .append('\'')
        	        .append(geoHash.charAt(i))
        	        .append('\'')
        	        .toString()
        	    );
        	}

        	if (i % 2 == 0) {
        	    lonBits =
        	    (lonBits << 3) | ((pos >> 2) & 4) | ((pos >> 1) & 2) | (pos & 1);
        	    latBits = (latBits << 2) | ((pos >> 2) & 2) | ((pos >> 1) & 1);
        	} else {
        	    latBits =
        	    (latBits << 3) | ((pos >> 2) & 4) | ((pos >> 1) & 2) | (pos & 1);
        	    lonBits = (lonBits << 2) | ((pos >> 2) & 2) | ((pos >> 1) & 1);
        	}
        }

    	int lonBitsSize = (int) Math.ceil(geoHash.length() * 5 / 2.0);
    	int latBitsSize = geoHash.length() * 5 - lonBitsSize;

    	long one = 1;

    	double lat = -90;
    	double latPrecision = 90;
    	for (int i = 0; i < latBitsSize; i++) {
    	  if (((one << (latBitsSize - 1 - i)) & latBits) != 0) {
    	    lat += latPrecision;
    	  }
    	  latPrecision /= 2;
    	}

    	double lon = -180;
    	double lonPrecision = 180;
    	for (int i = 0; i < lonBitsSize; i++) {
    	  if (((one << (lonBitsSize - 1 - i)) & lonBits) != 0) {
    	    lon += lonPrecision;
    	  }
    	  lonPrecision /= 2;
    	}

    	return new Envelope2D(
			lon,
			lat,
			lon + lonPrecision * 2,
			lat + latPrecision * 2
			);
    }

	/**
	 * Computes the geohash that contains a point at a certain precision
	 * @param pt A point represented as lat/long pair
	 * @param characterLength - The precision of the geohash
	 * @return The geohash of containing pt as a String
	 */
	public static String toGeohash(Point2D pt, int characterLength) {
		if (characterLength < 1) {
			throw new InvalidParameterException(
				"CharacterLength cannot be less than 1"
			);
		}
		if (characterLength > 24) {
			throw new InvalidParameterException(GEOHASH_EXCEED_MAX_PRECISION_MESSAGE);
		}

		int precision = 63;
		double lat = pt.y;
		double lon = pt.x;
		long latBit = Geohash.convertToBinary(
			lat,
			new double[] { -90, 90 },
			precision
		);

		long lonBit = Geohash.convertToBinary(
			lon,
			new double[] { -180, 180 },
			precision
		);

		return Geohash
			.binaryToBase32(lonBit, latBit, precision)
			.substring(0, characterLength);
	}

	/**
	 * Computes the base32 value of the binary string given
	 * @param lonBits (long) longtitude bits
	 * @param latBits (long) latitude bits
	 * @param len (int) number of bits
	 * @return base32 string of the binStr in chunks of 5 binary digits
	 */

	private static String binaryToBase32(long lonBits, long latBits, int len) {
		StringBuilder base32Str = new StringBuilder();
		int i = len - 1;
		long curr = 1;
		int currLen = 0;
		while (i >= 0) {
			long currLon = (lonBits >>> i) & 1;
			long currLat = (latBits >>> i) & 1;
			if (currLen >= 5) {
				base32Str.append(base32.charAt((int) (curr & 0x1F)));
				curr = 1;
				currLen = 0;
			}
			curr = (curr << 1) | currLon;
			currLen++;
			if (currLen >= 5) {
				base32Str.append(base32.charAt((int) (curr & 0x1F)));
				curr = 1;
				currLen = 0;
			}
			curr = (curr << 1) | currLat;
			currLen++;

			i--;
		}

		return base32Str.toString();
	}

	/**
	 * Converts the value given to a binary string with the given precision and range
	 * @param value (double) The value to be converted to a binary number
	 * @param r (double[]) The range at which the value is to be compared with
	 * @param precision (int) The Precision (number of bits) that the binary number needs
	 * @return (long) A binary number of the value with the given range and precision
	 */

	private static long convertToBinary(double value, double[] r, int precision) {
		long binVal = 1;
		for (int i = 0; i < precision; i++) {
			double mid = (r[0] + r[1]) / 2;
			if (value >= mid) {
				binVal = binVal << 1;
				binVal = binVal | 1;
				r[0] = mid;
			} else {
				binVal = binVal << 1;
				r[1] = mid;
			}
		}
		return binVal;
	}

	/**
	 * Compute the longest geohash that contains the envelope
	 * @param envelope
	 * @return the geohash as a string
	 */
	public static String containingGeohash(Envelope2D envelope) {
		double posMinX = envelope.xmin + 180;
		double posMaxX = envelope.xmax + 180;
		double posMinY = envelope.ymin + 90;
		double posMaxY = envelope.ymax + 90;
		int chars = 0;
		double xmin = 0;
		double xmax = 0;
		double ymin = 0;
		double ymax = 0;
		double deltaLon = 360;
		double deltaLat = 180;

		while (xmin == xmax && ymin == ymax && chars < 25) {
			if (chars % 2 == 0) {
				deltaLon = deltaLon / 8;
				deltaLat = deltaLat / 4;
			} else {
				deltaLon = deltaLon / 4;
				deltaLat = deltaLat / 8;
			}

			xmin = Math.floor(posMinX / deltaLon);
			xmax = Math.floor(posMaxX / deltaLon);
			ymin = Math.floor(posMinY / deltaLat);
			ymax = Math.floor(posMaxY / deltaLat);

			chars++;
		}

		if (chars == 1) return "";

		return toGeohash(new Point2D(envelope.xmin, envelope.ymin), chars - 1);
	}

	/**
	 *
	 * @param envelope
	 * @return up to four geohashes that completely cover given envelope
	 */
	public static String[] coveringGeohash(Envelope2D envelope) {
		double xmin = envelope.xmin;
		double ymin = envelope.ymin;
		double xmax = envelope.xmax;
		double ymax = envelope.ymax;

		if (NumberUtils.isNaN(xmax)) {
			return new String[] { "" };
		}
		String[] geoHash = { containingGeohash(envelope) };
		if (geoHash[0] != "") {
			return geoHash;
		}

		int grid = 45;
		int gridMaxLon = (int) Math.floor(xmax / grid);
		int gridMinLon = (int) Math.floor(xmin / grid);
		int gridMaxLat = (int) Math.floor(ymax / grid);
		int gridMinLat = (int) Math.floor(ymin / grid);
		int deltaLon = gridMaxLon - gridMinLon + 1;
		int deltaLat = gridMaxLat - gridMinLat + 1;
		String[] geoHashes = new String[deltaLon * deltaLat];

		if (deltaLon * deltaLat > 4) {
			return new String[] { "" };
		} else {
			for (int i = 0; i < deltaLon; i++) {
				for (int j = 0; j < deltaLat; j++) {
					Point2D p = new Point2D(xmin + i * grid, ymin + j * grid);
					geoHashes[i * deltaLat + j] = toGeohash(p, 1);
				}
			}
		}
		return geoHashes;
	}
}
