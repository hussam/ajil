package ajil.quotaBarter.events;

import ajil.quotaBarter.RateLimiter;
import ajil.infrastructure.RouterNode;
import java.util.HashMap;
import java.util.HashSet;
import naseem.Node;
import naseem.NodeId;
import naseem.events.Event;

/**
 *
 * @author Hussam
 */
public class AcceptSenderEvent extends Event
{
   private NodeId destination;
   private HashMap<Integer, HashSet<NodeId>> groupsMembers;

   public AcceptSenderEvent(NodeId sender, NodeId destination, HashMap<Integer, HashSet<NodeId>> groupsMembers)
   {
      super(sender);
      this.destination = destination;
      this.groupsMembers = groupsMembers;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         node.unicast(this, destination);
      } else {
         RateLimiter rl = (RateLimiter) node.getService(RateLimiter.class.getSimpleName());
         for (int groupId : groupsMembers.keySet()) {
            for (NodeId member : groupsMembers.get(groupId)) {
               rl.newGroupMember(groupId, member);
            }
         }
         rl.acceptAsSender();
      }
   }

}
