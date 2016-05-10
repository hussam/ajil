#set term png
set term post eps enhanced

set size 0.65, 0.65
set xtic auto
set ytic auto
set pointsize 1.5

set style line 1 lt 9 lw 2.5
set style fill solid 0.5 border

set output "ajil-policy1.eps"
set title "Multicast Traffic with Ajil using Policy #1"
set xlabel "Time (s)"
set ylabel "Traffic (KB/s)"
set yr[200:1200]
set xr[0:1000]

plot "p1/data.csv" using 1:2 every 1 title "IPMC no AJIL" with line lw 1.5, \
      "p1/data.csv" using 1:3 every 1 title "AJIL, Policy#1" with line lw 1.5, \
      "p1/data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1

set output "ajil-policy2.eps"
set title "Multicast Traffic with Ajil using Policy #2"
plot "p2/data.csv" using 1:2 every 1 title "IPMC no AJIL" with line lw 1.5, \
      "p2/data.csv" using 1:3 every 1 title "AJIL, Policy#2" with line lw 1.5, \
      "p2/data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1



set output "groups-utility.eps"
set title  "Average Utility per Group with Different Policies"
set xlabel "Time (s)"
set ylabel "Utility"
set yr[0:1.5]
set xr[0:1000]

plot "p1/data.csv" using 1:6:7 every 50 title "Avg Group Utility, Policy#1" with yerrorbars lw 1.5,\
      "p1/data.csv" using 1:6  every 50 title "" with line lw 1.5,\
      "p2/data.csv" using 1:6:7 every 50 title "Avg Group Utility, Policy#2" with yerrorbars lw 1.5,\
      "p2/data.csv" using 1:6  every 50 title "" with line lw 1.5,\
      "p1/data.csv" using 1:7  every 10 title "Standard Deviation, Policy#1" with \
         boxes fs pattern 1 lw 1.5,\
      "p2/data.csv" using 1:7  every 10 title "Standard Deviation, Policy#2" with \
         boxes fs solid 0.5 lw 1.5


set output "channels-utility.eps"
set title  "Average Utility per Channel with Different Policies"
set xlabel "Time (s)"
set ylabel "Utility"
set yr[0:1.5]
set xr[0:1000]

plot "p1/channel_data.csv" using 1:2:3 every 50 title "Avg Channel Utility, Policy#1" with yerrorbars lw 1.5,\
      "p1/channel_data.csv" using 1:2  every 50 title "" with line lw 1.5,\
      "p2/channel_data.csv" using 1:2:3 every 50 title "Avg Channel Utility, Policy#2" with yerrorbars lw 1.5,\
      "p2/channel_data.csv" using 1:2  every 50 title "" with line lw 1.5,\
      "p1/channel_data.csv" using 1:3  every 10 title "Standard Deviation, Policy#1" with\
         boxes fs pattern 1 lw 1.5,\
      "p2/channel_data.csv" using 1:3  every 10 title "Standard Deviation, Policy#2" with\
         boxes fs solid 0.5 lw 1.5
