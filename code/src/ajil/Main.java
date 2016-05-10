package ajil;

import java.io.IOException;
import naseem.workload.WorkloadDriver;

/**
 *
 * @author Hussam
 */
public class Main
{
   public static void main(String[] args)
   {
      if (args.length < 1) {
         System.err.println("USAGE: ajil [-no-sim] workload_file");
         System.exit(-1);
      }

      boolean isSimulated = true;
      String workloadFile = "";

      for (String arg : args) {
         if(arg.equals("-no-sim"))
            isSimulated = false;
         else
            workloadFile = arg;
      }

      WorkloadDriver.init(isSimulated);
      try {
         WorkloadDriver.getInstance().processWorkload(workloadFile, new AjilProcessor());
      } catch (IOException e) {
         System.err.println("Error reading workload file");
      }
   }
}
