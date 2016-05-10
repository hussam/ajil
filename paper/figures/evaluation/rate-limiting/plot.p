#set term png
set term post eps enhanced

set size 0.65, 0.65
set xtic auto
set ytic auto
set pointsize 1.5

set style line 1 lt 9 lw 2.5
set style fill solid 0.5 border

set output "ajil-peek.eps"
set title "Multicast Traffic with Ajil Rate-Limiting"
set xlabel "Time (s)"
set ylabel "Traffic (KB)"
set yr[0:1200]
set xr[0:1000]

plot "peek/data.csv" using 1:2 every 1 title "IPMC no AJIL" with line lw 1.5, \
      "peek/data.csv" using 1:3 every 1 title "AJIL" with line lw 1.5, \
      "peek/data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1, \
      "peek/data.csv" using 1:5 every 1 title "Broadcast Traffic" with boxes

set output "ajil-square.eps"
plot "square/data.csv" using 1:2 every 1 title "IPMC no AJIL" with line lw 1.5, \
      "square/data.csv" using 1:3 every 1 title "AJIL" with line lw 1.5, \
      "square/data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1, \
      "square/data.csv" using 1:5 every 1 title "Broadcast Traffic" with boxes

set yr[0:3000]
set output "ajil-linear.eps"
plot "linear/data.csv" using 1:2 every 1 title "IPMC no AJIL" with line lw 1.5, \
      "linear/data.csv" using 1:3 every 1 title "AJIL" with line lw 1.5, \
      "linear/data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1, \
      "linear/data.csv" using 1:5 every 1 title "Broadcast Traffic" with boxes
