#set term png
set term post eps enhanced

set size 0.65, 0.65
set xtic auto
set ytic auto
set pointsize 1.5

set style line 1 lt 9 lw 2.5
set style fill solid 0.5 border

set output "ajil.eps"
set title "Multicast Traffic with Ajil Rate-Limiting"
set xlabel "Time (s)"
set ylabel "Traffic (KB/s)"
set yr[200:1200]
set xr[0:1000]

plot "rd1/data.csv" using 1:2 every 1 title "IPMC no AJIL" with line lw 1.5, \
      "rd1/data.csv" using 1:3 every 1 title "AJIL, RD=12.5%" with line lw 1.5, \
      "rd1/data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1, \
      "rd1/data.csv" using 1:5 every 1 title "Broadcast Traffic" with boxes


set output "utility-rd1.eps"
set title  "Average Utility per Group/Channel with RD = 100%"
set xlabel "Time (s)"
set ylabel "Utility"
set yr[0:1.5]
set xr[0:1000]

plot "rd1/channel_data.csv" using 1:2:3 every 50 title "Avg Channel Utility" with yerrorbars lw 1.5,\
      "rd1/channel_data.csv" using 1:2  every 50 title "" with line lw 1.5,\
      "rd1/data.csv" using 1:6:7 every 50 title "Avg Group Utility" with yerrorbars lw 1.5,\
      "rd1/data.csv" using 1:6  every 50 title "" with line lw 1.5,\
      "rd1/channel_data.csv" using 1:3  every 10 title "Channel Standard Deviation" with\
         boxes fs pattern 1 lw 1.5,\
      "rd1/data.csv" using 1:7  every 10 title "Group Standard Deviation" with \
         boxes fs solid 0.5 lw 1.5

set output "utility-rd.125.eps"
set title  "Average Utility per Group/Channel with RD = 12.5%"
set xlabel "Time (s)"
set ylabel "Utility"
set yr[0:1.5]
set xr[0:1000]

plot "rd.125/channel_data.csv" using 1:2:3 every 50 title "Avg Channel Utility" with yerrorbars lw 1.5,\
      "rd.125/channel_data.csv" using 1:2  every 50 title "" with line lw 1.5,\
      "rd.125/data.csv" using 1:6:7 every 50 title "Avg Group Utility" with yerrorbars lw 1.5,\
      "rd.125/data.csv" using 1:6  every 50 title "" with line lw 1.5,\
      "rd.125/channel_data.csv" using 1:3  every 10 title "Channel Standard Deviation" with\
         boxes fs pattern 1 lw 1.5,\
      "rd.125/data.csv" using 1:7  every 10 title "Group Standard Deviation" with \
         boxes fs solid 0.5 lw 1.5
