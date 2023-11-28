package tech.jmcs.floortech.detailing.app.service;

import org.apache.commons.io.FileUtils;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.DirectFieldAccessor;
import tech.jmcs.floortech.common.helper.StringHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class TestHelpers {
    static String count(List<BlockingQueue<?>> queues) {
        int n = 0;
        for (BlockingQueue<?> queue : queues) {
            n += queue.size();
        }
        return "Total queue size: " + n;
    }

    static List<BlockingQueue<?>> getQueues(SimpleMessageListenerContainer container) {
        DirectFieldAccessor accessor = new DirectFieldAccessor(container);
        List<BlockingQueue<?>> queues = new ArrayList<BlockingQueue<?>>();
        @SuppressWarnings("unchecked")
        Set<BlockingQueueConsumer> consumers = (Set<BlockingQueueConsumer>) accessor.getPropertyValue("consumers");
        for (BlockingQueueConsumer consumer : consumers) {
            accessor = new DirectFieldAccessor(consumer);
            queues.add((BlockingQueue<?>) accessor.getPropertyValue("queue"));
        }
        return queues;
    }

    static void cleanupFilesFolders(Path rootPath, String clientName, String clientId) {
        System.out.println("Root storage path: " + rootPath);
        var toDelete = Paths.get(rootPath.toString(), StringHelper.stripInvalidPathCharacters(clientName) + "_" + StringHelper.stripInvalidPathCharacters(clientId));
        System.out.println("To delete folder path: " + toDelete);
        try {
            FileUtils.deleteDirectory(toDelete.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
