package io.tau;

import com.frostwire.jlibtorrent.SessionParams;
import com.frostwire.jlibtorrent.swig.*;

public final class SessionSettings {

    private static final int DHT_MAX_ITEMS = 1000000;

    private static SessionParams sSessionParams;

    static {
        settings_pack sp = new settings_pack();
	sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), dhtBootstrapNodes());

	session_params sparams = new session_params(sp);
	dht_settings ds = new dht_settings();
	ds.setMax_dht_items(DHT_MAX_ITEMS);
	sparams.setDht_settings(ds);

        sSessionParams = new SessionParams(sparams);
    }

    public static SessionParams getTauSessionParams() {
        return sSessionParams;
    }

    private static String dhtBootstrapNodes() {
        StringBuilder sb = new StringBuilder();

        sb.append("dht.libtorrent.org:25401").append(",");
        sb.append("router.bittorrent.com:6881").append(",");
        sb.append("dht.transmissionbt.com:6881").append(",");
        // for DHT IPv6
        sb.append("router.silotis.us:6881");

	// tau dht nodes

        return sb.toString();
    }
}
