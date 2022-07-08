package com.lmk.mqtt.service.impl;

import com.lmk.mqtt.queue.QueueEnum;
import com.lmk.mqtt.service.api.QueueService;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueueServiceImpl implements QueueService {
    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Override
    public void createQueue(QueueEnum queueEnum) {
        rabbitAdmin.declareQueue(
                new Queue(queueEnum.getQueueName(),
                        queueEnum.isDurable(),
                        queueEnum.isExclusive(),
                        queueEnum.isAutoDelete(),
                        queueEnum.getArguments()
                )
        );
    }
}
