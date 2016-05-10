set term png
#set term post eps enhanced

set size 0.65, 0.65
set xtic auto
set ytic auto
set pointsize 1.5

set style line 1 lt 9 lw 2.5
set style fill solid 0.5 border

set output "ajil-lib.png"
set title "Multicast Traffic with Ajil Rate-Limiting"
set xlabel "Time (s)"
set ylabel "Traffic"
set yr[0:20]
set xr[0:40]

plot "out.csv" using 1:2 every 1 title "IPMC no AJIL" with line lw 1.5, \
      "out.csv" using 1:3 every 1 title "AJIL" with line lw 1.5, \
      9 title "Desired Limit" with l ls 1
