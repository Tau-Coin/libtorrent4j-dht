package io.tau;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.frostwire.jlibtorrent.swig.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

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

    private static final String List_nodes = "list_nodes: list dht nodes";

    private static final String Compress = "compress <rand bytes size> <test accounts>: compress tests";

    private static final String Quit = "quit: exit this application";

    private static final String Help = "commands list:" + "\n"
            + PutImmutableItem + "\n" + MSPutImmutableItem  + "\n" + GetImmutableItem + "\n"
	        + GetPeers + "\n" + GenKeyPair + "\n"
            + PutMutableItem + "\n" + MSPutMutableItem + "\n" + GutMutableItem + "\n"
	        + Count_Nodes + "\n" + Put_Bomb + "\n" + List_nodes + "\n"
            + Compress + "\n" + Quit + "\n";

    private static final SimpleDateFormat LogTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String sUndefinedEntry = entry.data_type.undefined_t.toString();

    private static final int Sessions_Count = 1;

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

                if (type == AlertType.LOG) {
                    LogAlert a = (LogAlert) alert;
                    log(a.message());
                }

                if (type == AlertType.DHT_PKT) {
                    DhtPktAlert a = (DhtPktAlert) alert;
                    log(a.message());
                }

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
		        }

                if (type == AlertType.DHT_PUT) {
                    DhtPutAlert a = (DhtPutAlert) alert;
                    log(a.message());
                }
            }
        };

        List<SessionManager> sessions = new ArrayList<>();
        for (int i = 0; i < Sessions_Count; i++) {
                SessionManager session = new SessionManager(true);
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
            } else if (is_get_peers(line)) {
                get_peers(s, line);
            } else if (is_announce(line)) {
                announce(s, line);
            } else if (is_mkeys(line)) {
                mkeys(line);
            } else if (is_mput(line)) {
                mput(s, line);
            } else if (is_msmput(line)) {
                msmput(sessions, line);
            } else if (is_mget(line)) {
                mget(s, line);
            } else if (is_magnet(line)) {
                magnet(s, line);
            } else if (is_count_nodes(line)) {
                count_nodes(s);
            } else if (is_putbomb(line)) {
                putBomb(s);
            } else if (is_list_nodes(line)) {
                list_nodes(s);
            } else if (is_pause(line)) {
                pause(s);
            } else if (is_resume(line)) {
                resume(s);
            } else if (is_applySettings(line)) {
                applySettings(s);
            } else if (is_compress(line)) {
                compress(s, line);
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
        String data = s.split(" ")[1];
        String sha1 = sm.dhtPutItem(new Entry(data)).toString();
        print("Wait for completion of put for key: " + sha1);
    }

    private static boolean is_preformatPut(String s) {
        return s.startsWith("preformatPut ");
    }

    private static void preformatPut(SessionManager sm, String s) {
        String data = s.split(" ")[1];
        String sha1 = sm.dhtPutItem(Utils.fromPreformattedBytes(data.getBytes())).toString();
        print("Wait for completion of put for key: " + sha1);
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

    private static void mput(SessionManager sm, String s) {
        String[] arr = s.split(" ");
        byte[] publicKey = Utils.fromHex(arr[1]);
        byte[] privateKey = Utils.fromHex(arr[2]);
        String data = arr[3];
	    byte[] salt = null;
	    if (arr.length > 4) {
            salt = arr[4].getBytes();
	    }
        sm.dhtPutItem(publicKey, privateKey, new Entry(data),
                salt == null ? new byte[0] : salt);
        print("Wait for completion of mput for public key: " + arr[1]);
    }

    private static boolean is_msmput(String s) {
        return s.startsWith("msmput ");
    }

    private static void msmput(List<SessionManager> smList, String s) {
        for (SessionManager sm : smList) {
            mput(sm, s);
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
           print("result length:" + result.length());
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
