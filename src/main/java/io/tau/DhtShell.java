package io.tau;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.frostwire.jlibtorrent.swig.*;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import static com.frostwire.jlibtorrent.SessionManager.MutableItem;

/**
 * @author gubatron
 * @author aldenml
 */
public final class DhtShell {

    // command list for help doc
    private static final String PutImmutableItem = "put <string value>: put immutable item";
    private static final String MSPutImmutableItem = "msput <string value>: put immutable item for all sessions";
    private static final String GetImmutableItem = "get <sha1>: get immutable item";

    private static final String GetPeers = "get_peers <sha1>: get peers which store sha1 value";

    private static final String GenKeyPair = "mkeys: generate ed25519 key pair";

    private static final String PutMutableItem = "mput <pub-key> <priv-key> <string value> <salt>: put mutable item";
   private static final String MSPutMutableItem = "msmput <pub-key> <priv-key> <string value> <salt>: put mutable item for all sessions";
    private static final String GutMutableItem = "mget <pub-key> <salt>: get mutable item";

    private static final String Count_Nodes = "count_nodes: count nodes of dht";

    private static final String Put_Bomb = "putbomb: put immutable item bombing";

    private static final String Batch_Put = "batchput <loop-number> <file>: put random string and write hash into <file>";

    private static final String Batch_Get = "batchget <file>: get item with sha1 hash from <file>";

    private static final String Unsync_Batch_Get = "unsync_batchget <file>: get item with sha1 hash from <file>";

    private static final String List_nodes = "list_nodes: list dht nodes";

    private static final String Direct_Request = "directReqest <ip:port> <string data>: directly send data";

    private static final String External_Address = "externalAddress: print nat public ip";

    private static final String Compress = "compress <rand bytes size> <test accounts>: compress tests";

    private static final String Quit = "quit: exit this application";

    private static final String Help = "commands list:" + "\n"
            + PutImmutableItem + "\n" + MSPutImmutableItem  + "\n" + GetImmutableItem + "\n"
	        + GetPeers + "\n" + GenKeyPair + "\n"
            + PutMutableItem + "\n" + MSPutMutableItem + "\n" + GutMutableItem + "\n"
	        + Count_Nodes + "\n" + Put_Bomb + "\n" + List_nodes + "\n"
            + Batch_Put + "\n" + Batch_Get + "\n"
            + Direct_Request + "\n" + External_Address + "\n"
            + Compress + "\n" + Quit + "\n";

    private static final SimpleDateFormat LogTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String sUndefinedEntry = entry.data_type.undefined_t.toString();

    private static final int Sessions_Count = 1;

    private static final UdpEndpoint TestEP = new UdpEndpoint("13.229.53.249", 8661);

    static class PutCallback {

        long start;
        long end;

        public PutCallback() {
            this.start = System.nanoTime();
        }
    }

    static class GetCallback {

        long start;
        long end;

        public GetCallback() {
            this.start = System.nanoTime();
        }
    }

    // put cache
    private static Map<String, PutCallback> immutablePutCache = Collections.synchronizedMap(
            new HashMap<String, PutCallback>());

    private static Map<String, GetCallback> immutableGetCache = Collections.synchronizedMap(
            new HashMap<String, GetCallback>());

    static class GetTracker {

        BigInteger timeCost;

        int total;
        int failed;
        int success;

        public GetTracker() {
            this.timeCost = BigInteger.ZERO;
            this.total = 0;
            this.failed = 0;
            this.success = 0;
        }
    }

    private static GetTracker sGetTracker = new GetTracker();

    public static void main(String[] args) throws Throwable {

        AlertListener mainListener = new AlertListener() {
            @Override
            public int[] types() {
                return null;
            }

            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();

                if (type == AlertType.DHT_LOG) {
                    DhtLogAlert a = (DhtLogAlert) alert;
                    log(a.message());
                }

                /*
                if (type == AlertType.LOG) {
                    LogAlert a = (LogAlert) alert;
                    log(a.message());
                }

                if (type == AlertType.DHT_PKT) {
                    DhtPktAlert a = (DhtPktAlert) alert;
                    log(a.message());

                    print("pkt content:" + new String(a.pktBuf()));
                }
				*/
                if (type == AlertType.LISTEN_SUCCEEDED) {
                    ListenSucceededAlert a = (ListenSucceededAlert) alert;
                    log(a.message());
                }

                if (type == AlertType.LISTEN_FAILED) {
                    ListenFailedAlert a = (ListenFailedAlert) alert;
                    log(a.message());
                }

		        if (type == AlertType.DHT_MUTABLE_ITEM) {
		            DhtMutableItemAlert a = (DhtMutableItemAlert) alert;
		            log(a.message());
		        }

		        if (type == AlertType.DHT_IMMUTABLE_ITEM) {
		            DhtImmutableItemAlert a = (DhtImmutableItemAlert) alert;
		            log(a.message());
                    handleImmutableGot(a);
		        }

                if (type == AlertType.DHT_PUT) {
                    DhtPutAlert a = (DhtPutAlert) alert;
                    log(a.message());

                    // statistic put time cost
                    if (immutablePutCache.size() > 0) {
                        Sha1Hash sha1 = a.target();
                        if (sha1 != null) {
                            PutCallback cb = immutablePutCache.get(sha1.toString());
                            cb.end = System.nanoTime();
                            print(sha1.toString() + ", timecost:" + (cb.end - cb.start) / 1000 + "ms");
                        }
                    }
                }
            }
        };

        List<SessionManager> sessions = new ArrayList<>();
        for (int i = 0; i < Sessions_Count; i++) {
                SessionManager session = new SessionManager();
                session.addListener(mainListener);
                session.start(SessionSettings.getTauSessionParams());
                sessions.add(session);
        }

        SessionManager s = sessions.get(0);

        /*
        Thread getThread = new Thread(new Runnable() {
            @Override
            public void run() {
                print("wait for 10m...");
                try {
                    Thread.sleep(600 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                getbomb(s, "getbomb nihao_nancha");
            }
        });
        getThread.start();
        */

        Scanner in = new Scanner(System.in);
	    System.out.println("dht shell is ready");
        while (in.hasNextLine()) {
            //System.out.print("$ ");

            String line = in.nextLine().trim();

	        if (is_help(line)) {
                print(Help);
	        } else if (is_quit(line)) {
                quit(s);
            } else if (is_put(line)) {
                put(s, line);
            } else if (is_preformatPut(line)) {
                preformatPut(s, line);
            } else if (is_msput(line)) {
                msput(sessions, line);
            } else if (is_get(line)) {
                get(s, line);
            } else if (is_getbomb(line)) {
                getbomb(s, line);
            } else if (is_externalAddress(line)) {
                externalAddress(s);
            } else if (is_ping(line)) {
                ping(s, line);
            } else if (is_get_peers(line)) {
                get_peers(s, line);
            } else if (is_announce(line)) {
                announce(s, line);
            } else if (is_mkeys(line)) {
                mkeys(line);
            } else if (is_mput(line)) {
                mput(sessions, line);
            } else if (is_multi_mget(line)) {
                multi_mget(s, line);
            } else if (is_multi_mput(line)) {
                multi_mput(s, line);
            } else if (is_msmput(line)) {
                msmput(sessions, line);
            } else if (is_mget(line)) {
                mget(s, line);
            } else if (is_async_mget(line)) {
                async_mget(s, line);
            } else if (is_magnet(line)) {
                magnet(s, line);
            } else if (is_count_nodes(line)) {
                count_nodes(s);
            } else if (is_putbomb(line)) {
                putBomb(s);
            } else if (is_batchput(line)) {
                batchput(s, line);
            } else if (is_batchget(line)) {
                batchget(s, line);
            } else if (is_mbatchget(line)) {
                mbatchget(s, line);
            } else if (is_unsync_batchget(line)) {
                unsync_batchget(s, line);
            } else if (is_list_nodes(line)) {
                list_nodes(s);
            } else if (is_set_searching_branch(line)) {
                set_searching_branch(s);
            } else if (is_pause(line)) {
                pause(s);
            } else if (is_resume(line)) {
                resume(s);
            } else if (is_applySettings(line)) {
                applySettings(s);
            } else if (is_compress(line)) {
                compress(s, line);
            } else if (is_directRequest(line)) {
                directRequest(s, line);
            } else if (is_invalid(line)) {
                invalid(line);
            }
        }
    }

    private static void print(String s, boolean dollar) {
        Date time = new Date();
        System.out.println();
        System.out.println(LogTimeFormat.format(time) + " " + s);
        if (dollar) {
            //System.out.print("$ ");
        }
    }

    private static void print(String s) {
        print(s, false);
    }

    private static void log(String s) {
        print(s, true);
    }

    private static boolean is_help(String s) {
        s = s.split(" ")[0];
        return s.equals("help");	
    }

    private static boolean is_quit(String s) {
        s = s.split(" ")[0];
        return s.equals("quit") || s.equals("exit") || s.equals("stop");
    }

    private static void quit(SessionManager s) {
        print("Exiting...");
        s.stop();
        System.exit(0);
    }

    private static boolean is_put(String s) {
        return s.startsWith("put ");
    }

    private static void put(SessionManager sm, String s) {
		for(int i= 0; i< 10; i++){
        	String data = "test put immubtable item"+ i;
        	String sha1 = sm.dhtPutItem(new Entry(data)).toString();
        	print("Wait for completion of put for key: " + sha1);
            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
		}
    }

    private static boolean is_preformatPut(String s) {
        return s.startsWith("preformatPut ");
    }

    private static void preformatPut(SessionManager sm, String s) {
        String data = s.split(" ")[1];
        Entry item = Utils.fromPreformattedBytes(data.getBytes());
        print("preformatPut:" + item + ", size:" + item.swig().preformatted_bytes().size());
        print("content:" + new String(Utils.preformattedEntryToBytes(item)));
        String sha1 = sm.dhtPutItem(item).toString();
        print("Wait for completion of put for key: " + sha1);
    }

    private static boolean is_directRequest(String s) {
        return s.startsWith("directRequest ");
    }

    private static void directRequest(SessionManager sm, String s) {
        String[] args = s.split(" ");
        if (args.length != 3) {
            print("Usage:" + Direct_Request);
            return;
        }

        String ep = args[1];
        String data = args[2];

        String ip = ep.split(":")[0];
        int port = Integer.parseInt(ep.split(":")[1]);
        UdpEndpoint targetEP = new UdpEndpoint(ip, port);

        entry item = Utils.fromStringBytes(data.getBytes()).swig();

        entry pkt = new entry();
        pkt.set("y", "q");
        pkt.set("q", "put");
        pkt.set("t", "pX");
        pkt.set("v", "LT01");

        entry a = new entry();
        String id = "1234567890";
        String token = "abcdefg";
        a.set("id", Vectors.bytes2byte_vector(id.getBytes()));
        a.set("token", Vectors.bytes2byte_vector(token.getBytes()));
        a.set("v", item);

        pkt.set("a", a);

        new SessionHandle(sm.swig()).dhtDirectRequest(targetEP, new Entry(pkt));
        print("direct request was sent");
    }

    private static boolean is_msput(String s) {
        return s.startsWith("msput ");
    }

    private static void msput(List<SessionManager> smList, String s) {
        for (SessionManager sm : smList) {
            put(sm, s);
        }
    }

    private static boolean is_putbomb(String s) {
        return s.startsWith("putbomb"); 
    }

    private static void putBomb(SessionManager sm) {
	    print("starting put loop bombing......");
        int i = 0;
	    int loop = 1000000;
	    while (i < loop) {
            String sha1 = sm.dhtPutItem(new Entry(String.valueOf(i))).toString();
	        print("Wait for completion of put for key: " + sha1 + ", value:" + i);
	        i++;

	        try {
	            Thread.sleep(60 * 1000);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }
	    print("ending put loop bombing......");
    }

    private static boolean is_batchput(String s) {
        return s.startsWith("batchput ");
    }

    private static void batchput(SessionManager sm, String s) {
        String[] options = s.split(" ");
        if (options.length != 3) {
            print("Usage:\n" + Batch_Put);
            return;
        }

        int loop;
        BigInteger dataCost = BigInteger.ZERO;
        BigInteger timeCost = BigInteger.ZERO;

        List<String> cache = new ArrayList<String>();

        try {
            loop = Integer.parseInt(options[1]);
        } catch (Exception e) {
            e.printStackTrace();
            print("Usage:\n" + Batch_Put);
            return;
        }

        FileWriter writer;
        BufferedWriter bw;

        try {
            writer = new FileWriter(options[2]);
            bw = new BufferedWriter(writer);
        } catch (Exception e) {
            e.printStackTrace();
            print("incorrect file path");
            return;
        }

        print("starting batch put......");

        int i = 0;
        while (i < loop) {
            Entry randomData = Utils.fromStringBytes(generateRandomArray(990));
            byte[] bencode = randomData.bencode();
            dataCost = dataCost.add(BigInteger.valueOf((long)bencode.length));

            String sha1 = sm.dhtPutItem(randomData).toString();
            immutablePutCache.put(sha1, new PutCallback());
            cache.add(sha1);
            print("Wait for completion of put for key: " + sha1 + ", loop:" + i);

            try {
                bw.write(sha1 + "\n");
                bw.flush();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

            i++;

            /*
            try {
                Thread.sleep(20 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }

        try {
            bw.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(30 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (String hash : cache) {
            PutCallback cb = immutablePutCache.get(hash);
            if (cb != null) {
                timeCost = timeCost.add(BigInteger.valueOf((cb.end - cb.start) / 1000000));
            }

            immutablePutCache.remove(hash);
        }

        print("ending batch put......");
        print("data cost:" + dataCost.toString(10) + " bytes, average time cost:"
                + timeCost.divide(BigInteger.valueOf((long)loop)) + " ms");
    }

    private static boolean is_batchget(String s) {
        return s.startsWith("batchget ");
    }

    private static void batchget(SessionManager sm, String s) {
        String[] options = s.split(" ");
        if (options.length != 2) {
            print("Usage:\n" + Batch_Get);
            return;
        }

        FileReader reader;
        BufferedReader br;

        try {
            reader = new FileReader(options[1]);
            br = new BufferedReader(reader);
        } catch (Exception e) {
            e.printStackTrace();
            print("incorrect file path");
            return;
        }

        print("starting batch get......");

        String sha1;
        BigInteger dataGot = BigInteger.ZERO;
        BigInteger timeCost = BigInteger.ZERO;

        int total = 0;
        int failed = 0;

        try {
            while ((sha1 = br.readLine()) != null) {

                total++;

                long startTime = System.nanoTime();
                Entry data = sm.dhtGetItem(new Sha1Hash(sha1), 10);
                long t = (System.nanoTime() - startTime) / 1000000;
                timeCost = timeCost.add(BigInteger.valueOf(t));
                print("get " + sha1 + ", cost " + t + "ms");

                if (data == null || data.swig().type() == entry.data_type.undefined_t) {
                    print("get failed:" + sha1);
                    failed++;
                } else {
                    byte[] bencode = data.bencode();
                    dataGot = dataGot.add(BigInteger.valueOf((long)bencode.length));
                }
             }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            br.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        BigInteger average = timeCost.divide(BigInteger.valueOf((long)total));

        print("ending batch gut, fail rate:(" + failed + "/" + total + ")"
                + ", effective got:" + dataGot.toString(10) + " bytes"
                + ", average time:" + average.toString(10) + " ms");
    }

    private static boolean is_mbatchget(String s) {
        return s.startsWith("mbatchget");
    }

    private static void mbatchget(SessionManager sm, String s) {
        byte[] seed = Ed25519.createSeed();

        Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seed);
        byte[] publicKey = keypair.first;
        byte[] privateKey = keypair.second;


        byte[][] keys = new byte[2][];
        keys[0] = publicKey;
        keys[1] = privateKey;

        String msg = "Save this key pair\n";
        msg += "Public: " + Utils.toHex(keys[0]) + "\n";
        msg += "Private: " + Utils.toHex(keys[1]) + "\n";
        print(msg);

        String salt = "taucoin";
        Entry randomData = Utils.fromStringBytes(generateRandomArray(10));
        sm.dhtPutItem(publicKey, privateKey, randomData, salt.getBytes());

        try {
            Thread.sleep(60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 1000; i++) {
            long start = System.nanoTime();
            MutableItem item = sm.dhtGetItem(publicKey, salt.getBytes(), 20);
            long t = (System.nanoTime() - start) / 1000000;
            print("[mget] loop:" + i + ", cost " + t + "ms" + ", item:" + item.item.toString());
        }
    }

    private static boolean is_multi_mget(String s) {
        return s.startsWith("multi_mget");
    }

    private static void multi_mget(SessionManager sm, String s) {
        String[] arr = s.split(" ");
        byte[] publicKey = Utils.fromHex(arr[1]);
        byte[] salt = arr[2].getBytes();

        long result = 0;

        while (true) {
            SessionManager.MutableItem data = sm.dhtGetItem(publicKey, salt, 5);
            if (data == null || data.item.swig().type() == entry.data_type.undefined_t) {
                print("result is null");
                continue;
            }

            long i;
            try {
                print("result:" + data.item.integer());
                i = data.item.integer();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            if (i > result) {
                result = i;
                print(result + " is got, ts:" + System.currentTimeMillis() / 1000);
            }
        }
    }

    private static boolean is_multi_mput(String s) {
        return s.startsWith("multi_mput");
    }

    private static void multi_mput(SessionManager sm, String s) {
        String[] arr = s.split(" ");
        byte[] publicKey = Utils.fromHex(arr[1]);
        byte[] privateKey = Utils.fromHex(arr[2]);
        byte[] salt = arr[3].getBytes();

        long i = 1;

        while (true) {
           print("starting put:" + i + " ts:" + System.currentTimeMillis() / 1000);
           sm.dhtPutItem(publicKey, privateKey, new Entry(i), salt);
           i++;

           try {
                Thread.sleep(30 * 1000);
           } catch (InterruptedException e) {
                e.printStackTrace();
           }
        }
    }

    private static boolean is_unsync_batchget(String s) {
        return s.startsWith("unsync_batchget ");
    }

    private static void unsync_batchget(SessionManager sm, String s) {
        String[] options = s.split(" ");
        if (options.length != 2) {
            print("Usage:\n" + Unsync_Batch_Get);
            return;
        }

        FileReader reader;
        BufferedReader br;

        try {
            reader = new FileReader(options[1]);
            br = new BufferedReader(reader);
        } catch (Exception e) {
            e.printStackTrace();
            print("incorrect file path");
            return;
        }

        print("starting unsync batch get......");

        String sha1;
        SessionHandle handle = new SessionHandle(sm.swig());

        try {
            while ((sha1 = br.readLine()) != null) {

                sGetTracker.total++;

                handle.dhtGetItem(new Sha1Hash(sha1));
                immutableGetCache.put(sha1, new GetCallback());

                //Thread.sleep(100);
             }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            br.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        print("wait for ending");

        while (true) {
            if (sGetTracker.failed + sGetTracker.success == sGetTracker.total) {
                BigInteger average = sGetTracker.timeCost.divide(
                    BigInteger.valueOf((long)sGetTracker.total));
                print("ending unsync batch get, fail rate:(" + sGetTracker.failed
                    + "/" + sGetTracker.total + ")"
                    + ", average time:" + average.toString(10) + " ms");
                break;
            } else {
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void handleImmutableGot(DhtImmutableItemAlert a) {

        Sha1Hash sha1 = a.target();
        GetCallback cb = immutableGetCache.get(sha1.toString());

        if (cb == null) {
            print("unknow immutable item:" + sha1.toString());
            return;
        }

        cb.end = System.nanoTime();
        long t = (cb.end - cb.start) / 1000000;
        print("get " + sha1.toString() + ", cost " + t + "ms");
        sGetTracker.timeCost = sGetTracker.timeCost.add(BigInteger.valueOf(t));

        Entry item = a.item();
        if (item == null || item.swig().type() == entry.data_type.undefined_t) {
            print("get failed:" + sha1);
            sGetTracker.failed++;
        } else {
            sGetTracker.success++;
        }

        immutableGetCache.remove(sha1.toString());
    }

    private static boolean is_get(String s) {
        return s.startsWith("get ");
    }

    private static void get(SessionManager sm, String s) {
        String sha1 = s.split(" ")[1];
        print("Waiting a max of 20 seconds to get data for key: " + sha1);
        Entry data = sm.dhtGetItem(new Sha1Hash(sha1), 20);
        print(data.toString());
    }

    private static boolean is_getbomb(String s) {
        return s.startsWith("getbomb");
    }

    private static void getbomb(SessionManager sm, String s) {
        String data = s.split(" ")[1];
        String sha1 = sm.dhtPutItem(new Entry(data)).toString();
        print("Wait for completion of put for key: " + sha1);

        try {
            Thread.sleep(60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        print("get bombing...");
        int i = 0;
        Entry result = null;
        while (i < 100) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            result = sm.dhtGetItem(new Sha1Hash(sha1), 20);
            if (result == null || isEntryUndefined(result)) {
                print("getbomb failed");
            } else {
                print("getbomb success");
            }

            i++;
        }

        print("getbomb is ending");
    }

    private static boolean is_externalAddress(String s) {
        return s.startsWith("externalAddress");
    }

    private static void externalAddress(SessionManager sm) {
        print(sm.externalAddress());
    }

    private static boolean is_ping(String s) {
        return s.startsWith("ping ");
    }

    private static void ping(SessionManager sm, String s) {
        String[] options = s.split(" ");
        if (options.length != 3) {
            print("Usage:\n" + "ping <ip> <port>");
            return;
        }

        String address = options[1];
        int port = Integer.parseInt(options[2]);

        Pair<String, Integer> node = new Pair<String, Integer>(address, port);
        print("ping " + address + ":" + port);

        new SessionHandle(sm.swig()).addDhtNode(node);
    }

    private static boolean is_get_peers(String s) {
        return s.startsWith("get_peers ");
    }

    private static void get_peers(SessionManager sm, String s) {
        String sha1 = s.split(" ")[1];
        print("Waiting a max of 20 seconds to get peers for key: " + sha1);
        ArrayList<TcpEndpoint> peers = sm.dhtGetPeers(new Sha1Hash(sha1), 20);
        print(peers.toString());
    }

    private static boolean is_announce(String s) {
        return s.startsWith("announce ");
    }

    private static void announce(SessionManager sm, String s) {
        String sha1 = s.split(" ")[1];
        sm.dhtAnnounce(new Sha1Hash(sha1), 9000, (byte)0);
        print("Wait for completion of announce for key: " + sha1);
    }

    private static boolean is_mkeys(String s) {
        return s.startsWith("mkeys");
    }

    private static void mkeys(String s) {
        byte[] seed = Ed25519.createSeed();

        Pair<byte[], byte[]> keypair = Ed25519.createKeypair(seed);
        byte[] publicKey = keypair.first;
        byte[] privateKey = keypair.second;


        byte[][] keys = new byte[2][];
        keys[0] = publicKey;
        keys[1] = privateKey;

        String msg = "Save this key pair\n";
        msg += "Public: " + Utils.toHex(keys[0]) + "\n";
        msg += "Private: " + Utils.toHex(keys[1]) + "\n";
        print(msg);
    }

    private static boolean is_mput(String s) {
        return s.startsWith("mput ");
    }

    private static void mput(List<SessionManager> smList, String s) {
        byte[] publicKey = Utils.fromHex("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124");
        byte[] privateKey = Utils.fromHex("f008065e3ff567d4471231a4a0609e118b28f0639f9768d3f8bb123f8f0b38706ade0527cb0dd1e57ad0003fbf8e5af51c0bf0471e639b4920ab49ac17ff88f1");
	    byte[] salt = "test".getBytes();

		int sizeSession = smList.size();	

		for(int i = 0; i< 10; i++){
			SessionManager sm = smList.get(i%sizeSession);
        	String data = "put mutable item";
        	sm.dhtPutItem(publicKey, privateKey, new Entry(data),
                	salt == null ? new byte[0] : salt);
        	print("Wait for completion of mput for public key: " + publicKey);

        	try {
            	Thread.sleep(60 * 1000);
        	} catch (InterruptedException e) {
            	e.printStackTrace();
        	}
		}
    }

    private static boolean is_msmput(String s) {
        return s.startsWith("msmput ");
    }

    private static void msmput(List<SessionManager> smList, String s) {
        for (SessionManager sm : smList) {
            //mput(sm, s);
        }
    }

    private static boolean is_mget(String s) {
        return s.startsWith("mget ");
    }

    private static void mget(SessionManager sm, String s) {
        String[] arr = s.split(" ");
        byte[] publicKey = Utils.fromHex(arr[1]);
	    byte[] salt = null;
	    if (arr.length > 2) {
	        salt = arr[2].getBytes();
	    }

        print("Waiting a max of 20 seconds to get mutable data for public key: " + arr[1]);
        SessionManager.MutableItem data = sm.dhtGetItem(publicKey,
                salt == null ? new byte[0] : salt, 20);
        print(data.item.toString());
    }

    private static boolean is_async_mget(String s) {
        return s.startsWith("async_mget ");
    }

    private static void async_mget(SessionManager sm, String s) {
        String[] arr = s.split(" ");
        byte[] publicKey = Utils.fromHex(arr[1]);
        byte[] salt = null;
        if (arr.length > 2) {
            salt = arr[2].getBytes();
        }

        print("async getting mutable item");
        new SessionHandle(sm.swig()).dhtGetItem(publicKey,
                salt == null ? new byte[0] : salt);
    }

    private static boolean is_magnet(String s) {
        return s.startsWith("magnet ");
    }

    private static void magnet(SessionManager session, String s) {
        String sha1 = s.split(" ")[1];
        String uri = "magnet:?xt=urn:btih:" + sha1;
        print("Waiting a max of 20 seconds to fetch magnet for sha1: " + sha1);
        byte[] data = session.fetchMagnet(uri, 20);
        print(Entry.bdecode(data).toString());
    }

    private static boolean is_count_nodes(String s) {
        return s.startsWith("count_nodes");
    }

    private static void count_nodes(SessionManager s) {
        log("DHT contains " + s.stats().dhtNodes() + " nodes");
    }

    private static boolean is_set_searching_branch(String s) {
        return s.startsWith("set_searching_branch");
    }

    private static void set_searching_branch(SessionManager s) {
        DhtSettings ds = new DhtSettings();
        ds.setSearchBranching(8);
        s.swig().set_dht_settings(ds.swig());
        log("set searching branch into 8");
    }

    private static boolean is_pause(String s) {
        return s.startsWith("pause");
    }

    private static void pause(SessionManager s) {
        s.pause();
        log("dht paused");
    }

    private static boolean is_resume(String s) {
        return s.startsWith("resume");
    }

    private static void resume(SessionManager s) {
        s.resume();
        log("dht resumed");
    }

    private static boolean is_applySettings(String s) {
        return s.startsWith("applySettings");
    }

    private static void applySettings(SessionManager s) {

        SettingsPack sp = s.settings();
        sp.swig().set_str(
                settings_pack.string_types.listen_interfaces.swigValue(),
                "0.0.0.0:8080");
        sp.swig().set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(),
                SessionSettings.dhtBootstrapNodes());
        sp.swig().set_bool(settings_pack.bool_types.enable_ip_notifier.swigValue(), false);

        new SessionHandle(s.swig()).reopenNetworkSockets();

        print("apply settings");
    }

    private static boolean is_list_nodes(String s) {
        return s.startsWith("list_nodes");
    }

    private static void list_nodes(SessionManager s) {
        byte[] state = new SessionHandle(s.swig()).saveState(SessionHandle.SAVE_DHT_STATE);
        if (state == null) {
            print("null state");
            return;
        }

        //entry e = new entry(Vectors.bytes2byte_vector(state));
        Entry stateEntry = Entry.bdecode(state);
        if (stateEntry == null) {
            print("decode entry err");
            return;
        }

        Map<String, Entry> dir = stateEntry.dictionary();
        Entry dhtState = dir.get("dht state");
        if (dhtState == null) {
            print("null dht state");
            return;
        }

        Map<String, Entry> nodesDir = dhtState.dictionary();
        Entry nodes = nodesDir.get("nodes");
        Entry nodes6 = nodesDir.get("nodes6");

        if (nodes != null) {
            for (Entry n : nodes.list()) {
                print(n.toString());
            }
        }
        if (nodes6 != null) {
            for (Entry n6 : nodes6.list()) {
                //print(n6.toString());
            }
        }
    }

    private static boolean is_compress(String s) {
        return s.startsWith("compress ");
    }

    private static void compress(SessionManager sm, String s) {
        String[] arguments = s.split(" ");

        int bytes_size = 1000;
        int loop_account = 1;

        if (arguments.length == 2) {
            bytes_size = Integer.parseInt(arguments[1]);
        } else if (arguments.length == 3) {
            bytes_size = Integer.parseInt(arguments[1]);
            loop_account = Integer.parseInt(arguments[2]);
        }

        for (int i = 0; i < loop_account; i++) {
            byte[] rand_bytes = generateRandomArray(bytes_size);
            try {
                byte[] compressed = CompressionUtils.compress(rand_bytes);
                print(String.format("%d origin length:%d, compressed length:%d, compressed rate:%.4f",
                        i + 1, rand_bytes.length, compressed.length,
                        (double)compressed.length / rand_bytes.length));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean is_invalid(String s) {
        return !s.isEmpty();
    }

    private static void invalid(String s) {
        print("Invalid command: " + s + "\n" + "Try ? for help");
    }

    private static String generateRandomString(int size) {
       byte[] array = new byte[size];
       new Random().nextBytes(array);

       String result = "";
       for (int i = 0; i < array.length; ++i) {
           byte b = array[i];
           result += "" + b;
           //print("result length:" + result.length());
       }

      return result;
    }

    private static byte[] generateRandomArray(int size) {
        byte[] array = new byte[size];
        new Random().nextBytes(array);
        return array;
    }

    private static String getEntryType(Entry e) {
        if (e == null || e.swig() == null) {
            return "";
        }

        entry eswig = e.swig();
        return eswig.type().toString();
    }

    private static boolean isEntryUndefined(Entry e) {
        return sUndefinedEntry.equals(getEntryType(e));
    }
}
