package ajil.infrastructure;

import java.util.HashMap;
import naseem.Node;
import naseem.NodeId;

/**
 *
 * @author Hussam
 */
public class StarTopology
{
   public static HashMap<NodeId, Node> initializeNodes (int numNodes)
   {
      HashMap<NodeId, Node> nodes = new HashMap<NodeId, Node>(numNodes+1);

      // Star center
      NodeId routerId = new NodeId(0);
      RouterNode router = new RouterNode(routerId);

      for (int i=1; i<=numNodes; i++)
      {
         NodeId nid = new NodeId(i);
         nodes.put(nid, new LeafNode(nid, routerId));
      }
      
      router.setNeighbors(nodes.keySet());   // router has everybody else as its neighbor
      nodes.put(routerId, router);           // finally add the router to node set
      
      return nodes;
   }
}
