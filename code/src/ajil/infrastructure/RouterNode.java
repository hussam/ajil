package ajil.infrastructure;

import ajil.quotaBarter.events.MulticastEvent;
import java.util.HashMap;
import java.util.HashSet;
import naseem.Node;
import naseem.NodeId;

/**
 *
 * @author Hussam
 */
public class RouterNode extends Node
{
   private HashMap<Integer, HashSet<NodeId>> groupsMembers;

   public RouterNode(NodeId id)
   {
      super(id);
      groupsMembers = new HashMap<Integer, HashSet<NodeId>>();
   }

   public void joinGroup(int groupId, NodeId memberId)
   {
      HashSet<NodeId> members;
      if (!groupsMembers.containsKey(groupId)) {
         members = new HashSet<NodeId>();
         groupsMembers.put(groupId, members);
      } else {
         members = groupsMembers.get(groupId);
      }
      members.add(memberId);
   }

   public void forwardMulticast(int groupId, MulticastEvent e)
   {
      multicast(e, groupsMembers.get(groupId));
   }
}
