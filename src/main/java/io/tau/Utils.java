package io.tau;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.swig.entry;
import com.frostwire.jlibtorrent.swig.byte_vector;
import com.frostwire.jlibtorrent.Vectors;

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

        StringBuilder sb = new StringBuilder();

        sb.append(bytes[0] & 0xFF);
        sb.append(".");
        sb.append(bytes[1] & 0xFF);
        sb.append(".");
        sb.append(bytes[2] & 0xFF);
        sb.append(".");
        sb.append(bytes[3] & 0xFF);
        sb.append(":");

        int port = bytes[0] & 0x000000FF;
        port |= (bytes[1] << 8) & 0x00000FF00;
        sb.append(port);

        return sb.toString();
    }

    public static short fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getShort();
    }

    public static Entry fromPreformattedBytes(byte[] data) {
        entry e = entry.from_preformatted_bytes(Vectors.bytes2byte_vector(data));
        return new Entry(e);
    }
}
