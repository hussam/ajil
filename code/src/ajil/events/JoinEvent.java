package ajil.events;

import ajil.infrastructure.RouterNode;
import naseem.Node;
import naseem.NodeId;
import naseem.events.Event;

/**
 *
 * @author Hussam
 */
public class JoinEvent extends Event
{
   private NodeId source;
   private int groupId;
   
   public JoinEvent(NodeId sender, int groupId)
   {
      super(sender);
      this.source = sender;
      this.groupId = groupId;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         ((RouterNode) node).joinGroup(groupId, source);
      } else {
         throw new UnsupportedOperationException("Operation only supported at RouterNodes");
      }
   }

}
