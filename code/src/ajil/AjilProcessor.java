package ajil;

import ajil.quotaBarter.RateLimiter;
import ajil.quotaBarter.QuotasManager;
import ajil.infrastructure.LeafNode;
import ajil.infrastructure.StarTopology;
import java.util.HashSet;
import java.util.Random;
import naseem.Node;
import naseem.NodeId;
import naseem.RandomnessManager;
import naseem.interfaces.StatsCollector;
import naseem.workload.WorkloadDriver;
import naseem.workload.WorkloadProcessor;

/**
 *
 * @author Hussam
 */
public class AjilProcessor extends WorkloadProcessor
{
   private Random rand;
   private NodeId quotasManager;

   private class AjilStatsCollector implements StatsCollector
   {
      private long startTime;

      public AjilStatsCollector(long startTime)
      {
         this.startTime = startTime;
      }
      
      @Override
      public void collectStats()
      {
         if (WorkloadDriver.getInstance().isSimulated()) {
            long time = WorkloadDriver.getRuntime().getTime() - startTime - 1;
            // -1 is subtracted because getTime() returns the time now
            // (of events yet to take place) and we want to report what happened
            // in the previous step
            
            long totalTraffic = 0;
            for (Node n : WorkloadDriver.getInstance().getSimulator().getNodes()) {
               if (n instanceof LeafNode) {
                  totalTraffic += ((RateLimiter) n.getService(RateLimiter.class.getSimpleName())).getNewTrafficCount();
               }
            }

            System.out.println(time + "\t" + totalTraffic);
         }
      }
   }

   @Override
   public boolean enableService(String str)
   {
      String tokens[] = str.split(" ");

      if (tokens[0].equals(Ajil.class.getSimpleName())) {
         rand = RandomnessManager.addRandomGenerator(Ajil.class.getSimpleName());
         
         if (WorkloadDriver.getInstance().isSimulated()) {
            for (Node n : WorkloadDriver.getInstance().getSimulator().getNodes()) {
               if (n instanceof LeafNode) {
                  n.setService(RateLimiter.class.getSimpleName(), new RateLimiter((LeafNode) n));
               }
            }
         }
         return true;
      }

      return false;
   }

   @Override
   public void initialize()
   {
      if (WorkloadDriver.getInstance().isSimulated()) {
         for (Node n : WorkloadDriver.getInstance().getSimulator().getNodes()) {
            RateLimiter rateLimiter = (RateLimiter) n.getService(RateLimiter.class.getSimpleName());
            if (rateLimiter != null) {
               rateLimiter.initializeService();
            }
            
            QuotasManager qm = (QuotasManager) n.getService(QuotasManager.class.getSimpleName());
            if (qm != null) {
               qm.initializeService();
            }
         }
      }
   }

   @Override
   public boolean process(long time, String str)
   {
      String tokens[] = str.split(" ");

      if (tokens[0].equals("create_network")) {
        if (WorkloadDriver.getInstance().isSimulated())  {
           if (tokens[1].equals("Star")) {
              int numNodes = Integer.parseInt(tokens[2]);
              WorkloadDriver.getInstance().getSimulator().initNodes(StarTopology.initializeNodes(numNodes));
           } else {
              return false;
           }
        }
        
      } else if (tokens[0].equals("set_quota_manager")) {
         quotasManager = NodeId.parse(tokens[1]);

         if (WorkloadDriver.getInstance().isSimulated()) {
            LeafNode node = (LeafNode) WorkloadDriver.getInstance().getSimulator().getNode(quotasManager);
            node.setService(QuotasManager.class.getSimpleName(), new QuotasManager(node));
         }

      } else if (tokens[0].equals("set_global_limit")) {
         long limit = Long.parseLong(tokens[1]);

         if (WorkloadDriver.getInstance().isSimulated()) {
            Node n = WorkloadDriver.getInstance().getSimulator().getNode(quotasManager);
            ((QuotasManager) n.getService(QuotasManager.class.getSimpleName())).setGlobalLimit(limit);
         }

      } else if (tokens[0].equals("all_join_rand_groups")) {
         int numGroupsToJoin = Integer.parseInt(tokens[1]);
         int totalNumGroups  = Integer.parseInt(tokens[2]);

         if (WorkloadDriver.getInstance().isSimulated()) {
            for (Node n : WorkloadDriver.getInstance().getSimulator().getNodes()) {
               RateLimiter rl = (RateLimiter) n.getService(RateLimiter.class.getSimpleName());
               if (rl != null) {
                  HashSet<Integer> groupsJoined = new HashSet<Integer>();

                  while (groupsJoined.size() < numGroupsToJoin) {
                     int groupId = rand.nextInt(totalNumGroups);
                     if (groupsJoined.contains(groupId) == false) {
                        groupsJoined.add(groupId);
                        rl.joinGroup(groupId);
                     }
                  }
               }
            }
         }

      } else if (tokens[0].equals("report_ajil_w_freq")) {
         int freq = Integer.parseInt(tokens[1]);
         WorkloadDriver.getInstance().addStatsCollector(freq, new AjilStatsCollector(time));

      } else if (tokens[0].equals("join_group")) {
         NodeId nodeId = NodeId.parse(tokens[1]);
         int groupId = Integer.parseInt(tokens[2]);

         if (WorkloadDriver.getInstance().isSimulated()) {
            Node n = WorkloadDriver.getInstance().getSimulator().getNode(nodeId);
            ((RateLimiter) n.getService(RateLimiter.class.getSimpleName())).joinGroup(groupId);
         }

      } else if (tokens[0].equals("send_mcast")) {
         NodeId nodeId = NodeId.parse(tokens[1]);
         int groupId = Integer.parseInt(tokens[2]);
         int msgSize = Integer.parseInt(tokens[3]);

         if (WorkloadDriver.getInstance().isSimulated()) {
            Node n = WorkloadDriver.getInstance().getSimulator().getNode(nodeId);
            ((RateLimiter) n.getService(RateLimiter.class.getSimpleName())).sendMulticast(groupId, msgSize);
         }

      } else {    // could not match command
         return false;
      }


      return true;
   }

}
