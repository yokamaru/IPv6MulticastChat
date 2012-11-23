package jp.naist.inet_lab.android.ipv6multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import android.content.Context;
import android.net.wifi.WifiManager;

/**
 * Manage communicating over the IP multicast.
 * 
 * @author yohei-ka
 * 
 */
public class MulticastManager {

    /**
     * An address of the joined multicast group.
     */
    InetAddress group_address;

    /**
     * A socket for communicating over the multicast.
     */
    MulticastSocket socket;

    /**
     * A state of a MulticastLock on the WiFi interface
     */
    protected WifiManager.MulticastLock multicast_lock;

    /**
     * Join the specified multicast group.
     * 
     * @param group_address_hr
     *            An address of the multicast group (Human-readable string)
     * @param local_port
     *            A port number which bind on the local. If specify 0 as a port
     *            number, it may automatically choose a port number from
     *            available ports.
     * @throws UnknownHostException
     *             The specified group address is not found, or invalid format.
     * @throws MulticastException
     */
    public void join(String group_address_hr, int local_port)
            throws MulticastException {
        try {
            // Convert the human-readable address to a machine-readable address
            this.group_address = InetAddress.getByName(group_address_hr);

            // Create a socket and join the multicast group
            this.socket = new MulticastSocket(local_port);
            this.socket.joinGroup(this.group_address);
        } catch (UnknownHostException e) {
            throw new MulticastException(e);
        } catch (IOException e) {
            throw new MulticastException(e);
        }
    }

    /**
     * Join the specified multicast group.
     * 
     * A local port will automatically chosen from available ports.
     * 
     * @throws MulticastException
     * 
     * @see MulticastManager#join(String, int)
     */
    public void join(String group_address_hr) throws MulticastException {
        this.join(group_address_hr, 0);
    }

    /**
     * Leave the multicast group that already joined.
     * 
     * @throws MulticastException
     */
    public void leave() throws MulticastException {
        try {
            this.socket.leaveGroup(this.group_address);
        } catch (IOException e) {
            throw new MulticastException(e);
        }
    }

    /**
     * Send data to the joined multicast group.
     * 
     * @param data
     *            Data that you want to send
     * @param remote_port
     *            Remote-side port number
     * @return Size of the data that I actually sent
     * @throws MulticastException
     */
    public int sendData(byte[] data, int remote_port) throws MulticastException {
        // Build datagram packet
        DatagramPacket packet = new DatagramPacket(data, data.length,
                this.group_address, remote_port);

        try {
            this.socket.send(packet);
        } catch (IOException e) {
            throw new MulticastException(e);
        }
        return 0;
    }

    /**
     * Receive data from the joined multicast group.
     * 
     * @param buffer_size
     *            Maximum size by byte that I receive
     * @return Size of the data that I actually received
     */
    public int receiveData(int buffer_size) {
        return 0;
    }

    /**
     * Enable IP multicast on WiFi interface.
     * 
     * A developer who want to use IP multicast on WiFi interface MUST call this
     * method manually before joining a multicast group. Also, you don't forget
     * to call disableMulticastOnWifi(). It may cause battery issue if you
     * forget to call that method.
     * 
     * 
     * @param context
     *            Context of the application
     * @param tag
     *            A tag for identify a state of the WiFi. This tag is only used
     *            internally.
     */
    public void enableMulticastOnWifi(Context context, String tag) {
        WifiManager wifiman = (WifiManager) context
                .getSystemService(android.content.Context.WIFI_SERVICE);
        this.multicast_lock = wifiman.createMulticastLock(tag);
        this.multicast_lock.setReferenceCounted(true);
        this.multicast_lock.acquire();
    }

    /**
     * Disable IP multicast on WiFi interface.
     */
    public void disableMulticastOnWifi() {
        this.multicast_lock.release();
    }

}
