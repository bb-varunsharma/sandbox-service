package com.bigbasket.sandbox.di.aerospike;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.async.EventPolicy;
import com.aerospike.client.async.NettyEventLoops;
import com.aerospike.client.policy.AuthMode;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.TlsPolicy;
import com.bigbasket.core.Util;
import com.bigbasket.core.dal.aerospike.AerospikeServiceImpl;
import com.bigbasket.core.tenant.secret.Secret;

import io.netty.channel.nio.NioEventLoopGroup;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class AerospikeServiceProxyImpl extends AerospikeServiceImpl implements AerospikeServiceProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AerospikeServiceProxyImpl.class);

    public static AerospikeServiceProxyImpl createAerospikeService(Vertx vertx, JsonObject config) {
        String secretsContent = config.getString("tenant-secrets", "");
        LOGGER.info("Sandbox Config secretsContent : {}, status: {}", secretsContent, secretsContent.isEmpty() ? "empty" : "not empty");
        Secret secret = null;
        try {
            secret = Util.getACMSSecret(config);
        } catch (Exception e) {
            LOGGER.debug("ACMS secret fetch failed, Exception: {}", e.getMessage());
        }
        if (secret == null) {
            throw new RuntimeException("Could not get ACMS secret for Aerospike");
        }
        return new AerospikeServiceProxyImpl(vertx, config, secret);
    }

    public AerospikeServiceProxyImpl(Vertx vertx, JsonObject config, Secret secret) {
        super(vertx, config, secret);

        final Integer aerospikeTimeoutInMs = config.getInteger("AEROSPIKE_TIMEOUT_IN_MS");

        EventPolicy eventPolicy = new EventPolicy();
        eventPolicy.maxCommandsInProcess = config.getInteger("MAX_COMMANDS_IN_PROCESS", 200);
        eventPolicy.maxCommandsInQueue = config.getInteger("MAX_COMMANDS_IN_QUEUE", 15000);
        int eventLoopSize = config.getInteger("NUMBER_OF_EVENT_LOOPS_AEROSPIKE", 2);

        NioEventLoopGroup nioGroup = new NioEventLoopGroup(eventLoopSize);
        eventLoops = new NettyEventLoops(eventPolicy, nioGroup);

        final String acmsHosts = secret.getHosts();
        final int acmsPort = Integer.parseInt(secret.getPort());
        final String acmsTLSName = secret.getTlsName();

        try {
            loadCertificate(secret.getCertificate(), secret.getPassword());
        } catch (IOException | KeyStoreException | KeyManagementException | NoSuchAlgorithmException |
                 CertificateException e) {
            throw new RuntimeException(e);
        }

        ClientPolicy policy = new ClientPolicy();
        policy.tlsPolicy = new TlsPolicy();
        policy.authMode = AuthMode.INTERNAL;
        policy.user = secret.getUsername();
        policy.password = secret.getPassword();
        policy.eventLoops = eventLoops;
        policy.readPolicyDefault.setTimeouts(config.getInteger("AEROSPIKE_SOCKET_TIMEOUT_MS", aerospikeTimeoutInMs), config.getInteger("AEROSPIKE_TOTAL_TIMEOUT_MS", aerospikeTimeoutInMs));
        policy.readPolicyDefault.maxRetries = config.getInteger("AEROSPIKE_MAX_RETRIES", 2);
        policy.writePolicyDefault.setTimeout(aerospikeTimeoutInMs);
        policy.maxConnsPerNode = eventPolicy.maxCommandsInProcess * eventLoopSize;

        final Host[] finalHosts = Arrays.stream(acmsHosts.split(","))
                .map(x -> new Host(x, acmsTLSName, acmsPort))
                .toArray(Host[]::new);

        LOGGER.info("AerospikeService Hosts are {}", Arrays.toString(finalHosts));
        client = new AerospikeClient(policy, finalHosts);
    }
}
