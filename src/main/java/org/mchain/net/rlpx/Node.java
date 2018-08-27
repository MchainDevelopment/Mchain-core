/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mchain.net.rlpx;

import org.mchain.crypto.ECKey;
import org.mchain.util.RLP;
import org.mchain.util.RLPList;
import org.mchain.util.Utils;
import org.spongycastle.util.encoders.Hex;

import static org.mchain.crypto.HashUtil.sha3;
import static org.mchain.util.ByteUtil.byteArrayToInt;
import static org.mchain.util.ByteUtil.bytesToIp;
import static org.mchain.util.ByteUtil.hostToBytes;
import static org.mchain.util.ByteUtil.toHexString;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class Node implements Serializable {
    private static final long serialVersionUID = -4267600517925770636L;

    byte[] id;
    String host;
    int port;
    // discovery endpoint doesn't have real nodeId for example
    private boolean isFakeNodeId = false;

    /**
     *  - create Node instance from enode if passed,
     *  - otherwise fallback to random nodeId, if supplied with only "address:port"
     * NOTE: validation is absent as method is not heavily used
     */
    public static Node instanceOf(String addressOrEnode) {
        try {
            URI uri = new URI(addressOrEnode);
            if (uri.getScheme().equals("enode")) {
                return new Node(addressOrEnode);
            }
        } catch (URISyntaxException e) {
            // continue
        }

        final ECKey generatedNodeKey = ECKey.fromPrivate(sha3(addressOrEnode.getBytes()));
        final String generatedNodeId = Hex.toHexString(generatedNodeKey.getNodeId());
        final Node node = new Node("enode://" + generatedNodeId + "@" + addressOrEnode);
        node.isFakeNodeId = true;
        return node;
    }

    public Node(String enodeURL) {
        try {
            URI uri = new URI(enodeURL);
            if (!uri.getScheme().equals("enode")) {
                throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT");
            }
            this.id = Hex.decode(uri.getUserInfo());
            this.host = uri.getHost();
            this.port = uri.getPort();
        } catch (URISyntaxException e) {
            throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT", e);
        }
    }

    public Node(byte[] id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public Node(RLPList nodeRLP) {
        byte[] hostB = nodeRLP.get(0).getRLPData();
        byte[] portB = nodeRLP.get(1).getRLPData();
        byte[] idB;

        if (nodeRLP.size() > 3) {
            idB = nodeRLP.get(3).getRLPData();
        } else {
            idB = nodeRLP.get(2).getRLPData();
        }

        int port = byteArrayToInt(portB);

        this.host = bytesToIp(hostB);
        this.port = port;
        this.id = idB;
    }

    public Node(byte[] rlp) {
        this((RLPList) RLP.decode2(rlp).get(0));
    }

    /**
     * @return true if this node is endpoint for discovery loaded from config
     */
    public boolean isDiscoveryNode() {
        return isFakeNodeId;
    }


    public byte[] getId() {
        return id;
    }

    public String getHexId() {
        return Hex.toHexString(id);
    }

    public String getHexIdShort() {
        return Utils.getNodeIdShort(getHexId());
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDiscoveryNode(boolean isDiscoveryNode) {
        isFakeNodeId = isDiscoveryNode;
    }

    /**
     * Full RLP
     * [host, udpPort, tcpPort, nodeId]
     * @return RLP-encoded node data
     */
    public byte[] getRLP() {
        byte[] rlphost = RLP.encodeElement(hostToBytes(host));
        byte[] rlpTCPPort = RLP.encodeInt(port);
        byte[] rlpUDPPort = RLP.encodeInt(port);
        byte[] rlpId = RLP.encodeElement(id);

        return RLP.encodeList(rlphost, rlpUDPPort, rlpTCPPort, rlpId);
    }

    /**
     * RLP without nodeId
     * [host, udpPort, tcpPort]
     * @return RLP-encoded node data
     */
    public byte[] getBriefRLP() {
        byte[] rlphost = RLP.encodeElement(hostToBytes(host));
        byte[] rlpTCPPort = RLP.encodeInt(port);
        byte[] rlpUDPPort = RLP.encodeInt(port);

        return RLP.encodeList(rlphost, rlpUDPPort, rlpTCPPort);
    }

    @Override
    public String toString() {
        return "Node{" +
                " host='" + host + '\'' +
                ", port=" + port +
                ", id=" + toHexString(id) +
                '}';
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (o instanceof Node) {
            return Arrays.equals(((Node) o).getId(), this.getId());
        }

        return false;
    }
}
