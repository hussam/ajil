package ajil.quotaBarter.events;

import ajil.Ajil;
import ajil.quotaBarter.QuotasManager;
import ajil.quotaBarter.RateLimiter;
import ajil.infrastructure.RouterNode;
import naseem.Node;
import naseem.NodeId;

/**
 *
 * @author Hussam
 */
public class UnusedQuotaEvent extends MulticastEvent
{
   private long totalUnused;
   private long assignedUnused;

   public UnusedQuotaEvent(NodeId sender, long totalUnusedQUota, long unusedAssignedQuota)
   {
      super(sender, Ajil.updatesGroupId, 1);
      this.source = sender;
      this.totalUnused = totalUnusedQUota;
      this.assignedUnused = unusedAssignedQuota;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         super.deliver(node);
      } else {
         RateLimiter rl = (RateLimiter) node.getService(RateLimiter.class.getSimpleName());
         QuotasManager qm = (QuotasManager) node.getService(QuotasManager.class.getSimpleName());
         if (rl != null)
            rl.updateAvailableQuotas(source, totalUnused, assignedUnused);
         if (qm != null)
            qm.updateAvailableQuotas(source, totalUnused, assignedUnused);
      }
   }
}
