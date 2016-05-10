#set term png
set term post eps enhanced

set size 0.65, 0.65
set xtic auto
set ytic auto
set pointsize 1.5

set style line 1 lt 9 lw 2.5
set style fill solid 0.5 border

set output "ajil.eps"
set title "Multicast Traffic with Varied Monitor Staleness"
set xlabel "Time (s)"
set ylabel "Traffic (KB/s)"
set yr[200:1200]
set xr[0:1000]

plot "c.01/data.csv" using 1:2 every 1 title "IPMC without AJIL" with line lw 1.5, \
      "c.01/data.csv" using 1:3 every 1 title "AJIL, C=0.01" with line lw 1.5, \
      "c.001/data.csv" using 1:3 every 1 title "AJIL, C=0.001" with line lw 1.5, \
      "c.01/data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1

set output "excess-during.eps"
set title "Violation Percentage During Spike"
set xlabel "Time (s)"
set ylabel "Violation %"
set yr[0:30]
set xr[300:750]

plot "c.001/data.csv" using 1:(100*($3-$4)/$3) every 10 title "C = .001" with \
         boxes fs pattern 1 lw 1.5,\
      "c.01/data.csv" using 1:(100*($3-$4)/$3) every 10 title "C = .01" with \
         boxes fs solid 0.5 lw 1.5

set output "utility-after.eps"
set title "Utility Percentage After Spike"
set xlabel "Time (s)"
set ylabel "Utility %"
set yr[90:105]
set xr[700:720]
plot "c.01/data.csv" using 1:($6*100) every 1 title "C = .01" with linespoints lw 1.5, \
      "c.001/data.csv" using 1:($6*100) every 1 title "C = .001" with linespoints lw 1.5
