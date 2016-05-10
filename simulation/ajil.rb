#!/usr/bin/ruby

require 'utils.rb'

$nodes  = (0..19).to_a
$groups = (0..9).to_a

$members = []         # members of each group
$groups_joined = []   # groups that a particular node has joined

# the following maps will be indexed by the (group, node) pair to maintain
# different info for the same node in different groups

$sent_in_tick = {}    # number of bytes a node in a group sent in this current tick
$wanted_in_tick = {}  # number of bytes a node wanted to send (without AJIL) in this tick
$epoch_length = {}    # how many ticks in an epoch
$slow_down_timer = {} # for how many ticks to slow down sending

$last_slow_down = {}

$epoch_sum = []       # total amount of aggregate traffic as last calculated by a node 'n'
$broadcast_chnl = []  # broadcast of per group transmission volume in the last epoch
$broadcast_time = []  # time of the last broadcast from a group (used to time out dead groups)


$epochs = {}          # (listening window) periods of monitoring incoming msgs per node
# epochs[[g,n]] = [epoch1, epoch2, epoch3]
# epoch1 = [n1, n2, n3 .. ]

$zeros = []             # used to initialize an epoch
$ticks = 1000           # total time to run the simulation
$rate_limit = 1000      # limit on num of msgs per "big tick"
$big_tick = 1           # ticks per "big tick"
$broadcast_timeout = 10 # how long to keep a broadcast message before discarding it

$group_sent_in_btick = []    # num of msgs sent so far in this "big tick" per group
$group_sent_in_tick  = []    # num of msgs sent so far in this current tick
$sent_in_broadcast = 0       # amount of broadcast messages sent

$patterns_file = nil    # file with application (want) sending rates
$traffic_shape = nil    # traffic pattern

# Ajil Knobs
$ALPHA  = 1     # the top ALPHA groups will be considered in the response domain
$BETA   = 1     # the top BETA nodes in a group will be in the response domain when the rate is exceeded
$GAMMA  = 1     # the smoothing factor for the Exponential Moving Average for the sending history
$C      = 0.1   # fraction of the total amount of traffic dedicated for broadcast
$SD     = 1.0   # percentage of traffic to Solw Down by if needed
$SU     = 0.50  # percentage of slow down to try to Speed Up by if possible (i.e. won't push over Limit)
$policy = 2     # indicates the slowdown policy to use
$threshold_limit = 0.95 * $rate_limit

def init
   # initialize membership arrays
   $groups.each do |g|
      $members[g] = []
      $group_sent_in_btick[g] = 0
      $group_sent_in_tick[g] = 0
      $broadcast_chnl[g] = 0
      $broadcast_time[g] = 0
   end

   $nodes.each do |n|
      $zeros[n] = 0     # set up the 'zeros' list
      $epoch_sum[n] = 0 # node has not seen any traffic yet

      # how many groups to join
      n_groups = rand($groups.length) + 1    # KNOB
      n_groups = $groups.length
      n_groups = 5
      # which groups to join
      $groups_joined[n] = []
      groups_list = $groups.clone
      n_groups.times do
         # for the number of groups we have to join,
         # delete an element of the list of groups that is available
         # and join the group corresponding to that element
         g = groups_list.delete_at( rand(groups_list.length) )
         $members[g] << n
         $groups_joined[n] << g

         # other per-group settings
         $epochs[[g,n]] = []
         $slow_down_timer[[g,n]] = 0
         $last_slow_down[[g,n]] = 0

         $epoch_length[[g,n]] = 1            # KNOB
      end

   end
end

# Reset all counters
def reset()
   $groups.each do |g|
      $group_sent_in_btick[g] = 0
      $group_sent_in_tick[g] = 0
      $broadcast_chnl[g] = 0
      $broadcast_time[g] = 0
   end

   $nodes.each do |n|
      $epoch_sum[n] = 0
      $groups_joined[n].each do |g|
         $epochs[[g,n]] = []
         $sent_in_tick[[g,n]] = {}
         $wanted_in_tick[[g,n]] = {}
         $slow_down_timer[[g,n]] = 0
         $last_slow_down[[g,n]] = 0
      end
   end
end

# instantiate a new listening window if needed
def prep_epoch(t)
   $groups.each do |g|
      $members[g].each do |n|
         if fmod(t, $epoch_length[[g,n]])
            $epochs[[g,n]].delete_at(0) if $epochs[[g,n]].size == 3
            $epochs[[g,n]] << $zeros.clone
         end
      end
   end
end

# let clients broadcast their group aggregates in this tick
def broadcast_info(t)
   $groups.each do |g|
      $members[g].each do |n|
         broadcast_probability = ($C * $group_sent_in_tick[g].to_f) / $members[g].size

         if rand() <= broadcast_probability
            $broadcast_chnl[g] = $group_sent_in_tick[g]
            $broadcast_time[g] = t
            $sent_in_broadcast += 1
         end
      end
   end
end

# let clients send their msgs (if any) in this tick
def send_msgs(t)
   $groups.each { |g| $group_sent_in_btick[g] = 0 } if fmod(t, $big_tick)
   $groups.each do |g|
      $group_sent_in_tick[g] = 0
      $members[g].each do |n|
         sending_rate(g, n, t)   # set the sending rate for a node in this
         $group_sent_in_btick[g] += $sent_in_tick[[g,n]]
         $group_sent_in_tick[g]  += $sent_in_tick[[g,n]]
      end
   end
end

# clients receive incoming messages in a tick
def recv_msgs(t)
   $groups.each do |g|
      $members[g].each do |n|
         $members[g].each do |m|
            $epochs[[g,n]].last[m] += $sent_in_tick[[g, m]]
         end
      end
   end
end

# node re-evaluate strategy for next epoch if epoch finished
def reval_strategy(t, n, g = nil)
   # are we exceeding the global limit ?
   traffic = Array.new($groups.length, 0)
   $epoch_sum[n] = 0
   $groups.each do |gg|
      traffic[gg] = $groups_joined[n].index(gg) ? $group_sent_in_tick[gg] : $broadcast_chnl[gg]
      $epoch_sum[n] += traffic[gg]
   end
   return if $epoch_sum[n] < $threshold_limit

   # so we are exceeding the global limit
   #
   # COMPUTE THE REACTION DOMAIN
   #
   # FIRST, see which groups should react (from "ALPHA")
   reaction_groups = []
   $groups.each { |gg| reaction_groups << [traffic[gg], gg] }
   reaction_groups = reaction_groups.sort!.reverse![0, to_next_i($ALPHA*$groups.length)].map! { |x| x[1] }

   # THEN
   # if 'g' is already specified, then react only to 'g' and only if it is in the reaction domain
   if g.nil? == false
      return if reaction_groups.index(g).nil?
      reaction_groups = [g]
   end

   # if 'n' is in any of the reaction groups, check if it is in the set of
   # "reaction nodes"
   reaction_groups.each do |g|
      if $groups_joined[n].index(g) && fmod(t, $epoch_length[[g,n]])
         reaction_nodes = []
         $members[g].each { |nn| reaction_nodes << [$epochs[[g,n]].last[nn], nn] }
         reaction_nodes = reaction_nodes.sort!.reverse![0, to_next_i($BETA*$members[g].length)].map! { |x| x[1] }

         if reaction_nodes.index(n)
#            puts "group #{g} node #{n} slowing down, tick #{t}"
            slow_down(g, n)
         end
      end
   end
end

def slow_down(g, n)
   $slow_down_timer[[g,n]] = $SD
end

def sending_rate(g, n, t)
   # This relies on having the wanted_in_tick to have been read for this tick

   # Apply a slowdown
   if $slow_down_timer[[g,n]] > 0
      # Policy Part
      # This part specifies how much to slow down by.
      # This could be a fixed amount (X KB) or a percentage of the current rate
      slow_down_amount = 0

      case $policy
      when 1 then
         # Policy #1: Fixed Perentage (MIMD).
         # Start from $SD * Wanted, then multiplicatively go back up
         slow_down_amount = $wanted_in_tick[[g,n]] * $slow_down_timer[[g,n]]

      when 2 then
         # Policy #2: Excess Percentage
         # Start from the percentage of excess above the limit
         excess = $epoch_sum[n] - $threshold_limit
         percent1 = (excess.to_f / $threshold_limit)
         percent1 = 1 if percent1 > 1
         percent2 = $last_slow_down[[g,n]]

         if percent1 > percent2
#            $slow_down_timer[[g,n]] = $SD
            $last_slow_down[[g,n]] = percent1
         end
         slow_down_amount = $wanted_in_tick[[g,n]] * $last_slow_down[[g,n]] * $slow_down_timer[[g,n]]
      end


      # This part does not depend on the policy
      # First see how much traffic is anticipated overall
      # The minimum amount you can send is how much you want minus the slow down
      # amount. The maximum you can ssend is acheived by decreasing the slow
      # down amount by the speed up percentage defined before. If it is
      # possible, send the maximum amount (and adjust), otherwise send the minimum.
      other_traffic = $epoch_sum[n] - $sent_in_tick[[g,n]]
      min_to_send = $wanted_in_tick[[g,n]] - (slow_down_amount).to_i
      max_to_send = $wanted_in_tick[[g,n]] - (slow_down_amount * (1 - $SU)).to_i

      if (other_traffic + max_to_send) < $threshold_limit
         $sent_in_tick[[g,n]] = max_to_send
         $slow_down_timer[[g,n]] *= (1 - $SU)
         $slow_down_timer[[g,n]] = 0 if $slow_down_timer[[g,n]] < 0.01
      else
         $sent_in_tick[[g,n]] = min_to_send
      end
#      puts "group #{g} node #{n} o_t = #{other_traffic} sd_t = #{$slow_down_timer[[g,n]]} want #{$wanted_in_tick[[g,n]]} min #{min_to_send} max #{max_to_send} sent #{$sent_in_tick[[g,n]]}"
   else
      $sent_in_tick[[g,n]] = $wanted_in_tick[[g,n]]
   end

   if $sent_in_tick[[g,n]] < 0
      puts "ERROR! Sending less than Zero!!"
      exit(0)
   end

   return $sent_in_tick[[g,n]]
end

# consume a tick
def tick(t)
   # time out old broadcast messages
   $groups.each do |g|
      if (t - $broadcast_time[g]) > $broadcast_timeout
         $broadcast_chnl[g] = 0
      end
   end
end


# Write the sending rates of all the nodes in all the groups into a file
# this is useful for rerunning an experiment with multiple parameters
def set_sending_patterns()
   File.delete("sending-patterns.csv") if File.exists?("sending-patterns.csv")
   $patterns_file = File.open("sending-patterns.csv", File::CREAT | File::RDWR)

   peeks = [150, 750]
   up = 350
   down = 700
   0.upto($ticks) do |t|
      abs_diffs = {}
      peeks.each do |p|
         abs_diffs[p] = t - p
         abs_diffs[p] *= -1 if abs_diffs[p] < 0
      end

      $groups.each do |g|
         $members[g].each do |n|
            # the sending rate for this tick
            rate = rand(20)
            # use the following for SQUARE pattern
            #$traffic_shape = "square"
            #rate += 20 + rand(20) if t > up && t < down && g == 9
            # use the following for the PEEK pattern
            $traffic_shape = "peek"
            peeks.each do |p|
               if rand(p) > abs_diffs[p]
                  rate += rand(10)
                  break
               end
            end
            # use the following for the GRADUAL INCREASE patter
            #$traffic_shape = "linear"
            #rate += rand(t/25).to_i

            $patterns_file.write "#{g} #{n} #{rate},"
         end
      end
      $patterns_file.write "\n"
   end
end



# A single run of the simulation
# Go through all the ticks,
# In each tick send, recv, broadcast, report
def run(aggregate_file, channel_file)
   File.delete(aggregate_file) if File.exists?(aggregate_file)
   ag_f = File.open(aggregate_file, File::CREAT | File::WRONLY)

   File.delete(channel_file) if File.exists?(channel_file)
   ch_f = File.open(channel_file, File::CREAT | File::WRONLY)

   $patterns_file.rewind

   # print aggregate legend
   str = "t\tdesired\tactual\tlimit\tb-cast\tf-avg\tf-sdev\t"
   $groups.each { |g| str << "f-g#{g}\t" }  # fairness to each group
   $groups.each { |g| str << "d-g#{g}\t" << "g#{g}\t" }  # group desired and group traffic
   ag_f.write(str+"\n")

   # print per channel legend
   str = "t\tu_avg\tu_sdev\t"
   $groups.each{|g| $members[g].each{|n| str << "d-g#{g}n#{n}\t" << "g#{g}n#{n}\t"}}
   ch_f.write(str+"\n")


   0.upto($ticks) do |t|
      # get this sending pattern in this tick
      tick_pattern = []
      $patterns_file.readline.scan(/(.+?),+/) { tick_pattern << $1.split(' ') }
      tick_pattern.each {|tp| $wanted_in_tick[[tp[0].to_i, tp[1].to_i]] = tp[2].to_i }

      prep_epoch(t)
      send_msgs(t)
      recv_msgs(t)
      broadcast_info(t)

      ag_f.write(t.to_s + "\t")
      ch_f.write(t.to_s + "\t")

      # show me the traffic in each big tick
      if fmod(t, $big_tick)
         # calculate what the traffic would have been without AJIL
         desired_traffic = 0
         group_wanted = []
         channels = []
         str = ""
         $groups.each do |g|
            group_wanted[g] = 0
            $members[g].each do |n|
               ch_wanted = $wanted_in_tick[[g,n]]
               desired_traffic += ch_wanted
               group_wanted[g] += ch_wanted
               if ch_wanted == 0
                  channels << 1.0
               else
                  channels << ($sent_in_tick[[g,n]].to_f / ch_wanted)
               end
               str << (ch_wanted.to_s + "\t" + $sent_in_tick[[g,n]].to_s + "\t")
            end
         end
         ch_avg, ch_sdev = avg_and_sdev(channels)
         ch_f.write("%.2f\t" % ch_avg)
         ch_f.write("%.2f\t" % ch_sdev)
         ch_f.write(str << "\n")

         # calculate what the traffic is with AJIL
         str = ""
         fairness_str = ""
         total_traffic = 0
         groups_fairness = []
         $groups.each do |g|
            total_traffic += $group_sent_in_btick[g]
            str << group_wanted[g].to_s + "\t"
            str << $group_sent_in_btick[g].to_s + "\t"

            gfair = 0.0
            if group_wanted[g] == 0
               gfair = 1.0
            else
               gfair = ($group_sent_in_btick[g].to_f / group_wanted[g])
            end

            groups_fairness << gfair
            fairness_str << "\t%.2f" % gfair
         end

         # aggregate fairness
         agf_avg, agf_sdev = avg_and_sdev(groups_fairness)
         fairness_str = ("%.2f\t" % agf_avg) + ("%.2f" % agf_sdev) + fairness_str + "\t"

         # print out traffic reports
         ag_f.write(desired_traffic.to_s + "\t" + total_traffic.to_s + "\t " + $rate_limit.to_s + "\t" + $sent_in_broadcast.to_s + "\t" + fairness_str + str + "" + "\n")
         $sent_in_broadcast = 0
      end

      $nodes.each { |n| reval_strategy(t, n) }
      tick(t)
   end
   ag_f.close
end


# Go through all the runs of the simulation
# Make all the runs and print the results
def start
   init()
   set_sending_patterns()

   membership_str = ""
   $groups.each do |g|
      str = "Member of Group #{g}: "
      $members[g].each { |n| str << (n.to_s + " ") }
      membership_str << str << "\n"
   end

   all_runs()

   puts membership_str
   $patterns_file.close
end

# All the runs in the experiment
def all_runs()
   experiment = 0

   case experiment
   when 0 then    # the RATE-LIMITING experiment
      # Note: sending pattern here is the single-peek (triangle)
      $ALPHA = 0.5; $BETA = 0.25; $C = 0.01; $policy = 2
      run_and_record("rate-limiting/#{$traffic_shape}")

   when 1 then    # the STALENESS experiment
      # differet broadcast probabilities for policy 2
      $ALPHA = 0.5; $BETA = 0.5; $C = 0.01; $policy = 2
      run_and_record("staleness/c.01")

      $ALPHA = 0.5; $BETA = 0.5; $C = 0.001; $policy = 2
      run_and_record("staleness/c.001")

   when 2 then    # the REACTION DOMAIN experiment
      # different sizes of the reaction domain
      $ALPHA = 1; $BETA = 1; $C = 0.01; $policy = 2
      run_and_record("rd/1")

      $ALPHA = 1; $BETA = 0.5; $C = 0.01; $policy = 2
      run_and_record("rd/0.5")

      $ALPHA = 0.5; $BETA = 0.5; $C = 0.01; $policy = 2
      run_and_record("rd/0.25")

      $ALPHA = 0.5; $BETA = 0.25; $C = 0.01; $policy = 2
      run_and_record("rd/0.125")

      $ALPHA = 0.25; $BETA = 0.25; $C = 0.01; $policy = 2
      run_and_record("rd/0.625")

   when 3 then    # the UTILITY & FAIRNESS experiment
      # Note: sending pattern here is the square-wave (up/down).
      # All groups experience the increase in traffc.
      # Increase by guaranteed 2 and random 5.
      $ALPHA = 0.5; $BETA = 0.25; $C = 0.01; $policy = 2
      run_and_record("fairness/rd.125")

      $ALPHA = 1; $BETA = 1; $C = 0.01; $policy = 2
      run_and_record("fairness/rd1")

   when 4 then    # the POLICY experiment
      $ALPHA = 0.5; $BETA = 0.25; $C = 0.01; $policy = 1
      run_and_record("policy/p1")

      $ALPHA = 0.5; $BETA = 0.25; $C = 0.01; $policy = 2
      run_and_record("policy/p2")
   end
   return
end

def run_and_record(dirname)
   dirs = dirname.split('/')
   dirs.each_index do |i|
      dir = dirs[0, i+1].join('/')
      Dir.mkdir(dir) unless File.exist?(dir)
   end

   run(dirname+"/data.csv", dirname+"/channel_data.csv")
   reset()
end


start()
