package io.tau;

import com.frostwire.jlibtorrent.SessionParams;
import com.frostwire.jlibtorrent.swig.*;

public final class SessionSettings {

    private static final int DHT_MAX_ITEMS = 800;

    private static settings_pack sp;

    private static int sStarting_Port = 6881;

    static {
        sp = new settings_pack();
	    sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), dhtBootstrapNodes());
    }

    public static SessionParams getTauSessionParams() {
        //sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(),
        //        "0.0.0.0:0" /*sStarting_Port*//*listenInterfaces()*/);
        sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(), listenInterfaces());

        //sp.set_int(settings_pack.int_types.upload_rate_limit.swigValue(), 512);
        //sp.set_int(settings_pack.int_types.download_rate_limit.swigValue(), 512);
        //sp.set_int(settings_pack.int_types.dht_upload_rate_limit.swigValue(), 512);

        session_params sparams = new session_params(sp);
        dht_settings ds = new dht_settings();
        ds.setMax_dht_items(DHT_MAX_ITEMS);
		/*
        ds.setUpload_rate_limit(512);
        ds.setRead_only(true);
        ds.setSearch_branching(20);
        ds.setMax_peers_reply(0);
        ds.setMax_fail_count(10);
        ds.setMax_torrents(0);
        ds.setMax_peers(0);
        ds.setMax_torrent_search_reply(0);
		*/
        sparams.setDht_settings(ds);

        return new SessionParams(sparams);
    }

    public static String dhtBootstrapNodes() {
        StringBuilder sb = new StringBuilder();

		/*
        sb.append("dht.libtorrent.org:25401").append(",");
        sb.append("router.bittorrent.com:6881").append(",");
        sb.append("router.utorrent.com:6881").append(",");
        sb.append("dht.transmissionbt.com:6881").append(",");
		*/
        // for DHT IPv6
        //sb.append("router.silotis.us:6881");
        //sb.append("tau.geekgalaxy.com:32777").append(",");
        sb.append("13.229.53.249:8661");

        return sb.toString();
    }

    private static String listenInterfaces() {
        String interfaces = "";
        final int count = 1;
        int port = 6881;

        for (int i = 1; i < count; i++) {
            interfaces = interfaces + "0.0.0.0:" + port + ",";
            port++;
        }

        interfaces = interfaces + "0.0.0.0:" + port;

        return interfaces;
    }
}
