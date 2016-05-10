set term png
set output "ajil.png"
#set term post eps enhanced
#set output "ajil.eps"
set size 0.65, 0.65

#set size 2,2
#set origin 0,0
#set multiplot

#set size 1,1
#set origin 0,1


set title "IPMC Traffic w/ AJIL"
set xlabel "Time"
set ylabel "Traffic (KB/s)"
set yr[0:1500]
set xtic auto
set ytic auto
set pointsize 1.5

set style line 1 lt 9 lw 2.5
set style fill solid 0.5 border

plot "data.csv" using 1:2 every 1 title "IPMC before AJIL" with line lw 1.5, \
      "data.csv" using 1:3 every 1 title "IPMC after AJIL" with line lw 1.5, \
      "data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1, \
      "data.csv" using 1:5 every 1 title "Broadcast Traffic" with boxes

#set output "excess.png"
#set title "Excess Percentage"
#set xlabel "Time"
#set ylabel "Excess Percentage"
#set yr[0:30]
#set xr[400:700]
#
#plot "~/research/ajil/evaluation/staleness/c.01/data.csv" using \
#         1:(100*($3-$4)/$3) every 10 title "C = .01" with linespoints lw 1.5, \
#      "~/research/ajil/evaluation/staleness/c.005/data.csv" using \
#          1:(100*($3-$4)/$3) every 10 title "C = .005" with linespoints lw 1.5, \
#      "~/research/ajil/evaluation/staleness/c.001/data.csv" using \
#          1:(100*($3-$4)/$3) every 10 title "C = .001" with linespoints lw 1.5
#
#set yr[.9:1.05]
#set xr[700:720]
#set output "utility.png"
#plot "~/research/ajil/evaluation/staleness/c.01/data.csv" using \
#         1:6 every 1 title "C = .01" with linespoints lw 1.5, \
#      "~/research/ajil/evaluation/staleness/c.001/data.csv" using \
#          1:6 every 1 title "C = .001" with linespoints lw 1.5
#
#
#
##set size 1,1
##set origin 1,1
#set output "groups-fairness.eps"
#set title  "Groups Utility"
#set ylabel "Utility %"
#set yr[0:1.5]
#
#plot "data.csv" using 1:6:7 every 10 title "Avg Group Utility" with yerrorbars lw 1.5,\
#      "data.csv" using 1:6  every 10 title "" with line lw 1.5,\
#      "data.csv" using 1:7  every 10 title "Standard Deviation" with boxes
#
#
##set size 1,1
##set origin 1,0
#set output "channel-fairness.eps"
#set title  "Channels Utility"
#set ylabel "Utility %"
#set yr[0:1.5]
#
#plot "channel_data.csv" using 1:2:3 every 10 title "Avg Channel Utility" with yerrorbars lw 1.5,\
#      "channel_data.csv" using 1:2  every 10 title "" with line lw 1.5,\
#      "channel_data.csv" using 1:3  every 10 title "Standard Deviation" with boxes
