set term png
set term post eps enhanced

set size 0.65, 0.65
set xtic auto
set ytic auto
set pointsize 1.5

set style line 1 lt 9 lw 2.5
set style fill solid 0.5 border

set output "ajil.eps"
set title "Multicast Traffic with Varied Reaction Domain Sizes"
set xlabel "Time"
set ylabel "Traffic (KB/s)"
set yr[200:1200]
set xr[0:1000]

plot "1/data.csv" using 1:2 every 1 title "IPMC without AJIL" with line lw 1.5, \
      "1/data.csv"    using 1:3 every 1 title "AJIL, Alpha=1, Beta=1" with line lw 1.5, \
      "0.5/data.csv"  using 1:3 every 1 title "AJIL, Alpha=1, Beta=0.5" with line lw 1.5, \
      "0.25/data.csv" using 1:3 every 1 title "AJIL, Alpha=0.5, Beta=0.5" with line lw 1.5, \
      "1/data.csv" using 1:4 every 1 title "Desired Limit" with l ls 1

set output "reaction-waves.eps"
set title "Traffic Percentage Compared to Limit During Spike"
set xlabel "Time (s)"
set ylabel "Traffic to Limit %"
set yr[-15:25]
set xr[350:700]

plot "1/data.csv" using 1:(100*($3-$4)/$3) every 8 title "RD=100%" with \
         boxes fs solid 0 lw 1.5,\
      "0.5/data.csv" using 1:(100*($3-$4)/$3) every 8 title "RD=50%" with \
         boxes fs pattern 1 lw 1.5,\
      "0.25/data.csv" using 1:(100*($3-$4)/$3) every 8 title "RD=25%" with \
         boxes fs pattern 2 lw 1.5,\
      "0.125/data.csv" using 1:(100*($3-$4)/$3) every 8 title "RD=12.5%" with \
         boxes fs solid 0.5 lw 1.5
#      "0.625/data.csv" using 1:(100*($3-$4)/$3) every 8 title "RD=6.25%" with \
#         boxes fs solid 1 lw 1.5
