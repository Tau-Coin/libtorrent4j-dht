package io.tau;

import com.frostwire.jlibtorrent.SessionParams;
import com.frostwire.jlibtorrent.swig.*;

public final class SessionSettings {

    private static final int DHT_MAX_ITEMS = 1000000;

    private static settings_pack sp;

    private static int sStarting_Port = 6881;

    static {
        sp = new settings_pack();
	    sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), dhtBootstrapNodes());
    }

    public static SessionParams getTauSessionParams() {
        sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(),
                "0.0.0.0:0" /*sStarting_Port*//*listenInterfaces()*/);
        sStarting_Port++;

        session_params sparams = new session_params(sp);
        dht_settings ds = new dht_settings();
        ds.setMax_dht_items(DHT_MAX_ITEMS);
        sparams.setDht_settings(ds);

        return new SessionParams(sparams);
    }

    public static String dhtBootstrapNodes() {
        StringBuilder sb = new StringBuilder();

        sb.append("dht.libtorrent.org:25401").append(",");
        sb.append("router.bittorrent.com:6881").append(",");
        sb.append("router.utorrent.com:6881").append(",");
        sb.append("dht.transmissionbt.com:6881").append(",");
        // for DHT IPv6
        sb.append("router.silotis.us:6881");
        //sb.append("tau.geekgalaxy.com:32777").append(",");
        //sb.append("tau.geekgalaxy.com:32778");

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
