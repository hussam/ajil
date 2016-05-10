package ajil.quotaBarter.events;

import ajil.Ajil;
import ajil.quotaBarter.RateLimiter;
import naseem.Node;
import naseem.NodeId;
import naseem.events.TimedEvent;
import naseem.workload.WorkloadDriver;

/**
 *
 * @author Hussam
 */
public class PublishQuotaEvent extends TimedEvent
{
   public PublishQuotaEvent(NodeId sender, long scheduleFor)
   {
      super(sender, scheduleFor);
   }

   @Override
   public void deliver(Node node)
   {
      ((RateLimiter) node.getService(RateLimiter.class.getSimpleName())).publishUnusedQuota();

      // Schedule this event for next time
      long now = WorkloadDriver.getRuntime().getTime();
      int offset = Ajil.epochLength / RateLimiter.unusedQuotaUpdatesPerEpoch;
      rescheduleFor(now + offset);

      node.scheduleEvent(this);
   }

}
