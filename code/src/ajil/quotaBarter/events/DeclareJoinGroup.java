package ajil.quotaBarter.events;

import ajil.Ajil;
import ajil.quotaBarter.QuotasManager;
import ajil.quotaBarter.RateLimiter;
import ajil.infrastructure.RouterNode;
import naseem.Node;
import naseem.NodeId;

/**
 *
 * @author Hussam
 */
public class DeclareJoinGroup extends MulticastEvent
{
   public static final int declareJoinMsgSize = 1;

   private int joinedGroupId;

   public DeclareJoinGroup(NodeId memberId, int joinedGroupId)
   {
      super(memberId, Ajil.updatesGroupId, declareJoinMsgSize);
      this.source = sender;
      this.joinedGroupId = joinedGroupId;
   }

   @Override
   public void deliver(Node node)
   {
      if (node instanceof RouterNode) {
         super.deliver(node);
      } else {
         RateLimiter rl = (RateLimiter) node.getService(RateLimiter.class.getSimpleName());
         QuotasManager qm = (QuotasManager) node.getService(QuotasManager.class.getSimpleName());

         if (rl != null)
            rl.newGroupMember(joinedGroupId, source);
         if (qm != null)
            qm.newGroupMember(joinedGroupId, source);
      }
   }

}
