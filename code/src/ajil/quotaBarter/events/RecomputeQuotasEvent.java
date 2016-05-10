package ajil.quotaBarter.events;

import ajil.Ajil;
import ajil.quotaBarter.QuotasManager;
import naseem.Node;
import naseem.NodeId;
import naseem.events.TimedEvent;
import naseem.workload.WorkloadDriver;

/**
 *
 * @author Hussam
 */
public class RecomputeQuotasEvent extends TimedEvent
{
   public RecomputeQuotasEvent(NodeId sender, long scheduleFor)
   {
      super(sender, scheduleFor);
   }

   @Override
   public void deliver(Node node)
   {
      ((QuotasManager) node.getService(QuotasManager.class.getSimpleName())).assignQuotas();

      // Schedule this event for next time
      long now = WorkloadDriver.getRuntime().getTime();
      rescheduleFor(now + Ajil.epochLength);

      node.scheduleEvent(this);
   }

}
