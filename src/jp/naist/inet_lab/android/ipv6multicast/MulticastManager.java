package jp.naist.inet_lab.android.ipv6multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

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
    InetAddress groupAddress;

    /**
     * A socket for communicating over the multicast.
     */
    MulticastSocket socket;

    /**
     * Is joining a multicast group or not
     */
    boolean joined = false;

    /**
     * A state of a MulticastLock on the WiFi interface
     */
    protected WifiManager.MulticastLock multicastLock;

    /**
     * Join the specified multicast group.
     * 
     * @param groupAddress
     *            An address of the multicast group
     * @param localPort
     *            A port number which bind on the local. If specify 0 as a port
     *            number, it may automatically choose a port number from
     *            available ports.
     * @throws MulticastException
     */
    public void join(InetAddress groupAddress, int localPort)
            throws MulticastException {
        try {
            // Create a socket and join the multicast group
            this.socket = new MulticastSocket(localPort);
            this.socket.joinGroup(groupAddress);

            this.groupAddress = groupAddress;

            this.joined = true;
        } catch (IOException e) {
            throw new MulticastException(e);
        }
    }

    /**
     * Join the specified multicast group.
     * 
     * @param groupAddressByHumanReadable
     *            An address of the multicast group (Human-readable string)
     * @param localPort
     *            A port number which bind on the local. If specify 0 as a port
     *            number, it may automatically choose a port number from
     *            available ports.
     * @throws MulticastException
     */
    public void join(String groupAddressByHumanReadable, int localPort)
            throws MulticastException {
        try {
            // Convert the human-readable address to a machine-readable address
            InetAddress groupAddress = InetAddress
                    .getByName(groupAddressByHumanReadable);

            this.join(groupAddress, localPort);
        } catch (UnknownHostException e) {
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
    public void join(String groupAddressByHumanReadable)
            throws MulticastException {
        this.join(groupAddressByHumanReadable, 0);
    }

    /**
     * Leave the multicast group that already joined.
     * 
     * @throws MulticastException
     */
    public void leave() throws MulticastException {
        try {
            this.socket.leaveGroup(this.groupAddress);

            this.joined = false;
        } catch (IOException e) {
            throw new MulticastException(e);
        }
    }

    /**
     * Send data to the joined multicast group.
     * 
     * @param data
     *            Data that you want to send
     * @param remotePort
     *            Remote-side port number
     * @return Size of the data that I actually sent
     * @throws MulticastException
     */
    public int sendData(byte[] data, int remotePort) throws MulticastException {
        // Build datagram packet
        DatagramPacket packet = new DatagramPacket(data, data.length,
                this.groupAddress, remotePort);

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
     * @param bufferSize
     *            Maximum size by byte that I receive
     * @return Received data
     * @throws MulticastException
     */
    public byte[] receiveData(int bufferSize) throws MulticastException {
        return receiveData(bufferSize, false);
    }

    /**
     * Receive data from the joined multicast group.
     * 
     * @param bufferSize
     * @param ignoreOwnSentPacket
     *            True then ignore the packet which sent by this node
     * @return
     * @throws MulticastException
     */
    public byte[] receiveData(int bufferSize, boolean ignoreOwnSentPacket)
            throws MulticastException {
        byte[] buffer = new byte[bufferSize];

        // Build packet and receive data into it
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            while (true) {
                this.socket.receive(packet);

                InetAddress sourceAddress = packet.getAddress();

                if ((ignoreOwnSentPacket)
                        && (getAllLocalIPv6Addresses().contains(sourceAddress))) {
                    Log.d("MulticastManager", "Ignore the packet which I sent.");
                    continue;
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            throw new MulticastException(e);
        }

        return buffer;
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
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(android.content.Context.WIFI_SERVICE);
        this.multicastLock = wifiManager.createMulticastLock(tag);
        this.multicastLock.setReferenceCounted(true);
        this.multicastLock.acquire();
    }

    /**
     * Disable IP multicast on WiFi interface.
     */
    public void disableMulticastOnWifi() {
        this.multicastLock.release();
    }

    /**
     * Check is joining the multicast group or not.
     * 
     * @return true if joining the multicast group
     */
    public boolean isJoined() {
        return this.joined;
    }

    /**
     * Get all IPv6 addresses which assigned to local interfaces.
     * 
     * @return List of IPv6 address which assigned
     */
    private List<InetAddress> getAllLocalIPv6Addresses() {
        List<InetAddress> v6Addresses = new ArrayList<InetAddress>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface
                    .getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                Enumeration<InetAddress> addresses = network.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    if ((address.getClass().equals(Inet6Address.class))
                            && (!address.isMulticastAddress())) {
                        v6Addresses.add(address);
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return v6Addresses;
    }
}
