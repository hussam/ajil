package ajil.quotaBarter.events;

import ajil.Ajil;
import ajil.quotaBarter.RateLimiter;
import ajil.infrastructure.RouterNode;
import naseem.Node;
import naseem.NodeId;

/**
 *
 * @author Hussam
 */
public class DeclareQuotasManagerEvent extends MulticastEvent
{
   private NodeId quotasManagerId;
   private long epoch;

   public DeclareQuotasManagerEvent(NodeId quotasManagerId, long epoch)
   {
      super(quotasManagerId, Ajil.updatesGroupId, 1);
      this.quotasManagerId = quotasManagerId;
      this.epoch = epoch;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         super.deliver(node);
      } else {
         ((RateLimiter) node.getService(RateLimiter.class.getSimpleName())).setQuotasManagerId(quotasManagerId);
      }
   }
}
