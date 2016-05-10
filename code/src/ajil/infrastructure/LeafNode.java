package ajil.infrastructure;

import java.util.Collection;
import naseem.Node;
import naseem.NodeId;
import naseem.events.Event;
import naseem.workload.WorkloadDriver;

/**
 *
 * @author Hussam
 */
public class LeafNode extends Node
{
   private NodeId router;

   public LeafNode(NodeId id, NodeId router)
   {
      super(id);
      this.router = router;
      neighbors.add(router);
   }

   public void send(Event e)
   {
      WorkloadDriver.getRuntime().unicast(e, this, router);
   }

   @Override
   public void unicast(Event e, NodeId destination)
   {
      throw new UnsupportedOperationException("LeafNodes can only communicate through a \"send\" operation");
   }

   @Override
   public void multicast(Event e, Collection<NodeId> destionations)
   {
      throw new UnsupportedOperationException("LeafNodes can only communicate through a \"send\" operation");
   }
}
