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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Keep tracks of all multicast sockets
     */
    HashMap<InetAddress, MulticastSocket> sockets;

    /**
     * A state of a MulticastLock on the WiFi interface
     */
    protected WifiManager.MulticastLock multicastLock;

    /**
     * An address of the joined multicast group. This is for the compatibility
     * with api4 or earlier.
     */
    InetAddress latestGroupAddress;

    public MulticastManager() {
        sockets = new HashMap<InetAddress, MulticastSocket>();
    }

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
        /* Do nothing if already joined. */
        if (sockets.containsKey(groupAddress)) {
            return;
        }

        MulticastSocket socket;
        try {
            // Create a socket and join the multicast group
            socket = new MulticastSocket(localPort);
            socket.joinGroup(groupAddress);

            sockets.put(groupAddress, socket);

            /*
             * For compatibility with api4 and earlier, keep track the group
             * address which most recently joined.
             */
            this.latestGroupAddress = groupAddress;
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
     * Leave the all multicast group that already joined.
     * 
     * @throws MulticastException
     */
    public void leave() throws MulticastException {
        try {
            for (Map.Entry<InetAddress, MulticastSocket> entry : sockets
                    .entrySet()) {
                InetAddress groupAddress = entry.getKey();
                MulticastSocket socket = entry.getValue();

                socket.leaveGroup(groupAddress);

                sockets.remove(groupAddress);
            }
        } catch (IOException e) {
            throw new MulticastException(e);
        }
    }

    /**
     * Leave the multicast group.
     * 
     * @param groupAddress
     * @throws MulticastException
     */
    public void leave(InetAddress groupAddress) throws MulticastException {
        MulticastSocket socket = sockets.get(groupAddress);

        try {
            socket.leaveGroup(groupAddress);

            sockets.remove(groupAddress);
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
        try {
            for (Map.Entry<InetAddress, MulticastSocket> entry : sockets
                    .entrySet()) {
                InetAddress groupAddress = entry.getKey();
                MulticastSocket socket = entry.getValue();

                /* Build a datagram packet and send it */
                DatagramPacket packet = new DatagramPacket(data, data.length,
                        groupAddress, remotePort);

                socket.send(packet);
            }
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
     * @deprecated Use
     *             {@link #startReceiver(InetAddress, int, boolean, Receiver)}
     *             instead.
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
     * @deprecated Use
     *             {@link #startReceiver(InetAddress, int, boolean, Receiver)}
     *             instead.
     */
    public byte[] receiveData(int bufferSize, boolean ignoreOwnSentPacket)
            throws MulticastException {
        return receiveDataWithDetail(bufferSize, ignoreOwnSentPacket).buffer;
    }

    /**
     * Receive data from the multicast group which the most recently joined, and
     * return the detailed data.
     * 
     * @param bufferSize
     * @param ignoreOwnSentPacket
     * @return
     * @throws MulticastException
     * @deprecated Use
     *             {@link #startReceiver(InetAddress, int, boolean, Receiver)}
     *             instead.
     */
    public ReceivedData receiveDataWithDetail(int bufferSize,
            boolean ignoreOwnSentPacket) throws MulticastException {
        byte[] buffer = new byte[bufferSize];
        MulticastSocket socket = sockets.get(this.latestGroupAddress);

        // Build packet and receive data into it
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        InetAddress sourceAddress;
        try {
            while (true) {
                socket.receive(packet);

                sourceAddress = packet.getAddress();

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

        ReceivedData receivedData = new ReceivedData();
        receivedData.buffer = buffer;
        receivedData.sourceAddress = sourceAddress;
        receivedData.sourcePort = packet.getPort();

        return receivedData;
    }

    /**
     * An interface of "Receiver"
     */
    public interface Receiver {
        public void run(ReceivedData receivedData);
    }

    /**
     * Start the receiver thread correspond to groupAddress
     * 
     * @param groupAddress
     * @param bufferSize
     *            Size of the buffer
     * @param ignoreOwnSentPacket
     *            Ignore the packet which I sent. See
     *            {@link java.net.MulticastSocket#setLoopbackMode(boolean)}
     * @param callback
     *            A method which execute after receive an any packet
     * @throws MulticastException
     */
    public void startReceiver(final InetAddress groupAddress,
            final int bufferSize, boolean ignoreOwnSentPacket,
            final Receiver callback) throws MulticastException {
        /* Get the socket and set the "Loopback Mode" */
        final MulticastSocket socket = sockets.get(groupAddress);
        try {
            socket.setLoopbackMode(ignoreOwnSentPacket);
        } catch (SocketException e) {
            throw new MulticastException(e);
        }

        Thread receiver = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isJoined(groupAddress)) {
                    byte[] buffer = new byte[bufferSize];
                    DatagramPacket packet = new DatagramPacket(buffer,
                            buffer.length);

                    try {
                        /* Wait here while receive an any packet */
                        socket.receive(packet);

                        /* Format received packet into ReceivedData */
                        ReceivedData receivedData = new ReceivedData();
                        receivedData.buffer = buffer;
                        receivedData.sourceAddress = packet.getAddress();
                        receivedData.sourcePort = packet.getPort();
                        receivedData.groupAddress = socket.getInetAddress();
                        receivedData.targetPort = socket.getLocalPort();

                        /* Execute the callback function */
                        callback.run(receivedData);
                    } catch (IOException e) {
                        /*
                         * This exception may cause if the socket is already
                         * left from the group. So now we can simply ignore this
                         * exception.
                         */
                        break;
                    }
                }
            }
        });
        receiver.start();
    }

    /**
     * Contain received data, include source address and port.
     */
    public class ReceivedData {
        /** Bytes of data which received. */
        public byte[] buffer;
        /** Source address (which means remote address) */
        public InetAddress sourceAddress;
        /** Group address */
        public InetAddress groupAddress;
        /** Source port */
        public int sourcePort;
        /** Target port (which means local port) */
        public int targetPort;
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
     * Check joining an any multicast group or not.
     * 
     * @return true if joining an any multicast group
     */
    public boolean isJoined() {
        return !this.sockets.isEmpty();
    }

    /**
     * Check joining the multicast group or not.
     * 
     * @param groupAddress
     * @return True if joined
     */
    public boolean isJoined(InetAddress groupAddress) {
        return sockets.containsKey(groupAddress);
    }

    /**
     * Get all IPv6 addresses which assigned to local interfaces.
     * 
     * @return List of IPv6 address which assigned
     * @throws SocketException
     */
    private List<InetAddress> getAllLocalIPv6Addresses() throws SocketException {
        List<InetAddress> v6Addresses = new ArrayList<InetAddress>();

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

        return v6Addresses;
    }
}
