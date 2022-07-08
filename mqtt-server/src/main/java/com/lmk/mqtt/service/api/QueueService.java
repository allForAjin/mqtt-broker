package com.lmk.mqtt.service.api;

import com.lmk.mqtt.queue.QueueEnum;

public interface QueueService {
    void createQueue(QueueEnum queueEnum);
}
