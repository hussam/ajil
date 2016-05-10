package ajil.quotaBarter.events;

import ajil.quotaBarter.QuotasManager;
import ajil.infrastructure.RouterNode;
import naseem.Node;
import naseem.NodeId;
import naseem.events.Event;

/**
 *
 * @author Hussam
 */
public class DeclareSenderEvent extends Event
{
   private NodeId source;
   private NodeId destination;

   public DeclareSenderEvent(NodeId sender, NodeId destination)
   {
      super(sender);
      this.source = sender;
      this.destination = destination;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         node.unicast(this, destination);
      } else {
         QuotasManager qm = (QuotasManager) node.getService(QuotasManager.class.getSimpleName());
         if (qm != null)
            qm.addSender(source);
         else
            throw new Error("No QuotasManager service is running on node receiving a \"DeclareSenderEvent\"");
      }
   }
}
