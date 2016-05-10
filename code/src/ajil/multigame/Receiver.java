package ajil.multigame;

import ajil.Ajil;
import ajil.infrastructure.LeafNode;
import java.util.ArrayList;
import java.util.HashSet;
import naseem.NodeId;

/**
 *
 * @author Hussam
 */
public class Receiver extends Ajil
{
   private int multicastTraffic = 0;
   private int newMulticastTraffic = 0;   // new since last report

   private ArrayList<NodeId> multicastSenders;

   public Receiver(LeafNode n)
   {
      super(n);
      multicastSenders = new ArrayList<NodeId>();
   }

   @Override
   public void joinGroup(int groupId)
   {
      super.joinGroup(groupId);
   }

   public void receiveMulticast(NodeId src, int msgSize)
   {
      if (!src.equals(node.id)) {
         multicastTraffic += msgSize;
         newMulticastTraffic += msgSize;
      }
   }

   @Override
   public void initializeService()
   {
      throw new UnsupportedOperationException("Not supported yet.");
   }
}
