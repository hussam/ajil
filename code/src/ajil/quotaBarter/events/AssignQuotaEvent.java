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
public class AssignQuotaEvent extends Event
{
   private NodeId destination;
   private long quota;
   private long epoch;

   public AssignQuotaEvent(NodeId sender, NodeId destination, long quota, long epoch)
   {
      super(sender);
      this.destination = destination;
      this.quota = quota;
      this.epoch = epoch;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         node.unicast(this, destination);
      } else {
         ((RateLimiter) node.getService(RateLimiter.class.getSimpleName())).setSendingQuota(quota, epoch);
      }
   }

}
