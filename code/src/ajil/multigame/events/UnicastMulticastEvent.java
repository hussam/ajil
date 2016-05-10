package ajil.multigame.events;

import ajil.quotaBarter.events.MulticastEvent;
import ajil.infrastructure.RouterNode;
import naseem.Node;
import naseem.NodeId;

/**
 *
 * @author Hussam
 */
public class UnicastMulticastEvent extends MulticastEvent
{
   protected NodeId destination;

   public UnicastMulticastEvent(NodeId sender, int groupId, int msgSize, NodeId destination)
   {
      super(sender, groupId, msgSize);
      this.destination = destination;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         node.unicast(this, destination);
      } else {
         super.deliver(node);
      }
   }


}
