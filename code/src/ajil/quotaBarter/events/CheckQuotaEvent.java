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
public class CheckQuotaEvent extends TimedEvent
{
   public CheckQuotaEvent(NodeId sender, long scheduledFor)
   {
      super(sender, scheduledFor);
   }

   @Override
   public void deliver(Node node)
   {
      ((RateLimiter) node.getService(RateLimiter.class.getSimpleName())).requestQuota();

      // Schedule this event for next time
      long now = WorkloadDriver.getRuntime().getTime();
      int offset = Ajil.epochLength / RateLimiter.quotaChecksPerEpoch;
      rescheduleFor(now + offset);

      node.scheduleEvent(this);
   }

}
