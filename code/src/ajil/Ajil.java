package ajil;

import ajil.events.JoinEvent;
import ajil.infrastructure.LeafNode;
import java.util.HashMap;
import java.util.HashSet;
import naseem.NodeId;
import naseem.services.Service;

/**
 *
 * @author Hussam
 */
public abstract class Ajil extends Service
{
   public static final int updatesGroupId = -1;   // used to published unused quotas info
   public static final int epochLength = 100;

   protected LeafNode node;

   protected HashMap<NodeId, Long> unusedQuotas;  // unused sending quotas at other nodes
   protected HashMap<Integer, HashSet<NodeId>> groupsMembers;

   public Ajil(LeafNode n)
   {
      super(n);
      this.node = (LeafNode) super.node;
      unusedQuotas = new HashMap<NodeId, Long>();
      groupsMembers = new HashMap<Integer, HashSet<NodeId>>();
   }

   public void joinGroup(int groupId)
   {
      node.send(new JoinEvent(node.id, groupId));
   }

   public void newGroupMember(int groupId, NodeId memberId)
   {
      if (!groupsMembers.containsKey(groupId))
         groupsMembers.put(groupId, new HashSet<NodeId>());
      groupsMembers.get(groupId).add(memberId);
   }

   public void updateAvailableQuotas(NodeId nodeId, long totalUnused, long assignedUnused)
   {
      if (totalUnused <= 0)
         unusedQuotas.remove(nodeId);
      else
         unusedQuotas.put(nodeId, totalUnused);
   }

   @Override
   public void handleLinkFailure(NodeId otherEndpoint)
   {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public void initializeService()
   {
      throw new UnsupportedOperationException("Not supported yet.");
   }
}
