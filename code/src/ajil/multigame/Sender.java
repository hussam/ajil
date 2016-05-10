package ajil.multigame;

import ajil.Ajil;
import ajil.quotaBarter.events.MulticastEvent;
import ajil.multigame.events.UnicastMulticastEvent;
import ajil.infrastructure.LeafNode;
import java.util.ArrayList;
import java.util.HashMap;
import naseem.NodeId;

/**
 *
 * @author Hussam
 */
public class Sender extends Ajil
{
   private HashMap<Integer, Integer> sendingRates;
   private HashMap<Integer, ArrayList<NodeId>> unicastReceivers;

   public Sender(LeafNode n)
   {
      super(n);
      sendingRates = new HashMap<Integer, Integer>();
      unicastReceivers = new HashMap<Integer, ArrayList<NodeId>>();
   }

   public void downgradeReceiver(int groupId, NodeId receiverId)
   {
      ArrayList<NodeId> unicasters = unicastReceivers.get(groupId);
      if (unicasters == null) {
         unicasters = new ArrayList<NodeId>();
         unicastReceivers.put(groupId, unicasters);
      }

      unicasters.add(receiverId);
   }

   public void sendMulticast(int groupId, int msgSize)
   {
      node.send(new MulticastEvent(node.id, groupId, msgSize));
      
      for (NodeId receiver : unicastReceivers.get(groupId)) {
         node.send(new UnicastMulticastEvent(node.id, groupId, msgSize, receiver));
      }
   }

}
