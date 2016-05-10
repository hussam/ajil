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
public class MulticastEvent extends Event
{
   protected NodeId source;
   protected int groupId;
   protected int msgSize;

   public MulticastEvent(NodeId sender, int groupId, int msgSize)
   {
      super(sender);
      this.source = sender;
      this.groupId = groupId;
      this.msgSize = msgSize;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
        ((RouterNode) node).forwardMulticast(groupId, this);
      } else {
        ((RateLimiter) node.getService(RateLimiter.class.getSimpleName())).receiveMulticast(source, msgSize);
      }
   }
}
