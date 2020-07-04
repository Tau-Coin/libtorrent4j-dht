package io.tau;

import java.nio.ByteBuffer;

public final class Utils {
	private Utils(){}

	public static String toHex(byte[] bytes) {
		final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for ( int j = 0; j < bytes.length; j++ ) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] fromHex(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

    public static String endpointFromHex(String s) {
        byte[] bytes = fromHex(s);

        String result = "";
        for (int i = 0; i < 4; i++) {
            if (i != 3) {
                result += String.valueOf(Byte.toUnsignedInt(bytes[i])) + ".";
            } else {
                result += String.valueOf(Byte.toUnsignedInt(bytes[i]));
            }
        }

        byte[] portBytes = new byte[2];
        System.arraycopy(bytes, 4, portBytes, 0, 2);
        result += ":" + fromByteArray(portBytes);

        return result;
    }

    public static short fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getShort();
    }
}
