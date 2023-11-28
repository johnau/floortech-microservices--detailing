package tech.jmcs.floortech.detailing.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import tech.jmcs.floortech.common.amqp.QueuesConfig;
import tech.jmcs.floortech.common.amqp.rpc.RPCServicesIndex;
import tech.jmcs.floortech.common.amqp.rpc.model.JobsInfoRpcPacket;
import tech.jmcs.floortech.common.dto.FloortechJobDto;
import tech.jmcs.floortech.detailing.domain.configs.FloortechJobInfoRpc;

import java.util.List;

@Deprecated
@Service
public class FloortechJobInfoRpcImpl implements FloortechJobInfoRpc {

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    QueuesConfig queuesConfig;

    @Override
    public FloortechJobDto getJobInformation(String jobId) {
        var jobInfoQueue = queuesConfig.getTopics().get(RPCServicesIndex.JOBS_INFO_RPC_KEY);
        var exchange = jobInfoQueue.getExchange();
        var routing = jobInfoQueue.getRoutingKey();
        var packet = new JobsInfoRpcPacket(jobId, null, List.of());
        var jobDto = rabbitTemplate.convertSendAndReceiveAsType(
                exchange,
                routing,
                packet,
                new ParameterizedTypeReference<FloortechJobDto>() {}
        );

        return jobDto;
    }
}
