package tech.jmcs.floortech.detailing.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Delivery;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang.IncompleteArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.RpcClient;
import reactor.rabbitmq.Sender;
import tech.jmcs.floortech.common.amqp.ExchangesConfig;
import tech.jmcs.floortech.common.amqp.QueuesConfig;
import tech.jmcs.floortech.common.amqp.rpc.RpcEndpoints;
import tech.jmcs.floortech.common.amqp.rpc.model.JobsInfoRpcPacket;
import tech.jmcs.floortech.common.amqp.rpc.model.UserInfoRpcPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@PropertySource("classpath:rabbitmq.properties")
public class RpcMessageSender {
    final static Logger log = LoggerFactory.getLogger(RpcMessageSender.class);
    final Sender sender;
    final ExchangesConfig exchangesConfig;
    final QueuesConfig queuesConfig;
    private Map<String, RpcClient> rpcClients;

    @Autowired
    public RpcMessageSender(Sender sender, ExchangesConfig exchangesConfig, QueuesConfig queuesConfig) {
        this.sender = sender;
        this.exchangesConfig = exchangesConfig;
        this.queuesConfig = queuesConfig;
    }

    @SuppressWarnings("unused")
    @PostConstruct
    public void init() {
        rpcClients = new HashMap<>();
        setupRpc(RpcEndpoints.GET_JOB_INFO);
        setupRpc(RpcEndpoints.GET_USER_INFO);
    }

    public Mono<Delivery> sendJobInfoRpc(@Nullable String jobUuid, @Nullable Integer floortechJobNumber) {
        if (jobUuid == null && floortechJobNumber == null) throw new IncompleteArgumentException("Must provide at least one of jobUuid or floortechJobNumber");
        log.info("Sending Job Info Remote Procedure Call request for details [{}, {}]", jobUuid, floortechJobNumber);

        var rpcClient = rpcClients.get(RpcEndpoints.GET_JOB_INFO.getRpcKey());
        var jobInfoMessagePacket = new JobsInfoRpcPacket(jobUuid, floortechJobNumber, List.of());
        log.info("Sending packet: {}", jobInfoMessagePacket);

        try {
            return sendMessage(rpcClient, jobInfoMessagePacket.toJsonString());
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException: " + e.getMessage());
            return Mono.error(e);
        }
    }

    public Mono<Delivery> sendUserInfoRpc(@Nullable String username, @Nullable String userId) {
        if (username == null && userId == null) throw new IncompleteArgumentException("Must provide at least username or userId");
        log.info("Sending User Info RPC request for details [{}, {}]", username, userId);

        var rpcClient = rpcClients.get(RpcEndpoints.GET_USER_INFO.getRpcKey());
        var usersInfoRpcPacket = new UserInfoRpcPacket(username, userId);
        log.info("Sending packet: {}", usersInfoRpcPacket);

        try {
            return sendMessage(rpcClient, usersInfoRpcPacket.toJsonString());
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException: " + e.getMessage());
            return Mono.error(e);
        }
    }

    private Mono<Delivery> sendMessage(RpcClient rpcClient, String jsonString) {
        if (rpcClient == null) return Mono.error(new Exception("Unable to access RPC"));
        var rpcClientRequest = new RpcClient.RpcRequest(jsonString.getBytes());
        log.info("Request (JSON): {}", jsonString);
        Mono<Delivery> response = rpcClient.rpc(Mono.just(rpcClientRequest));
        return response;
    }

    private void setupRpc(RpcEndpoints endpoint) {
        var rpcKey = endpoint.getRpcKey();
        if (queuesConfig.getTopics().containsKey(rpcKey)) {
            var config = queuesConfig.getTopics().get(rpcKey);
            var routingKey = config.getRoutingKey();
            var exchange = config.getExchange();
            if (!rpcClients.containsKey(rpcKey)) {
                var rpcClient = sender.rpcClient(exchange, routingKey);
                rpcClients.put(rpcKey, rpcClient);
            } else {
                log.error("There is already a rpc client with a matching key: {}", rpcKey);
            }
        } else {
            log.error("Could not create RPC client for " + rpcKey);
        }
    }

    @SuppressWarnings("unused")
    @PreDestroy
    public void close() {
        if (rpcClients != null) {
            rpcClients.entrySet().forEach(entry -> {
                entry.getValue().close();
            });
        }
    }
}

