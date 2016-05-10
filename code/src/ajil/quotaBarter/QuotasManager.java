package ajil.quotaBarter;

import ajil.Ajil;
import ajil.quotaBarter.events.AcceptSenderEvent;
import ajil.quotaBarter.events.AssignQuotaEvent;
import ajil.quotaBarter.events.RecomputeQuotasEvent;
import ajil.quotaBarter.events.DeclareQuotasManagerEvent;
import ajil.quotaBarter.events.UnusedQuotaEvent;
import ajil.infrastructure.LeafNode;
import java.util.HashMap;
import java.util.Map.Entry;
import naseem.NodeId;
import naseem.workload.WorkloadDriver;

/**
 *
 * @author Hussam
 */
public class QuotasManager extends Ajil
{
   private long globalLimit;
   private long epoch;

   private HashMap<NodeId, Long> sendersQuotas;


   public QuotasManager(LeafNode node)
   {
      super(node);
      globalLimit = 0;
      epoch = 0;

      sendersQuotas = new HashMap<NodeId, Long>();
   }

   @Override
   public void initializeService()
   {
      joinGroup(updatesGroupId);

      long now = WorkloadDriver.getRuntime().getTime();
      node.scheduleEvent(new RecomputeQuotasEvent(node.id, now));
   }

   public void setGlobalLimit(long bytesLimit)
   {
      this.globalLimit = bytesLimit;
   }

   public void addSender(NodeId sender)
   {
      sendersQuotas.put(sender, (long) 0);
      node.send(new AcceptSenderEvent(node.id, sender, groupsMembers));

      // If this is the only known sender, give assign him his quota now
      // rather than waiting for the next epoch
      if (sendersQuotas.size() == 1)
         assignQuotas();
   }

   @Override
   public void updateAvailableQuotas(NodeId nodeId, long totalUnused, long assignedUnused)
   {
      // we only care about the unused assigned quota here
      // do not delete entries even if unused quota is 0
      unusedQuotas.put(nodeId, assignedUnused);
   }

   public void assignQuotas()
   {
      epoch++;

      // NOTE: The QM periodically advertises its position to overcome
      // node arrival ordering issues and to help detect a failed QM
      // TODO: should the QM periodically declare itself?
      node.send(new DeclareQuotasManagerEvent(node.id, epoch));


      // NOTE: the keyset for unusedQuotas may not be the same as that for sendersQuotas due to network delays
      if (sendersQuotas.size() > 0) {
         // start off by dividing the global limit equally amongst senders
         long quotaPerSender = globalLimit / sendersQuotas.size();
         for (NodeId sender : sendersQuotas.keySet()) {
            sendersQuotas.put(sender, quotaPerSender);
         }

         // then make modifications based on reported unused quotas
         long reclaimableQuota = 0;
         int numHighSenders = 0;
         // ... first count total unused quota
         // ... and count number of senders who used all their previously assigned quota
         for (Entry<NodeId, Long> senderUnused : unusedQuotas.entrySet()) {
            reclaimableQuota += Math.min(senderUnused.getValue(), quotaPerSender);
            if (senderUnused.getValue() == 0)
               numHighSenders++;
         }

         // ... if there are senders who used all their previous quota,
         // ... then divide reclaimed unused quota equally amongst high senders
         if (numHighSenders > 0) {
            long extraQuota = reclaimableQuota / numHighSenders;
            for (NodeId sender : unusedQuotas.keySet()) {
               long unused = unusedQuotas.get(sender);
               if (unused == 0) {
                  sendersQuotas.put(sender, quotaPerSender + extraQuota);
               } else {
                  sendersQuotas.put(sender, Math.max(quotaPerSender - unused, 0));
               }
            }
         }

         // finally, publish new quotas to all senders
         for (Entry<NodeId, Long> sq : sendersQuotas.entrySet()) {
            node.send(new AssignQuotaEvent(node.id, sq.getKey(), sq.getValue(), epoch));
         }
      }
   }
}
