package ajil.quotaBarter;

import ajil.Ajil;
import ajil.quotaBarter.events.CheckQuotaEvent;
import ajil.quotaBarter.events.DeclareJoinGroup;
import ajil.quotaBarter.events.DeclareSenderEvent;
import ajil.quotaBarter.events.GiveQuotaEvent;
import ajil.quotaBarter.events.MulticastEvent;
import ajil.quotaBarter.events.PublishQuotaEvent;
import ajil.quotaBarter.events.RequestQuotaEvent;
import ajil.quotaBarter.events.UnusedQuotaEvent;
import ajil.infrastructure.LeafNode;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;
import naseem.NodeId;
import naseem.RandomnessManager;
import naseem.workload.WorkloadDriver;

/**
 *
 * @author Hussam
 */
public class RateLimiter extends Ajil
{
   public static final int quotaChecksPerEpoch = 20;
   public static final int unusedQuotaUpdatesPerEpoch = 5;

   private long lastMasterEpoch;    // last known epoch in which the quota manager sent out a new assignment

   private long lastAssignedQuota;  // last quota assigned from quota manager
   private long usedAssignedQuota;  // amount of manager-assigned quota used to send messages (and not given to other senders)
   private long remainingQuota;     // remaining from last assignment from quota manager
   private LinkedHashMap<NodeId, Long> addedQuota;    // received from bartering clients

   private HashMap<Integer, ArrayDeque<Integer>> bufferedMsgs;    // messages buffered waiting for sending quota
   private long bufferMax;

   private long multicastTraffic;
   private long newMulticastTraffic;   // new since last reported

   private NodeId quotasManagerId;
   private boolean declaredAsSender;

   private HashSet<Integer> joinedGroups;

   public RateLimiter(LeafNode n)
   {
      super(n);

      lastMasterEpoch = -1;

      lastAssignedQuota = 0;
      usedAssignedQuota = 0;
      remainingQuota = 0;

      /*
      lastAssignedQuota = Long.MAX_VALUE;
      remainingQuota = Long.MAX_VALUE;
       * 
       */

      addedQuota = new LinkedHashMap<NodeId, Long>();

      bufferedMsgs = new HashMap<Integer, ArrayDeque<Integer>>();
      bufferMax = Long.MAX_VALUE;
      
      multicastTraffic = 0;
      newMulticastTraffic = 0;

      quotasManagerId = null;
      declaredAsSender = false;

      joinedGroups = new HashSet<Integer>();
   }

   @Override
   public void initializeService()
   {
      long now = WorkloadDriver.getRuntime().getTime();
      int checkQuotaOffset = epochLength / quotaChecksPerEpoch;
      int publishQuotaOffset = epochLength / unusedQuotaUpdatesPerEpoch;

      joinGroup(updatesGroupId);

      node.scheduleEvent(new CheckQuotaEvent(node.id, now+checkQuotaOffset));
      node.scheduleEvent(new PublishQuotaEvent(node.id, now+publishQuotaOffset));
   }

   public void setQuotasManagerId(NodeId qmId)
   {
      this.quotasManagerId = qmId;
   }

   @Override
   public void joinGroup(int groupId)
   {
      if (groupId != updatesGroupId)
         node.send(new DeclareJoinGroup(node.id, groupId));

      joinedGroups.add(groupId);
      super.joinGroup(groupId);
   }

   public void acceptAsSender()
   {
      declaredAsSender = true;
   }

   /**
    * Attempts to send a multicast message of the given size to the given group.
    * Returns false if the message was buffered or dropped due to insufficient quota,
    * and true otherwise.
    */
   public boolean sendMulticast(int groupId, int msgSize)
   {
      return sendMulticast(groupId, msgSize, false);
   }

   private boolean sendMulticast(int groupId, int msgSize, boolean sendingFromBuffer)
   {
      if (!declaredAsSender) {
         // notify quotas manager so as to be included in quota allocation
         if (quotasManagerId == null) {
            throw new UnsupportedOperationException("Node " + node.id + " can not send msgs because it doesn't know who the quotas manager is");
         }
         
         node.send(new DeclareSenderEvent(node.id, quotasManagerId));
      }

      if (!groupsMembers.containsKey(groupId)) {
         // TODO: what should i do if i try to mcast to a group with no known receivers?
         return true;  // no known receivers for this group, so return
      }

      // Calculate the actual size of this message in terms of quota consumption
      // That is, a message of size 100 that is sent to 10 recipients will consume
      // 100*10 units from the quota
      long msgCost;
      if (joinedGroups.contains(groupId)) {
         // no need to send to one's self
         msgCost = msgSize * (groupsMembers.get(groupId).size() - 1);
      } else {
         msgCost = msgSize * groupsMembers.get(groupId).size();
      }

      long totalQuota = getTotalQuota();

      if (totalQuota < msgCost || (!sendingFromBuffer && bufferedMsgs.containsKey(groupId))) {
         if (sendingFromBuffer) {
            return false;
         } else {
            long bufTotal = getBufferSize(false);

            if (bufTotal + msgSize <= bufferMax) {
               if (bufferedMsgs.containsKey(groupId) == false) {
                  bufferedMsgs.put(groupId, new ArrayDeque<Integer>());
               }
               bufferedMsgs.get(groupId).add(msgSize);
            }
            // otherwise the message gets dropped

            return false;  // the message was not sent
         }
      } else {
         getQuotaToUse(msgSize, true);  // no need to use return value because at this point we must have enough quota to cover msgSize
         MulticastEvent e = new MulticastEvent(node.id, groupId, msgSize);
         node.send(e);

         return true;   // the message was sent
      }
   }

   public void quotaRequested(NodeId requester, long requestedQuota)
   {
      if (bufferedMsgs.isEmpty()) {
         long quotaToGive = getQuotaToUse(requestedQuota, false);

         if (quotaToGive > 0) {  // give quota to requester
            GiveQuotaEvent e = new GiveQuotaEvent(node.id, requester, quotaToGive, lastMasterEpoch);
            node.send(e);
         }
      }
   }

   public void requestQuota()
   {
      NodeId[] senders = new NodeId[unusedQuotas.size()];
      senders = unusedQuotas.keySet().toArray(senders);

      long quotaNeeded = getBufferSize(true) - remainingQuota;

      if (senders.length == 0 || quotaNeeded == 0)
         return;

      Random rand = RandomnessManager.getRandomGenerator(Ajil.class.getSimpleName());

      // NOTE: I currently request additional quota from other nodes assuming that
      // each other node had the same sending quota as me.
      // This is how the number of nodes to ask is determined
      int numNodesToAsk = Math.min((int) (1 + (quotaNeeded / (lastAssignedQuota + 1))), senders.length);
      HashSet<NodeId> nodesToAsk = new HashSet<NodeId>(numNodesToAsk);

      while (nodesToAsk.size() < numNodesToAsk) {
         nodesToAsk.add(senders[rand.nextInt(senders.length)]);
      }

      for (NodeId requestFrom : nodesToAsk) {
         RequestQuotaEvent e = new RequestQuotaEvent(node.id, requestFrom, quotaNeeded);
         node.send(e);
      }
   }

   public void publishUnusedQuota()
   {
      if (declaredAsSender)
         node.send(new UnusedQuotaEvent(node.id, getTotalQuota(), lastAssignedQuota - usedAssignedQuota));
   }

   public void receiveMulticast(NodeId src, long msgSize)
   {
      if (!src.equals(node.id)) {
         multicastTraffic += msgSize;
         newMulticastTraffic += msgSize;
      }
   }

   public long getNewTrafficCount()
   {
      long report = newMulticastTraffic;
      newMulticastTraffic = 0;
      return report;
   }

   public void setSendingQuota(long quota, long masterEpoch)
   {
      remainingQuota = quota;
      usedAssignedQuota = 0;
      lastAssignedQuota = quota;
      // TODO: should the check be a != or a < ??
      if (lastMasterEpoch != masterEpoch) {
         // invalidate given quotas by other peers
         addedQuota.clear();
         lastMasterEpoch = masterEpoch;
      }
      sendFromBuffer();
   }

   public void receiveAddedQuota(NodeId sender, long quota, long masterEpoch)
   {
      if (masterEpoch < lastMasterEpoch) {
         return;  // do nothing
      } else if (masterEpoch > lastMasterEpoch) {
         addedQuota.clear();  // invalidate previously given quotas in older epochs
      }

      addedQuota.put(sender, quota);
      sendFromBuffer();
   }

   /**
    * send what you can from the buffer
    */
   private void sendFromBuffer()
   {
      // list of groups with buffered messages
      Iterator<Integer> bufferedGroups = bufferedMsgs.keySet().iterator();

      while (bufferedGroups.hasNext()) {
         int groupId = bufferedGroups.next();
         // list of msgs buffered for this group
         ArrayDeque<Integer> groupBuffer = bufferedMsgs.get(groupId);

         // try to send this message if you can
         while (sendMulticast(groupId, groupBuffer.peek(), true)) {
            // if sending was successful, remove the message from the buffer
            groupBuffer.remove();

            if (groupBuffer.isEmpty()) {
               bufferedGroups.remove();
               break;
            }
         }
      }
   }

   private long getBufferSize(boolean adjustForGroupSize)
   {
      long bufTotal = 0;
      for (int group : bufferedMsgs.keySet()) {
         for (long msg : bufferedMsgs.get(group)) {
            if (adjustForGroupSize) {
               if (joinedGroups.contains(group))
                  bufTotal += msg * (groupsMembers.get(group).size() - 1);
               else
                  bufTotal += msg * groupsMembers.get(group).size();
            } else {
               bufTotal += msg;
            }
         }
      }
      return bufTotal;
   }

   private long getTotalQuota()
   {
      long totalQuota = remainingQuota;
      for (long q : addedQuota.values())
         totalQuota += q;
      
      return totalQuota;
   }

   private long getQuotaToUse(long neededQuota, boolean isToSendMsg)
   {
      long quotaObtained = 0;

      // First try to satisfy quota need from the quotas obtained from others
      Iterator<Entry<NodeId, Long>> iter = addedQuota.entrySet().iterator();
      while (iter.hasNext()) {
         Entry<NodeId, Long> givenQuota = iter.next();
         long q = givenQuota.getValue();

         if (q > neededQuota) {
            givenQuota.setValue(q - neededQuota);
            quotaObtained += neededQuota;
            neededQuota = 0;
         } else {
            quotaObtained += q;
            neededQuota -= q;
            iter.remove();
         }

         if (neededQuota == 0)
            break;
      }

      // If we still need some more, use own quota assigned from QuotasManager
      if (neededQuota > 0 && remainingQuota > 0) {
         if (remainingQuota > neededQuota) {
            quotaObtained += neededQuota;
            usedAssignedQuota += (isToSendMsg) ? neededQuota : 0;
            remainingQuota -= neededQuota;
         } else {
            quotaObtained += remainingQuota;
            usedAssignedQuota += (isToSendMsg) ? remainingQuota : 0;
            remainingQuota = 0;
         }
      }

      return quotaObtained;
   }
}
