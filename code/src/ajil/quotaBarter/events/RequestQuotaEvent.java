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
public class RequestQuotaEvent extends Event
{
   private long requestedQuota;
   private NodeId requester;
   private NodeId requestFrom;
   
   public RequestQuotaEvent(NodeId sender, NodeId requestFrom, long requestedQuota)
   {
      super(sender);
      this.requester = sender;
      this.requestFrom = requestFrom;
      this.requestedQuota = requestedQuota;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         node.unicast(this, requestFrom);
      } else if (!node.id.equals(requester)) {    // node is a LeafNode
         ((RateLimiter) node.getService(RateLimiter.class.getSimpleName())).quotaRequested(requester, requestedQuota);
      }
   }
}
