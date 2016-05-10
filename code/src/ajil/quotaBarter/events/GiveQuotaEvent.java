package ajil.quotaBarter.events;

import ajil.quotaBarter.RateLimiter;
import ajil.infrastructure.RouterNode;
import naseem.Node;
import naseem.NodeId;
import naseem.events.Event;

/**
 *
 * @author Hussam
 */
public class GiveQuotaEvent extends Event
{
   private NodeId source;
   private long quota;
   private long lastMasterEpoch;
   private NodeId receiver;

   public GiveQuotaEvent(NodeId sender, NodeId receiver, long quota, long lastMasterEpoch)
   {
      super(sender);
      this.source = sender;
      this.receiver = receiver;
      this.quota = quota;
      this.lastMasterEpoch = lastMasterEpoch;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         node.unicast(this, receiver);
      } else {    // node is a LeafNode
         ((RateLimiter) node.getService(RateLimiter.class.getSimpleName())).receiveAddedQuota(source, quota, lastMasterEpoch);
      }
   }

}
