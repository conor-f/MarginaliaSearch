package nu.marginalia.mq;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import nu.marginalia.mq.inbox.MqAsynchronousInbox;
import nu.marginalia.mq.inbox.MqInboxIf;
import nu.marginalia.mq.inbox.MqSingleShotInbox;
import nu.marginalia.mq.inbox.MqSynchronousInbox;
import nu.marginalia.mq.outbox.MqOutbox;
import nu.marginalia.mq.persistence.MqPersistence;

import java.util.UUID;

@Singleton
public class MessageQueueFactory {
    private final MqPersistence persistence;

    @Inject
    public MessageQueueFactory(MqPersistence persistence) {
        this.persistence = persistence;
    }

    public MqSingleShotInbox createSingleShotInbox(String inboxName, int node, UUID instanceUUID)
    {
        return new MqSingleShotInbox(persistence, inboxName + ":" + node, instanceUUID);
    }


    public MqAsynchronousInbox createAsynchronousInbox(String inboxName, int node, UUID instanceUUID)
    {
        return new MqAsynchronousInbox(persistence, inboxName + ":" + node, instanceUUID);
    }

    public MqSynchronousInbox createSynchronousInbox(String inboxName, int node, UUID instanceUUID)
    {
        return new MqSynchronousInbox(persistence, inboxName + ":" + node, instanceUUID);
    }


    public MqOutbox createOutbox(String inboxName, int inboxNode,  String outboxName, int outboxNode, UUID instanceUUID)
    {
        return new MqOutbox(persistence, inboxName, inboxNode, outboxName, outboxNode, instanceUUID);
    }
}
